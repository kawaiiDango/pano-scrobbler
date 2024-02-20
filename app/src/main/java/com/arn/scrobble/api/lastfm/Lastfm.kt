package com.arn.scrobble.api.lastfm

import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.DrawerData
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.CacheMarkerInterceptor
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getPageResult
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.friends.UserCached.Companion.toUserCached
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.cacheStrategy
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.Stuff.setMidnight
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.http.parametersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import okhttp3.Cache
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Calendar
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

open class LastFm(userAccount: UserAccountSerializable) : Scrobblable(userAccount) {
    open val apiKey: String = Stuff.LAST_KEY
    open val secret: String = Stuff.LAST_SECRET

    private val apiRoot = when (userAccount.type) {
        AccountType.LASTFM -> Stuff.LASTFM_API_ROOT
        AccountType.LIBREFM -> Stuff.LIBREFM_API_ROOT
        else -> userAccount.apiRoot!!
    }

    val client: HttpClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                    if (!Stuff.isRunningInTest)
                        cache(Cache(File(App.context.cacheDir, "ktor"), 10 * 1024 * 1024))
                    readTimeout(Stuff.READ_TIMEOUT_SECS, TimeUnit.SECONDS)
                }
                addNetworkInterceptor(CacheInterceptor(LastfmExpirationPolicy()))
                addInterceptor(CacheMarkerInterceptor())
            }

            install(ContentNegotiation) {
                json(Stuff.myJson)
            }

            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.ANDROID
                    level = LogLevel.ALL
                }
            }

            defaultRequest {
                url(apiRoot)
            }

            install(HttpCallValidator) {
                validateResponse { response ->
                    if (response.status.isSuccess()) return@validateResponse
                    val fmException = response.body<FmErrorResponse>()
                        .let { FmException(it.code, it.message) }

                    throw fmException
                }
            }

