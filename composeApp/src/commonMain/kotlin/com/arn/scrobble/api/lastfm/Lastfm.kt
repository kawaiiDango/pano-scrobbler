package com.arn.scrobble.api.lastfm

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getPageResult
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.api.Requesters.parseJsonBody
import com.arn.scrobble.api.Requesters.postResult
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleResult
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.UserCached.Companion.toUserCached
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.charts.ListeningActivity
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.cacheStrategy
import com.arn.scrobble.utils.Stuff.setMidnight
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.parametersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Calendar
import java.util.TreeMap

open class LastFm(userAccount: UserAccountSerializable) : Scrobblable(userAccount) {
    class CookiesInvalidatedException :
        IllegalStateException("cookies invalidated, please re-login")

    protected open val apiKey = Requesters.lastfmUnauthedRequester.apiKey
    protected open val apiSecret = Requesters.lastfmUnauthedRequester.apiSecret

    private val apiRoot = when (userAccount.type) {
        AccountType.LASTFM -> Stuff.LASTFM_API_ROOT
        AccountType.LIBREFM -> Stuff.LIBREFM_API_ROOT
        else -> userAccount.apiRoot!!
    }

    val client: HttpClient by lazy {
        Requesters.genericKtorClient.config {
            defaultRequest {
                url(apiRoot)
            }
        }
    }

    override suspend fun updateNowPlaying(scrobbleData: ScrobbleData): Result<ScrobbleResult> {
        val params = mutableMapOf(
            "method" to "track.updateNowPlaying",
            "artist" to scrobbleData.artist,
            "track" to scrobbleData.track,
            "duration" to scrobbleData.safeDuration()?.div(1000)?.toString(),
            "album" to scrobbleData.album,
            "trackNumber" to scrobbleData.trackNumber?.toString(),
            "albumArtist" to scrobbleData.albumArtist,
            "sk" to userAccount.authKey,
            "api_key" to apiKey,
            "format" to "json"
        )

        return client.postResult<NowPlayingResponse> {
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
        }.map { ScrobbleResult(it.nowplaying.ignoredMessage.code != 0) }
    }

    override suspend fun scrobble(scrobbleData: ScrobbleData): Result<ScrobbleResult> {
        val params = mutableMapOf(
            "method" to "track.scrobble",
            "artist" to scrobbleData.artist,
            "track" to scrobbleData.track,
            "duration" to scrobbleData.safeDuration()?.div(1000)?.toString(),
            "album" to scrobbleData.album,
            "trackNumber" to scrobbleData.trackNumber?.toString(),
            "albumArtist" to scrobbleData.albumArtist,
            "timestamp" to (scrobbleData.timestamp / 1000).toString(),
            "chosenByUser" to "1",
            "sk" to userAccount.authKey,
            "api_key" to apiKey,
            "format" to "json"
        )

        return client.postResult<ScrobbleResponse> {
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
        }.map { ScrobbleResult(it.scrobbles.attr.ignored > 0) }
    }

    override suspend fun scrobble(scrobbleDatas: List<ScrobbleData>): Result<ScrobbleResult> {
        val params = mutableMapOf<String, String?>(
            "method" to "track.scrobble",
            "sk" to userAccount.authKey,
            "api_key" to apiKey,
            "format" to "json"
        )

        scrobbleDatas.forEachIndexed { index, scrobbleData ->
            params["artist[$index]"] = scrobbleData.artist
            params["track[$index]"] = scrobbleData.track
            params["duration[$index]"] = scrobbleData.safeDuration()?.div(1000)?.toString()
            params["album[$index]"] = scrobbleData.album
            params["trackNumber[$index]"] = scrobbleData.trackNumber?.toString()
            params["albumArtist[$index]"] = scrobbleData.albumArtist
            params["timestamp[$index]"] = (scrobbleData.timestamp / 1000).toString()
            params["chosenByUser[$index]"] = "1"
        }

        return client.postResult<ScrobbleResponse> {
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
        }.map { ScrobbleResult(it.scrobbles.attr.ignored > 0) }
    }

    override suspend fun loveOrUnlove(track: Track, love: Boolean): Result<ScrobbleResult> {
        val params = mutableMapOf(
            "method" to if (love) "track.love" else "track.unlove",
            "artist" to track.artist.name,
            "track" to track.name,
            "sk" to userAccount.authKey,
            "api_key" to apiKey,
            "format" to "json"
        )

        return client.postResult<String> {
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
        }.map { ScrobbleResult(false) }
    }

