package com.arn.scrobble.api.lastfm

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getPageResult
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.lastfm.LastFm.Companion.toFormParametersWithSig
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.SeenTrackAlbumAssociation
import com.arn.scrobble.utils.Stuff
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import kotlinx.coroutines.launch

class LastFmUnauthedRequester {

    val apiKey = Stuff.xorWithKey(
        BuildKonfig.LASTFM_KEY,
        BuildKonfig.APP_ID
    )
    val apiSecret = Stuff.xorWithKey(
        BuildKonfig.LASTFM_SECRET,
        BuildKonfig.APP_ID
    )
    private val client get() = Requesters.genericKtorClient

    private val outerScope = Stuff.appScope

    // search
    suspend fun search(
        term: String,
        limitEach: Int? = null,
    ): Result<SearchResults> {
        val request = HttpRequestBuilder().apply {
            this.url(Stuff.LASTFM_API_ROOT)
            parameter("limit", limitEach)
            parameter("format", "json")
            parameter("api_key", apiKey)
        }

        // search API sometimes returns duplicates
        val artists = client.getResult<ArtistSearchResponse>(Stuff.LASTFM_API_ROOT) {
            takeFrom(request)
            parameter("method", "artist.search")
            parameter("artist", term)
        }.map { it.results.artistmatches.entries }

        val albums = client.getResult<AlbumSearchResponse>(Stuff.LASTFM_API_ROOT) {
            takeFrom(request)
            parameter("method", "album.search")
            parameter("album", term)
        }.map { it.results.albummatches.entries }

        val tracks = client.getResult<TrackSearchResponse>(Stuff.LASTFM_API_ROOT) {
            takeFrom(request)
            parameter("method", "track.search")
            parameter("track", term)
        }.map { it.results.trackmatches.entries }

        if (artists.isFailure || albums.isFailure || tracks.isFailure)
            return Result.failure(
                artists.exceptionOrNull()
                    ?: albums.exceptionOrNull()
                    ?: tracks.exceptionOrNull()!!
            )

        val sr = SearchResults(
            term,
            lovedTracks = listOf(),
            tracks = tracks.getOrDefault(listOf()),
            artists = artists.getOrDefault(listOf()),
            albums = albums.getOrDefault(listOf()),
        )
        
        return Result.success(sr)
    }


    suspend fun getTrackInfo2(
        musicEntry: Track,
    ) = getTrackInfo(musicEntry, null)

    suspend fun getTrackInfo(
        musicEntry: Track,
        username: String? = null,
    ) = client.getResult<TrackInfoResponse> {
        url(Stuff.LASTFM_API_ROOT)
        parameter("username", username)
        parameter("format", "json")
        parameter("api_key", apiKey)
        parameter("method", "track.getInfo")
        doubleEncodePlusParam("artist", musicEntry.artist.name)
        doubleEncodePlusParam("track", musicEntry.name)
    }.map {
        // run cache hook
        outerScope.launch {
            if (it.track.album != null) {
                PanoDb.db.getSeenEntitiesDao().saveRecentTrack(
                    artist = it.track.artist.name,
                    track = it.track.name,
                    albumArtist = it.track.album.artist?.name ?: it.track.artist.name,
                    album = it.track.album.name,
                    artUrl = it.track.album.webp300,
                    isLoved = it.track.userloved,
                    priority = SeenTrackAlbumAssociation.Priority.TRACK_INFO,
                )
            } else {
                PanoDb.db.getSeenEntitiesDao().markTrackInfoFetched(
                    artist = it.track.artist.name,
                    track = it.track.name,
                )
            }
        }

        // fix duration returned in millis
        (it.track.copy(duration = it.track.duration?.div(1000)))
    }

    suspend fun getAlbumInfo(
        musicEntry: Album,
        username: String? = null,
    ) = client.getResult<AlbumInfoResponse> {
        url(Stuff.LASTFM_API_ROOT)
        parameter("username", username)
        parameter("format", "json")
        parameter("api_key", apiKey)
        // this does not have double encoding bug
        parameter("method", "album.getInfo")
        parameter("artist", musicEntry.artist!!.name)
        parameter("album", musicEntry.name)
    }.map {
        // run cache hook
        outerScope.launch {
            val trackList = it.album.tracks?.track ?: emptyList()
            if (trackList.isNotEmpty()) {
                trackList.forEach { track ->
                    PanoDb.db.getSeenEntitiesDao().saveTrackAlbumAssociation(
                        artist = track.artist.name,
                        track = track.name,
                        albumArtist = it.album.artist?.name ?: track.artist.name,
                        album = it.album.name,
                        artUrl = it.album.webp300,
                        priority = SeenTrackAlbumAssociation.Priority.ALBUM_INFO,
                    )
                }
            } else if (it.album.webp300 != null) {
                PanoDb.db.getSeenEntitiesDao().saveAlbumArtIfMissing(
                    artist = it.album.artist?.name!!,
                    album = it.album.name,
                    artUrl = it.album.webp300!!,
                )
            }
        }

        it.album
    }

