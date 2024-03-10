package com.arn.scrobble.api.maloja

import com.arn.scrobble.App
import com.arn.scrobble.DrawerData
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.CacheStrategy
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.api.listenbrainz.TimeNullableSerializer
import com.arn.scrobble.api.listenbrainz.TimeSerializer
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.utils.Stuff
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import okhttp3.Cache
import java.io.File
import java.util.concurrent.TimeUnit

class Maloja(userAccount: UserAccountSerializable) :
    Scrobblable(userAccount) {
    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                    cache(Cache(File(App.context.cacheDir, "ktor"), 10 * 1024 * 1024))
                    readTimeout(Stuff.READ_TIMEOUT_SECS, TimeUnit.SECONDS)
                }
            }

            install(HttpCallValidator) {
                validateResponse { response ->
                    if (response.status.isSuccess()) return@validateResponse
                    try {
                        val errorResponse = response.body<MalojaErrorResponse>()
                        throw ApiException(response.status.value, errorResponse.error)
                    } catch (e: Exception) {
                        throw ApiException(response.status.value, response.status.description, e)
                    }
                }
            }

            install(ContentNegotiation) {
                json(Stuff.myJson)
            }

            defaultRequest {
                url(this@Maloja.userAccount.apiRoot!! + "apis/mlj_1/")
            }

//            expectSuccess = true
            // https://youtrack.jetbrains.com/issue/KTOR-4225
        }
    }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData): Result<ScrobbleIgnored> {
        // no op
        return Result.success(ScrobbleIgnored(false))
    }

    override suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleIgnored> {
        val scrobble = MalojaScrobbleData(
            artists = listOf(scrobbleData.artist),
            title = scrobbleData.track,
            album = scrobbleData.album,
            albumartists = scrobbleData.albumArtist?.let { listOf(it) },
            duration = scrobbleData.duration,
            time = scrobbleData.timestamp,
            key = userAccount.authKey
        )

        return client.postResult<String>("newscrobble") {
            contentType(ContentType.Application.Json)
            setBody(scrobble)
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
        return client.getResult<MalojaTracksResponse>("scrobbles") {
            parameter("page", page)
            parameter("perpagge", limit)
        }
            .map {
                val tracks = it.list.map { trackItem ->
                    Track(
                        artist = Artist(trackItem.track.artists.joinToString()),
                        name = trackItem.track.title,
                        album = trackItem.track.album?.let {
                            Album(it.albumtitle, Artist(it.artists.joinToString()))
                        },
                        duration = trackItem.duration ?: 0,
                        date = trackItem.time
                    )
                }
                PageResult(
                    PageAttr(
                        page = it.pagination.page,
                        totalPages = if (it.pagination.next_page == null) it.pagination.page else it.pagination.page + 2,
                        total = tracks.size,
                    ),
                    tracks,
                    false
                )
            }
    }

    override suspend fun getLoves(
        page: Int,
        username: String,
        cached: Boolean,
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

    override suspend fun loadDrawerData(username: String): DrawerData? {
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

    companion object {
        suspend fun authAndGetSession(userAccountTemp: UserAccountTemp): Result<Unit> {
            return Requesters.genericKtorClient.getResult<String>("${userAccountTemp.apiRoot!!}/apis/mlj_1/test") {
                parameter("key", userAccountTemp.authKey)
            }.map {
                Requesters.genericKtorClient.getResult<ServerInfo>("${userAccountTemp.apiRoot}/apis/mlj_1/serverinfo")
                    .map { it.name }.getOrNull() ?: App.context.getString(R.string.maloja)

            }.onSuccess { username ->
                val account = UserAccountSerializable(
                    AccountType.MALOJA,
                    UserCached(
                        username,
                        userAccountTemp.apiRoot,
                        username,
                        "",
                        -1,
                    ),
                    userAccountTemp.authKey,
                    userAccountTemp.apiRoot,
                )

                Scrobblables.add(account)
            }
                .onFailure { it.printStackTrace() }
                .map { }
        }
    }
}

@Serializable
private data class MalojaErrorResponse(
    val error: String
)

@Serializable
private data class MalojaTracksResponse(
    val list: List<MalojaTrackItem>,
    val pagination: MalojaPagination
)

@Serializable
private data class MalojaTrackItem(
    @Serializable(with = TimeSerializer::class)
    val time: Long,
    val track: MalojaTrack,
    val duration: Long?,
)

@Serializable
private data class MalojaTrack(
    val artists: List<String>,
    val title: String,
    val album: MalojaAlbum?,
    val length: Long?
)

@Serializable
private data class MalojaAlbum(
    val artists: List<String>,
    val albumtitle: String
)

@Serializable
private data class MalojaPagination(
    val page: Int,
    val next_page: String?,
)

@Serializable
private data class MalojaScrobbleData(
    val artists: List<String>,
    val title: String,
    val album: String?,
    val albumartists: List<String>?,
    @Serializable(with = TimeNullableSerializer::class)
    val duration: Long?,
    val length: Int? = null,
    @Serializable(with = TimeNullableSerializer::class)
    val time: Long?,
    val nofix: Boolean? = null,
    val key: String,
)

@Serializable
private data class ServerInfo(
    val name: String,
)
