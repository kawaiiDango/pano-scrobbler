package com.arn.scrobble.api.lastfm

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getPageResult
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.lastfm.LastFm.Companion.toFormParametersWithSig
import com.arn.scrobble.utils.Stuff
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

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

    // search
    suspend fun search(
        term: String,
        limitEach: Int? = null,
    ): Result<SearchResults> = supervisorScope {
        val request = HttpRequestBuilder().apply {
            this.url(Stuff.LASTFM_API_ROOT)
            parameter("limit", limitEach)
            parameter("format", "json")
            parameter("api_key", apiKey)
        }

        // search API sometimes returns duplicates
        val artists = async {
            client.getResult<ArtistSearchResponse>(Stuff.LASTFM_API_ROOT) {
                takeFrom(request)
                parameter("method", "artist.search")
                parameter("artist", term)
            }.map { it.results.artistmatches.entries }
        }.await()

        val albums = async {
            client.getResult<AlbumSearchResponse>(Stuff.LASTFM_API_ROOT) {
                takeFrom(request)
                parameter("method", "album.search")
                parameter("album", term)
            }.map { it.results.albummatches.entries }
        }.await()

        val tracks = async {
            client.getResult<TrackSearchResponse>(Stuff.LASTFM_API_ROOT) {
                takeFrom(request)
                parameter("method", "track.search")
                parameter("track", term)
            }.map { it.results.trackmatches.entries }
        }.await()

        if (artists.isFailure || albums.isFailure || tracks.isFailure)
            return@supervisorScope Result.failure(
                artists.exceptionOrNull()
                    ?: albums.exceptionOrNull()
                    ?: tracks.exceptionOrNull()!!
            )

        val sr = SearchResults(
            term,
            searchType = SearchType.GLOBAL,
            lovedTracks = listOf(),
            tracks = tracks.getOrDefault(listOf()),
            artists = artists.getOrDefault(listOf()),
            albums = albums.getOrDefault(listOf()),
        )
        Result.success(sr)
    }


    suspend fun <T : MusicEntry> getInfo(
        musicEntry: T,
        username: String? = null,
        autocorrect: Boolean = true,
    ): Result<T> {
        val reqBuilder = HttpRequestBuilder().apply {
            url(Stuff.LASTFM_API_ROOT)
            parameter("autocorrect", if (autocorrect) 1 else 0)
            parameter("username", username)
            parameter("format", "json")
            parameter("api_key", apiKey)
        }

        return when (musicEntry) {
            is Artist -> client.getResult<ArtistInfoResponse> {
                takeFrom(reqBuilder)
                parameter("method", "artist.getInfo")
                doubleEncodePlusParam("artist", musicEntry.name)
            }.map { it.artist as T }

            is Album -> client.getResult<AlbumInfoResponse> {
                takeFrom(reqBuilder)
                parameter("method", "album.getInfo")
                doubleEncodePlusParam("artist", musicEntry.artist!!.name)
                doubleEncodePlusParam("album", musicEntry.name)
            }.map { it.album as T }

            is Track -> client.getResult<TrackInfoResponse> {
                takeFrom(reqBuilder)
                parameter("method", "track.getInfo")
                doubleEncodePlusParam("artist", musicEntry.artist.name)
                doubleEncodePlusParam("track", musicEntry.name)
            }.map {
                // fix duration returned in millis
                (it.track.copy(duration = it.track.duration?.div(1000)) as T)
            }
        }
    }

    suspend fun <T : MusicEntry> getTopTags(
        musicEntry: T,
        autocorrect: Boolean = true,
    ): Result<TagsResponse> {
        val reqBuilder = HttpRequestBuilder().apply {
            url(Stuff.LASTFM_API_ROOT)
            parameter("autocorrect", if (autocorrect) 1 else 0)
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
        autocorrect: Boolean = true,
        limit: Int? = null,
        page: Int? = null,
    ) = client.getPageResult<TopTracksResponse, Track>(
        Stuff.LASTFM_API_ROOT,
        { it.toptracks }
    ) {
        parameter("method", "artist.getTopTracks")
        doubleEncodePlusParam("artist", artist.name)
        parameter("autocorrect", if (autocorrect) 1 else 0)
        parameter("limit", limit)
        parameter("page", page)
        parameter("format", "json")
        parameter("api_key", apiKey)
    }

    suspend fun artistGetTopAlbums(
        artist: Artist,
        autocorrect: Boolean = true,
        limit: Int? = null,
        page: Int? = null,
    ) = client.getPageResult<TopAlbumsResponse, Album>(
        Stuff.LASTFM_API_ROOT,
        { it.topalbums }
    ) {
        parameter("method", "artist.getTopAlbums")
        doubleEncodePlusParam("artist", artist.name)
        parameter("autocorrect", if (autocorrect) 1 else 0)
        parameter("limit", limit)
        parameter("page", page)
        parameter("format", "json")
        parameter("api_key", apiKey)
    }

    suspend fun artistGetSimilar(
        artist: Artist,
        autocorrect: Boolean = true,
        limit: Int? = null,
    ) = client.getResult<SimilarArtistsResponse> {
        url(Stuff.LASTFM_API_ROOT)
        parameter("method", "artist.getSimilar")
        doubleEncodePlusParam("artist", artist.name)
        parameter("autocorrect", if (autocorrect) 1 else 0)
        parameter("limit", limit)
        parameter("format", "json")
        parameter("api_key", apiKey)
    }.map { it.similarartists.artist }


    suspend fun trackGetSimilar(
        track: Track,
        autocorrect: Boolean = true,
        limit: Int? = null,
    ) = client.getResult<SimilarTracksResponse> {
        url(Stuff.LASTFM_API_ROOT)
        parameter("method", "track.getSimilar")
        doubleEncodePlusParam("artist", track.artist.name)
        doubleEncodePlusParam("track", track.name)
        parameter("autocorrect", if (autocorrect) 1 else 0)
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