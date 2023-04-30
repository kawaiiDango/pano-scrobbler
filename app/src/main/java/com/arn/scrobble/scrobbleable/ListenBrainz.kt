package com.arn.scrobble.scrobbleable

import androidx.annotation.IntRange
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.CacheMarkerInterceptor
import com.arn.scrobble.CacheMarkerInterceptor.Companion.isFromCache
import com.arn.scrobble.DrawerData
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.cacheStrategy
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.friends.UserSerializable
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.CacheInterceptor
import de.umass.lastfm.Caller
import de.umass.lastfm.ImageSize
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Period
import de.umass.lastfm.Track
import de.umass.lastfm.User
import de.umass.lastfm.cache.ExpirationPolicy
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendIfNameAbsent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min


class ListenBrainz(userAccount: UserAccountSerializable) : Scrobblable(userAccount) {

    private val jsonSerializer by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
    private val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                    cache(Cache(File(App.context.cacheDir, "ktor"), 10 * 1024 * 1024))
                    readTimeout(Stuff.READ_TIMEOUT_SECS, TimeUnit.SECONDS)
                }
                addNetworkInterceptor(CacheInterceptor(ListenbrainzExpirationPolicy()))
                addInterceptor(CacheMarkerInterceptor())
            }

            install(ContentNegotiation) {
                json(jsonSerializer)
            }

            defaultRequest {
                url(userAccount.apiRoot + "1/")
                headers.appendIfNameAbsent(
                    HttpHeaders.Authorization,
                    "token ${userAccount.authKey}"
                )
            }

            expectSuccess = true
            // https://youtrack.jetbrains.com/issue/KTOR-4225
        }
    }

    private suspend fun submitListens(
        scrobbleDatas: List<ScrobbleData>,
        listenType: String
    ): ScrobbleResult {

//      "listened_at timestamp should be greater than 1033410600 (2002-10-01 00:00:00 UTC).",

        val listen = ListenBrainzListen(listenType,
            scrobbleDatas.map { scrobbleData ->
                ListenBrainzPayload(
                    if (listenType != "playing_now") scrobbleData.timestamp else null,
                    ListenBrainzTrackMetadata(
                        artist_name = scrobbleData.artist,
                        release_name = if (!scrobbleData.album.isNullOrEmpty()) scrobbleData.album else null,
                        track_name = scrobbleData.track,
                        additional_info = ListenBrainzAdditionalInfo(
                            duration_ms = if (scrobbleData.duration > 30) scrobbleData.duration * 1000 else null,
                            media_player = null,
                            submission_client = App.context.getString(R.string.app_name),
                            submission_client_version = BuildConfig.VERSION_NAME,
                        )
                    )
                )
            }
        )


        try {
            val response = client.post("submit-listens") {
                contentType(ContentType.Application.Json)
                setBody(listen)
                expectSuccess = false
            }.body<ListenBrainzResponse>()
            return ScrobbleResult.createHttp200OKResult(
                response.code ?: 200,
                response.status,
                response.status,
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
            val statusCode = (e as? ResponseException)?.response?.status?.value
            return ScrobbleResult.createHttp200OKResult(
                statusCode ?: 0,
                statusCode.toString(),
                e.javaClass.simpleName,
            )
        }
    }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData) =
        submitListens(listOf(scrobbleData), "playing_now")

    override suspend fun scrobble(scrobbleData: ScrobbleData) =
        submitListens(listOf(scrobbleData), "single")

    override suspend fun scrobble(scrobbleDatas: MutableList<ScrobbleData>) =
        submitListens(scrobbleDatas, "import")

    suspend fun lookupMbid(track: Track): ListenBrainzMbidLookup {
        val response = client.get("metadata/lookup") {
            parameter("artist_name", track.artist)
            parameter("recording_name", track.name)
        }.body<ListenBrainzMbidLookup>()

        return response
    }

    private fun createImageMap(releaseMbid: String?): Map<ImageSize, String> {
        return if (releaseMbid != null) mapOf(
            ImageSize.MEDIUM to "https://coverartarchive.org/release/$releaseMbid/front-250",
            ImageSize.LARGE to "https://coverartarchive.org/release/$releaseMbid/front-500",
            ImageSize.EXTRALARGE to "https://coverartarchive.org/release/$releaseMbid/front-500",
        )
        else
            mapOf()
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean) =
        feedback(track, if (love) 1 else 0)

    suspend fun hate(track: Track) = feedback(track, -1)

    private suspend fun feedback(track: Track, @IntRange(-1, 1) score: Int): Boolean {
        try {
            val msid = track.msid
            val mbid = if (msid == null)
                track.mbid ?: lookupMbid(track).recording_mbid
            else
                null

            Stuff.log("msid: $msid mbid: $mbid")

            if (msid == null && mbid == null) {
                Stuff.log("Track mbid not found, skipping")
                return true // ignore
            }


            val response = client.post("feedback/recording-feedback") {
                contentType(ContentType.Application.Json)
                setBody(ListenBrainzFeedback(mbid, msid, score))
                expectSuccess = false
            }

            // e.g. Servers that doesn't support love and will return 404 or INVALID_METHOD
            // treat it as a success
            if (response.status == HttpStatusCode.NotFound)
                return true

            val body = response.body<ListenBrainzResponse>()

            if (body.error == INVALID_METHOD)
                return true

            return body.isOk
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return false
        }
    }


    override suspend fun getRecents(
        page: Int,
        usernamep: String?,
        cached: Boolean,
        from: Long,
        to: Long,
        includeNowPlaying: Boolean,
        limit: Int,
    ): PaginatedResult<Track> {
        val username = usernamep ?: userAccount.user.name

        val cacheStrategy =
            if (!Stuff.isOnline && cached)
                Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
            else if (cached)
                Caller.CacheStrategy.CACHE_FIRST
            else
                Caller.CacheStrategy.NETWORK_ONLY

        val fromCache: Boolean
        val listens = client.get("user/$username/listens") {
            if (to > 0L)
                parameter("max_ts", to / 1000)
            if (from > 0L)
                parameter("min_ts", from / 1000)
            parameter("count", limit)
            cacheStrategy(cacheStrategy)
        }
            .also {
                fromCache = it.isFromCache
            }
            .body<ListenBrainzData<ListenBrainzListensPayload>>()

        val listensMutable = listens.payload.listens.toMutableList()

        if (includeNowPlaying) {
            client.get("user/$username/playing-now") {
                cacheStrategy(cacheStrategy)
            }.body<ListenBrainzData<ListenBrainzListensPayload>>()
                .payload
                .listens
                .firstOrNull()
                ?.let {
                    listensMutable.add(0, it)
                }
        }

        val tracks = listensMutable.map {
            Track(
                0,
                it.track_metadata.track_name,
                null,
                it.track_metadata.mbid_mapping?.recording_mbid,
                it.recording_msid,
                -1,
                -1,
                0,
                -1,
                it.track_metadata.additional_info?.duration_ms ?: -1,
                false,
                it.track_metadata.release_name,
                it.track_metadata.mbid_mapping?.release_mbid,
                it.track_metadata.artist_name,
                null,
                it.track_metadata.mbid_mapping?.artist_mbids?.firstOrNull(),
                it.listened_at?.let { Date(it * 1000L) },
                false,
                it.playing_now == true,
            ).apply {
                imageUrlsMap = createImageMap(it.track_metadata.mbid_mapping?.release_mbid)
            }
        }

        val totalPages = if (tracks.size < limit) page else page + 1
        val pr = PaginatedResult(page, totalPages, tracks.size, tracks)
        pr.isStale = fromCache && cacheStrategy == Caller.CacheStrategy.CACHE_FIRST
        return pr
    }

    override suspend fun delete(track: Track): Boolean {
        val msid = track.msid ?: return true // ignore error

        try {
            val response = client.post("delete-listen") {
                contentType(ContentType.Application.Json)
                setBody(
                    ListenBrainzDeleteRequest(
                        (track.playedWhen.time / 1000).toInt(),
                        msid,
                    )
                )
            }.body<ListenBrainzResponse>()

            if (response.error == INVALID_METHOD) // maloja
                return true

            return response.isOk
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override suspend fun getLoves(
        page: Int,
        usernamep: String?,
        cached: Boolean,
        limit: Int,
    ): PaginatedResult<Track> {
        val cacheStrategy =
            if (!Stuff.isOnline && cached)
                Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
            else if (cached)
                Caller.CacheStrategy.CACHE_FIRST
            else
                Caller.CacheStrategy.NETWORK_ONLY

        Stuff.log(this::getLoves.name + " " + page)

        val username = usernamep ?: userAccount.user.name
        val fromCache: Boolean

        val feedbacks = client.get("feedback/user/$username/get-feedback") {
            parameter("metadata", true)
            parameter("offset", limit * (page - 1))
            cacheStrategy(cacheStrategy)
        }.also {
            fromCache = it.isFromCache
        }.body<ListenBrainzFeedbacks>()
        val tracks = feedbacks.feedback
            .filter { it.track_metadata != null }
            .map {
                Track(
                    0,
                    it.track_metadata!!.track_name,
                    null,
                    it.recording_mbid,
                    it.recording_msid,
                    -1,
                    -1,
                    it.score,
                    -1,
                    it.track_metadata.additional_info?.duration_ms ?: -1,
                    false,
                    it.track_metadata.release_name,
                    it.track_metadata.mbid_mapping?.release_mbid,
                    it.track_metadata.artist_name,
                    null,
                    it.track_metadata.mbid_mapping?.artist_mbids?.firstOrNull(),
                    Date(it.created * 1000L),
                    false,
                    false,
                ).apply {
                    imageUrlsMap = createImageMap(it.track_metadata.mbid_mapping?.release_mbid)
                }
            }
        val totalPages = ceil(feedbacks.total_count.toFloat() / limit).toInt()
        val pr = PaginatedResult(page, totalPages, tracks.size, tracks)
        pr.isStale = fromCache && cacheStrategy == Caller.CacheStrategy.CACHE_FIRST
        return pr
    }

    override suspend fun getFriends(page: Int, usernamep: String?): PaginatedResult<User> {
        val cacheStrategy = if (Stuff.isOnline)
            Caller.CacheStrategy.NETWORK_ONLY
        else
            Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED

        val username = usernamep ?: userAccount.user.name

        val users = client.get("user/$username/following") {
            cacheStrategy(cacheStrategy)
        }.body<ListenBrainzFollowing>()
            .following
            .map {
                User(it, "https://listenbrainz.org/user/$it", "", "None", -1, mapOf())
            }

        return PaginatedResult(1, 1, users.size, users)
    }

    override suspend fun loadDrawerData(username: String): DrawerData {
        val isSelf = username == userAccount.user.name

        val totalCount = client.get("user/$username/listen-count")
            .also { if (it.status != HttpStatusCode.OK) return DrawerData(0) }
            .body<ListenBrainzData<ListenBrainzCountPayload>>()
            .payload.count

        val dd = DrawerData(totalCount)
        if (isSelf)
            App.prefs.drawerDataCached = dd

        return dd
    }

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        usernamep: String?,
        cacheStrategy: Caller.CacheStrategy,
        limit: Int
    ): PaginatedResult<out MusicEntry> {
        val username = usernamep ?: userAccount.user.name
        val range = timePeriod.tag ?: "all_time"

        val typeStr = when (type) {
            Stuff.TYPE_ARTISTS -> "artists"
            Stuff.TYPE_ALBUMS -> "releases"
            Stuff.TYPE_TRACKS -> "recordings"
            else -> throw IllegalArgumentException("Unknown type")
        }

        val payload =
            client.get("stats/user/$username/$typeStr") {
                if (limit > -1) {
                    parameter("count", limit)
                    parameter("offset", limit * (page - 1))
                }
                parameter("range", range)
                cacheStrategy(cacheStrategy)
            }.also {
                if (it.status == HttpStatusCode.NoContent)
                    return PaginatedResult(1, 1, 0, emptyList())
            }
                .body<ListenBrainzData<ListenBrainzStatsEntriesPayload>>()
                .payload

        val musicEntries = payload.let {
            when (type) {
                Stuff.TYPE_ARTISTS -> it.artists
                Stuff.TYPE_ALBUMS -> it.releases
                Stuff.TYPE_TRACKS -> it.recordings
                else -> throw IllegalArgumentException("Unknown type")
            }
        }?.map {
            when (type) {
                Stuff.TYPE_ARTISTS -> Artist(
                    0,
                    it.artist_name,
                    null,
                    it.artist_mbids?.firstOrNull(),
                    it.listen_count,
                    it.listen_count,
                    -1,
                    false
                )

                Stuff.TYPE_ALBUMS -> Album(
                    0,
                    it.release_name,
                    null,
                    it.release_mbid,
                    it.listen_count,
                    it.listen_count,
                    -1,
                    it.artist_name,
                    null,
                    it.artist_mbids?.firstOrNull(),
                    false
                ).apply { imageUrlsMap = createImageMap(it.release_mbid) }

                Stuff.TYPE_TRACKS -> Track(
                    0,
                    it.track_name,
                    null,
                    it.recording_mbid,
                    null,
                    it.listen_count,
                    it.listen_count,
                    0,
                    -1,
                    -1,
                    false,
                    it.release_name,
                    it.release_mbid,
                    it.artist_name,
                    null,
                    it.artist_mbids?.firstOrNull(),
                    null,
                    false,
                    false,
                ).apply { imageUrlsMap = createImageMap(it.release_mbid) }

                else -> throw IllegalArgumentException("Unknown type")
            }
        }
        val totalPages = ceil(
            min(
                payload.total_artist_count ?: payload.total_release_count
                ?: payload.total_recording_count ?: 0,
                1000
            ).toFloat() / limit
        ).toInt()

        return PaginatedResult(page, totalPages, musicEntries?.size ?: 0, musicEntries)
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserSerializable?,
        cacheStrategy: Caller.CacheStrategy,
    ): Map<TimePeriod, Int> {
        fun String.transformName(): String {
            return if (timePeriod.period == Period.OVERALL)
                "'" + takeLast(2) // 4 digit year
            else
                take(3).trim()
        }

        val username = user?.name ?: userAccount.user.name
        val payload =
            client.get("stats/user/$username/listening-activity") {
                parameter("range", timePeriod.tag ?: return emptyMap())
                cacheStrategy(cacheStrategy)
            }.also {
                if (it.status == HttpStatusCode.NoContent)
                    return emptyMap()
            }
                .body<ListenBrainzData<ListenBrainzActivityPayload>>()
                .payload

        val n = when (TimeUnit.MILLISECONDS.toDays(timePeriod.end - timePeriod.start)) {
            in 367 until Long.MAX_VALUE -> 10
            in 90 until 367 -> 12
            in 10 until 90 -> 10
            else -> 7
        }

        return payload.listening_activity
            .takeLast(n)
            .associate {
                TimePeriod(
                    it.from_ts.toLong(),
                    it.to_ts.toLong(),
                    null,
                    it.time_range.transformName()
                ) to it.listen_count
            }
    }

    companion object {
        suspend fun validateAndGetUsername(userAccountTemp: UserAccountTemp): String? {
            val client =
                if (userAccountTemp.tlsNoVerify) LFMRequester.okHttpClientTlsNoVerify else LFMRequester.okHttpClient

            val result = client.newCall(
                Request(
                    "${userAccountTemp.apiRoot}1/validate-token".toHttpUrl(),
                    headers = Headers.headersOf(
                        "Authorization",
                        "token ${userAccountTemp.authKey}"
                    ),
                )
            ).execute().use { response ->
                Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<ValidateToken>(response.body.string())
            }

            result.user_name ?: return null

            val isCustom = userAccountTemp.apiRoot != Stuff.LISTENBRAINZ_API_ROOT
            val profileUrl = if (isCustom)
                userAccountTemp.apiRoot + result.user_name
            else
                "https://listenbrainz.org/user/${result.user_name}"

            val account = UserAccountSerializable(
                if (isCustom) AccountType.CUSTOM_LISTENBRAINZ else AccountType.LISTENBRAINZ,
                UserSerializable(
                    result.user_name,
                    profileUrl,
                    result.user_name,
                    "",
                    -1,
                    mapOf()
                ),
                userAccountTemp.authKey,
                userAccountTemp.apiRoot,
                userAccountTemp.tlsNoVerify,
            )

            Scrobblables.add(account)

            return result.user_name
        }
    }
}

class ListenbrainzExpirationPolicy : ExpirationPolicy {
    private val ONE_WEEK = TimeUnit.DAYS.toMillis(7)
    private val FIVE_MINUTES = TimeUnit.MINUTES.toMillis(5)

    override fun getExpirationTime(url: HttpUrl?): Long {
        return when (url?.pathSegments?.lastOrNull()) {
            "playing-now",
            "listens",
            "following",
            "get-feedback",
            -> ONE_WEEK

            "artists",
            "releases",
            "recordings",
            -> FIVE_MINUTES

            else -> -1
        }
    }
}

enum class ListenbrainzRanges {
    this_week, this_month, this_year, week, month, year, quarter, half_yearly, all_time
}

const val INVALID_METHOD = "Invalid Method"