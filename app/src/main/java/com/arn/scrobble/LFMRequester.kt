package com.arn.scrobble

import android.content.Context
import android.content.Intent
import android.util.LruCache
import android.webkit.URLUtil
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.Stuff.ignoreSslErrors
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.db.*
import com.arn.scrobble.db.CachedAlbum.Companion.toAlbum
import com.arn.scrobble.db.CachedAlbum.Companion.toCachedAlbum
import com.arn.scrobble.db.CachedArtist.Companion.toArtist
import com.arn.scrobble.db.CachedArtist.Companion.toCachedArtist
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.db.CachedTrack.Companion.toTrack
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.Lastfm
import com.arn.scrobble.scrobbleable.Scrobblable
import com.arn.scrobble.search.SearchResultsExperimentAdapter
import com.arn.scrobble.search.SearchVM
import com.arn.scrobble.ui.UiUtils.toast
import de.umass.lastfm.*
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.io.InterruptedIOException
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow


/**
 * Created by arn on 18-03-2017.
 */

class LFMRequester(
    context: Context,
    private val scope: CoroutineScope,
    private val liveData: MutableLiveData<*>? = null,
    private val errorLiveData: MutableLiveData<Throwable>? = null,
) {
    private var contextWr = WeakReference(context.applicationContext)
    private val context
        get() = contextWr.get()!!
    private val prefs by lazy { MainPrefs(this.context) }
    private lateinit var job: Job
    private var launchExecOnSet = true

    private val coExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        when (throwable) {
            is CancellationException,
            is InterruptedIOException,
            is InterruptedException -> {

            }
            else -> {
                if (errorLiveData != null)
                    errorLiveData.postValue(throwable)
                else {
                    Timber.e(throwable)
                    if (contextWr.get() != null && BuildConfig.DEBUG) {
                        scope.launch(Dispatchers.Main) {
                            context.toast("err: " + throwable.message)
                        }
                    }
                }
            }
        }
    }

    private var toExec: (suspend () -> Any?) = {}
        set(fn) {
            field = fn

            if (!launchExecOnSet)
                return

            job = scope.launch(coExceptionHandler) {
                val res = withContext(Dispatchers.IO) {
                    fn()
                }
                withContext(Dispatchers.Main) {
                    liveData?.value = res
                }
            }
        }
    private val lastfmSessKey by lazy { prefs.lastfmSessKey }
    val lastfmSession by lazy {
        Session.createSession(
            Stuff.LAST_KEY,
            Stuff.LAST_SECRET,
            lastfmSessKey
        )!!
    }

    private val lastfmUsername by lazy { prefs.lastfmUsername }

    private fun checkSession(usernamep: String? = null) {
        if (usernamep == null && lastfmSessKey == null)
            throw Exception("Login required")
    }

    fun getRecents(
        page: Int,
        usernamep: String?,
        cached: Boolean = false,
        from: Long = -1,
        to: Long = -1,
        includeNowPlaying: Boolean = false,
        doDeltaIndex: Boolean = false,
        limit: Int = 600,
    ) {
        toExec = {
            checkSession(usernamep)
            lastfmSession.cacheStrategy =
                if (!Stuff.isOnline && cached)
                    Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
                else if (cached)
                    Caller.CacheStrategy.CACHE_FIRST
                else
                    Caller.CacheStrategy.NETWORK_ONLY

            val _from = if (to > 0L && from <= 0L) 1000L else from
            val _to = if (from > 0L && to <= 0L) System.currentTimeMillis() else to

            val pr = User.getRecentTracks(
                usernamep, page, limit, true, _from / 1000, _to / 1000,
                lastfmSession
            )

            // remove np
            pr?.pageResults?.firstOrNull()?.let {
                if (!includeNowPlaying && it.isNowPlaying)
                    pr.pageResults.remove(it)
            }

            if (doDeltaIndex && !lastfmSession.result.isFromCache && page == 1 && usernamep == null) {
                val firstTrack = pr?.pageResults?.find { it.playedWhen != null }
                val indexedScrobbleTime = prefs.lastMaxIndexedScrobbleTime
                if (firstTrack != null && indexedScrobbleTime != null && firstTrack.playedWhen.time > indexedScrobbleTime)
                    LFMRequester(context, scope).runDeltaIndex(pr)
            }

            // load uncached version
            if (lastfmSession.result.isFromCache && lastfmSession.cacheStrategy == Caller.CacheStrategy.CACHE_FIRST) {
                LFMRequester(context, scope, liveData).getRecents(
                    page,
                    usernamep,
                    false,
                    from,
                    to,
                    includeNowPlaying,
                    doDeltaIndex,
                    limit
                )
            }
            pr
        }
    }

    fun getLoves(page: Int, usernamep: String?, cached: Boolean = false, limit: Int = 50) {
        toExec = {
            checkSession(usernamep)
            lastfmSession.cacheStrategy =
                if (!Stuff.isOnline && cached)
                    Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
                else if (cached)
                    Caller.CacheStrategy.CACHE_FIRST
                else
                    Caller.CacheStrategy.NETWORK_ONLY

            Stuff.log(this::getLoves.name + " " + page)
            val pr = User.getLovedTracks(
                usernamep,
                limit,
                page,
                lastfmSession
            )
            pr.pageResults.forEach {
                it.isLoved = true
                it.imageUrlsMap = null
            }
            // load uncached version
            if (lastfmSession.result.isFromCache && lastfmSession.cacheStrategy == Caller.CacheStrategy.CACHE_FIRST) {
                LFMRequester(context, scope, liveData).getLoves(page, usernamep, false, limit)
            }
            pr
        }
    }

    fun getTrackScrobbles(track: Track, page: Int, usernamep: String?, limit: Int = 50) {
        toExec = {
            checkSession(usernamep)
            User.getTrackScrobbles(track.artist, track.name, usernamep, page, limit, lastfmSession)
        }
    }

    fun getTrackFirstScrobble(pr: PaginatedResult<Track>?, usernamep: String?, limit: Int = 50) {
        toExec = {
            val track = pr?.pageResults?.firstOrNull()
            var firstScrobbleDate: Date? = null

            if (track != null) {
                firstScrobbleDate = if (pr.total > limit)
                    User.getTrackScrobbles(
                        track.artist,
                        track.name,
                        usernamep,
                        pr.total,
                        1,
                        lastfmSession
                    )
                        .pageResults.firstOrNull()?.playedWhen
                else
                    pr.pageResults.lastOrNull()?.playedWhen
            }
            firstScrobbleDate
        }
    }

    fun getSimilarTracks(artist: String, track: String) {
        toExec = {
            Track.getSimilar(artist, track, Stuff.LAST_KEY, 100)
        }
    }

    fun getSimilarArtists(artist: String) {
        toExec = {
            Artist.getSimilar(artist, 100, Stuff.LAST_KEY)
        }
    }

    fun getArtistTopTracks(artist: String) {
        toExec = {
            Artist.getTopTracks(artist, 100, Stuff.LAST_KEY)
        }
    }

    fun getArtistTopAlbums(artist: String) {
        toExec = {
            Artist.getTopAlbums(artist, 100, Stuff.LAST_KEY)
        }
    }

    fun getFriendsRecents(username: String) {
        toExec = {
            lastfmSession.cacheStrategy =
                if (Stuff.isOnline) Caller.CacheStrategy.NETWORK_ONLY else Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
            Pair(
                username,
                User.getRecentTracks(
                    username,
                    1,
                    1,
                    false,
                    -1,
                    -1,
                    lastfmSession
                )
            )
        }
    }

    fun getDrawerInfo() {
        toExec = {
            checkSession()
            Stuff.log(this::getDrawerInfo.name)
            val profile = User.getInfo(lastfmSession)
            val cal = Calendar.getInstance()
            cal.setMidnight()
            val recents = User.getRecentTracks(
                null, 1, 1,
                cal.timeInMillis / 1000, System.currentTimeMillis() / 1000, lastfmSession
            )

            if (profile != null && recents != null)
                DrawerData(
                    scrobblesToday = recents.totalPages,
                    scrobblesTotal = profile.playcount,
                    registeredDate = profile.registeredDate?.time ?: 0,
                    profilePicUrl = profile.getWebpImageURL(ImageSize.EXTRALARGE) ?: "",
                ).apply { saveToPref(context) }
            else
                DrawerData.loadFromPref(context)
        }
    }

    fun getFriends(page: Int, usernamep: String?) {
        toExec = {
            Stuff.log(this::getFriends.name + " " + page)
            checkSession(usernamep)
            lastfmSession.cacheStrategy =
                if (Stuff.isOnline) Caller.CacheStrategy.NETWORK_ONLY else Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED

            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            var pr: PaginatedResult<User>
            try {
                pr = User.getFriends(
                    username,
                    page,
                    30,
                    lastfmSession
                )
            } catch (e: NullPointerException) {
                val request = Request.Builder()
                    .url("https://www.last.fm/user/$username/following?page=$page")
                    .build()
                val users = mutableListOf<User>()
                try {
                    okHttpClient.newCall(request).execute()
                        .use { response ->
                            if (!response.isSuccessful)
                                throw IOException("Response code ${response.code}")
                            val body = response.body!!.string()
                            if (body.isEmpty())
                                throw IOException("Empty body")

                            var idx = body.indexOf("<ul class=\"user-list\">", 50000)
                            var idx2: Int
                            if (idx > -1) {
                                do {
                                    idx = body.indexOf("  link-block-target", idx)
                                    if (idx > -1)
                                        idx = body.indexOf(">", idx + 1)
                                    if (idx > -1) {
                                        idx += 1
                                        idx2 = body.indexOf("<", idx)
                                        val uname = body.substring(idx, idx2)
                                        idx = body.indexOf("<img", idx2)
                                        idx = body.indexOf("\"", idx)
                                        idx2 = body.indexOf("\"", idx + 1)
                                        val imageUrl = body.substring(idx + 1, idx2)
                                        val user = User(uname, "https://www.last.fm/user/$uname")
                                        user.imageURL = imageUrl
                                        users.add(user)
                                        idx = idx2
                                    }
                                } while (idx > -1)
                            }
                        }
                } catch (e: Exception) {
                }
                val totalPages = if (users.isEmpty())
                    page
                else
                    10
                pr = PaginatedResult(page, totalPages, users.size, users)
            }
            pr
        }
    }

    fun getCharts(
        type: Int,
        timePeriod: TimePeriod,
        page: Int,
        usernamep: String?,
        cacheStrategy: Caller.CacheStrategy = Caller.CacheStrategy.CACHE_FIRST,
        limit: Int = if (timePeriod.period != null) 50 else -1
    ) {
        toExec = {
            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            lastfmSession.cacheStrategy = cacheStrategy

            val pr: PaginatedResult<out MusicEntry>

            if (timePeriod.period != null) {
                pr = when (type) {
                    Stuff.TYPE_ARTISTS -> User.getTopArtists(
                        username,
                        timePeriod.period,
                        limit,
                        page,
                        lastfmSession
                    )
                    Stuff.TYPE_ALBUMS -> User.getTopAlbums(
                        username,
                        timePeriod.period,
                        limit,
                        page,
                        lastfmSession
                    )
                    else -> User.getTopTracks(
                        username,
                        timePeriod.period,
                        limit,
                        page,
                        lastfmSession
                    )
                }
            } else {
                val fromStr = (timePeriod.start / 1000).toString()
                val toStr = (timePeriod.end / 1000).toString()

                val chart = when (type) {
                    Stuff.TYPE_ARTISTS -> User.getWeeklyArtistChart(
                        username,
                        fromStr,
                        toStr,
                        limit,
                        lastfmSession
                    )
                    Stuff.TYPE_ALBUMS -> User.getWeeklyAlbumChart(
                        username,
                        fromStr,
                        toStr,
                        limit,
                        lastfmSession
                    )
                    else -> User.getWeeklyTrackChart(
                        username,
                        fromStr,
                        toStr,
                        limit,
                        lastfmSession
                    )
                }
                pr = PaginatedResult(1, 1, chart.entries.size, chart.entries)
            }
            pr
        }
    }

    fun getChartsWithStonks(
        type: Int,
        timePeriod: TimePeriod,
        prevTimePeriod: TimePeriod?,
        page: Int,
        usernamep: String?,
        networkOnly: Boolean = false,
        limit: Int = if (timePeriod.period != null) 50 else -1
    ) {
        Stuff.log(this::getChartsWithStonks.name + " $type timePeriod: $timePeriod prevTimePeriod: $prevTimePeriod")

        fun toHashableEntry(entry: MusicEntry): Any = when (type) {
            Stuff.TYPE_ARTISTS -> {
                (entry as Artist).toCachedArtist().apply {
                    userPlayCount = 0
                }
            }
            Stuff.TYPE_ALBUMS -> {
                (entry as Album).toCachedAlbum().apply {
                    userPlayCount = 0
                    largeImageUrl = null
                }
            }
            Stuff.TYPE_TRACKS -> {
                (entry as Track).toCachedTrack().apply {
                    userPlayCount = 0
                    durationSecs = 0
                    isLoved = false
                }
            }
            else -> throw IllegalArgumentException("Unknown type")
        }

        toExec = {
            val prevCharts = if (prevTimePeriod != null)
                execHere<PaginatedResult<out MusicEntry>> {
                    getCharts(
                        type,
                        prevTimePeriod,
                        1,
                        usernamep,
                        Caller.CacheStrategy.CACHE_FIRST_INCLUDE_EXPIRED,
                        -1
                    )
                }
            else
                null

            val prevChartsMap = prevCharts?.pageResults?.associate {
                toHashableEntry(it) to it.rank
            } ?: emptyMap()

            val cacheStrategy = if (networkOnly)
                Caller.CacheStrategy.NETWORK_ONLY
            else if (!Stuff.isOnline)
                Caller.CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED
            else
                Caller.CacheStrategy.CACHE_FIRST

            val currentCharts = execHere<PaginatedResult<out MusicEntry>> {
                getCharts(type, timePeriod, page, usernamep, cacheStrategy, limit)
            }

            val doStonks =
                (limit == -1 || page * limit < (0.7 * prevChartsMap.size)) && (prevChartsMap.isNotEmpty())

            currentCharts?.pageResults?.forEach {
                val hashableEntry = toHashableEntry(it)
                val prevRank = prevChartsMap[hashableEntry]
                if (doStonks) {
                    it.stonksDelta = if (prevRank != null)
                        prevRank - it.rank
                    else
                        Int.MAX_VALUE
                }
            }

            currentCharts
        }
    }

    fun getTagCloud(
        musicEntries: List<MusicEntry>, // from getCharts
        progressLd: MutableLiveData<Double>,
    ) {
        toExec = {
            Stuff.log(this::getTagCloud.name)

            class TagStats {
                var score = 0.0
                var artists = 0
                var percentSum = 0
            }

            val nArtists = 30
            val minArtists = 5
            val nTags = 65
            val minTags = 20
            val delayPerN = 150L
            val nParallel = 2
            val tags = mutableMapOf<String, TagStats>()
            var errored = false
            var currentIndex = 0
            val tagScales = mutableMapOf<String, Double>()

            if (musicEntries.size < minArtists) {
                throw IllegalStateException("Not enough artists")
            }

            musicEntries.take(nArtists).mapConcurrently(nParallel) { musicEntry ->
                if (errored)
                    return@mapConcurrently

                // caller.call is non nullable
                val result: Result = try {
                    val caller = Caller.getInstance()
                    when (musicEntry) {
                        is Artist -> {
                            caller.call(
                                "artist.getTopTags",
                                Stuff.LAST_KEY,
                                "artist",
                                musicEntry.name
                            )
                        }
                        is Album -> {
                            caller.call(
                                "album.getTopTags",
                                Stuff.LAST_KEY,
                                "artist",
                                musicEntry.artist,
                                "album",
                                musicEntry.name
                            )
                        }
                        is Track -> {
                            caller.call(
                                "track.getTopTags",
                                Stuff.LAST_KEY,
                                "artist",
                                musicEntry.artist,
                                "track",
                                musicEntry.name
                            )
                        }
                        else -> {
                            throw ClassCastException("Unknown music entry type")
                        }
                    }
                } catch (e: CallException) {
                    Timber.e(e)
                    errored = true
                    throw e
                }

                val fetchedTags = ResponseBuilder.buildCollection(result, Tag::class.java)

                if (!result.isFromCache)
                    delay(delayPerN * nParallel)

                fetchedTags.forEach {
                    val s = musicEntry.playcount.toDouble() * it.count / 100
                    val name = it.name.trim()
                    synchronized(tags) {
                        if (!tags.containsKey(name))
                            tags[name] = TagStats()
                        tags[name]!!.score += s
                        tags[name]!!.artists += 1
                        tags[name]!!.percentSum += it.count
                    }
                }

                progressLd.postValue((++currentIndex).toDouble() / min(nArtists, musicEntries.size))
            }

            if (tags.size < minTags) {
                throw IllegalStateException("Not enough tags")
            }

            val topTags = tags
                .toList()
                .filter { it.first.lowercase() !in MetadataUtils.tagSpam && it.first.lowercase() !in prefs.hiddenTags }
                .sortedByDescending { it.second.score }
                .take(nTags)
                .toMap()

            topTags.forEach { (_, stats) ->
                stats.score = stats.score.pow(3.0)
            }
            val scoresList = topTags.map { it.value.score }
            val minScore = scoresList.minOrNull()!!
            val maxScore = scoresList.maxOrNull()!!
            topTags.forEach { (tag, stats) ->
                tagScales[tag] = log10((stats.score - minScore) * 99 / maxScore + 1) / 2 * 50// + 10
//            tagScales[tag] = log10(stats.score)
                //in sp
            }
//        tagScales.toList()
//            .sortedByDescending { it.second }
//            .toMap()
            tagScales
        }
    }

    fun getUserInfo(username: String) {
        toExec = {
            Stuff.log(this::getUserInfo.name)
            val user = User.getInfo(username, lastfmSession)
            user
        }
    }

    fun getRandom(
        type: Int,
        totalp: Int,
        usernamep: String?,
        timePeriod: TimePeriod
    ) {
        toExec = {

            suspend fun getOne(page: Int): RandomVM.RandomMusicData {
                val _entry: MusicEntry?
                val _total: Int

                when (type) {
                    Stuff.TYPE_TRACKS -> {
                        var to = -1L
                        var from = -1L

                        if (timePeriod.period == null) {
                            from = timePeriod.start
                            to = timePeriod.end
                        } else if (timePeriod.period != Period.OVERALL) {
                            val approxTimePeriod = timePeriod.period.toTimePeriod()
                            from = approxTimePeriod.start
                            to = approxTimePeriod.end
                        }

                        execHere<PaginatedResult<out MusicEntry>> {
                            getRecents(
                                page,
                                usernamep,
                                from = from,
                                to = to,
                                limit = 1
                            )
                        }.let {
                            _entry = it?.pageResults?.firstOrNull()
                            _total = it?.totalPages ?: 0
                        }
                    }
                    Stuff.TYPE_LOVES -> {
                        execHere<PaginatedResult<out MusicEntry>> {
                            getLoves(
                                page,
                                usernamep,
                                limit = 1
                            )
                        }.let {
                            _entry = it?.pageResults?.firstOrNull()
                            (_entry as Track?)?.isLoved = true
                            _total = it?.totalPages ?: 0
                        }
                    }

                    else -> {
                        execHere<PaginatedResult<out MusicEntry>> {
                            getCharts(
                                type,
                                timePeriod,
                                page,
                                usernamep,
                                limit = if (timePeriod.period != null) 1 else -1
                            )
                        }.let {
                            if (timePeriod.period != null) {
                                _entry = it?.pageResults?.firstOrNull()
                                _total = it?.totalPages ?: 0
                            } else {
                                _entry = it?.pageResults?.randomOrNull()
                                _total = it?.pageResults?.size ?: 0
                            }
                        }
                    }
                }

                return RandomVM.RandomMusicData(_total, _entry, type)
            }

            checkSession()

            coroutineScope {
                var total = totalp
                var result: RandomVM.RandomMusicData
                val isCharts = type != Stuff.TYPE_TRACKS && type != Stuff.TYPE_LOVES

                if (total == -1) {
                    result = getOne(1)
                    total = result.total

                    if (total > 0 && isCharts && timePeriod.period == null) {
                        // weekly charts. Already randomised
                        return@coroutineScope result
                    }
                }

                if (total > 0) {
                    val page = (1..total).random()
                    result = getOne(page)

                    if (result.entry != null && !isCharts) {
                        val track = result.entry as Track
                        Track.getInfo(
                            track.artist,
                            track.name,
                            null,
                            lastfmUsername,
                            null,
                            Stuff.LAST_KEY
                        )?.let {
                            track.userPlaycount = it.userPlaycount
                            if (type == Stuff.TYPE_LOVES) {
                                track.imageUrlsMap = it.imageUrlsMap
                            }
                        }
                    }
                } else {
                    result = RandomVM.RandomMusicData(0, null, type)
                }

                result
            }
        }
    }

    fun getSearches(term: String) {
        toExec = {
            supervisorScope {
                val artists = async {
                    kotlin.runCatching { Artist.search(term, Stuff.LAST_KEY).toList() }
                        .getOrNull() ?: emptyList()
                }
                val albums = async {
                    kotlin.runCatching { Album.search(term, Stuff.LAST_KEY).toList() }
                        .getOrNull() ?: emptyList()
                }
                val tracks = async {
                    kotlin.runCatching { Track.search(term, Stuff.LAST_KEY).toList() }
                        .getOrNull() ?: emptyList()
                }
                SearchVM.SearchResults(
                    term,
                    SearchResultsExperimentAdapter.SearchType.GLOBAL,
                    emptyList(),
                    tracks.await(),
                    artists.await(),
                    albums.await(),
                )
            }
        }
    }

    fun getLocalSearches(term: String) {
        toExec = {
            supervisorScope {
                val db = PanoDb.getDb(context)

                val artists = async {
                    kotlin.runCatching {
                        db.getCachedArtistsDao().find(term).map { it.toArtist() }
                    }.getOrNull() ?: emptyList()
                }
                val albums = async {
                    kotlin.runCatching {
                        db.getCachedAlbumsDao().find(term).map { it.toAlbum() }
                    }.getOrNull() ?: emptyList()
                }
                val tracks = async {
                    kotlin.runCatching {
                        db.getCachedTracksDao().findTop(term).map { it.toTrack() }
                    }.getOrNull() ?: emptyList()
                }
                val lovedTracks = async {
                    kotlin.runCatching {
                        db.getCachedTracksDao().findLoved(term).map { it.toTrack() }
                    }.getOrNull() ?: emptyList()
                }
                SearchVM.SearchResults(
                    term,
                    SearchResultsExperimentAdapter.SearchType.LOCAL,
                    lovedTracks.await(),
                    tracks.await(),
                    artists.await(),
                    albums.await(),
                )
            }
        }
    }

    fun runFullIndex() {
        toExec = {
            Stuff.log(this::runFullIndex.name)


            val db = PanoDb.getDb(context)
            val limitPerPage = 1000
            val numPages = Stuff.MAX_INDEXED_ITEMS / limitPerPage
            val maxCalls = numPages * 4
            var callsMade = 0

            suspend fun postProgress(finished: Boolean = false) {
                if (finished)
                    callsMade = maxCalls
                else
                    callsMade++
                withContext(Dispatchers.Main) {
                    liveData!!.value = callsMade.toDouble() / maxCalls
                }
                if (!finished)
                    delay(250)
            }

            val list = mutableListOf<MusicEntry>()

            val lastScrobbledTrack = User.getRecentTracks(
                null,
                1,
                1,
                lastfmSession
            ).pageResults.find { it.playedWhen != null }
                ?: throw  IllegalStateException("No scrobbled tracks found")

            for (i in 1..numPages) {
                val artists = User.getTopArtists(
                    lastfmUsername,
                    Period.OVERALL,
                    limitPerPage,
                    i,
                    lastfmSession
                )
                postProgress()
                list.addAll(artists.pageResults)
                if (i >= artists.totalPages)
                    break
            }

            db.getCachedArtistsDao().apply {
                nuke()
                insert(list.map { (it as Artist).toCachedArtist() })
            }
            list.clear()


            for (i in 1..numPages) {
                val albums = User.getTopAlbums(
                    lastfmUsername,
                    Period.OVERALL,
                    limitPerPage,
                    i,
                    lastfmSession
                )
                postProgress()
                list.addAll(albums.pageResults)
                if (i >= albums.totalPages)
                    break
            }

            db.getCachedAlbumsDao().apply {
                nuke()
                insert(list.map { (it as Album).toCachedAlbum() })
            }
            list.clear()

            for (i in 1..numPages) {
                val tracks = User.getTopTracks(
                    lastfmUsername,
                    Period.OVERALL,
                    limitPerPage,
                    i,
                    lastfmSession
                )
                postProgress()
                list.addAll(tracks.pageResults)
                if (i >= tracks.totalPages)
                    break
            }

            val lovedTracksList = mutableListOf<Track>()

            for (i in 1..numPages) {
                val lovedTracks =
                    User.getLovedTracks(lastfmUsername, limitPerPage, i, lastfmSession)
                postProgress()
                lovedTracksList.addAll(lovedTracks.pageResults)
                if (i >= lovedTracks.totalPages)
                    break
            }

            val tracksMap = mutableMapOf<Pair<String, String>, MusicEntry>()

            list.forEach {
                it as Track
                tracksMap[it.artist to it.name] = it
            }

            lovedTracksList.forEach {
                val pair = it.artist to it.name
                if (pair in tracksMap) {
                    val existingTrack = tracksMap[pair] as Track
                    existingTrack.isLoved = true
                    existingTrack.playedWhen = it.playedWhen
                } else {
                    it.isLoved = true
                    list += it
                }
            }

            db.getCachedTracksDao().apply {
                nuke()
                insert(list.map { (it as Track).toCachedTrack() })
            }

            prefs.lastFullIndexedScrobbleTime = lastScrobbledTrack.playedWhen.time
            prefs.lastFullIndexTime = System.currentTimeMillis()

            prefs.lastDeltaIndexedScrobbleTime = null
            prefs.lastDeltaIndexTime = null

            // todo make it thread safe

            postProgress(finished = true)

            null
        }
    }

    fun runDeltaIndex(prFromRecents: PaginatedResult<Track>? = null) {
        toExec = {
            Stuff.log(this::runDeltaIndex.name)

            val from = prefs.lastMaxIndexedScrobbleTime
                ?: throw IllegalStateException("Full index never run")
            val to = System.currentTimeMillis()
            val limitPerPage = 1000
            val maxCalls = 15
            var currentPage = 1
            val tracks = mutableListOf<Track>()

            if (prFromRecents == null) {
                val recentsCall: LFMRequester.() -> Unit = {
                    getRecents(
                        currentPage,
                        null,
                        from = from,
                        to = to,
                        limit = limitPerPage,
                    )
                }

                val firstPage = execHere<PaginatedResult<Track>>(recentsCall)!!

                if (firstPage.totalPages > maxCalls)
                    throw IllegalStateException("Too many pages, run full index instead")

                tracks += firstPage.pageResults

                for (i in 2..firstPage.totalPages) {
                    currentPage = i
                    val pr = execHere<PaginatedResult<Track>>(recentsCall)!!
                    tracks += pr.pageResults
                }
            } else {
                val lastTrack = prFromRecents.pageResults.lastOrNull()

                if (prFromRecents.page == 1 && lastTrack != null) {
                    if (lastTrack.playedWhen.time > from)
                        throw IllegalStateException("More than one page, run indexing manually")

                    //todo handle pending scrobbles submitted at an earlier time

                    for (track in prFromRecents.pageResults) {
                        if (!track.isNowPlaying && track.playedWhen != null && track.playedWhen.time > from)
                            tracks += track
                        else
                            break
                    }
                }
            }

            val tracksLastPlayedMap = mutableMapOf<CachedTrack, Long>()
            val trackCounts = mutableMapOf<CachedTrack, Int>()
            val albumCounts = mutableMapOf<CachedAlbum, Int>()
            val artistCounts = mutableMapOf<CachedArtist, Int>()

            tracks.forEach {
                val cachedTrack = it.toCachedTrack().apply { lastPlayed = -1 }
                val cachedAlbum = if (!it.album.isNullOrEmpty()) it.toCachedAlbum() else null
                val cachedArtist = it.toCachedArtist()

                val playedWhen = it.playedWhen?.time ?: -1

                // put max time
                if (tracksLastPlayedMap[cachedTrack] == null || tracksLastPlayedMap[cachedTrack]!! < playedWhen)
                    tracksLastPlayedMap[cachedTrack] = playedWhen

                trackCounts[cachedTrack] = (trackCounts[cachedTrack] ?: 0) + 1
                if (cachedAlbum != null)
                    albumCounts[cachedAlbum] = (albumCounts[cachedAlbum] ?: 0) + 1
                artistCounts[cachedArtist] = (artistCounts[cachedArtist] ?: 0) + 1
            }

            val db = PanoDb.getDb(context)

            trackCounts.forEach { (track, count) ->
                track.lastPlayed = tracksLastPlayedMap[track] ?: -1
                db.getCachedTracksDao().deltaUpdate(track, count)
            }

            albumCounts.forEach { (album, count) ->
                db.getCachedAlbumsDao().deltaUpdate(album, count)
            }

            artistCounts.forEach { (artist, count) ->
                db.getCachedArtistsDao().deltaUpdate(artist, count)
            }

            tracks.firstOrNull()?.let {
                prefs.lastDeltaIndexedScrobbleTime = it.playedWhen!!.time
                prefs.lastDeltaIndexTime = System.currentTimeMillis()
            }

            Unit
        }
    }

    fun getScrobbleCounts(periods: List<TimePeriod>, usernamep: String?) {
        toExec = {
            Stuff.log(this::getScrobbleCounts.name)
            lastfmSession.cacheStrategy = Caller.CacheStrategy.NETWORK_ONLY

            val periodCountsMap = mutableMapOf<TimePeriod, Int>()
            periods.forEach { periodCountsMap[it] = 0 }

            supervisorScope {
                periods.mapConcurrently(5) {
                    if (it.start < System.currentTimeMillis()) {
                        kotlin.runCatching {
                            val pr = User.getRecentTracks(
                                usernamep,
                                1,
                                1,
                                false,
                                it.start / 1000,
                                it.end / 1000,
                                lastfmSession
                            )
                            periodCountsMap[it] = pr.total
                        }
                    }
                }
            }
            periodCountsMap
        }
    }

    fun getTagInfo(tag: String) {
        toExec = {
            Tag.getInfo(tag, Stuff.LAST_KEY) to
//                        Tag.getSimilar(tag, Stuff.LAST_KEY) //this doesn't work anymore
                    null
        }
    }

    fun getUserTagsForEntry(entry: MusicEntry, historyPref: HistoryPref) {
        toExec = {

            // populate tag suggestions from User.getTopTags
            if (!prefs.userTopTagsFetched) {
                val topUserTags = User.getTopTags(null, 20, lastfmSession)
                topUserTags
                    ?.reversed()
                    ?.forEach {
                        historyPref.add(it.name)
                    }
                historyPref.save()
                prefs.userTopTagsFetched = true
            }

            val list = when (entry) {
                is Artist -> Artist.getTags(entry.name, lastfmSession)
                is Album -> Album.getTags(entry.artist, entry.name, lastfmSession)
                is Track -> Track.getTags(entry.artist, entry.name, lastfmSession)
                else -> throw RuntimeException("invalid type")
            }
            list.toMutableSet()
        }
    }

    fun addUserTagsForEntry(entry: MusicEntry, tags: String) {
        toExec = {
            when (entry) {
                is Artist -> Artist.addTags(entry.name, tags, lastfmSession)
                is Album -> Album.addTags(entry.artist, entry.name, tags, lastfmSession)
                is Track -> Track.addTags(entry.artist, entry.name, tags, lastfmSession)
                else -> throw RuntimeException("invalid type")
            }
        }
    }

    fun deleteUserTagsForEntry(entry: MusicEntry, tag: String) {
        toExec = {
            when (entry) {
                is Artist -> Artist.removeTag(entry.name, tag, lastfmSession)
                is Album -> Album.removeTag(entry.artist, tag, entry.name, lastfmSession)
                is Track -> Track.removeTag(entry.artist, tag, entry.name, lastfmSession)
                else -> throw RuntimeException("invalid type")
            }
        }
    }

    fun getInfos(artist: String, album: String?, track: String?, usernamep: String?) {
        toExec = {
            supervisorScope {
                Stuff.log(this@LFMRequester::getInfos.name)
                val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")

                val infoMap = mutableMapOf<String, MusicEntry>()

                // put old values as a fallback
                (liveData!!.value as? Map<String, MusicEntry>)
                    ?.let { infoMap.putAll(it) }

                var albumArtistName: String? = null
                var albumName = album

                val trackDef = async {
                    kotlin.runCatching {
                        Track.getInfo(artist, track!!, null, username, null, Stuff.LAST_KEY)
                    }.getOrNull()
                }

                if (!track.isNullOrEmpty()) {
                    val trackInfo = trackDef.await()
                    if (trackInfo != null)
                        infoMap[NLService.B_TRACK] = trackInfo

                    if (album == null)
                        albumName = trackInfo?.album
                    albumArtistName = trackInfo?.albumArtist

                }

                val artistDef = async {
                    kotlin.runCatching {
                        Artist.getInfo(artist, null, username, false, Stuff.LAST_KEY)
                    }.getOrNull()
                }

                val albumArtistDef = async {
                    if (!albumArtistName.isNullOrEmpty() && albumArtistName.lowercase() != artist.lowercase())
                        kotlin.runCatching {
                            Artist.getInfo(albumArtistName, null, username, false, Stuff.LAST_KEY)
                        }.getOrNull()
                    else
                        null
                }

                val albumDef = async {
                    kotlin.runCatching {
                        Album.getInfo(
                            albumArtistName ?: artist,
                            albumName!!,
                            username,
                            Stuff.LAST_KEY
                        )
                    }.getOrNull()
                }

                val (artistInfo, albumArtistInfo, albumInfo) = awaitAll(
                    artistDef,
                    albumArtistDef,
                    albumDef
                )

                if (artistInfo != null)
                    infoMap[NLService.B_ARTIST] = artistInfo
                if (albumArtistInfo != null)
                    infoMap[NLService.B_ALBUM_ARTIST] = albumArtistInfo
                if (albumInfo != null)
                    infoMap[NLService.B_ALBUM] = albumInfo

                infoMap
            }
        }
    }

    fun getListenerTrend(url: String) {
        toExec = {
            val monthlyPlayCounts = mutableListOf(0, 0, 0, 0, 0)
            if (Stuff.isOnline && URLUtil.isHttpsUrl(url)) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .build()
                    okHttpClient.newCall(request).execute()
                        .use { response ->
                            if (!response.isSuccessful)
                                throw IOException("Response code ${response.code}")
                            val body = response.body!!.string()

                            if (body.isEmpty())
                                throw IOException("Empty body")

                            var idx = body.indexOf("charts/listener-trend", 200000)
                            var idx2: Int
                            var days = 0
                            val daily = arrayListOf<Int>()
                            if (idx > -1) {
                                val stop1 = "data-value=\""
                                do {
                                    idx = body.indexOf(stop1, idx)
                                    if (idx > -1) {
                                        idx += stop1.length
                                        idx2 = body.indexOf("\"", idx)
                                        val value = body.substring(idx, idx2).toInt()
                                        daily.add(value)
                                    }
                                } while (idx > -1)
                                for (i in daily.size - 1 downTo 0) {
                                    monthlyPlayCounts[4 - days / 30] += daily[i]
                                    days++
                                    if (days / 30 > 4)
                                        break
                                }
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            monthlyPlayCounts
        }
    }

    fun scrobble(
        nowPlaying: Boolean,
        scrobbleData: ScrobbleData,
        hash: Int,
        packageName: String,
        unparsedData: ScrobbleData? = null
    ) {
        toExec = {
            Stuff.log(
                this::scrobble.name + " " +
                        (if (nowPlaying) "np" else "submit")
                        + " " + scrobbleData.artist + " - " + scrobbleData.track
            )

            coroutineScope {
                checkSession()

                val scrobbleResults = mutableMapOf</*@StringRes */Int, ScrobbleResult>()
                var savedAsPending = false
                val forceable = unparsedData == null

                if (scrobbleData.duration < 30)
                    scrobbleData.duration = -1 // default
                val scrobblablesMap by lazy { Scrobblable.getScrobblablesMap(prefs) }

                fun doFallbackScrobble(): Boolean {
                    if (packageName in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK && unparsedData != null) {
                        val b = unparsedData.toBundle().apply {
                            putInt(NLService.B_HASH, hash)
                            putString(NLService.B_PACKAGE_NAME, packageName)
                        }
                        val i = Intent(NLService.iMETA_UPDATE_S).apply {
                            putExtras(b)
                        }
                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                        LFMRequester(context, scope, liveData)
                            .scrobble(nowPlaying, unparsedData, hash, packageName)
                        return true
                    }
                    return false
                }

                fun shouldBlockScrobble(otherArtist: String?): Boolean {
                    if (prefs.proStatus) {
                        val blockedMetadata = PanoDb.getDb(context)
                            .getBlockedMetadataDao()
                            .getBlockedEntry(scrobbleData, otherArtist)
                        if (blockedMetadata != null) {
                            val i = Intent(NLService.iCANCEL).apply {
                                putExtra(NLService.B_HASH, hash)
                            }
                            context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)

                            if (blockedMetadata.skip || blockedMetadata.mute) {
                                val i2 = Intent(NLService.iBLOCK_ACTION_S).apply {
                                    putExtra(Stuff.ARG_DATA, blockedMetadata)
                                    putExtra(NLService.B_HASH, hash)
                                }
                                context.sendBroadcast(i2, NLService.BROADCAST_PERMISSION)
                            }
                            return true
                        }
                    }
                    return false
                }

                if (nowPlaying) {
                    // some players put the previous song and then switch to the current song in like 150ms
                    // potentially wasting an api call. sleep and throw cancellation exception in that case
                    delay(Stuff.META_WAIT)

                    var track: Track? = null
                    var correctedArtist: String? = null

                    val editsDao = PanoDb.getDb(context).getSimpleEditsDao()
                    var edit = editsDao.performEdit(scrobbleData)

                    val oldArtist = scrobbleData.artist
                    val oldTrack = scrobbleData.track

                    val regexEdits = PanoDb.getDb(context)
                        .getRegexEditsDao()
                        .performRegexReplace(scrobbleData)

                    if (scrobbleData.artist.isNullOrBlank())
                        scrobbleData.artist = oldArtist

                    if (scrobbleData.track.isNullOrBlank())
                        scrobbleData.track = oldTrack

                    if (regexEdits.values.sum() > 0)
                        edit = editsDao.performEdit(scrobbleData)

                    if (shouldBlockScrobble(unparsedData?.artist))
                        return@coroutineScope

                    if (scrobbleData.artist.isNullOrBlank() || scrobbleData.track.isNullOrBlank()) {
                        if (!doFallbackScrobble()) {
                            val b = scrobbleData.toBundle().apply {
                                putInt(NLService.B_HASH, hash)
                                putBoolean(NLService.B_FORCEABLE, forceable)
                                putString(
                                    NLService.B_ERR_MSG,
                                    context.getString(R.string.parse_error)
                                )
                                putString(NLService.B_PACKAGE_NAME, packageName)
                            }
                            val i = Intent(NLService.iBAD_META_S).apply {
                                putExtras(b)
                            }
                            context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                        }
                        return@coroutineScope
                    } else if (edit != null || regexEdits.values.sum() > 0) {
                        val b = scrobbleData.toBundle().apply {
                            putInt(NLService.B_HASH, hash)
                        }
                        val i = Intent(NLService.iMETA_UPDATE_S).apply {
                            putExtras(b)
                        }

                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                    }

                    if (Stuff.isOnline) {
                        val (lastScrobbleData, lastTime) = lastNp
                        lastNp = scrobbleData to System.currentTimeMillis()
                        lastScrobbleData.timestamp = scrobbleData.timestamp

                        val cacheOnly = if (lastScrobbleData == scrobbleData &&
                            System.currentTimeMillis() - lastTime < Stuff.TRACK_INFO_VALIDITY
                        ) {
                            if (System.currentTimeMillis() - lastTime < 1000)
                                Timber.tag(Stuff.TAG).w(Exception("Possible duplicate scrobble"))
                            true
                        } else
                            false

                        track = getValidTrack(
                            scrobbleData.artist,
                            scrobbleData.track,
                            lastfmUsername!!,
                            cacheOnly = cacheOnly
                        )
                        //works even if the username is wrong

                        if (!isActive)
                            return@coroutineScope
                        val scrobbleDataBeforeAutocorrect = ScrobbleData(scrobbleData)
                        if (track != null) {
                            if (scrobbleData.duration == -1)
                                scrobbleData.duration = track.duration

                            if (scrobbleData.album == "") {
                                scrobbleData.artist = track.artist
                                if (track.album != null)
                                    scrobbleData.album = track.album
                                if (track.albumArtist != null)
                                    scrobbleData.albumArtist = track.albumArtist
                                scrobbleData.track = track.name
                            } else if (!track.albumArtist.isNullOrEmpty() &&
                                prefs.fetchAlbumArtist &&
                                scrobbleData.album.equals(track.album, ignoreCase = true) &&
                                (scrobbleData.albumArtist.isNullOrEmpty() || scrobbleData.artist == scrobbleData.albumArtist)
                            ) {
                                scrobbleData.albumArtist = track.albumArtist
                            }
                        }
                        correctedArtist =
                            if (track != null && (track.listeners >= Stuff.MIN_LISTENER_COUNT || forceable))
                                track.artist
                            else if (forceable)
                                scrobbleData.artist
                            else
                                getValidArtist(
                                    scrobbleData.artist,
                                    prefs.allowedArtists
                                )
                        if (correctedArtist != null && scrobbleData.album == "")
                            scrobbleData.artist = correctedArtist

                        if (scrobbleDataBeforeAutocorrect != scrobbleData) {
                            edit = editsDao.performEdit(scrobbleData, false)

                            PanoDb.getDb(context)
                                .getRegexEditsDao()
                                .performRegexReplace(scrobbleData)

                            if (regexEdits.values.sum() > 0)
                                edit = editsDao.performEdit(scrobbleData, false)

                            if (shouldBlockScrobble(null))
                                return@coroutineScope

                            if (edit != null)
                                track = getValidTrack(
                                    scrobbleData.artist,
                                    scrobbleData.track,
                                    lastfmUsername!!
                                )
                        }
                    }

                    if (edit != null || track != null) {
                        val b = scrobbleData.toBundle().apply {
                            putInt(NLService.B_HASH, hash)
                            if (track != null) {
                                putBoolean(NLService.B_USER_LOVED, track!!.isLoved)
                                putInt(NLService.B_USER_PLAY_COUNT, track!!.userPlaycount)
                            }
                        }
                        val i = Intent(NLService.iMETA_UPDATE_S)
                            .putExtras(b)

                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                    }
                    if (Stuff.isOnline) {
                        if (correctedArtist != null || edit != null) {
                            if (prefs.submitNowPlaying) {
                                scrobblablesMap.forEach { (stringId, scrobblable) ->
                                    if (scrobblable != null) {
                                        if (!isActive)
                                            throw CancellationException()
                                        scrobbleResults[stringId] =
                                            scrobblable.updateNowPlaying(scrobbleData)
                                    }
                                }
                            }
                        } else {
                            // unrecognized artist
                            if (!doFallbackScrobble()) {
                                val b = scrobbleData.toBundle().apply {
                                    putInt(NLService.B_HASH, hash)
                                    putBoolean(NLService.B_FORCEABLE, forceable)
                                    putString(NLService.B_PACKAGE_NAME, packageName)
                                    putString(
                                        NLService.B_ERR_MSG,
                                        context.getString(R.string.state_unrecognised_artist)
                                    )
                                }
                                val i = Intent(NLService.iBAD_META_S).apply {
                                    putExtras(b)
                                }
                                context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                            }
                        }
                    }
                } else { // scrobble

                    // track player
                    val scrobbleSource = ScrobbleSource(
                        timeMillis = scrobbleData.timestamp * 1000L,
                        pkg = packageName
                    )
                    PanoDb.getDb(context)
                        .getScrobbleSourcesDao()
                        .insert(scrobbleSource)

                    if (Stuff.isOnline) {
                        scrobblablesMap.forEach { (stringId, scrobblable) ->
                            if (scrobblable != null) {
                                scrobbleResults[stringId] = scrobblable.scrobble(scrobbleData)
                            }
                        }
                    }
                    if (scrobbleResults.isEmpty() ||
                        scrobbleResults.values.any { !it.isSuccessful }
                    ) {
                        val dao = PanoDb.getDb(context).getScrobblesDao()
                        val entry = PendingScrobble().apply {
                            artist = scrobbleData.artist
                            album = scrobbleData.album
                            track = scrobbleData.track
                            if (scrobbleData.albumArtist != null)
                                albumArtist = scrobbleData.albumArtist
                            timestamp = scrobbleData.timestamp.toLong() * 1000
                            duration = scrobbleData.duration.toLong() * 1000
                        }

                        if (scrobbleResults.isEmpty())
                            scrobblablesMap.forEach { (key, scrobblable) ->
                                if (scrobblable != null)
                                    entry.state =
                                        entry.state or (1 shl Stuff.SERVICE_BIT_POS[key]!!)
                            }
                        else
                            scrobbleResults.forEach { (key, result) ->
                                if (!result.isSuccessful) {
                                    entry.state =
                                        entry.state or (1 shl Stuff.SERVICE_BIT_POS[key]!!)
                                }
                            }
                        if (scrobbleResults.isNotEmpty())
                            entry.autoCorrected = 1
                        dao.insert(entry)
                        savedAsPending = true
                        PendingScrJob.checkAndSchedule(context)
                    }
                }

                try {
                    val failedTextLines = mutableListOf<String>()
                    var ignored = false
                    scrobbleResults.forEach { (key, result) ->
                        if (!result.isSuccessful) {
                            val errMsg = scrobbleResults[key]?.errorMessage
                                ?: context.getString(R.string.network_error)
                            failedTextLines += "<b>" + context.getString(key) + ":</b> $errMsg"
                        } else if (result.isSuccessful && result.isIgnored) {
                            failedTextLines += "<b>" + context.getString(key) + ":</b> " +
                                    context.getString(R.string.scrobble_ignored)
                            ignored = true
                        }
                    }
                    if (failedTextLines.isNotEmpty()) {
                        val failedText = failedTextLines.joinToString("<br>\n")
                        Stuff.log("failedText= $failedText")
                        val i = if (ignored) {
                            val b = scrobbleData.toBundle().apply {
                                putInt(NLService.B_HASH, hash)
                                putBoolean(NLService.B_FORCEABLE, forceable)
                                putString(
                                    NLService.B_ERR_MSG,
                                    context.getString(R.string.scrobble_ignored)
                                )
                                putString(NLService.B_ERR_DESC, failedText)
                                putString(NLService.B_PACKAGE_NAME, packageName)
                            }
                            Intent(NLService.iBAD_META_S).apply {
                                putExtras(b)
                            }
                        } else {
                            Intent(NLService.iOTHER_ERR_S).apply {
                                putExtra(NLService.B_ERR_MSG, failedText)
                                putExtra(NLService.B_PENDING, savedAsPending)
                                putExtra(NLService.B_HASH, hash)
                            }
                        }
                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                    }

                } catch (e: NullPointerException) {
                }
            }
        }
    }

    fun loveOrUnlove(track: Track, love: Boolean, callback: ((Boolean) -> Unit)? = null) {
        toExec = {
            checkSession()
            Stuff.log(this::loveOrUnlove.name + " " + love)
            var submittedAll = true
            val scrobblablesMap = Scrobblable.getScrobblablesMap(prefs, supportsLove = true)

            val dao = PanoDb.getDb(context).getLovesDao()
            val pl = dao.find(track.artist, track.name)
            if (pl != null) {
                if (pl.shouldLove == !love) {
                    pl.shouldLove = love
                    scrobblablesMap.forEach { (key, scrobblable) ->
                        if (scrobblable != null)
                            pl.state = pl.state or (1 shl Stuff.SERVICE_BIT_POS[key]!!)
                    }
                    dao.update(pl)
                }
                submittedAll = false
            } else {
                val results = mutableMapOf</*@StringRes */Int, Result>()

                scrobblablesMap.forEach { (stringId, scrobblable) ->
                    if (scrobblable != null) {
                        results[stringId] = scrobblable.loveOrUnlove(track, love)
                    }
                }

                if (results.values.any { !it.isSuccessful }) {
                    val entry = PendingLove()
                    entry.artist = track.artist
                    entry.track = track.name
                    entry.shouldLove = love
                    results.forEach { (id, result) ->
                        if (!result.isSuccessful && result.errorCode != 7)
                            entry.state = entry.state or (1 shl Stuff.SERVICE_BIT_POS[id]!!)
                    }
                    if (entry.state != 0) {
                        dao.insert(entry)
                        PendingScrJob.checkAndSchedule(context)
                        submittedAll = false
                    }
                }
            }
            withContext(Dispatchers.Main) {
                callback?.invoke(submittedAll)
            }
            null
        }
    }

    fun delete(track: Track, callback: (suspend (Boolean) -> Unit)?) {
        toExec = {
            val scrobblablesMap = Scrobblable.getScrobblablesMap(prefs, supportsLove = true)
            val unscrobbler = LastfmUnscrobbler(context)
            val success = unscrobbler.haveCsrfCookie() &&
                    unscrobbler.unscrobble(track.artist, track.name, track.playedWhen.time)

            withContext(Dispatchers.Main) {
                callback!!.invoke(success)
            }

            arrayOf(scrobblablesMap[R.string.librefm], scrobblablesMap[R.string.gnufm])
                .forEach {
                    if (it is Lastfm) {
                        Library.removeScrobble(
                            track.artist,
                            track.name,
                            track.playedWhen.time / 1000,
                            it.session
                        )
                    }
                }

            CachedTracksDao.deltaUpdateAll(context, track, -1)

            null
        }
    }

    // adb shell am start -W -a android.intent.action.VIEW -d "pscrobble://auth/lastfm?token=hohoho" com.arn.scrobble

    fun doAuth(@StringRes type: Int, token: String?) {
        toExec = {
            if (!token.isNullOrEmpty()) {
                when (type) {
                    R.string.lastfm -> {
                        val lastfmSession =
                            Authenticator.getSession(null, token, Stuff.LAST_KEY, Stuff.LAST_SECRET)
                        if (lastfmSession != null) {
                            prefs.lastfmUsername = lastfmSession.username
                            prefs.lastfmSessKey = lastfmSession.key

                            getDrawerInfo()
                        }
                    }
                    R.string.librefm -> {
                        val librefmSession = Authenticator.getSession(
                            Stuff.LIBREFM_API_ROOT,
                            token, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY
                        )
                        if (librefmSession != null) {
                            prefs.librefmUsername = librefmSession.username
                            prefs.librefmSessKey = librefmSession.key
                        }
                    }
                    R.string.gnufm -> {
                        val gnufmSession = Authenticator.getSession(
                            prefs.gnufmRoot + "2.0/",
                            token, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY
                        )
                        if (gnufmSession != null) {
                            prefs.gnufmUsername = gnufmSession.username
                            prefs.gnufmSessKey = gnufmSession.key
                        }
                    }
                }
                val intent = Intent(NLService.iSESS_CHANGED_S)
                context.sendBroadcast(intent, NLService.BROADCAST_PERMISSION)
            }
            null
        }
    }

    fun cancel() {
        job.cancel()
    }

    suspend fun <T> execHere(func: LFMRequester.() -> Unit): T? {
        launchExecOnSet = false
        func()
        return toExec() as T?
    }

    val isCompleted get() = ::job.isInitialized && job.isCompleted

    companion object {

        private val validArtistsCache = LruCache<String, String>(30)
        private val validTracksCache = LruCache<Pair<String, String>, Pair<Track?, Long>>(30)

        private var lastNp = ScrobbleData() to 0L

        private var lastNpInfoTime = 0L
        private var lastNpInfoCount = 0

        val okHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .followRedirects(false)
                .build()
        }

        val okHttpClientTlsNoVerify by lazy {
            okHttpClient.newBuilder()
                .ignoreSslErrors()
                .build()
        }

        fun getValidTrack(
            artist: String,
            title: String,
            lastfmUsername: String,
            cacheOnly: Boolean = false
        ): Track? {
            val entry = validTracksCache[artist to title]
            if (entry != null) {
                val (track, time) = entry
                if (System.currentTimeMillis() - time < Stuff.TRACK_INFO_VALIDITY || cacheOnly)
                    return track
            }
            if (cacheOnly)
                return null

            val now = System.currentTimeMillis()
            if (now - lastNpInfoTime < Stuff.TRACK_INFO_WINDOW && !BuildConfig.DEBUG) {
                lastNpInfoCount++
                if (lastNpInfoCount >= Stuff.TRACK_INFO_REQUESTS)
                    return null
            } else {
                lastNpInfoTime = now
                lastNpInfoCount = 0
            }
            val track = try {
                Track.getInfo(artist, title, null, lastfmUsername, null, Stuff.LAST_KEY)
            } catch (e: Exception) {
                null
            }
            validTracksCache.put(artist to title, track to System.currentTimeMillis())

            if (track != null && track.listeners >= Stuff.MIN_LISTENER_COUNT)
                validArtistsCache.put(artist, track.artist)

            return track
        }

        fun getValidArtist(artist: String, set: Set<String>? = null): String? {
            if (set?.contains(artist) == true)
                return artist
            if (validArtistsCache[artist] != null && validArtistsCache[artist].isEmpty())
                return null
            else if (validArtistsCache[artist] != null)
                return validArtistsCache[artist]
            else {
                var artistInfo: Artist?
                var errCode = Caller.getInstance().lastError?.errorCode
                //6 = artist not found on lastfm, 7 = invalid resource specified on librefm
                if (errCode != null && errCode != 6 && errCode != 7)
                    artistInfo = getArtistInfoLibreFM(artist)
                else {
                    artistInfo = Artist.getInfo(artist, true, Stuff.LAST_KEY)
                    errCode = Caller.getInstance().lastError?.errorCode
                    if (artistInfo == null && errCode != null && errCode != 6 && errCode != 7)
                        artistInfo = getArtistInfoLibreFM(artist)
                }
                errCode = Caller.getInstance().lastError?.errorCode
                if (artistInfo == null && errCode != null && errCode != 6 && errCode != 7)
                    return artist

                Stuff.log("artistInfo: $artistInfo")
                //nw err throws an exception
                if (artistInfo != null && artistInfo.name?.trim() != "") {
                    if (artistInfo.listeners == -1 || artistInfo.listeners >= Stuff.MIN_LISTENER_COUNT) {
                        validArtistsCache.put(artist, artistInfo.name)
                        return artistInfo.name
                    } else
                        validArtistsCache.put(artist, "")
                } else
                    validArtistsCache.put(artist, "")
            }
            return null
        }

        fun getArtistInfoLibreFM(artist: String): Artist? {
            val result = Caller.getInstance().call(
                Stuff.LIBREFM_API_ROOT, "artist.getInfo",
                "", mapOf("artist" to artist, "autocorrect" to "1")
            )
            return ResponseBuilder.buildItem(result, Artist::class.java)
        }

        fun getArtistInfoSpotify(artist: String): Artist? {
            if (Tokens.SPOTIFY_ARTIST_INFO_SERVER.isEmpty() || Tokens.SPOTIFY_ARTIST_INFO_KEY.isEmpty())
                return null
            val result = Caller.getInstance().call(
                Tokens.SPOTIFY_ARTIST_INFO_SERVER, "artist.getInfo.spotify",
                Tokens.SPOTIFY_ARTIST_INFO_KEY, mapOf("artist" to artist)
            )
            return ResponseBuilder.buildItem(result, Artist::class.java)
        }

        fun getCorrectedDataOld(artist: String, track: String): Pair<String, String>? {
            val correction = Track.getCorrection(artist, track, Stuff.LAST_KEY)
            var cArtist = correction?.artist?.trim() ?: ""
            var cTrack = correction?.name?.trim() ?: ""

            return if (cArtist == "" && cTrack == "")
                null
            else {
                if (cArtist == "")
                    cArtist = artist
                if (cTrack == "")
                    cTrack = artist
                Pair(cArtist, cTrack)
            }
        }
    }
}