    suspend fun getArtistInfo(
        musicEntry: Artist,
        username: String? = null,
    ) = client.getResult<ArtistInfoResponse> {
        url(Stuff.LASTFM_API_ROOT)
        parameter("username", username)
        parameter("format", "json")
        parameter("api_key", apiKey)
        parameter("method", "artist.getInfo")
        doubleEncodePlusParam("artist", musicEntry.name)
    }.map { it.artist }

    suspend fun <T : MusicEntry> getTopTags(
        musicEntry: T,
    ): Result<TagsResponse> {
        val reqBuilder = HttpRequestBuilder().apply {
            url(Stuff.LASTFM_API_ROOT)
            parameter("format", "json")
            parameter("api_key", apiKey)
        }

        return when (musicEntry) {
            is Artist -> client.getResult<TagsResponse> {
                takeFrom(reqBuilder)
                parameter("method", "artist.getTopTags")
                doubleEncodePlusParam("artist", musicEntry.name)
            }

            is Album -> client.getResult<TagsResponse> {
                takeFrom(reqBuilder)
                parameter("method", "album.getTopTags")
                doubleEncodePlusParam("artist", musicEntry.artist!!.name)
                doubleEncodePlusParam("album", musicEntry.name)
            }

            is Track -> client.getResult<TagsResponse> {
                takeFrom(reqBuilder)
                parameter("method", "track.getTopTags")
                doubleEncodePlusParam("artist", musicEntry.artist.name)
                doubleEncodePlusParam("track", musicEntry.name)
            }
        }
    }

    suspend fun artistGetTopTracks(
        artist: Artist,
        limit: Int? = null,
        page: Int? = null,
    ) = client.getPageResult<TopTracksResponse, Track>(
        Stuff.LASTFM_API_ROOT,
        { it.toptracks }
    ) {
        parameter("method", "artist.getTopTracks")
        doubleEncodePlusParam("artist", artist.name)
        parameter("limit", limit)
        parameter("page", page)
        parameter("format", "json")
        parameter("api_key", apiKey)
    }

    suspend fun artistGetTopAlbums(
        artist: Artist,
        limit: Int? = null,
        page: Int? = null,
    ) = client.getPageResult<TopAlbumsResponse, Album>(
        Stuff.LASTFM_API_ROOT,
        { it.topalbums }
    ) {
        parameter("method", "artist.getTopAlbums")
        doubleEncodePlusParam("artist", artist.name)
        parameter("limit", limit)
        parameter("page", page)
        parameter("format", "json")
        parameter("api_key", apiKey)
    }

    suspend fun artistGetSimilar(
        artist: Artist,
        limit: Int? = null,
    ) = client.getResult<SimilarArtistsResponse> {
        url(Stuff.LASTFM_API_ROOT)
        parameter("method", "artist.getSimilar")
        doubleEncodePlusParam("artist", artist.name)
        parameter("limit", limit)
        parameter("format", "json")
        parameter("api_key", apiKey)
    }.map { it.similarartists.artist }


    suspend fun trackGetSimilar(
        track: Track,
        limit: Int? = null,
    ) = client.getResult<SimilarTracksResponse> {
        url(Stuff.LASTFM_API_ROOT)
        parameter("method", "track.getSimilar")
        doubleEncodePlusParam("artist", track.artist.name)
        doubleEncodePlusParam("track", track.name)
        parameter("limit", limit)
        parameter("format", "json")
        parameter("api_key", apiKey)
    }.map { it.similartracks.track }

    // tag
    suspend fun tagGetInfo(tag: String) =
        client.getResult<TagGetInfoResponse> {
            url(Stuff.LASTFM_API_ROOT)
            parameter("method", "tag.getInfo")
            parameter("tag", tag)
            parameter("format", "json")
            parameter("api_key", apiKey)
        }.map { it.tag }

    suspend fun getToken(
        apiRoot: String,
        apiKey: String,
        apiSecret: String,
    ): Result<TokenResponse> {
        val params = mutableMapOf<String, String?>(
            "method" to "auth.getToken",
            "api_key" to apiKey,
        )

        return client.postResult<TokenResponse>(apiRoot) {
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
        }
    }

    suspend fun getMobileSession(
        apiRoot: String,
        apiKey: String,
        apiSecret: String,
        username: String,
        password: String,
    ): Result<Session> {
        val params = mutableMapOf<String, String?>(
            "method" to "auth.getMobileSession",
            "api_key" to apiKey,
            "username" to username,
            "password" to password,
        )

        return client.postResult<SessionResponse>(apiRoot) {
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
        }.map { it.session }
    }

    suspend fun getSession(
        apiRoot: String,
        apiKey: String,
        apiSecret: String,
        token: String,
    ): Result<Session> {
        val params = mutableMapOf<String, String?>(
            "method" to "auth.getSession",
            "api_key" to apiKey,
            "token" to token,
        )

        return client.postResult<SessionResponse>(apiRoot) {
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
        }.map { it.session }
    }

    // lfm double encoding bug
    private fun HttpRequestBuilder.doubleEncodePlusParam(
        key: String,
        value: String?
    ) {
        parameter(key, value?.replace("+", "%2B"))
    }
}