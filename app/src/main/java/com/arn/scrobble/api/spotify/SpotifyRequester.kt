package com.arn.scrobble.api.spotify

import com.arn.scrobble.Tokens
import com.arn.scrobble.api.CustomCachePlugin
import com.arn.scrobble.api.ExpirationPolicy
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.main.App
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.parametersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

class SpotifyRequester {
    private val prefs = App.prefs
    private val authMutex by lazy { Mutex() }
    private val client: HttpClient by lazy {
        Requesters.genericKtorClient.config {
            install(CustomCachePlugin) {
                policy = SpotifyCacheExpirationPolicy()
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
            expectSuccess = true
        }
    }

    suspend fun search(query: String, type: SpotifySearchType, limit: Int = 5) =
        try {
            withContext(Dispatchers.IO) {
                client.get("https://api.spotify.com/v1/search") {
                    parameter("q", query)
                    parameter("type", type.name)
                    parameter("limit", limit)
//                    parameter("market", "US")
                }.body<SpotifySearchResponse>()
                    .let { Result.success(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun trackFeatures(trackId: String) =
        try {
            withContext(Dispatchers.IO) {
                client.get("https://api.spotify.com/v1/audio-features/$trackId")
                    .body<TrackFeatures>()
                    .let { Result.success(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun artist(artistId: String) =
        try {
            withContext(Dispatchers.IO) {
                client.get("https://api.spotify.com/v1/artists/$artistId")
                    .body<ArtistItem>()
                    .let { Result.success(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun album(albumId: String) =
        try {
            withContext(Dispatchers.IO) {
                client.get("https://api.spotify.com/v1/albums/$albumId")
                    .body<AlbumItem>()
                    .let { Result.success(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}

class SpotifyCacheExpirationPolicy : ExpirationPolicy {

    private val ONE_MONTH = TimeUnit.DAYS.toMillis(30)
    private val ONE_YEAR = TimeUnit.DAYS.toMillis(365)

    override fun getExpirationTime(url: Url) = when {
        url.pathSegments.last() == "search" -> ONE_MONTH
        url.pathSegments[url.pathSegments.size - 2] == "audio-features" -> ONE_YEAR
        url.pathSegments[url.pathSegments.size - 2] == "artists" -> ONE_MONTH // artist images can change
        url.pathSegments[url.pathSegments.size - 2] == "albums" -> ONE_YEAR // album arts usually don't change
        else -> -1
    }

}