    override suspend fun delete(track: Track) = runCatching {
        LastfmUnscrobbler.unscrobble(track, userAccount.user.name)
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
        val _from = if (to > 0 && from <= 0) 1 else (from / 1000)
        val _to =
            if (from > 0 && to <= 0) (System.currentTimeMillis() / 1000) else (to / 1000)

        return client.getPageResult<RecentTracksResponse, Track>(
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
            it.fixTracksPage(includeNowPlaying)
        }
    }


    override suspend fun getLoves(
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
    ): Result<PageResult<Track>> {
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
        limit: Int,
    ): Result<PageResult<User>> {
        val cacheStrategy = if (cached) CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
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
            if (it is ApiException && it.code == 6) {
                PageResult(
                    PageAttr(
                        page = 1,
                        totalPages = 1,
                        total = 0,
                    ),
                    emptyList(),
                )
            } else throw it
        }
    }

    override suspend fun loadDrawerData(username: String): Result<DrawerData>? {
        return userGetInfo(username).map { user ->
            val cal = Calendar.getInstance()
            cal.setMidnight()

            val scrobblesToday = getRecents(
                username = username,
                page = 1,
                limit = 1,
                cached = false,
                from = cal.timeInMillis,
                to = System.currentTimeMillis()
            ).map {
                it.attr.total ?: 0
            }

            DrawerData(
                scrobblesToday = scrobblesToday.getOrDefault(0),
                scrobblesTotal = user.playcount ?: 0,
                artistCount = user.artist_count ?: 0,
                albumCount = user.album_count ?: 0,
                trackCount = user.track_count ?: 0,
                profilePicUrl = user.webp300
            )
        }
    }

    override suspend fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        username: String,
        cacheStrategy: CacheStrategy,
        limit: Int,
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

        val pr = if (timePeriod.lastfmPeriod != null) {
            request.parameter("period", timePeriod.lastfmPeriod.value)
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
                )
            }
        }
        return pr
    }


    override suspend fun getListeningActivity(
        timePeriod: TimePeriod, user: UserCached?, cacheStrategy: CacheStrategy,
    ): ListeningActivity {
        val username = user?.name ?: userAccount.user.name
        val registeredTime = user?.registeredTime ?: userAccount.user.registeredTime

        val listeningActivity = TimePeriodsGenerator.forListeningActivity(
            timePeriod, registeredTime
        )

        val timePeriodsToCounts = listeningActivity.timePeriodsToCounts.mapValues { (it, _) ->
            if (it.start < System.currentTimeMillis()) {
                getRecents(
                    username = username,
                    from = it.start,
                    to = it.end,
                    page = 1,
                    limit = 1
                ).map { it.attr.totalPages }.getOrDefault(0)
            } else
                0
        }

        return listeningActivity.copy(timePeriodsToCounts = timePeriodsToCounts)
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
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
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
            setBody(FormDataContent(toFormParametersWithSig(params, apiSecret)))
        }
    }

    suspend fun getUserTagsFor(entry: MusicEntry) = client.getResult<TagsResponse> {
        when (entry) {
            is Artist -> {
                parameter("method", "artist.getTags")
                doubleEncodePlusParam("artist", entry.name)
            }

            is Album -> {
                parameter("method", "album.getTags")
                doubleEncodePlusParam("artist", entry.artist!!.name)
                doubleEncodePlusParam("album", entry.name)
            }

            is Track -> {
                parameter("method", "track.getTags")
                doubleEncodePlusParam("artist", entry.artist.name)
                doubleEncodePlusParam("track", entry.name)
            }
        }

        parameter("autocorrect", 1)
        parameter("format", "json")
        parameter("api_key", apiKey)
        parameter("sk", userAccount.authKey)
    }

    suspend fun userGetInfo(
        username: String,
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
            // does not have the double encoding bug for some reason
            parameter("artist", track.artist.name)
            parameter("track", track.name)
            parameter("user", username)
            parameter("autocorrect", if (autocorrect) 1 else 0)
            parameter("limit", limit)
            parameter("page", page)
            parameter("format", "json")
            parameter("api_key", apiKey)
            parameter("sk", userAccount.authKey)
        }.map { it.fixTracksPage() }

    private fun PageResult<Track>.fixTracksPage(includeNowPlaying: Boolean = false): PageResult<Track> {
        val newEntries = entries.mapNotNull {
            if (!includeNowPlaying && it.isNowPlaying)
                null
            else if (it.album?.name == "")
                it.copy(album = null)
            else
            // copy the album art
                it.copyImageToAlbum()
        }

        return copy(entries = newEntries)
    }

    // lfm double encoding bug
    private fun HttpRequestBuilder.doubleEncodePlusParam(
        key: String,
        value: String?
    ) {
        parameter(key, value?.replace("+", "%2B"))
    }

    private object LastfmUnscrobbler {
        suspend fun unscrobble(track: Track, username: String): Unit =
            lock.withLock { // does this fix the csrf invalidation problem?
                val csrfToken =
                    cookieStorage.get(Url(URL_USER)).find { it.name == COOKIE_CSRFTOKEN }?.value

                val parameters = Parameters.build {
                    append(
                        FIELD_CSRFTOKEN,
                        csrfToken ?: throw CookiesInvalidatedException()
                    )
                    append(FIELD_ARTIST, track.artist.name)
                    append(FIELD_TRACK, track.name)
                    append(
                        FIELD_TIMESTAMP,
                        track.date?.div(1000)?.toString() ?: throw IllegalStateException("no date")
                    )
                    append(FIELD_AJAX, "1")
                }

                val url = "$URL_USER$username/library/delete"

                val response = unscrobbleClient.submitForm(url, parameters) {
                    header(HttpHeaders.Referrer, URL_USER + username)
                }

                if (response.status == HttpStatusCode.OK) {
                    val success = response.parseJsonBody<DeleteScrobbleResponse>().result

                    if (!success)
                        throw IllegalStateException("LastfmUnscrobbler: error unscrobbling")
                } else if (response.status == HttpStatusCode.Forbidden) {
                    cookieStorage.clear()
                    throw CookiesInvalidatedException()
                } else {
                    throw ResponseException(
                        response,
                        "LastfmUnscrobbler: error unscrobbling: " + response.status.value
                    )
                }

                // add a random delay to prevent 406 error
                delay((1000L..10000L).random())
            }

        private const val COOKIE_CSRFTOKEN = "csrftoken"
        private const val COOKIE_SESSIONID = "sessionid"

        private const val URL_USER = "https://www.last.fm/user/"

        private const val FIELD_CSRFTOKEN = "csrfmiddlewaretoken"

        private const val FIELD_ARTIST = "artist_name"
        private const val FIELD_TRACK = "track_name"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_AJAX = "ajax"

        private val lock by lazy { Mutex() }

        val cookieStorage by lazy { CookiesDatastore() }
        private val unscrobbleClient by lazy {
            Requesters.genericKtorClient.config {
                install(HttpCookies) {
                    storage = cookieStorage
                }
            }
        }
    }

    companion object {

        private fun String.md5(): String {
            val md = MessageDigest.getInstance("MD5")
            return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
        }

        fun toFormParametersWithSig(
            params: Map<String, String?>, secret: String,
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

        suspend fun authAndGetSession(
            userAccountTemp: UserAccountTemp,
            cookies: List<Cookie> = emptyList()
        ): Result<Session> {
            val apiKey = if (userAccountTemp.type == AccountType.LASTFM)
                Requesters.lastfmUnauthedRequester.apiKey
            else
                Stuff.LIBREFM_KEY

            val apiSecret = if (userAccountTemp.type == AccountType.LASTFM)
                Requesters.lastfmUnauthedRequester.apiSecret
            else
                Stuff.LIBREFM_KEY

            val apiRoot = userAccountTemp.apiRoot ?: Stuff.LASTFM_API_ROOT

            val session = Requesters.lastfmUnauthedRequester.getSession(
                apiRoot,
                apiKey,
                apiSecret,
                userAccountTemp.authKey
            ).onSuccess {
                // store cookies

                val lastfmUrl = Url(Stuff.LASTFM_URL)

                cookies.forEach { cookie ->
                    LastfmUnscrobbler.cookieStorage.addCookie(lastfmUrl, cookie)
                }

                // get user info
                Requesters.genericKtorClient.getResult<UserGetInfoResponse> {
                    url(userAccountTemp.apiRoot ?: Stuff.LASTFM_API_ROOT)
                    parameter("method", "user.getInfo")
                    parameter("format", "json")
                    parameter("api_key", apiKey)
                    parameter("sk", it.key)
                    parameter("user", it.name)
                }.map { it.user }
                    .onSuccess { user ->
                        // save account
                        val account = UserAccountSerializable(
                            userAccountTemp.type,
                            user.toUserCached(),
                            it.key,
                            userAccountTemp.apiRoot,
                        )

                        Scrobblables.add(account)
                    }
            }

            return session
        }
    }
}
