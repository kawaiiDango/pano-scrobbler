package com.arn.scrobble.api.lastfm

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getPageResult
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.Scrobblables
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

    private fun HttpRequestBuilder.commonReq() {
        url(Stuff.LASTFM_API_ROOT)
        parameter("format", "json")
        parameter("api_key", apiKey)
    }

    // search
    suspend fun search(
        term: String,
        limitEach: Int? = null,
    ): Result<SearchResults> {
        // search API sometimes returns duplicates
        val artists = client.getResult<ArtistSearchResponse>(Stuff.LASTFM_API_ROOT) {
            parameter("method", "artist.search")
            parameter("artist", term)
            parameter("limit", limitEach)
            commonReq()
        }.map { it.results.artistmatches.entries }

        val albums = client.getResult<AlbumSearchResponse>(Stuff.LASTFM_API_ROOT) {
            parameter("method", "album.search")
            parameter("album", term)
            parameter("limit", limitEach)
            commonReq()
        }.map { it.results.albummatches.entries }

        val tracks = client.getResult<TrackSearchResponse>(Stuff.LASTFM_API_ROOT) {
            parameter("method", "track.search")
            parameter("track", term)
            parameter("limit", limitEach)
            commonReq()
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
        // fix duration returned in millis
        val track = it.track.copy(duration = it.track.duration?.div(1000))

        Logger.d { "getTrackInfo for ${musicEntry.artist.name} - ${musicEntry.name}" }

        // run cache hook
        outerScope.launch {
            val isSelf =
                Scrobblables.current?.userAccount?.user?.let { it.name == username } == true

            PanoDb.db.getSeenEntitiesDao().saveRecentTracks(
                listOf(track),
                mayHaveAlbumArt = true,
                savedLoved = isSelf,
                priority = SeenTrackAlbumAssociation.Priority.TRACK_INFO,
            )
        }

        track
    }

    suspend fun getAlbumInfo(
        musicEntry: Album,
        username: String? = null,
    ) = client.getResult<AlbumInfoResponse> {
        parameter("method", "album.getInfo")
        parameter("username", username)
        // this does not have double encoding bug
        parameter("artist", musicEntry.artist!!.name)
        parameter("album", musicEntry.name)
        commonReq()
    }.map {
        Logger.d { "getAlbumInfo for ${musicEntry.artist?.name} - ${musicEntry.name}" }

        // run cache hook
        outerScope.launch {
            PanoDb.db.getSeenEntitiesDao().saveAlbumInfoTracklist(it.album)
        }

        it.album
    }

    suspend fun getArtistInfo(
        musicEntry: Artist,
        username: String? = null,
    ) = client.getResult<ArtistInfoResponse> {
        parameter("username", username)
        parameter("method", "artist.getInfo")
        doubleEncodePlusParam("artist", musicEntry.name)
        commonReq()
    }.map {
        Logger.d { "getArtistInfo for ${musicEntry.name}" }
        it.artist
    }

    suspend fun <T : MusicEntry> getTopTags(
        musicEntry: T,
    ): Result<TagsResponse> {
        return client.getResult<TagsResponse> {
            when (musicEntry) {
                is Artist -> {
                    parameter("method", "artist.getTopTags")
                    doubleEncodePlusParam("artist", musicEntry.name)
                }

                is Album -> {
                    parameter("method", "album.getTopTags")
                    doubleEncodePlusParam("artist", musicEntry.artist!!.name)
                    doubleEncodePlusParam("album", musicEntry.name)
                }

                is Track -> {
                    parameter("method", "track.getTopTags")
                    doubleEncodePlusParam("artist", musicEntry.artist.name)
                    doubleEncodePlusParam("track", musicEntry.name)
                }
            }
            commonReq()
        }
    }

    suspend fun artistGetTopTracks(
        artist: Artist,
        limit: Int? = null,
        page: Int? = null,
    ) = client.getPageResult<TopTracksResponse, Track>(
        transform = { it.toptracks }
    ) {
        parameter("method", "artist.getTopTracks")
        doubleEncodePlusParam("artist", artist.name)
        parameter("limit", limit)
        parameter("page", page)
        commonReq()
    }

    suspend fun artistGetTopAlbums(
        artist: Artist,
        limit: Int? = null,
        page: Int? = null,
    ) = client.getPageResult<TopAlbumsResponse, Album>(
        transform = { it.topalbums }
    ) {
        parameter("method", "artist.getTopAlbums")
        doubleEncodePlusParam("artist", artist.name)
        parameter("limit", limit)
        parameter("page", page)
        commonReq()
    }

    suspend fun artistGetSimilar(
        artist: Artist,
        limit: Int? = null,
    ) = client.getResult<SimilarArtistsResponse> {
        parameter("method", "artist.getSimilar")
        doubleEncodePlusParam("artist", artist.name)
        parameter("limit", limit)
        commonReq()
    }.map { it.similarartists.artist }


    suspend fun trackGetSimilar(
        track: Track,
        limit: Int? = null,
    ) = client.getResult<SimilarTracksResponse> {
        parameter("method", "track.getSimilar")
        doubleEncodePlusParam("artist", track.artist.name)
        doubleEncodePlusParam("track", track.name)
        parameter("limit", limit)
        commonReq()
    }.map { it.similartracks.track }

    // tag
    suspend fun tagGetInfo(tag: String) =
        client.getResult<TagGetInfoResponse> {
            parameter("method", "tag.getInfo")
            parameter("tag", tag)
            commonReq()
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