//            expectSuccess = true
            // https://youtrack.jetbrains.com/issue/KTOR-4225
        }
    }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData): Result<ScrobbleIgnored> {
        val params = mutableMapOf(
            "method" to "track.updateNowPlaying",
            "artist" to scrobbleData.artist,
            "track" to scrobbleData.track,
            "duration" to scrobbleData.duration?.toString(),
            "album" to scrobbleData.album,
            "trackNumber" to scrobbleData.trackNumber?.toString(),
            "mbid" to scrobbleData.mbid,
            "albumArtist" to scrobbleData.albumArtist,
            "sk" to userAccount.authKey,
            "api_key" to apiKey,
            "format" to "json"
        )

        return client.postResult<NowPlayingResponse> {
            setBody(FormDataContent(toFormParametersWithSig(params, secret)))
        }.map { ScrobbleIgnored(it.nowplaying.ignoredMessage.code != 0) }
    }

    override suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleIgnored> {
        val params = mutableMapOf(
            "method" to "track.scrobble",
            "artist" to scrobbleData.artist,
            "track" to scrobbleData.track,
            "duration" to scrobbleData.duration?.toString(),
            "album" to scrobbleData.album,
            "trackNumber" to scrobbleData.trackNumber?.toString(),
            "mbid" to scrobbleData.mbid,
            "albumArtist" to scrobbleData.albumArtist,
            "timestamp" to scrobbleData.timestamp.toString(),
            "chosenByUser" to "1",
            "sk" to userAccount.authKey,
            "api_key" to apiKey,
            "format" to "json"
        )

        return client.postResult<ScrobbleResponse> {
            setBody(FormDataContent(toFormParametersWithSig(params, secret)))
        }.map { ScrobbleIgnored(it.scrobbles.attr.ignored > 0) }
    }

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>): Result<ScrobbleIgnored> {
        val params = mutableMapOf<String, String?>(
            "method" to "track.scrobble",
            "sk" to userAccount.authKey,
            "api_key" to apiKey,
            "format" to "json"
        )

        scrobbleDatas.forEachIndexed { index, scrobbleData ->
            params["artist[$index]"] = scrobbleData.artist
            params["track[$index]"] = scrobbleData.track
            params["duration[$index]"] = scrobbleData.duration?.toString()
            params["album[$index]"] = scrobbleData.album
            params["trackNumber[$index]"] = scrobbleData.trackNumber?.toString()
            params["mbid[$index]"] = scrobbleData.mbid
            params["albumArtist[$index]"] = scrobbleData.albumArtist
            params["timestamp[$index]"] = scrobbleData.timestamp.toString()
            params["chosenByUser[$index]"] = "1"
        }

        return client.postResult<ScrobbleResponse> {
            setBody(FormDataContent(toFormParametersWithSig(params, secret)))
        }.map { ScrobbleIgnored(it.scrobbles.attr.ignored > 0) }
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean): Result<ScrobbleIgnored> {
        val params = mutableMapOf(
            "method" to if (love) "track.love" else "track.unlove",
            "artist" to track.artist.name,
            "track" to track.name,
            "sk" to userAccount.authKey,
            "api_key" to apiKey,
            "format" to "json"
        )

        return client.postResult<String> {
            setBody(FormDataContent(toFormParametersWithSig(params, secret)))
        }.map { ScrobbleIgnored(false) }
    }

    override suspend fun delete(track: Track) = runCatching {
        LastfmUnscrobbler.unscrobble(track, userAccount.user.name)
    }

    override suspend fun getRecents(
        page: Int,
        username: String,
        cached: Boolean,
        from: Int,
        to: Int,
        includeNowPlaying: Boolean,
        limit: Int,
    ): Result<PageResult<Track>> {
        return coroutineScope {
            val cacheStrategy =
                if (!Stuff.isOnline && cached) CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
                else if (cached) CacheStrategy.CACHE_FIRST
                else CacheStrategy.NETWORK_ONLY

            val _from = if (to > 0 && from <= 0) 1 else from
            val _to = if (from > 0 && to <= 0) (System.currentTimeMillis() / 1000).toInt() else to

            client.getPageResult<RecentTracksResponse, Track>(
                transform = { it.recenttracks },
            ) {
                parameter("method", "user.getRecentTracks")
                parameter("user", username)
                parameter("limit", limit)
                parameter("page", page)

                if (_from > 0 && _to > 0) {
                    parameter("from", _from)
                    parameter("to", _to)
                }

                parameter("extended", 1)
                parameter("format", "json")
                parameter("api_key", apiKey)
                parameter("sk", userAccount.authKey)
                cacheStrategy(cacheStrategy)
            }.map {
                if (!includeNowPlaying) it.copy(entries = it.entries.filterNot { it.isNowPlaying })
                else it
            }

        }
    }


    override suspend fun getLoves(
        page: Int,
        username: String,
        cached: Boolean,
        limit: Int,
    ): Result<PageResult<Track>> {
        val cacheStrategy = if (!Stuff.isOnline && cached) CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
        else if (cached) CacheStrategy.CACHE_FIRST
        else CacheStrategy.NETWORK_ONLY

        Stuff.log(this::getLoves.name + " " + page)

        return client.getPageResult<LovedTracksResponse, Track>(transform = { it.lovedtracks }) {
            parameter("method", "user.getLovedTracks")
            parameter("user", username)
            parameter("limit", limit)
            parameter("page", page)
            parameter("extended", 1)
            parameter("format", "json")
            parameter("api_key", apiKey)
            parameter("sk", userAccount.authKey)
            cacheStrategy(cacheStrategy)
        }.map {
            it.copy(entries = it.entries.map { it.copy(userloved = true) })
        }
    }

    override suspend fun getFriends(
        page: Int,
        username: String,
        cached: Boolean,
        limit: Int
    ): Result<PageResult<User>> {
        val cacheStrategy = if (!Stuff.isOnline && cached) CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
        else if (cached) CacheStrategy.CACHE_FIRST
        else CacheStrategy.NETWORK_ONLY

        return client.getPageResult<FriendsResponse, User>(transform = { it.friends }) {
            parameter("method", "user.getFriends")
            parameter("user", username)
            parameter("format", "json")
            parameter("api_key", apiKey)
            parameter("sk", userAccount.authKey)
            parameter("page", page)
            parameter("limit", limit)
            cacheStrategy(cacheStrategy)
        }.recoverCatching {
            // {"message":"no such page","error":6}
            if (it is FmException && it.code == 6) {
                PageResult(
                    PageAttr(
                        page = 1,
                        totalPages = 1,
                        total = 0,
                    ),
                    emptyList(),
                    false
                )
            } else throw it
        }
    }

    override suspend fun loadDrawerData(username: String): DrawerData? {
        Stuff.log(this::loadDrawerData.name)

        val user = userGetInfo(username).getOrNull()
        val isSelf = username == userAccount.user.name
        val prefs = App.prefs

        val cal = Calendar.getInstance()
        cal.setMidnight()

        val scrobblesToday = getRecents(
            username = username,
            page = 1,
            limit = 1,
            cached = false,
            from = (cal.timeInMillis / 1000).toInt(),
            to = (System.currentTimeMillis() / 1000).toInt()
        ).getOrNull()?.attr?.total

        val drawerData = DrawerData(
            scrobblesToday = scrobblesToday ?: 0,
            scrobblesTotal = user?.playcount ?: 0,
            artistCount = user?.artist_count ?: 0,
            albumCount = user?.album_count ?: 0,
            trackCount = user?.track_count ?: 0,
            profilePicUrl = user?.webp300
        ).also {
            if (isSelf) prefs.drawerDataCached = it
        }

        return drawerData
    }

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int
    ): Result<PageResult<out MusicEntry>> {

        val request = HttpRequestBuilder().apply {
            parameter("user", username)
            if (limit > 0)
                parameter("limit", limit)
            parameter("format", "json")
            parameter("api_key", apiKey)
            parameter("sk", userAccount.authKey)
            cacheStrategy(cacheStrategy)
        }

        val pr = if (timePeriod.period != null) {
            request.parameter("period", timePeriod.period.value)
            request.parameter("page", page)

            when (type) {
                Stuff.TYPE_ARTISTS -> return client.getPageResult<TopArtistsResponse, Artist>(
                    transform = { it.topartists },
                ) {
                    takeFrom(request)
                    parameter("method", "user.getTopArtists")
                }

                Stuff.TYPE_ALBUMS -> client.getPageResult<TopAlbumsResponse, Album>(
                    transform = { it.topalbums },
                ) {
                    takeFrom(request)
                    parameter("method", "user.getTopAlbums")
                }

                else -> client.getPageResult<TopTracksResponse, Track>(
                    transform = { it.toptracks },
                ) {
                    takeFrom(request)
                    parameter("method", "user.getTopTracks")
                }
            }
        } else {
            val fromStr = (timePeriod.start / 1000).toString()
            val toStr = (timePeriod.end / 1000).toString()

            request.parameter("from", fromStr)
            request.parameter("to", toStr)

            when (type) {
                Stuff.TYPE_ARTISTS -> client.getResult<WeeklyArtistChartResponse> {
                    takeFrom(request)
                    parameter("method", "user.getWeeklyArtistChart")
                }.map { it.weeklyartistchart.artist }

                Stuff.TYPE_ALBUMS -> client.getResult<WeeklyAlbumChartResponse> {
                    takeFrom(request)
                    parameter("method", "user.getWeeklyAlbumChart")
                }.map { it.weeklyalbumchart.album }

                else -> client.getResult<WeeklyTrackChartResponse> {
                    takeFrom(request)
                    parameter("method", "user.getWeeklyTrackChart")
                }.map { it.weeklytrackchart.track }
            }.map {

                PageResult(
                    PageAttr(
                        page = 1,
                        totalPages = 1,
                        total = it.size,
                    ),
                    it,
                    false
                )
            }
        }
        return pr
    }


    override suspend fun getListeningActivity(
        timePeriod: TimePeriod, user: UserCached?, cacheStrategy: CacheStrategy
    ): Map<TimePeriod, Int> {
        val username = user?.name ?: userAccount.user.name
        val registeredTime = user?.registeredTime ?: userAccount.user.registeredTime

        val timePeriods = TimePeriodsGenerator.getScrobblingActivityPeriods(
            timePeriod, registeredTime
        )

        val periodCountsMap = mutableMapOf<TimePeriod, Int>()
        timePeriods.forEach { periodCountsMap[it] = 0 }

        supervisorScope {
            timePeriods.mapConcurrently(5) {
                if (it.start < System.currentTimeMillis()) {
                    kotlin.runCatching {
                        val resp = getRecents(
                            username = username,
                            from = (it.start / 1000).toInt(),
                            to = (it.end / 1000).toInt(),
                            page = 1,
                            limit = 1
                        )
                        periodCountsMap[it] = resp.map { it.attr.totalPages }.getOrDefault(0)
                    }
                }
            }
        }
        return periodCountsMap
    }


    suspend fun addUserTagsFor(entry: MusicEntry, tags: String): Result<String> {
        val params = mutableMapOf(
            "tags" to tags,
            "format" to "json",
            "api_key" to apiKey,
            "sk" to userAccount.authKey,
        )
        when (entry) {
            is Artist -> {
                params["method"] = "artist.addTags"
                params["artist"] = entry.name
            }

            is Album -> {
                params["method"] = "album.addTags"
                params["artist"] = entry.artist!!.name
                params["album"] = entry.name
            }

            is Track -> {
                params["method"] = "track.addTags"
                params["artist"] = entry.artist.name
                params["track"] = entry.name
            }
        }

        return client.postResult<String> {
            setBody(FormDataContent(toFormParametersWithSig(params, secret)))
        }
    }

    suspend fun removeUserTagFor(entry: MusicEntry, tag: String): Result<String> {
        val params = mutableMapOf(
            "tag" to tag,
            "format" to "json",
            "api_key" to apiKey,
            "sk" to userAccount.authKey,
        )
        when (entry) {
            is Artist -> {
                params["method"] = "artist.removeTag"
                params["artist"] = entry.name
            }

            is Album -> {
                params["method"] = "album.removeTag"
                params["artist"] = entry.artist!!.name
                params["album"] = entry.name
            }

            is Track -> {
                params["method"] = "track.removeTag"
                params["artist"] = entry.artist.name
                params["track"] = entry.name
            }
        }

        return client.postResult<String> {
            setBody(FormDataContent(toFormParametersWithSig(params, secret)))
        }
    }

    suspend fun getUserTagsFor(entry: MusicEntry) = client.getResult<TagsResponse> {
        when (entry) {
            is Artist -> {
                parameter("method", "artist.getTags")
                parameter("artist", entry.name)
            }

            is Album -> {
                parameter("method", "album.getTags")
                parameter("artist", entry.artist!!.name)
                parameter("album", entry.name)
            }

            is Track -> {
                parameter("method", "track.getTags")
                parameter("artist", entry.artist.name)
                parameter("track", entry.name)
            }
        }

        parameter("autocorrect", 1)
        parameter("format", "json")
        parameter("api_key", apiKey)
        parameter("sk", userAccount.authKey)
    }

    suspend fun userGetInfo(
        username: String
    ) = client.getResult<UserGetInfoResponse> {
        parameter("method", "user.getInfo")
        parameter("user", username)
        parameter("format", "json")
        parameter("api_key", apiKey)
        parameter("sk", userAccount.authKey)
    }.map { it.user }


    suspend fun userGetTopTags(
        username: String = userAccount.user.name,
        limit: Int? = null,
    ) = client.getResult<TagsResponse> {
        parameter("method", "user.getTopTags")
        parameter("user", username)
        parameter("limit", limit)
        parameter("format", "json")
        parameter("api_key", apiKey)
        parameter("sk", userAccount.authKey)
    }

    suspend fun userGetTrackScrobbles(
        track: Track,
        page: Int? = null,
        username: String = userAccount.user.name,
        limit: Int? = null,
        autocorrect: Boolean = true,
    ) =
        client.getPageResult<TrackScrobblesResponse, Track>(transform = { it.trackscrobbles }) {
            parameter("method", "user.getTrackScrobbles")
            parameter("artist", track.artist.name)
            parameter("track", track.name)
            parameter("user", username)
            parameter("autocorrect", if (autocorrect) 1 else 0)
            parameter("limit", limit)
            parameter("page", page)
            parameter("format", "json")
            parameter("api_key", apiKey)
            parameter("sk", userAccount.authKey)
        }


    companion object {

        private fun String.md5(): String {
            val md = MessageDigest.getInstance("MD5")
            return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
        }

        fun toFormParametersWithSig(
            params: Map<String, String?>, secret: String
        ): Parameters {
            val paramsOrdered =
                TreeMap<String, String>(params.filter { !it.value.isNullOrEmpty() && it.key.lowercase() != "format" })
            val b = StringBuilder(100)

            paramsOrdered.forEach { (key, value) ->
                b.append(key)
                b.append(value)
            }

            b.append(secret)
            paramsOrdered["api_sig"] = b.toString().md5()
            paramsOrdered["format"] = "json"

            return parametersOf(paramsOrdered.mapValues { (k, v) -> listOf(v) })
        }

        suspend fun authAndGetSession(userAccountTemp: UserAccountTemp): Result<Session> {
            val apiKey = if (userAccountTemp.type == AccountType.LASTFM) Stuff.LAST_KEY
            else Stuff.LIBREFM_KEY

            val apiSecret = if (userAccountTemp.type == AccountType.LASTFM) Stuff.LAST_SECRET
            else Stuff.LIBREFM_KEY

            val client = Requesters.genericKtorClient

            val params = mutableMapOf<String, String?>(
                "method" to "auth.getSession",
                "api_key" to apiKey,
                "token" to userAccountTemp.authKey,
            )

            val session = client.postResult<SessionResponse>(
                userAccountTemp.apiRoot ?: Stuff.LASTFM_API_ROOT,
            ) {
                setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
            }.map { it.session }
            session.onSuccess { session ->
                // get user info
                client.getResult<UserGetInfoResponse> {
                    url(userAccountTemp.apiRoot ?: Stuff.LASTFM_API_ROOT)
                    parameter("method", "user.getInfo")
                    parameter("format", "json")
                    parameter("api_key", apiKey)
                    parameter("sk", session.key)
                    parameter("user", session.name)
                }.map { it.user }
                    .onSuccess { user ->
                        // save account
                        val account = UserAccountSerializable(
                            userAccountTemp.type,
                            user.toUserCached(),
                            session.key,
                            userAccountTemp.apiRoot,
                        )

                        Scrobblables.add(account)
                    }
            }

            return session
        }
    }
}
