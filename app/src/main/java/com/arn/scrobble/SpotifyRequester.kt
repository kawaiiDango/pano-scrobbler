package com.arn.scrobble

import com.arn.scrobble.Stuff.similarity
import com.arn.scrobble.pref.MainPrefs
import de.umass.lastfm.CacheInterceptor
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import de.umass.lastfm.cache.ExpirationPolicy
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import kotlin.collections.set

object SpotifyRequester {
    private val prefs by lazy { MainPrefs(App.context) }
    private val authMutex by lazy { Mutex() }
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
                json(Json {
                    ignoreUnknownKeys = true
                })
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

        try {
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

        } catch (e: Exception) {
            Timber.tag(Stuff.TAG).w(e)
        }

        return imagesMap
    }


    suspend fun getSpotifyLink(track: Track): String? {
        val similarityThreshold = 0.7

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
            val artistItem = trackItem.getJSONArray("artists").getJSONObject(0)
            val artistItemName = artistItem.getString("name")

            if (artistItemName.similarity(track.artist) >= similarityThreshold &&
                    trackItemName.similarity(track.name) >= similarityThreshold)
                return trackItem.getJSONObject("external_urls").getString("spotify")

            return null
        } catch (e: Exception) {
            Timber.tag(Stuff.TAG).w(e)
        }

        return null
    }
}

class SpotifyCacheExpirationPolicy : ExpirationPolicy {

    private val ONE_WEEK = 1000L * 60 * 60 * 24 * 7

    private val pathsToCache = setOf(
        "search",
    )

    override fun getExpirationTime(url: HttpUrl): Long {
        if (url.pathSegments.last() in pathsToCache)
            return ONE_WEEK
        return -1
    }

}