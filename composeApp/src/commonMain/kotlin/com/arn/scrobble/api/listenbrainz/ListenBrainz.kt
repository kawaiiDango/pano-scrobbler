package com.arn.scrobble.api.listenbrainz

import androidx.annotation.IntRange
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.CustomCachePlugin
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getPageResult
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.Requesters.setJsonBody
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleResult
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ImageSize
import com.arn.scrobble.api.lastfm.LastFmImage
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageEntries
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Session
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.User
import com.arn.scrobble.charts.ListeningActivity
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.cacheStrategy
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.util.appendIfNameAbsent
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min


class ListenBrainz(userAccount: UserAccountSerializable) : Scrobblable(userAccount) {

    private val client: HttpClient by lazy {
        Requesters.genericKtorClient.config {

            install(CustomCachePlugin) {
                policy = ListenbrainzExpirationPolicy()
            }

            defaultRequest {
                url(userAccount.apiRoot + "1/")
                headers.appendIfNameAbsent(
                    HttpHeaders.Authorization,
                    "token ${userAccount.authKey}"
                )
            }

            // https://youtrack.jetbrains.com/issue/KTOR-4225
        }
    }

    private suspend fun submitListens(
        scrobbleDatas: List<ScrobbleData>,
        listenType: ListenBrainzListenType,
    ): Result<ScrobbleResult> {

//      "listened_at timestamp should be greater than 1033410600 (2002-10-01 00:00:00 UTC).",

        val listen = ListenBrainzListen(
            listenType,
            scrobbleDatas.map { scrobbleData ->

//                val pkgName = scrobbleData.pkgName?.let { PackageName(it) }

                ListenBrainzPayload(
                    scrobbleData.timestamp.takeIf { listenType != ListenBrainzListenType.playing_now },
                    ListenBrainzTrackMetadata(
                        artist_name = scrobbleData.artist,
                        release_name = scrobbleData.album?.ifEmpty { null },
                        track_name = scrobbleData.track,
                        additional_info = ListenBrainzAdditionalInfo(
                            duration_ms = scrobbleData.safeDuration(),
                            submission_client = BuildKonfig.APP_NAME,
                            submission_client_version = BuildKonfig.VER_NAME,
                        )
                    )
                )
            }
        )

        return client.postResult<ListenBrainzSubmitResponse>("submit-listens") {
            if (listenType == ListenBrainzListenType.playing_now)
                parameter("return_msid", true)

            setJsonBody(listen)
        }.map {
            if (it.isOk)
                ScrobbleResult(false, msid = it.recording_msid)
            else
                ScrobbleResult(true)
        }
    }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData) =
        submitListens(listOf(scrobbleData), ListenBrainzListenType.playing_now)

    override suspend fun scrobble(scrobbleData: ScrobbleData) =
        submitListens(listOf(scrobbleData), ListenBrainzListenType.single)

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>) =
        submitListens(scrobbleDatas, ListenBrainzListenType.import)

    private suspend fun lookupMbid(track: Track): Result<ListenBrainzMbidLookup> {
        return client.getResult<ListenBrainzMbidLookup>("metadata/lookup") {
            parameter("artist_name", track.artist.name)
            parameter("recording_name", track.name)
            parameter("release_name", track.album?.name)
        }
//        200 OK – lookup succeeded, does not indicate whether a match was found or not
    }

    private fun createImageMap(releaseMbid: String?): List<LastFmImage>? {
        return if (releaseMbid != null) listOf(
            LastFmImage(
                ImageSize.medium.name,
                "https://coverartarchive.org/release/$releaseMbid/front-250"
            ),
            LastFmImage(
                ImageSize.large.name,
                "https://coverartarchive.org/release/$releaseMbid/front-500"
            ),
            LastFmImage(
                ImageSize.extralarge.name,
                "https://coverartarchive.org/release/$releaseMbid/front-500"
            ),
        )
        else
            null
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean) =
        feedback(track, if (love) 1 else 0)

    suspend fun hate(track: Track) = feedback(track, -1)

    private suspend fun feedback(
        track: Track,
        @IntRange(-1, 1) score: Int,
    ): Result<ScrobbleResult> {
        val mbid = track.mbid
        var msid = track.msid?.takeIf { track.mbid == null }

        // send a temporary now playing for official listenbrainz servers
        if (mbid == null && msid == null && userAccount.apiRoot == Stuff.LISTENBRAINZ_API_ROOT) {
            val scrobbleData = ScrobbleData(
                artist = track.artist.name,
                track = track.name,
                album = track.album?.name,
                albumArtist = track.album?.artist?.name,
                timestamp = 0L,
                duration = track.duration,
                appId = null
            )
            updateNowPlaying(scrobbleData)
                .onSuccess {
                    msid = it.msid
                    Logger.d { "msid lookup result: $msid" }
                }.onFailure {
                    return Result.failure(it)
                }
        }


        if (msid == null && mbid == null) {
            Logger.w { "Track mbid/msid not found, skipping feedback" }
            return Result.success(ScrobbleResult(true)) // ignore
        }

        return client.postResult<ListenBrainzSubmitResponse>("feedback/recording-feedback") {
            setJsonBody(ListenBrainzFeedback(mbid, msid, score))
        }.map {
            if (it.isOk)
                ScrobbleResult(false)
            else
                ScrobbleResult(true)
        }
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
        fun trackMap(it: ListenBrainzListensListens): Track {
            val artist = Artist(
                name = it.track_metadata.artist_name,
                mbid = it.track_metadata.mbid_mapping?.artist_mbids?.firstOrNull(),
            )

            val album = if (it.track_metadata.release_name != null)
                Album(
                    name = it.track_metadata.release_name,
                    mbid = it.track_metadata.mbid_mapping?.release_mbid,
                    artist = artist,
                    image = createImageMap(it.track_metadata.mbid_mapping?.release_mbid),
                )
            else
                null

            return Track(
                name = it.track_metadata.track_name,
                album = album,
                artist = artist,
                mbid = it.track_metadata.mbid_mapping?.recording_mbid,
                msid = it.recording_msid,
                duration = it.track_metadata.additional_info?.duration_ms,
                date = it.listened_at,
                userloved = null,
                isNowPlaying = it.playing_now == true,
            )
        }

        val cacheStrategy = if (cached)
            CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
        else
            CacheStrategy.NETWORK_ONLY

        val actualLimit = if (limit > 0) limit else 25

        val listens =
            client.getPageResult<ListenBrainzListensData, Track>(
                "user/$username/listens",
                {
                    it.payload
                        .listens
                        .map { trackMap(it) }
                        .let { PageEntries(it) }
                },
                {
                    val oldestListenTs = it.payload.oldest_listen_ts
                    val oldestListenTsInPage = it.payload.listens.lastOrNull()?.listened_at
                    val totalPages =
                        if (oldestListenTsInPage != null && oldestListenTs != null && oldestListenTsInPage > oldestListenTs)
                            page + 1
                        else
                            page

                    PageAttr(
                        page,
                        totalPages,
                        it.payload.count,
                    )
                }
            ) {
                if (to > 0L)
                    parameter("max_ts", to / 1000)
                if (from > 0L)
                    parameter("min_ts", from / 1000)
                parameter("count", actualLimit)
                cacheStrategy(cacheStrategy)
            }


        if (includeNowPlaying) {
            client.getResult<ListenBrainzListensData>("user/$username/playing-now") {
                cacheStrategy(cacheStrategy)
            }.map { it.payload.listens.firstOrNull()?.let { trackMap(it) } }
                .getOrNull()
                ?.let { npTrack ->
                    return listens.map {
                        it.copy(entries = listOf(npTrack) + it.entries)
                    }
                }
        }

        return listens
    }

    override suspend fun delete(track: Track): Result<Unit> {
        track.date ?: return Result.failure(IllegalStateException("no date"))
        val msid = track.msid ?: return Result.success(Unit) // ignore error

        return client.postResult<ListenBrainzSubmitResponse>("delete-listen") {
            setJsonBody(ListenBrainzDeleteRequest(track.date, msid))
        }.map { }
    }

    override suspend fun getLoves(
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
    ): Result<PageResult<Track>> {
        fun mapTrack(it: ListenBrainzFeedbackPayload): Track {
            val artist = if (it.track_metadata?.artist_name != null)
                Artist(
                    name = it.track_metadata.artist_name,
                    mbid = it.track_metadata.mbid_mapping?.artist_mbids?.firstOrNull(),
                )
            else
                null

            val album = if (it.track_metadata?.release_name != null)
                Album(
                    name = it.track_metadata.release_name,
                    mbid = it.track_metadata.mbid_mapping?.release_mbid,
                    artist = artist!!,
                    image = createImageMap(it.track_metadata.mbid_mapping?.release_mbid),
                )
            else
                null

            return Track(
                name = it.track_metadata!!.track_name,
                album = album,
                artist = artist!!,
                mbid = it.recording_mbid,
                msid = it.recording_msid,
                duration = it.track_metadata.additional_info?.duration_ms,
                date = it.created,
                userloved = it.score == 1,
                userHated = it.score == -1,
            )
        }

        val actualLimit = if (limit > 0) limit else 25

        return client.getPageResult<ListenBrainzFeedbacks, Track>(
            "feedback/user/$username/get-feedback",
            {
                it.feedback
                    .filter { it.track_metadata != null }
                    .map { mapTrack(it) }
                    .let { PageEntries(it) }
            },
            {
                val totalPages = ceil(it.total_count.toFloat() / actualLimit).toInt()
                PageAttr(page, totalPages, it.total_count)
            }
        ) {
            parameter("offset", actualLimit * (page - 1))
            parameter("metadata", true)
            parameter("count", actualLimit)
            cacheStrategy(cacheStrategy)
        }
    }

    override suspend fun getFriends(
        page: Int,
        username: String,
        cached: Boolean,
        limit: Int,
    ): Result<PageResult<User>> {
        val cacheStrategy = if (cached) CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
        else CacheStrategy.NETWORK_ONLY

        return client.getPageResult<ListenBrainzFollowing, User>(
            "user/$username/following",
            {
                it.following.map {
                    User(it, url = "https://listenbrainz.org/user/$it")
                }
                    .let { PageEntries(it) }
            }
        ) {
            cacheStrategy(cacheStrategy)
        }
    }

    override suspend fun loadDrawerData(username: String): Result<DrawerData> {
        return client.getResult<ListenBrainzCountData>("user/$username/listen-count")
            .map { DrawerData(it.payload.count) }
    }

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
    ): Result<PageResult<out MusicEntry>> {
        fun mapMusicEntry(it: ListenBrainzStatsEntry): MusicEntry {
            return when (type) {
                Stuff.TYPE_ARTISTS -> {
                    Artist(
                        name = it.artist_name,
                        mbid = it.artist_mbids?.firstOrNull(),
                        playcount = it.listen_count.toLong(),
                        userplaycount = it.listen_count,
                    )
                }

                Stuff.TYPE_ALBUMS -> {
                    Album(
                        name = it.release_name!!,
                        artist = Artist(
                            name = it.artist_name,
                            mbid = it.artist_mbids?.firstOrNull(),
                        ),
                        mbid = it.release_mbid,
                        playcount = it.listen_count.toLong(),
                        userplaycount = it.listen_count,
                        image = createImageMap(it.release_mbid),
                    )
                }

                Stuff.TYPE_TRACKS -> {
                    val artist = Artist(
                        name = it.artist_name,
                        mbid = it.artist_mbids?.firstOrNull(),
                    )

                    val album = it.release_name?.let { albumName ->
                        Album(
                            name = albumName,
                            artist = artist,
                            mbid = it.release_mbid,
                            image = createImageMap(it.release_mbid),
                        )
                    }

                    Track(
                        name = it.track_name!!,
                        album = album,
                        artist = artist,
                        mbid = it.recording_mbid,
                        playcount = it.listen_count.toLong(),
                        userplaycount = it.listen_count,
                    )
                }

                else -> {
                    throw IllegalArgumentException("Unknown type")
                }
            }
        }

        val range = timePeriod.listenBrainzRange ?: "all_time"
        val actualLimit = if (limit > 0) limit else 25

        val typeStr = when (type) {
            Stuff.TYPE_ARTISTS -> "artists"
            Stuff.TYPE_ALBUMS -> "releases"
            Stuff.TYPE_TRACKS -> "recordings"
            else -> throw IllegalArgumentException("Unknown type")
        }

        return client.getPageResult<ListenBrainzStatsEntriesData, MusicEntry>(
            "stats/user/$username/$typeStr",
            {
                it.payload.let {
                    when (type) {
                        Stuff.TYPE_ARTISTS -> it.artists
                        Stuff.TYPE_ALBUMS -> it.releases
                        Stuff.TYPE_TRACKS -> it.recordings
                        else -> throw IllegalArgumentException("Unknown type")
                    }
                }!!.map { mapMusicEntry(it) }
                    .let { PageEntries(it) }
            },
            {
                val total = it.payload.total_artist_count ?: it.payload.total_release_count
                ?: it.payload.total_recording_count ?: 0
                val totalPages = ceil(min(total, 1000).toFloat() / actualLimit).toInt()

                PageAttr(page, totalPages, total)
            }
        ) {
            parameter("offset", actualLimit * (page - 1))
            parameter("range", range)
            parameter("count", actualLimit)
            cacheStrategy(cacheStrategy)
        }
    }

    override suspend fun getListeningActivity(
        timePeriod: TimePeriod,
        user: UserCached?,
        cacheStrategy: CacheStrategy,
    ): ListeningActivity {

        timePeriod.listenBrainzRange ?: return ListeningActivity()
        val username = user?.name ?: userAccount.user.name

        val result =
            client.getResult<ListenBrainzActivityData>("stats/user/$username/listening-activity") {
                parameter("range", timePeriod.listenBrainzRange)
                cacheStrategy(cacheStrategy)
            }

        val type = when (timePeriod.listenBrainzRange) {
            ListenBrainzRange.all_time -> TimePeriodType.YEAR

            ListenBrainzRange.year,
            ListenBrainzRange.this_year,
            ListenBrainzRange.half_yearly
                -> TimePeriodType.MONTH

            else -> TimePeriodType.DAY
        }


        val dateFormatter = when (type) {
            TimePeriodType.YEAR -> SimpleDateFormat("''yy", Locale.getDefault())
            TimePeriodType.MONTH -> SimpleDateFormat("MM", Locale.getDefault())
            TimePeriodType.DAY -> SimpleDateFormat("dd.\nMM", Locale.getDefault())
            else -> null
        }

//        val n = when (type) {
//            TimePeriodType.YEAR -> 10
//            TimePeriodType.MONTH -> 12
//            else -> 15
//        }

        val timePeriodsMap = result.getOrNull()?.payload?.listening_activity
//            ?.takeLast(n)
            ?.associate {
                TimePeriod(
                    it.from_ts,
                    it.to_ts,
                    name = dateFormatter?.format(it.from_ts) ?: it.time_range
                ) to it.listen_count
            } ?: emptyMap()

        return ListeningActivity(
            timePeriodsToCounts = timePeriodsMap,
            type = type,
        )
    }

    companion object {

        suspend fun authAndGetSession(userAccountTemp: UserAccountTemp): Result<Session> {
            val client = Requesters.genericKtorClient

            val result =
                client.getResult<ValidateToken>("${userAccountTemp.apiRoot}1/validate-token") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "token ${userAccountTemp.authKey}")
                }

            result.onSuccess { validateToken ->
                validateToken.user_name ?: return Result.failure(ApiException(-1, "Invalid token"))
                val isCustom = userAccountTemp.apiRoot != Stuff.LISTENBRAINZ_API_ROOT
                val profileUrl = if (isCustom)
                    userAccountTemp.apiRoot!!.toHttpUrl()
                        .let { url -> url.scheme + "://" + url.host }
                else
                    "https://listenbrainz.org/user/${validateToken.user_name}"

                val account = UserAccountSerializable(
                    userAccountTemp.type,
                    UserCached(
                        validateToken.user_name,
                        profileUrl,
                        validateToken.user_name,
                        "",
                        -1,
                    ),
                    userAccountTemp.authKey,
                    userAccountTemp.apiRoot,
                )

                Scrobblables.add(account)
            }

            return result.map { Session(it.user_name, userAccountTemp.authKey) }
        }
    }
}