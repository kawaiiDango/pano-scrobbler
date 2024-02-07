package com.arn.scrobble.api.spotify

import com.arn.scrobble.App
import com.arn.scrobble.Tokens
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.lastfm.CacheInterceptor
import com.arn.scrobble.api.lastfm.ExpirationPolicy
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.similarity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.parametersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SpotifyRequester {
    private val prefs = App.prefs
    private val authMutex by lazy { Mutex() }
    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            expectSuccess = true

            engine {
                config {
                    followRedirects(true)
                    cache(okhttp3.Cache(File(App.context.cacheDir, "ktor"), 10 * 1024 * 1024))
                }
                addNetworkInterceptor(CacheInterceptor(SpotifyCacheExpirationPolicy()))
            }

            install(ContentNegotiation) {
                json(Stuff.myJson)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        if (prefs.spotifyAccessTokenExpires >= System.currentTimeMillis())
                            BearerTokens(prefs.spotifyAccessToken, Tokens.SPOTIFY_REFRESH_TOKEN)
                        else
                            null
                    }

                    refreshTokens {
                        authMutex.withLock {
                            if (prefs.spotifyAccessTokenExpires >= System.currentTimeMillis())
                                return@withLock

                            withContext(Dispatchers.IO) {
                                client.post("https://accounts.spotify.com/api/token") {
                                    markAsRefreshTokenRequest()
                                    header(
                                        HttpHeaders.Authorization,
                                        "Basic ${Tokens.SPOTIFY_REFRESH_TOKEN}"
                                    )
                                    setBody(
                                        FormDataContent(
                                            parametersOf("grant_type", "client_credentials")
                                        )
                                    )
                                }
                                    .body<SpotifyTokenResponse>()
                                    .let {
                                        prefs.spotifyAccessToken = it.access_token
                                        prefs.spotifyAccessTokenExpires =
                                            System.currentTimeMillis() + (it.expires_in - 60) * 1000
                                    }
                            }
                        }
                        BearerTokens(prefs.spotifyAccessToken, Tokens.SPOTIFY_REFRESH_TOKEN)
                    }
                }
            }
        }
    }

    suspend fun getSpotifyArtistImage(artist: String): String? {
//        val imagesMap = mutableMapOf<ImageSize, String>()

        val response = withContext(Dispatchers.IO) {
            client.get("https://api.spotify.com/v1/search") {
                parameter("q", artist)
                parameter("type", "artist")
                parameter("limit", 1)
            }
        }

        val jsonObject = JSONObject(response.bodyAsText())
        val artists = jsonObject.getJSONObject("artists")
        if (artists.getInt("total") == 0)
            return null

        val artistItem = artists.getJSONArray("items").getJSONObject(0)
        val artistItemName = artistItem.getString("name")

        if (!artistItemName.equals(artist, ignoreCase = true))
            return null

        val images = artistItem.getJSONArray("images")
        val imagesLength = images.length()

        if (imagesLength == 0)
            return null

//        val mediumImage = images.getJSONObject(imagesLength - 1).getString("url")
        val largeImage = images.getJSONObject(imagesLength - 2).getString("url")
//        val hugeImage = images.getJSONObject(0).getString("url")

//        imagesMap[ImageSize.SMALL] = mediumImage
//        imagesMap[ImageSize.MEDIUM] = mediumImage
//        imagesMap[ImageSize.LARGE] = mediumImage
//        imagesMap[ImageSize.EXTRALARGE] = largeImage

        return largeImage
    }


    suspend fun getSpotifyTrack(track: Track, similarityThreshold: Float = 1f): SpotifyTrack? {
        val response = withContext(Dispatchers.IO) {
            client.get("https://api.spotify.com/v1/search") {
                parameter("q", "${track.artist.name} ${track.name}")
                parameter("type", "track")
                parameter("limit", 1)
            }
        }

        val jsonObject = JSONObject(response.bodyAsText())
        val tracks = jsonObject.getJSONObject("tracks")
        if (tracks.getInt("total") == 0)
            return null

        val trackItem = tracks.getJSONArray("items").getJSONObject(0)
        val trackItemName = trackItem.getString("name")
        val albumItem = trackItem.getJSONObject("album")
        val albumItemName = albumItem.getString("name")
        val artistItem = trackItem.getJSONArray("artists").getJSONObject(0)
        val artistItemName = artistItem.getString("name")
        val id = trackItem.getString("id")
        val popularity = trackItem.getInt("popularity")
        val releaseDate = albumItem.getString("release_date")
        val releaseDatePrecision = albumItem.getString("release_date_precision")
        val durationMs = trackItem.getLong("duration_ms")

        if (artistItemName.similarity(track.artist.name) >= similarityThreshold &&
            trackItemName.similarity(track.name) >= similarityThreshold
        )
            return SpotifyTrack(
                trackItemName,
                albumItemName,
                artistItemName,
                id,
                durationMs,
                popularity,
                releaseDate,
                releaseDatePrecision,
            )
        return null
    }

    suspend fun getTrackFeatures(trackId: String): Result<TrackFeatures> =
        client.getResult("https://api.spotify.com/v1/audio-features/$trackId")
}

data class SpotifyTrack(
    val track: String,
    val album: String,
    val artist: String,
    val id: String,
    val durationMs: Long,
    val popularity: Int,
    val releaseDate: String,
    val releaseDatePrecision: String,
    val features: TrackFeatures? = null,
) {
    fun getReleaseDateDate(): Date? {
        val sdf = when (releaseDatePrecision) {
            "year" -> SimpleDateFormat("yyyy", Locale.getDefault())
            "month" -> SimpleDateFormat("yyyy-MM", Locale.getDefault())
            "day" -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            else -> return null
        }
        return sdf.parse(releaseDate)
    }
}

@Serializable
data class SpotifyTokenResponse(
    val access_token: String,
    val expires_in: Int,
)

@Serializable
data class TrackFeatures(
    val acousticness: Float,
    val danceability: Float,
    val duration_ms: Int,
    val energy: Float,
    val id: String,
    val instrumentalness: Float,
    val key: Int,
    val liveness: Float,
    val loudness: Float,
    val mode: Int,
    val speechiness: Float,
    val tempo: Float,
    val time_signature: Int,
    val valence: Float,
) {
    fun getKeyString(): String? {
        var scale = when (key) {
            0 -> "C"
            1 -> "C♯"
            2 -> "D"
            3 -> "D♯"
            4 -> "E"
            5 -> "F"
            6 -> "F♯"
            7 -> "G"
            8 -> "G♯"
            9 -> "A"
            10 -> "A♯"
            11 -> "B"
            else -> null
        }

        if (scale != null)
            scale += (if (mode == 1) "" else "m")
        return scale
    }
}

class SpotifyCacheExpirationPolicy : ExpirationPolicy {

    private val ONE_WEEK = TimeUnit.DAYS.toSeconds(7).toInt()
    private val ONE_YEAR = TimeUnit.DAYS.toSeconds(365).toInt()

    override fun getExpirationTimeSecs(url: HttpUrl) = when {
        url.pathSegments.last() == "search" -> ONE_WEEK
        url.pathSegments[url.pathSegments.size - 2] == "audio-features" -> ONE_YEAR
        else -> -1
    }

}