package com.arn.scrobble.api.pleroma

import androidx.core.text.HtmlCompat
import com.arn.scrobble.Tokens
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.CustomCachePlugin
import com.arn.scrobble.api.ExpirationPolicy
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
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
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.main.App
import com.arn.scrobble.main.DrawerData
import com.arn.scrobble.utils.Stuff
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
        limit: Int
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
                        artist = Artist(track.artist.unescapeHtml()),
                        name = track.title.unescapeHtml(),
                        album = track.album?.let { Album(it.unescapeHtml()) },
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
        limit: Int
    ): Result<PageResult<Track>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun getFriends(
        page: Int,
        username: String,
        cached: Boolean,
        limit: Int
    ): Result<PageResult<User>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun loadDrawerData(username: String): DrawerData {
        val isSelf = username == userAccount.user.name

        val dd = DrawerData(0)
        if (isSelf) {
            val drawerData = App.prefs.drawerData.toMutableMap()
            drawerData[userAccount.type] = dd
            App.prefs.drawerData = drawerData
        }

        return dd
    }

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int
    ): Result<PageResult<out MusicEntry>> {
        // no op
        return Result.success(createEmptyPageResult())
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserCached?,
        cacheStrategy: CacheStrategy
    ): Map<TimePeriod, Int> {
        // no op
        return emptyMap()
    }

    private fun String.unescapeHtml() =
        HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()

    companion object {
        suspend fun authAndGetSession(userAccountTemp: UserAccountTemp): Result<Unit> {
            val tokenResponse =
                Requesters.genericKtorClient.postResult<TokenResponse>("${userAccountTemp.apiRoot!!}oauth/token") {
                    parameters {
                        parameter("client_id", Tokens.PLEROMA_CLIENT_ID)
                        parameter("client_secret", Tokens.PLEROMA_CLIENT_SECRET)
                        parameter("grant_type", "authorization_code")
                        parameter("redirect_uri", Stuff.DEEPLINK_PROTOCOL_NAME + "://auth/pleroma")
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
        when (url.pathSegments.lastOrNull()) {
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
