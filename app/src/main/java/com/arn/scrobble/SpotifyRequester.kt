package com.arn.scrobble

import com.arn.scrobble.Stuff.similarity
import com.arn.scrobble.pref.MainPrefs
import de.umass.lastfm.CacheInterceptor
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import de.umass.lastfm.cache.ExpirationPolicy
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.set
import kotlin.coroutines.cancellation.CancellationException

object SpotifyRequester {
    private val prefs by lazy { MainPrefs(App.context) }
    private val authMutex by lazy { Mutex() }
    private val jsonSerializer by lazy { Json { ignoreUnknownKeys = true } }
    private val spotifyClient: HttpClient by lazy {
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
                json(jsonSerializer)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(prefs.spotifyAccessToken, Tokens.SPOTIFY_REFRESH_TOKEN)
                    }

                    refreshTokens {
                        putTokens()
                        BearerTokens(prefs.spotifyAccessToken, Tokens.SPOTIFY_REFRESH_TOKEN)
                    }
                }
            }
        }
    }

    private suspend fun putTokens() {
        authMutex.withLock {
            if (prefs.spotifyAccessTokenExpires < System.currentTimeMillis()) { // expired
                try {
                    val request = Request(
                        "https://accounts.spotify.com/api/token".toHttpUrl(),
                        headers = Headers.headersOf(
                            "Authorization", "Basic ${Tokens.SPOTIFY_REFRESH_TOKEN}",
                            "Content-Type", "application/x-www-form-urlencoded",
                        ),
                        body = FormBody.Builder()
                            .add("grant_type", "client_credentials")
                            .build()
                    )
                    LFMRequester.okHttpClient
                        .newCall(request)
                        .execute().use { response ->
                            if (response.isSuccessful) {
                                val jsonObject = JSONObject(response.body.string())
                                prefs.spotifyAccessToken =
                                    jsonObject.getString("access_token")
                                prefs.spotifyAccessTokenExpires =
                                    System.currentTimeMillis() + (jsonObject.getLong("expires_in") - 60) * 1000
                            }
                        }
                } catch (e: Exception) {
                    Timber.tag(Stuff.TAG).w(e)
                }
            }
        }
    }

    suspend fun getSpotifyArtistImages(artist: String): Map<ImageSize, String> {
        val imagesMap = mutableMapOf<ImageSize, String>()

        val response = spotifyClient.get {
            url("https://api.spotify.com/v1/search")
            parameter("q", artist)
            parameter("type", "artist")
            parameter("limit", 1)
        }
        val jsonObject = JSONObject(response.bodyAsText())
        val artists = jsonObject.getJSONObject("artists")
        if (artists.getInt("total") == 0)
            return imagesMap

        val artistItem = artists.getJSONArray("items").getJSONObject(0)
        val artistItemName = artistItem.getString("name")

        if (!artistItemName.equals(artist, ignoreCase = true))
            return imagesMap

        val images = artistItem.getJSONArray("images")
        val imagesLength = images.length()

        if (imagesLength == 0)
            return imagesMap

        val mediumImage = images.getJSONObject(imagesLength - 1).getString("url")
        val largeImage = images.getJSONObject(imagesLength - 2).getString("url")
        val hugeImage = images.getJSONObject(0).getString("url")

        imagesMap[ImageSize.SMALL] = mediumImage
        imagesMap[ImageSize.MEDIUM] = mediumImage
        imagesMap[ImageSize.LARGE] = mediumImage
        imagesMap[ImageSize.EXTRALARGE] = largeImage
        imagesMap[ImageSize.MEGA] = hugeImage

        return imagesMap
    }


    suspend fun getSpotifyTrack(track: Track, similarityThreshold: Float = 1f): SpotifyTrack? {
        try {
            val response = spotifyClient.get {
                url("https://api.spotify.com/v1/search")
                parameter("q", "${track.artist} ${track.name}")
                parameter("type", "track")
                parameter("limit", 1)
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

            if (artistItemName.similarity(track.artist) >= similarityThreshold &&
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(Stuff.TAG).w(e)
        }

        return null
    }

    suspend fun getTrackFeatures(trackId: String): TrackFeatures? {
        try {
            val response = spotifyClient.get {
                url("https://api.spotify.com/v1/audio-features/$trackId")
            }
            return jsonSerializer.decodeFromString<TrackFeatures>(response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(Stuff.TAG).w(e)
        }
        return null
    }
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
    var features: TrackFeatures? = null,
) {
    fun getReleaseDateDate(): Date? {
        val sdf = when(releaseDatePrecision) {
            "year" -> SimpleDateFormat("yyyy", Locale.getDefault())
            "month" -> SimpleDateFormat("yyyy-MM", Locale.getDefault())
            "day" -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            else -> return null
        }
        return sdf.parse(releaseDate)
    }
}

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

    private val ONE_WEEK = 1000L * 60 * 60 * 24 * 7
    private val ONE_YEAR = 1000L * 60 * 60 * 24 * 365

    override fun getExpirationTime(url: HttpUrl) = when {
        url.pathSegments.last() == "search" -> ONE_WEEK
        url.pathSegments[url.pathSegments.size - 2] == "audio-features" -> ONE_YEAR
        else -> -1
    }

}