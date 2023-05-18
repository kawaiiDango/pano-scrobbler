package com.arn.scrobble

import android.content.Intent
import android.util.LruCache
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.db.BlockedMetadataDao.Companion.getBlockedEntry
import com.arn.scrobble.db.CachedAlbum
import com.arn.scrobble.db.CachedAlbum.Companion.toAlbum
import com.arn.scrobble.db.CachedAlbum.Companion.toCachedAlbum
import com.arn.scrobble.db.CachedAlbumsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedArtist
import com.arn.scrobble.db.CachedArtist.Companion.toArtist
import com.arn.scrobble.db.CachedArtist.Companion.toCachedArtist
import com.arn.scrobble.db.CachedArtistsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedTrack
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.db.CachedTrack.Companion.toTrack
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.CachedTracksDao.Companion.deltaUpdate
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEditsDao.Companion.performEdit
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.friends.UserSerializable.Companion.toUserSerializable
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.GnuFm
import com.arn.scrobble.scrobbleable.Scrobblable
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.search.SearchResultsAdapter
import com.arn.scrobble.search.SearchVM
import com.arn.scrobble.ui.UiUtils.toast
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.Authenticator
import de.umass.lastfm.CallException
import de.umass.lastfm.Caller
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Period
import de.umass.lastfm.ResponseBuilder
import de.umass.lastfm.Result
import de.umass.lastfm.Session
import de.umass.lastfm.Tag
import de.umass.lastfm.Track
import de.umass.lastfm.User
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.io.InterruptedIOException
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow


/**
 * Created by arn on 18-03-2017.
 */

