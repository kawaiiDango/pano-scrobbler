package com.arn.scrobble.api.spotify

import com.arn.scrobble.Tokens
import com.arn.scrobble.api.CustomCachePlugin
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.parseJsonBody
import com.arn.scrobble.api.cache.ExpirationPolicy
import com.arn.scrobble.utils.PlatformStuff
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SpotifyRequester {
    private val mainPrefs = PlatformStuff.mainPrefs
    private val client: HttpClient by lazy {
        Requesters.genericKtorClient.config {
            install(CustomCachePlugin) {
                policy = SpotifyCacheExpirationPolicy()
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val spotifyAccessToken =
                            mainPrefs.data.map { it.spotifyAccessToken }.first()
                        val spotifyAccessTokenExpires =
                            mainPrefs.data.map { it.spotifyAccessTokenExpires }.first()

                        if (spotifyAccessTokenExpires >= System.currentTimeMillis())
                            BearerTokens(spotifyAccessToken, Tokens.SPOTIFY_REFRESH_TOKEN)
                        else
                            null
                    }

                    refreshTokens {
                        val tokenResponse = withContext(Dispatchers.IO) {
                            client.submitForm(
                                url = "https://accounts.spotify.com/api/token",
                                formParameters = parameters {
                                    append("grant_type", "client_credentials")
                                }
                            ) {
                                markAsRefreshTokenRequest()

                                header(
                                    HttpHeaders.Authorization,
                                    "Basic ${Tokens.SPOTIFY_REFRESH_TOKEN}"
                                )
                            }.parseJsonBody<SpotifyTokenResponse>()
                        }

                        if (!PlatformStuff.isDesktop) {
                            mainPrefs.updateData {
                                it.copy(
                                    spotifyAccessToken = tokenResponse.access_token,
                                    spotifyAccessTokenExpires = System.currentTimeMillis() + (tokenResponse.expires_in - 60) * 1000
                                )
                            }
                        }

                        BearerTokens(tokenResponse.access_token, Tokens.SPOTIFY_REFRESH_TOKEN)
                    }
                }
            }
        }
    }

    suspend fun search(query: String, type: SpotifySearchType, limit: Int) =
        client.getResult<SpotifySearchResponse>("https://api.spotify.com/v1/search") {
            parameter("q", query)
            parameter("type", type.name)
            parameter("limit", limit)
//                    parameter("market", "US")
        }

    suspend fun trackFeatures(trackId: String) =
        client.getResult<TrackFeatures>("https://api.spotify.com/v1/audio-features/$trackId")

    suspend fun artist(artistId: String) =
        client.getResult<ArtistItem>("https://api.spotify.com/v1/artists/$artistId")

    suspend fun album(albumId: String) =
        client.getResult<AlbumItem>("https://api.spotify.com/v1/albums/$albumId")
}

class SpotifyCacheExpirationPolicy : ExpirationPolicy {

    private val ONE_MONTH = TimeUnit.DAYS.toMillis(30)
    private val ONE_YEAR = TimeUnit.DAYS.toMillis(365)

    override fun getExpirationTime(url: Url) = when {
        url.segments.last() == "search" -> ONE_MONTH
        url.segments[url.segments.size - 2] == "audio-features" -> ONE_YEAR
        url.segments[url.segments.size - 2] == "artists" -> ONE_MONTH // artist images can change
        url.segments[url.segments.size - 2] == "albums" -> ONE_YEAR // album arts usually don't change
        else -> -1
    }

}