package com.arn.scrobble.api.pleroma

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.CustomCachePlugin
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.ExpirationPolicy
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.CacheStrategy
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.PlatformStuff.toHtmlAnnotatedString
import com.arn.scrobble.utils.Stuff.cacheStrategy
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.concurrent.TimeUnit

class Pleroma(userAccount: UserAccountSerializable) : Scrobblable(userAccount) {
    private val client: HttpClient by lazy {
        Requesters.genericKtorClient.config {
            install(CustomCachePlugin) {
                policy = PleromaExpirationPolicy()
            }

            defaultRequest {
                url(userAccount.apiRoot!!)
                bearerAuth(userAccount.authKey)
            }
        }
    }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData): Result<ScrobbleIgnored> {
        // no op
        return Result.success(ScrobbleIgnored(false))
    }

    override suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleIgnored> {
        val pleromaTrack = PleromaTrack(
            artist = scrobbleData.artist,
            title = scrobbleData.track,
            album = scrobbleData.album,
            album_artist = scrobbleData.albumArtist,
            length = scrobbleData.duration
        )

        return client.postResult<String>("api/v1/pleroma/scrobble") {
            contentType(ContentType.Application.Json)
            setBody(pleromaTrack)
        }.map { ScrobbleIgnored(false) }
    }

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>): Result<ScrobbleIgnored> {
        for (scrobbleData in scrobbleDatas) {
            val result = scrobble(scrobbleData)
            if (result.isFailure)
                return result

            delay(1000L)
        }

        return Result.success(ScrobbleIgnored(false))
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean): Result<ScrobbleIgnored> {
        // no op
        return Result.success(ScrobbleIgnored(false))
    }

    override suspend fun delete(track: Track): Result<Unit> {
        // no op
        return Result.success(Unit)
    }

    override suspend fun getRecents(
        page: Int,
        username: String,
        cached: Boolean,
        from: Long,
        to: Long,
        includeNowPlaying: Boolean,
        limit: Int,
    ): Result<PageResult<Track>> {
        val cacheStrategy = if (cached)
            CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
        else
            CacheStrategy.NETWORK_ONLY

        return client.getResult<List<PleromaTrack>>("api/v1/pleroma/accounts/${userAccount.user.name}/scrobbles") {
            cacheStrategy(cacheStrategy)
        }
            .map {
                val tracks = it.map { track ->
                    Track(
                        artist = Artist(track.artist.toHtmlAnnotatedString().text),
                        name = track.title.toHtmlAnnotatedString().text,
                        album = track.album?.let { Album(it.toHtmlAnnotatedString().text) },
                        duration = track.length,
                        date = Instant.parse(track.created_at!!).toEpochMilli()
                    )
                }
                PageResult(
                    PageAttr(
                        page = 1,
                        totalPages = 1,
                        total = tracks.size,
                    ),
                    tracks,
                )
            }
    }

    override suspend fun getLoves(
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
    ): Result<PageResult<Track>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun getFriends(
        page: Int,
        username: String,
        cached: Boolean,
        limit: Int,
    ): Result<PageResult<User>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun loadDrawerData(username: String): DrawerData {
        val isSelf = username == userAccount.user.name

        val dd = DrawerData(0)
        if (isSelf) {
            PlatformStuff.mainPrefs.updateData {
                it.copy(drawerData = it.drawerData + (userAccount.type to dd))
            }
        }

        return dd
    }

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
    ): Result<PageResult<out MusicEntry>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserCached?,
        cacheStrategy: CacheStrategy,
    ): Map<TimePeriod, Int> {
        // no op
        return emptyMap()
    }

    companion object {
        suspend fun createApp(apiRoot: String, redirectUri: String) =
            Requesters.genericKtorClient.postResult<PleromaOauthClientCreds>("$apiRoot/api/v1/apps") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateAppRequest(
                        client_name = BuildKonfig.APP_NAME,
                        redirect_uris = redirectUri,
                        scopes = "read write",
//                        website = getString(R.string.github_link)
                    )
                )
            }

        suspend fun authAndGetSession(
            userAccountTemp: UserAccountTemp,
            oauthClientCreds: PleromaOauthClientCreds,
        ): Result<Unit> {
            val tokenResponse =
                Requesters.genericKtorClient.postResult<TokenResponse>("${userAccountTemp.apiRoot!!}oauth/token") {
                    parameters {
                        parameter("client_id", oauthClientCreds.client_id)
                        parameter("client_secret", oauthClientCreds.client_secret)
                        parameter("grant_type", "authorization_code")
                        parameter("redirect_uri", oauthClientCreds.redirect_uri)
                        parameter("code", userAccountTemp.authKey)
                    }.let { setBody(FormDataContent(it)) }
                }

            tokenResponse.onSuccess {
                // it.me is profile url
                val username = it.me.substringAfterLast("/")

                val account = UserAccountSerializable(
                    AccountType.PLEROMA,
                    UserCached(
                        username,
                        it.me,
                        username,
                        "",
                        -1,
                    ),
                    it.access_token,
                    userAccountTemp.apiRoot,
                )

                Scrobblables.add(account)
            }
                .onFailure { it.printStackTrace() }

            return tokenResponse.map { }
        }
    }
}


class PleromaExpirationPolicy : ExpirationPolicy {
    private val ONE_WEEK = TimeUnit.DAYS.toMillis(7)

    override fun getExpirationTime(url: Url) =
        when (url.segments.lastOrNull()) {
            "scrobbles",
                -> ONE_WEEK

            else -> -1
        }
}

@Serializable
private data class PleromaTrack(
    val artist: String,
    val title: String,
    val album: String?,
    val album_artist: String?,
    val length: Long?,
    val created_at: String? = null, // null for input
)

@Serializable
private data class TokenResponse(
    val scope: String,
    val me: String,
    val expires_in: Long,
    val access_token: String,
    val refresh_token: String,
)

@Serializable
private data class CreateAppRequest(
    val client_name: String,
    val redirect_uris: String,
    val scopes: String,
    val website: String? = null,
)

@Serializable
data class PleromaOauthClientCreds(
    val id: String,
    val name: String,
    val client_id: String,
    val client_secret: String,
    val redirect_uri: String,
    val website: String?,
    val vapid_key: String,
)