class LFMRequester(
    private val scope: CoroutineScope,
    private val liveData: MutableLiveData<*>? = null,
    private val errorLiveData: MutableLiveData<Throwable>? = null,
) {
    private val context = App.context
    private val prefs = App.prefs
    private lateinit var job: Job
    private var launchExecOnSet = true

    private var toExec: (suspend () -> Any?) = {}
        set(fn) {
            field = fn

            if (!launchExecOnSet)
                return

            job = scope.launch(ExceptionNotifier(errorLiveData)) {
                val res = withContext(Dispatchers.IO) {
                    fn()
                }
                withContext(Dispatchers.Main) {
                    liveData?.value = res
                }
            }
        }

    private val currentScrobblable by lazy {
        Scrobblables.current as? GnuFm
    }

    // copy
    val session: Session by lazy {
        currentScrobblable!!.sessionCopy()
    }

    private val gnufmAccount get() = currentScrobblable?.userAccount

    private fun checkSession(usernamep: String? = null) {
        if (usernamep == null && gnufmAccount == null)
            throw Exception("Login required")
    }

    fun getTrackScrobbles(track: Track, page: Int, usernamep: String?, limit: Int = 50) {
        toExec = {
            checkSession(usernamep)
            User.getTrackScrobbles(track.artist, track.name, usernamep, page, limit, session)
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
                        session
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
            val prevCharts =
                if (prevTimePeriod != null && gnufmAccount?.type == AccountType.LASTFM)
                    currentScrobblable!!.getCharts(
                        type,
                        prevTimePeriod,
                        1,
                        usernamep,
                        Caller.CacheStrategy.CACHE_FIRST_ONE_DAY,
                        -1
                    )
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

            val currentCharts = Scrobblables.current
                ?.getCharts(type, timePeriod, page, usernamep, cacheStrategy, limit)

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
                    Timber.tag(Stuff.TAG).e(e)
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
                .filter { it.first.isNotEmpty() && it.first.lowercase() !in prefs.hiddenTags }
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
            val user = User.getInfo(username, session)
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

                when {
                    type == Stuff.TYPE_TRACKS &&
                            Scrobblables.current!!.userAccount.type == AccountType.LASTFM -> {
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

                        Scrobblables.current!!.getRecents(
                            page,
                            usernamep,
                            from = from,
                            to = to,
                            limit = 1,
                        ).let {
                            _entry = it.pageResults?.firstOrNull()
                            _total = it.totalPages
                        }
                    }

                    type == Stuff.TYPE_LOVES -> {
                        Scrobblables.current!!.getLoves(
                            page,
                            usernamep,
                            limit = 1,
                        ).let {
                            _entry = it.pageResults?.firstOrNull()
                            _total = it.totalPages
                        }
                    }

                    else -> {
                        Scrobblables.current!!.getCharts(
                            type,
                            timePeriod,
                            page,
                            usernamep,
                            limit = if (timePeriod.period != null) 1 else -1
                        ).let {
                            if (timePeriod.period != null) {
                                _entry = it.pageResults?.firstOrNull()
                                _total = it.totalPages ?: 0
                            } else {
                                _entry = it.pageResults?.randomOrNull()
                                _total = it.pageResults?.size ?: 0
                            }
                        }
                    }
                }

                return RandomVM.RandomMusicData(_total, _entry, type)
            }

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
                            usernamep,
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
                    SearchResultsAdapter.SearchType.GLOBAL,
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
                val db = PanoDb.db

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
                    SearchResultsAdapter.SearchType.LOCAL,
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
            indexingMutex.withLock {
                Stuff.log(this::runFullIndex.name)
                if (gnufmAccount?.type != AccountType.LASTFM) return@withLock null


                val db = PanoDb.db
                val limitPerPage = 1000
                val numPages = Stuff.MAX_INDEXED_ITEMS / limitPerPage
                val maxCalls = numPages * 4
                var callsMade = 0

                if (db.getPendingScrobblesDao().count > 0 || db.getPendingLovesDao().count > 0) {
                    throw IllegalStateException("Cannot run when there are pending scrobbles")
                }

                suspend fun postProgress(finished: Boolean = false) {
                    if (finished)
                        callsMade = maxCalls
                    else
                        callsMade++
                    withContext(Dispatchers.Main) {
                        liveData?.value = callsMade.toDouble() / maxCalls
                    }
                    if (!finished)
                        delay(50)
                }

                val list = mutableListOf<MusicEntry>()

                val lastScrobbledTrack = User.getRecentTracks(
                    null,
                    1,
                    1,
                    session
                ).pageResults.find { it.playedWhen != null }
                    ?: throw IllegalStateException("No scrobbled tracks found")

                for (i in 1..numPages) {
                    val artists = User.getTopArtists(
                        gnufmAccount!!.user.name,
                        Period.OVERALL,
                        limitPerPage,
                        i,
                        session
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
                        gnufmAccount!!.user.name,
                        Period.OVERALL,
                        limitPerPage,
                        i,
                        session
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
                        gnufmAccount!!.user.name,
                        Period.OVERALL,
                        limitPerPage,
                        i,
                        session
                    )
                    postProgress()
                    list.addAll(tracks.pageResults)
                    if (i >= tracks.totalPages)
                        break
                }

                val lovedTracksList = mutableListOf<Track>()

                for (i in 1..numPages) {
                    val lovedTracks =
                        User.getLovedTracks(
                            gnufmAccount!!.user.name,
                            limitPerPage,
                            i,
                            session
                        )
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

                postProgress(finished = true)

                null
            }
        }
    }

    fun runDeltaIndex(prFromRecents: PaginatedResult<Track>? = null) {
        toExec = {
            indexingMutex.withLock {

                Stuff.log(this::runDeltaIndex.name)

                if (gnufmAccount?.type != AccountType.LASTFM) return@withLock null

                val db = PanoDb.db
                val from = prefs.lastMaxIndexedScrobbleTime
                    ?: throw IllegalStateException("Full index never run")
                val to = System.currentTimeMillis()
                val limitPerPage = 1000
                val maxCalls = 15
                var currentPage = 1
                val tracks = mutableListOf<Track>()

                if (db.getPendingScrobblesDao().count > 0 || db.getPendingLovesDao().count > 0) {
                    throw IllegalStateException("Cannot run when there are pending scrobbles")
                }

                withContext(Dispatchers.Main) {
                    liveData?.value = 0.5
                }

                if (prFromRecents == null) {
                    val recentsCall = suspend {
                        currentScrobblable!!
                            .getRecents(
                                currentPage,
                                null,
                                from = from,
                                to = to,
                                limit = limitPerPage,
                            )
                    }

                    val firstPage = recentsCall()

                    if (firstPage.totalPages > maxCalls)
                        throw IllegalStateException("Too many pages, run full index instead")

                    tracks += firstPage.pageResults

                    for (i in 2..firstPage.totalPages) {
                        currentPage = i
                        val pr = recentsCall()
                        tracks += pr.pageResults
                    }
                } else {
                    val lastTrack = prFromRecents.pageResults.lastOrNull()

                    if (prFromRecents.page == 1 && lastTrack != null) {
                        if (lastTrack.playedWhen.time > from)
                            throw IllegalStateException("More than one page, run indexing manually")

                        // todo handle pending scrobbles submitted at an earlier time

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

                withContext(Dispatchers.Main) {
                    liveData?.value = 1.0
                }

                null
            }
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
                val topUserTags = User.getTopTags(null, 20, session)
                topUserTags
                    ?.reversed()
                    ?.forEach {
                        historyPref.add(it.name)
                    }
                historyPref.save()
                prefs.userTopTagsFetched = true
            }

            val list = when (entry) {
                is Artist -> Artist.getTags(entry.name, session)
                is Album -> Album.getTags(entry.artist, entry.name, session)
                is Track -> Track.getTags(entry.artist, entry.name, session)
                else -> throw RuntimeException("invalid type")
            }
            list.toMutableSet()
        }
    }

    fun addUserTagsForEntry(entry: MusicEntry, tags: String) {
        toExec = {
            when (entry) {
                is Artist -> Artist.addTags(entry.name, tags, session)
                is Album -> Album.addTags(entry.artist, entry.name, tags, session)
                is Track -> Track.addTags(entry.artist, entry.name, tags, session)
                else -> throw RuntimeException("invalid type")
            }
        }
    }

    fun deleteUserTagsForEntry(entry: MusicEntry, tag: String) {
        toExec = {
            when (entry) {
                is Artist -> Artist.removeTag(entry.name, tag, session)
                is Album -> Album.removeTag(entry.artist, tag, entry.name, session)
                is Track -> Track.removeTag(entry.artist, tag, entry.name, session)
                else -> throw RuntimeException("invalid type")
            }
        }
    }

    fun getInfos(artist: String, album: String?, track: String?, usernamep: String?) {
        toExec = {
            supervisorScope {

                Stuff.log(this@LFMRequester::getInfos.name)
                val username = if (gnufmAccount?.type == AccountType.LASTFM)
                    usernamep ?: gnufmAccount?.user?.name ?: throw Exception("Login required")
                else
                    null

                val infoMap = mutableMapOf<String, MusicEntry>()
                val db = PanoDb.db

                fun doDirtyDeltaUpdates(
                    artist: Artist?,
                    album: Album?,
                    track: Track?,
                    albumArtist: Artist?
                ) {
                    if (gnufmAccount?.type != AccountType.LASTFM)
                        return

                    track?.let {
                        db.getCachedTracksDao()
                            .deltaUpdate(
                                it.toCachedTrack()
                                    .apply { userPlayCount = it.userPlaycount },
                                0,
                                DirtyUpdate.DIRTY_ABSOLUTE
                            )
                    }
                    album?.let {
                        db.getCachedAlbumsDao()
                            .deltaUpdate(
                                it.toCachedAlbum().apply { userPlayCount = it.userPlaycount },
                                0, DirtyUpdate.DIRTY_ABSOLUTE
                            )
                    }
                    artist?.let {
                        db.getCachedArtistsDao()
                            .deltaUpdate(
                                it.toCachedArtist().apply { userPlayCount = it.userPlaycount },
                                0,
                                DirtyUpdate.DIRTY_ABSOLUTE
                            )
                    }
                    albumArtist?.let {
                        db.getCachedArtistsDao()
                            .deltaUpdate(
                                it.toCachedArtist().apply { userPlayCount = it.userPlaycount },
                                0,
                                DirtyUpdate.DIRTY_ABSOLUTE
                            )
                    }
                }
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
                    if (trackInfo != null) {
                        infoMap[NLService.B_TRACK] = trackInfo
                    }

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

                artistDef.await()?.let {
                    infoMap[NLService.B_ARTIST] = it
                }
                albumArtistDef.await()?.let {
                    infoMap[NLService.B_ALBUM_ARTIST] = it
                }
                albumDef.await()?.let {
                    infoMap[NLService.B_ALBUM] = it
                }

                doDirtyDeltaUpdates(
                    artistDef.await(),
                    albumDef.await(),
                    trackDef.await(),
                    albumArtistDef.await()
                )

                infoMap
            }
        }
    }

    fun getTrackFeatures(artist: String, track: String) {
        toExec = {
            SpotifyRequester.getSpotifyTrack(Track(track, null, artist))
                ?.also { it.features = SpotifyRequester.getTrackFeatures(it.id) }
        }
    }

    fun getListenerTrend(url: String) {
        toExec = {
            val monthlyPlayCounts = mutableListOf(0, 0, 0, 0, 0)
            if (Stuff.isOnline && URLUtil.isHttpsUrl(url)) {
                try {
                    val request = Request(url.toHttpUrl())
                    okHttpClient.newCall(request).execute()
                        .use { response ->
                            if (!response.isSuccessful)
                                throw IOException("Response code ${response.code}")
                            val body = response.body.string()

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
        trackInfo: PlayingTrackInfo,
        unparsedData: ScrobbleData? = null
    ) {
        toExec = {
            Stuff.log(
                this::scrobble.name + " " +
                        (if (nowPlaying) "np" else "submit")
                        + " " + trackInfo.artist + " - " + trackInfo.title
            )

            coroutineScope {
                Scrobblables.current ?: return@coroutineScope

                var scrobbleResults = mapOf<Scrobblable, ScrobbleResult>()
                var savedAsPending = false
                val forceable = unparsedData == null

                val scrobbleData = trackInfo.toScrobbleData()

                fun doFallbackScrobble(): Boolean {
                    if (trackInfo.canDoFallbackScrobble && unparsedData != null) {

                        val newTrackInfo = trackInfo.updateMetaFrom(unparsedData)
                        val i = Intent(NLService.iMETA_UPDATE_S)
                            .setPackage(context.packageName)
                            .putSingle(newTrackInfo)
                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)

                        LFMRequester(scope, liveData)
                            .scrobble(nowPlaying, newTrackInfo)
                        return true
                    }
                    return false
                }

                fun shouldBlockScrobble(otherArtist: String?): Boolean {
                    if (prefs.proStatus) {
                        val blockedMetadata = PanoDb.db
                            .getBlockedMetadataDao()
                            .getBlockedEntry(scrobbleData, otherArtist)
                        if (blockedMetadata != null) {
                            val i = Intent(NLService.iCANCEL).apply {
                                `package` = context.packageName
                                putExtra(NLService.B_HASH, trackInfo.hash)
                            }
                            context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)

                            if (blockedMetadata.skip || blockedMetadata.mute) {
                                val i2 = Intent(NLService.iBLOCK_ACTION_S).apply {
                                    `package` = context.packageName
                                    putSingle(blockedMetadata)
                                    putExtra(NLService.B_HASH, trackInfo.hash)
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

                    val editsDao = PanoDb.db.getSimpleEditsDao()
                    var edit = editsDao.performEdit(scrobbleData)

                    val oldArtist = scrobbleData.artist
                    val oldTrack = scrobbleData.track

                    val regexEdits = PanoDb.db
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
                            val i = Intent(NLService.iBAD_META_S)
                                .setPackage(context.packageName)
                                .putSingle(
                                    trackInfo.updateMetaFrom(scrobbleData)
                                )
                                .putSingle(
                                    ScrobbleError(
                                        context.getString(R.string.parse_error),
                                        null,
                                        trackInfo.packageName,
                                        canForceScrobble = forceable
                                    )
                                )
                            context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                        }
                        return@coroutineScope
                    } else if (edit != null || regexEdits.values.sum() > 0) {
                        val i = Intent(NLService.iMETA_UPDATE_S)
                            .setPackage(context.packageName)
                            .putSingle(trackInfo.updateMetaFrom(scrobbleData))

                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                    }

                    if (Stuff.isOnline) {
                        val (lastScrobbleData, lastTime) = lastNp
                        lastNp = scrobbleData to System.currentTimeMillis()
                        lastScrobbleData.timestamp = scrobbleData.timestamp

                        if (scrobbleData.album.isEmpty() && prefs.fetchAlbum)
                            track = getValidTrack(scrobbleData.artist, scrobbleData.track)

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

                            PanoDb.db
                                .getRegexEditsDao()
                                .performRegexReplace(scrobbleData)

                            if (regexEdits.values.sum() > 0)
                                edit = editsDao.performEdit(scrobbleData, false)

                            if (shouldBlockScrobble(null))
                                return@coroutineScope
                        }
                    }

                    val cachedTrack: CachedTrack? =
                        if (prefs.lastMaxIndexTime != null)
                            PanoDb.db.getCachedTracksDao()
                                .findExact(scrobbleData.artist, scrobbleData.track)
                        else
                            null
                    if (edit != null || cachedTrack != null) {
                        trackInfo.updateMetaFrom(scrobbleData).apply {
                            cachedTrack?.let {
                                userPlayCount = it.plays
                                userLoved = it.isLoved
                            }
                        }
                        val i = Intent(NLService.iMETA_UPDATE_S)
                            .setPackage(context.packageName)
                            .putSingle(trackInfo)

                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                    }
                    if (Stuff.isOnline) {
                        if (correctedArtist != null || edit != null) {
                            if (prefs.submitNowPlaying) {
                                scrobbleResults = Scrobblables.all.mapConcurrently(5) {
                                    it to it.updateNowPlaying(scrobbleData)
                                }.toMap()
                            }
                        } else {
                            // unrecognized artist
                            if (!doFallbackScrobble()) {
                                val i = Intent(NLService.iBAD_META_S)
                                    .setPackage(context.packageName)
                                    .putSingle(trackInfo.updateMetaFrom(scrobbleData))
                                    .putSingle(
                                        ScrobbleError(
                                            context.getString(R.string.state_unrecognised_artist),
                                            null,
                                            trackInfo.packageName,
                                            canForceScrobble = forceable
                                        )
                                    )
                                context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                            }
                        }
                    }
                } else { // scrobble

                    // track player
                    val scrobbleSource = ScrobbleSource(
                        timeMillis = scrobbleData.timestamp * 1000L,
                        pkg = trackInfo.packageName
                    )
                    PanoDb.db
                        .getScrobbleSourcesDao()
                        .insert(scrobbleSource)

                    if (Stuff.isOnline) {
                        scrobbleResults = Scrobblables.all.mapConcurrently(5) {
                            it to it.scrobble(scrobbleData)
                        }.toMap()
                    }

                    if (scrobbleResults.isEmpty() ||
                        scrobbleResults.values.any { !it.isSuccessful }
                    ) {
                        // failed
                        val dao = PanoDb.db.getPendingScrobblesDao()
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
                            Scrobblables.all.forEach {
                                entry.state =
                                    entry.state or (1 shl it.userAccount.type.ordinal)
                            }
                        else
                            scrobbleResults.forEach { (scrobblable, result) ->
                                if (!result.isSuccessful) {
                                    entry.state =
                                        entry.state or (1 shl scrobblable.userAccount.type.ordinal)
                                }
                            }
                        if (scrobbleResults.isNotEmpty())
                            entry.autoCorrected = 1
                        dao.insert(entry)
                        savedAsPending = true
                        PendingScrJob.checkAndSchedule(context)
                    } else {
                        // successful
                        CachedTracksDao.deltaUpdateAll(
                            Track(
                                scrobbleData.track,
                                null,
                                scrobbleData.album,
                                scrobbleData.artist,
                            ).apply { playedWhen = Date(scrobbleData.timestamp * 1000L) },
                            1,
                            DirtyUpdate.DIRTY
                        )
                    }
                }

                try {
                    val failedTextLines = mutableListOf<String>()
                    var ignored = false
                    scrobbleResults.forEach { (scrobblable, result) ->
                        if (!result.isSuccessful) {
                            val errMsg = scrobbleResults[scrobblable]?.errorMessage
                                ?: context.getString(R.string.network_error)
                            failedTextLines += "<b>" + scrobblable.userAccount.type + ":</b> $errMsg"
                        } else if (result.isSuccessful && result.isIgnored) {
                            failedTextLines += "<b>" + scrobblable.userAccount.type + ":</b> " +
                                    context.getString(R.string.scrobble_ignored)
                            ignored = true
                        }
                    }
                    if (failedTextLines.isNotEmpty()) {
                        val failedText = failedTextLines.joinToString("<br>\n")
                        Stuff.log("failedText= $failedText")
                        val i = if (ignored) {
                            Intent(NLService.iBAD_META_S)
                                .setPackage(context.packageName)
                                .putSingle(trackInfo.updateMetaFrom(scrobbleData))
                                .putSingle(
                                    ScrobbleError(
                                        "",
                                        failedText,
                                        trackInfo.packageName,
                                        canForceScrobble = forceable
                                    )
                                )
                        } else {
                            Intent(NLService.iOTHER_ERR_S)
                                .setPackage(context.packageName)
                                .putSingle(
                                    ScrobbleError(
                                        if (savedAsPending)
                                            context.getString(R.string.saved_as_pending)
                                        else
                                            "",
                                        failedText,
                                        trackInfo.packageName,
                                    )
                                )
                        }
                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                    }

                } catch (e: NullPointerException) {
                }
            }
        }
    }

    fun loveOrUnlove(track: Track, love: Boolean, callback: ((Boolean) -> Unit)? = null) {
        toExec = toExec@{
            Stuff.log(this::loveOrUnlove.name + " " + love)

            if (track.artist.isNullOrEmpty() || track.name.isNullOrEmpty())
                return@toExec null

            var submittedAll = true

            // update the cache
            PanoDb.db.getCachedTracksDao().apply {
                val tr = findExact(track.artist, track.name) ?: track.toCachedTrack()
                tr.isLoved = love
                insert(listOf(tr))
            }

            val dao = PanoDb.db.getPendingLovesDao()
            val pl = dao.find(track.artist, track.name)
            if (pl != null) {
                if (pl.shouldLove == !love) {
                    pl.shouldLove = love
                    Scrobblables.all.forEach {
                        pl.state = pl.state or (1 shl it.userAccount.type.ordinal)
                    }
                    dao.update(pl)
                }
                submittedAll = false
            } else {
                val successes = Scrobblables.all.mapConcurrently(5) {
                    it to it.loveOrUnlove(track, love)
                }.toMap()

                if (successes.values.any { !it }) {
                    val entry = PendingLove()
                    entry.artist = track.artist
                    entry.track = track.name
                    entry.shouldLove = love
                    successes.forEach { (scrobblable, success) ->
                        if (!success)
                            entry.state =
                                entry.state or (1 shl scrobblable.userAccount.type.ordinal)
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

    fun delete(track: Track, callback: (suspend (Boolean) -> Unit)? = null) {
        toExec = {
            val results = Scrobblables.all.mapConcurrently(5) {
                runCatching {
                    val succ = it.delete(track)
                    if (!succ)
                        Stuff.log("Delete failed for " + it::class.java.name)
                    assert(succ)
                }
            }

            val success = results.all { it.isSuccess }

            withContext(Dispatchers.Main) {
                callback!!.invoke(success)
            }

            if (success)
                CachedTracksDao.deltaUpdateAll(track, -1, DirtyUpdate.BOTH)

            null
        }
    }

    fun doAuth(userAccountTemp: UserAccountTemp) {
        toExec = {
            if (userAccountTemp.authKey.isNotEmpty()) {
                when (userAccountTemp.type) {
                    AccountType.LASTFM -> {
                        val session =
                            Authenticator.getSession(
                                null,
                                userAccountTemp.authKey,
                                Stuff.LAST_KEY,
                                Stuff.LAST_SECRET
                            )

                        if (session != null) {
                            UserAccountSerializable(
                                AccountType.LASTFM,
                                User.getInfo(session).toUserSerializable(),
                                session.key,
                            ).let {
                                Scrobblables.add(it)
                            }
                        }
                    }

                    AccountType.LIBREFM -> {
                        val session = Authenticator.getSession(
                            Stuff.LIBREFM_API_ROOT,
                            userAccountTemp.authKey, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY
                        )

                        if (session != null) {
                            Scrobblables.add(
                                UserAccountSerializable(
                                    AccountType.LIBREFM,
                                    User.getInfo(session.username, session).toUserSerializable(),
                                    session.key,
                                    Stuff.LIBREFM_API_ROOT
                                )
                            )
                        }
                    }

                    AccountType.GNUFM -> {
                        val session = Authenticator.getSession(
                            userAccountTemp.apiRoot + "2.0/",
                            userAccountTemp.authKey, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY
                        )

                        if (session != null) {
                            Scrobblables.add(
                                UserAccountSerializable(
                                    AccountType.GNUFM,
                                    User.getInfo(session.username, session).toUserSerializable(),
                                    session.key,
                                    session.apiRootUrl.substringBeforeLast("2.0/"),
                                    session.isTlsNoVerify
                                )
                            )
                        }
                    }

                    AccountType.LISTENBRAINZ, AccountType.CUSTOM_LISTENBRAINZ -> {
                        TODO("NOT IMPLEMENTED")
                    }
                }
            }
            true
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

        private var lastNp = ScrobbleData() to 0L

        private var lastNpInfoTime = 0L
        private var lastNpInfoCount = 0

        private val indexingMutex = Mutex()

        val okHttpClient by lazy {
            OkHttpClient.Builder()
                .followRedirects(false)
                .readTimeout(Stuff.READ_TIMEOUT_SECS, TimeUnit.SECONDS)
                .build()
            // default timeouts are 10 seconds for each step and none for call
        }

        val okHttpClientTlsNoVerify by lazy {
            Caller.getInstance().createOkHttpClientIgnoreSslErrors(okHttpClient)!!
        }

        fun getValidTrack(
            artist: String,
            title: String,
        ): Track? {
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
                Track.getInfo(artist, title, Stuff.LAST_KEY)
            } catch (e: Exception) {
                null
            }
            if (track != null)
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

        @Deprecated("now done client side")
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

        fun ExceptionNotifier(
            errorLiveData: MutableLiveData<Throwable>? = null,
            timberLog: Boolean = false
        ) =
            CoroutineExceptionHandler { coroutineContext, throwable ->
                when (throwable) {
                    is CancellationException,
                    is InterruptedIOException,
                    is InterruptedException -> {

                    }

                    else -> {
                        if (errorLiveData != null)
                            errorLiveData.postValue(throwable)
                        else {
                            if (timberLog)
                                Timber.tag(Stuff.TAG).e(throwable)
                            else
                                throwable.printStackTrace()
                            if (BuildConfig.DEBUG) {
                                App.context.toast("err: " + throwable.message)
                            }
                        }
                    }
                }
            }

    }
}
