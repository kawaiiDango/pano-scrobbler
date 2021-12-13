package com.arn.scrobble

import android.content.Context
import android.content.Intent
import android.util.LruCache
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.charts.ChartsOverviewFragment
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.pref.HistoryPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.Lastfm
import com.arn.scrobble.scrobbleable.Scrobblable
import com.arn.scrobble.search.SearchVM
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


/**
 * Created by arn on 18-03-2017.
 */

class LFMRequester(
    context: Context,
    private val scope: CoroutineScope,
    private val liveData: MutableLiveData<*>? = null,
) {
    private var contextWr = WeakReference(context.applicationContext)
    private val context
        get() = contextWr.get()!!
    private val prefs by lazy { MainPrefs(this.context) }
    private lateinit var job: Job
    private val coExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        when(throwable) {
            is CancellationException,
                is InterruptedIOException,
                is InterruptedException -> {

            }
            else -> {
                throwable.printStackTrace()
                if (contextWr.get() != null && BuildConfig.DEBUG) {
                    scope.launch(Dispatchers.Main) {
                        Stuff.toast(context, "err: " + throwable.message)
                    }
                }
            }
        }
    }

    private var toExec: (suspend () -> Any?) = {}
        set(fn) {
            field = fn

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
    private val lastfmSession by lazy { Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey) }
    private val lastfmUsername by lazy { prefs.lastfmUsername }

    private fun checkSession(usernamep: String? = null) {
        if (usernamep == null && lastfmSessKey == null)
            throw Exception("Login required")
    }

    fun getRecents(page: Int, to: Long, cached: Boolean, usernamep: String?) {
        toExec = {
            checkSession(usernamep)
            Stuff.log(this::getRecents.name + " " + page)
            val from = if (to == 0L) 0L else 1L
            User.getRecentTracks(usernamep, page, 50, true, from, to/1000, cached, lastfmSession)
        }
    }

    fun getLoves(page: Int, cached: Boolean, usernamep: String?) {
        toExec = {
            checkSession(usernamep)
            Stuff.log(this::getLoves.name + " " + page)
            val pr = User.getLovedTracks(usernamep, page, 50, cached, lastfmSession)
            pr.pageResults.forEach {
                it.isLoved = true
                it.imageUrlsMap = null
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

    fun getTrackFirstScrobble(pr: PaginatedResult<Track>?, limit: Int = 50) {
        toExec = {
            val track = pr?.pageResults?.firstOrNull()
            var firstScrobbleDate: Date? = null

            if (track != null) {
                firstScrobbleDate = if (pr.total > limit)
                    User.getTrackScrobbles(
                        track.artist,
                        track.name,
                        pr.username,
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
            Pair(username, User.getRecentTracks(username, 1, 1, false, 0, 0, !MainActivity.isOnline, lastfmSession))
        }
    }

    fun getDrawerInfo() {
        toExec = {
            checkSession()
            Stuff.log(this::getDrawerInfo.name)
            val profile = User.getInfo(lastfmSession)
            val cal = Calendar.getInstance()
            cal.setMidnight()
            val recents = User.getRecentTracks(null, 1, 1,
                    cal.timeInMillis/1000, System.currentTimeMillis()/1000, lastfmSession)

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
            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            var pr: PaginatedResult<User>
            try {
                pr = User.getFriends(username, page, 30, !MainActivity.isOnline, lastfmSession)
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
                pr = PaginatedResult(page, totalPages, users.size, users, null)
            }
            pr
        }
    }

    fun getCharts(type: Int, period: Period, page: Int, usernamep: String?, limit: Int = 50) {
        toExec = {
            Stuff.log(this::getCharts.name + " " + page)
            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            val pr = when (type) {
                Stuff.TYPE_ARTISTS -> User.getTopArtists(username, period, limit, page, lastfmSession)
                Stuff.TYPE_ALBUMS -> User.getTopAlbums(username, period, limit, page, lastfmSession)
                else -> User.getTopTracks(username, period, limit, page, lastfmSession)
            }
            pr
        }
    }

    fun getWeeklyChartsList(usernamep: String?, scrobblingSincep: Long) {
        toExec = {
            Stuff.log(this::getWeeklyChartsList.name)
            val username: String
            var scrobblingSince = scrobblingSincep
            if (usernamep == null) {
                username = lastfmUsername ?: throw Exception("Login required")
                scrobblingSince = prefs.scrobblingSince
            } else {
                username = usernamep
                if (scrobblingSincep == 0L)
                    scrobblingSince = User.getInfo(username, lastfmSession)?.registeredDate?.time ?: 0
            }

            User.getWeeklyChartListAsCharts(username, lastfmSession)
                    .filter { it.to.time > scrobblingSince }
                    .reversed()
        }
    }

    fun getWeeklyCharts(type: Int, from: Long, to: Long, usernamep: String?) {
        toExec = {
            Stuff.log(this::getWeeklyCharts.name)
            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            val chart = when (type) {
                Stuff.TYPE_ARTISTS -> User.getWeeklyArtistChart(username, from.toString(), to.toString(), -1, lastfmSession)
                Stuff.TYPE_ALBUMS -> User.getWeeklyAlbumChart(username, from.toString(), to.toString(), -1, lastfmSession)
                else -> User.getWeeklyTrackChart(username, from.toString(), to.toString(), -1, lastfmSession)
            }
            PaginatedResult(1, 1, chart.entries.size, chart.entries, username)
        }
    }

    fun getDigest(period: Period, callback: (List<String>) -> Unit) {
        toExec = {
            supervisorScope {
                val limit = 3
                val digestArr = mutableListOf<String>()
                val artists = async {
                    User.getTopArtists(
                        lastfmUsername,
                        period,
                        limit,
                        1,
                        lastfmSession
                    )
                }
                val albums = async {
                    User.getTopAlbums(
                        lastfmUsername,
                        period,
                        limit,
                        1,
                        lastfmSession
                    )
                }
                val tracks = async {
                    User.getTopTracks(
                        lastfmUsername,
                        period,
                        limit,
                        1,
                        lastfmSession
                    )
                }

                try {
                    artists.await()?.let {
                        if (!it.isEmpty) {
                            digestArr += context.getString(R.string.top_artists) + ":"
                            digestArr += it.pageResults.joinToString { it.name }
                        }
                    }
                } catch (e: Exception) {}

                try {
                    albums.await()?.let {
                        if (!it.isEmpty) {
                            digestArr += context.getString(R.string.top_albums) + ":"
                            digestArr += it.pageResults.joinToString { it.name }
                        }
                    }
                } catch (e: Exception) {}

                try {
                    tracks.await()?.let {
                        if (!it.isEmpty) {
                            digestArr += context.getString(R.string.top_tracks) + ":"
                            digestArr += it.pageResults.joinToString { it.name }
                        }
                    }
                } catch (e: Exception) {}

                withContext(Dispatchers.Main) {
                    callback(digestArr)
                }

                null
            }
        }
    }

    fun getRandom(type: Int, totalp: Int, usernamep: String?) {
        toExec = {
            checkSession()
            val page: Int
            var total = totalp
            if (total == -1) {
                total = when (type) {
                    Stuff.TYPE_TRACKS -> User.getRecentTracks(usernamep, 1, 1, false, lastfmSession)?.totalPages
                            ?: 0
                    Stuff.TYPE_LOVES -> User.getLovedTracks(usernamep, 1, 1, lastfmSession)?.totalPages
                            ?: 0
                    else -> 0
                }
            }
            var track: Track? = null
            if (total > 0) {
                page = (1..total).random()
                track = when (type) {
                    Stuff.TYPE_TRACKS -> User.getRecentTracks(usernamep, page, 1, true, lastfmSession)?.pageResults?.last()
                    //if there's a now playing, it returns two items
                    Stuff.TYPE_LOVES -> {
                        val t = User.getLovedTracks(usernamep, page, 1, lastfmSession)?.pageResults?.last()
                        if (t != null) {
                            val info = Track.getInfo(t.artist, t.name, Stuff.LAST_KEY)
                            if (info != null)
                                t.imageUrlsMap = info.imageUrlsMap
                        }
                        t
                    }
                    else -> null
                }
            }
            RandomVM.RandomTrackData(total, track, type)
        }
    }

    fun getSearches(term:String) {
        toExec = {
            supervisorScope {
                val artists = async {
                    try {
                        Artist.search(term, Stuff.LAST_KEY).toList()
                    } catch (e: Exception) {
                        listOf<Artist>()
                    }
                }
                val albums = async {
                    try {
                        Album.search(term, Stuff.LAST_KEY).toList()
                    } catch (e: Exception) {
                        listOf<Album>()
                    }
                }
                val tracks = async {
                    try {
                        Track.search(term, Stuff.LAST_KEY).toList()
                    } catch (e: Exception) {
                        listOf<Track>()
                    }
                }
                SearchVM.SearchResults(term, artists.await(), albums.await(), tracks.await())
            }
        }
    }

    fun getScrobbleCounts(periods: List<ChartsOverviewFragment.ScrobbleCount>, usernamep: String?) {
        toExec = {
            Stuff.log(this::getScrobbleCounts.name)
            supervisorScope {
                periods.mapConcurrently(5) {
                    try {
                        val pr = User.getRecentTracks(
                            usernamep,
                            1,
                            1,
                            false,
                            it.from / 1000,
                            it.to / 1000,
                            false,
                            lastfmSession
                        )
                        it.count = pr.total
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                periods
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
                val topUserTags = User.getTopTags(null, 20, lastfmSession)
                topUserTags
                    ?.reversed()
                    ?.forEach {
                       historyPref.add(it.name)
                    }
                historyPref.save()
                prefs.userTopTagsFetched = true
            }

            val list = when(entry) {
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
            when(entry) {
                is Artist -> Artist.addTags(entry.name, tags, lastfmSession)
                is Album -> Album.addTags(entry.artist, entry.name, tags, lastfmSession)
                is Track -> Track.addTags(entry.artist, entry.name, tags, lastfmSession)
                else -> throw RuntimeException("invalid type")
            }
        }
    }

    fun deleteUserTagsForEntry(entry: MusicEntry, tag: String) {
        toExec = {
            when(entry) {
                is Artist -> Artist.removeTag(entry.name, tag, lastfmSession)
                is Album -> Album.removeTag(entry.artist, tag, entry.name, lastfmSession)
                is Track -> Track.removeTag(entry.artist, tag, entry.name, lastfmSession)
                else -> throw RuntimeException("invalid type")
            }
        }
    }

    fun getInfo(artist: String, album: String?, track: String?, usernamep: String?) {
        toExec = {
            Stuff.log(this::getInfo.name)
            supervisorScope {
                val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")

                var albumArtistName: String? = null
                var albumName = album

                val trackDef = async(start = CoroutineStart.LAZY) {
                    try {
                        Track.getInfo(artist, track!!, null, username, null, Stuff.LAST_KEY)
                    } catch (e: Exception) {
                        null
                    }
                }

                val artistDef = async(start = CoroutineStart.LAZY) {
                    try {
                        Artist.getInfo(artist, null, username, true, Stuff.LAST_KEY)
                    } catch (e: Exception) {
                        null
                    }
                }

                val albumArtistDef = async(start = CoroutineStart.LAZY) {
                    try {
                        if (!albumArtistName.isNullOrEmpty() && albumArtistName!!.lowercase() != artist.lowercase())
                            Artist.getInfo(albumArtistName!!, null, username, true, Stuff.LAST_KEY)
                        else
                            null
                    } catch (e: Exception) {
                        null
                    }
                }

                val albumDef = async(start = CoroutineStart.LAZY) {
                    try {
                        Album.getInfo(
                            albumArtistName ?: artist,
                            albumName!!,
                            username,
                            Stuff.LAST_KEY
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                if (!track.isNullOrEmpty()) {
                    val trackInfo = trackDef.await()

                    if (album == null)
                        albumName = trackInfo?.album
                    albumArtistName = trackInfo?.albumArtist

                    withContext(Dispatchers.Main) {
                        liveData!!.value = NLService.B_TRACK to trackInfo
                    }
                }

                val (artistInfo, albumArtistInfo, albumInfo) = awaitAll(artistDef, albumArtistDef, albumDef)

                withContext(Dispatchers.Main) {
                    liveData!!.value = NLService.B_ARTIST to artistInfo
                    if (albumArtistInfo != null)
                        liveData.value = NLService.B_ALBUM_ARTIST to albumArtistInfo
                    liveData.value = NLService.B_ALBUM to albumInfo
                }
            }
            null
        }
    }

    fun getListenerTrend(url: String) {
        toExec = {
            val monthlyPlayCounts = mutableListOf(0, 0, 0, 0, 0)
            if (MainActivity.isOnline && url != "") {
                val request = Request.Builder()
                    .url(url)
                    .build()
                try {
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
                }
            }
            monthlyPlayCounts
        }
    }

    fun scrobble(nowPlaying: Boolean, scrobbleData: ScrobbleData, hash: Int, packageName: String, unparsedData: ScrobbleData? = null) {
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
                        if (PanoDb.getDb(context)
                                .getBlockedMetadataDao()
                                .isBlocked(scrobbleData, otherArtist)
                        ) {
                            val i = Intent(NLService.iCANCEL).apply {
                                putExtra(NLService.B_HASH, hash)
                            }
                            context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
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
                    var edit =
                        editsDao.find(scrobbleData.artist, scrobbleData.album, scrobbleData.track)
                    edit?.let {
                        scrobbleData.artist = it.artist
                        scrobbleData.album = it.album
                        scrobbleData.track = it.track
                        scrobbleData.albumArtist = it.albumArtist
                    }

                    val oldArtist = scrobbleData.artist
                    val oldTrack = scrobbleData.track

                    val regexEdits = PanoDb.getDb(context)
                        .getRegexEditsDao()
                        .performRegexReplace(scrobbleData)

                    if (scrobbleData.artist.isNullOrBlank())
                        scrobbleData.artist = oldArtist

                    if (scrobbleData.track.isNullOrBlank())
                        scrobbleData.track = oldTrack

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

                    if (MainActivity.isOnline) {
                        val (lastScrobbleData, lastTime) = lastNp
                        lastNp = scrobbleData to System.currentTimeMillis()
                        lastScrobbleData.timestamp = scrobbleData.timestamp

                        val cacheOnly = if (lastScrobbleData == scrobbleData &&
                            System.currentTimeMillis() - lastTime < Stuff.TRACK_INFO_VALIDITY) {
                            if (System.currentTimeMillis() - lastTime < 1000)
                                Timber.tag(Stuff.TAG).w(Exception("Possible duplicate scrobble"))
                            true
                        }
                        else
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
                            edit = editsDao.find(
                                scrobbleData.artist,
                                scrobbleData.album,
                                scrobbleData.track
                            )
                            edit?.let {
                                scrobbleData.artist = it.artist
                                scrobbleData.album = it.album
                                if (it.albumArtist.isNotBlank())
                                    scrobbleData.albumArtist = it.albumArtist
                                scrobbleData.track = it.track
                                track =
                                    getValidTrack(
                                        scrobbleData.artist,
                                        scrobbleData.track,
                                        lastfmUsername!!
                                    )
                            }
                            PanoDb.getDb(context)
                                .getRegexEditsDao()
                                .performRegexReplace(scrobbleData)

                            if (shouldBlockScrobble(null))
                                return@coroutineScope
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
                    if (MainActivity.isOnline) {
                        if (correctedArtist != null || edit != null) {
                            if (prefs.submitNowPlaying) {
                                scrobblablesMap.forEach { (stringId, scrobblable) ->
                                    if (scrobblable != null) {
                                        if (!isActive)
                                            throw CancellationException()
                                        scrobbleResults[stringId] = scrobblable.updateNowPlaying(scrobbleData)
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

                    if (MainActivity.isOnline) {
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
                                    entry.state = entry.state or (1 shl Stuff.SERVICE_BIT_POS[key]!!)
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
                    scrobbleResults.forEach { (key, result) ->
                        if (!result.isSuccessful) {
                            val errMsg = scrobbleResults[key]?.errorMessage
                                ?: context.getString(R.string.network_error)
                            failedTextLines += "<b>" + context.getString(key) + ":</b> $errMsg"
                        } else if (result.isSuccessful && result.isIgnored) {
                            failedTextLines += "<b>" + context.getString(key) + ":</b> " +
                                    context.getString(R.string.scrobble_ignored)
                        }
                    }
                    if (failedTextLines.isNotEmpty()) {
                        val failedText = failedTextLines.joinToString("<br>\n")
                        Stuff.log("failedText= $failedText")
                        val i = Intent(NLService.iOTHER_ERR_S).apply {
                            putExtra(NLService.B_ERR_MSG, failedText)
                            putExtra(NLService.B_PENDING, savedAsPending)
                            putExtra(NLService.B_HASH, hash)
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
            Stuff.log(this::loveOrUnlove.name+ " " + love)
            var submittedAll = true
            val scrobblablesMap = Scrobblable.getScrobblablesMap(prefs, shouldSupportLove = true)

            val dao = PanoDb.getDb(context).getLovesDao()
            val pl = dao.find(track.artist, track.name)
            if (pl != null){
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
            val scrobblablesMap = Scrobblable.getScrobblablesMap(prefs, shouldSupportLove = true)
            val unscrobbler = LastfmUnscrobbler(context)
            val success = unscrobbler.haveCsrfCookie() &&
                    unscrobbler.unscrobble(track.artist, track.name, track.playedWhen.time)

            withContext(Dispatchers.Main) {
                callback!!.invoke(success)
            }

            scrobblablesMap[R.string.librefm]?.let {
                it as Lastfm
                Library.removeScrobble(track.artist, track.name, track.playedWhen.time/1000, it.session)
            }

            scrobblablesMap[R.string.gnufm]?.let {
                it as Lastfm
                Library.removeScrobble(track.artist, track.name, track.playedWhen.time/1000, it.session)
            }
            null
        }
    }

    // adb shell am start -W -a android.intent.action.VIEW -d "pscrobble://auth?token=hohoho" com.arn.scrobble

    fun doAuth(@StringRes type: Int, token: String?) {
        toExec = {
            if (!token.isNullOrEmpty()) {
                when (type) {
                    R.string.lastfm -> {
                        val lastfmSession = Authenticator.getSession(null, token, Stuff.LAST_KEY, Stuff.LAST_SECRET)
                        if (lastfmSession != null) {
                            prefs.lastfmUsername = lastfmSession.username
                            prefs.lastfmSessKey = lastfmSession.key
                        }
                    }
                    R.string.librefm -> {
                        val librefmSession = Authenticator.getSession(Stuff.LIBREFM_API_ROOT,
                                token, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY)
                        if (librefmSession != null) {
                            prefs.librefmUsername = librefmSession.username
                            prefs.librefmSessKey = librefmSession.key
                        }
                    }
                    R.string.gnufm -> {
                        val gnufmSession = Authenticator.getSession(
                                prefs.gnufmRoot + "2.0/",
                                token, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY)
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

        fun getValidTrack(artist: String, title: String, lastfmUsername: String, cacheOnly: Boolean = false): Track? {
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

        fun getValidArtist(artist:String, set:Set<String>? = null): String? {
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
                if (artistInfo!= null && artistInfo.name?.trim() != ""){
                    if(artistInfo.listeners == -1 || artistInfo.listeners >= Stuff.MIN_LISTENER_COUNT) {
                        validArtistsCache.put(artist, artistInfo.name)
                        return artistInfo.name
                    } else
                        validArtistsCache.put(artist, "")
                } else
                    validArtistsCache.put(artist, "")
            }
            return null
        }

        fun getArtistInfoLibreFM(artist: String):Artist? {
            val result = Caller.getInstance().call(Stuff.LIBREFM_API_ROOT, "artist.getInfo",
                    "", mapOf("artist" to artist, "autocorrect" to "1"))
            return ResponseBuilder.buildItem(result, Artist::class.java)
        }

        fun getArtistInfoSpotify(artist: String):Artist? {
            if (Tokens.SPOTIFY_ARTIST_INFO_SERVER.isEmpty() || Tokens.SPOTIFY_ARTIST_INFO_KEY.isEmpty())
                return null
            val result = Caller.getInstance().call(Tokens.SPOTIFY_ARTIST_INFO_SERVER, "artist.getInfo.spotify",
                    Tokens.SPOTIFY_ARTIST_INFO_KEY, mapOf("artist" to artist))
            return ResponseBuilder.buildItem(result, Artist::class.java)
        }

        fun getCorrectedDataOld(artist:String, track: String): Pair<String, String>? {
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
