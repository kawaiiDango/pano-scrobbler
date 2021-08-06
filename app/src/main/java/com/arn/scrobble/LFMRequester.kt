package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.LruCache
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.Stuff.toBundle
import com.arn.scrobble.charts.ChartsOverviewFragment
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MultiPreferences
import com.arn.scrobble.search.SearchVM
import de.umass.lastfm.*
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import timber.log.Timber
import java.io.IOException
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import kotlin.random.Random


/**
 * Created by arn on 18-03-2017.
 */

class LFMRequester(context: Context) {
    private var contextWr = WeakReference(context.applicationContext)
    private val context
        get() = contextWr.get()!!
    private val prefs by lazy { DualPref(skipCP, this.context) }
    private var skipCP = false
    private lateinit var toExec: (() -> Any?)
    private val lastfmSessKey by lazy { prefs.getString(Stuff.PREF_LASTFM_SESS_KEY, null) }
    private val lastfmSession by lazy { Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey) }
    private val lastfmUsername by lazy { prefs.getString(Stuff.PREF_LASTFM_USERNAME, null) }

    private fun checkSession(usernamep: String? = null) {
        if (usernamep == null && lastfmSessKey == null)
            throw Exception("Login required")
    }

    fun getRecents(page: Int, to: Long, cached: Boolean, usernamep: String?): LFMRequester {
        toExec = {
            checkSession(usernamep)
            Stuff.log(this::getRecents.name + " " + page)
            val from = if (to == 0L) 0L else 1L
            User.getRecentTracks(usernamep, page, 50, true, from, to/1000, cached, lastfmSession, null)
        }
        return this
    }

    fun getLoves(page: Int, cached: Boolean, usernamep: String?): LFMRequester {
        toExec = {
            checkSession(usernamep)
            Stuff.log(this::getLoves.name + " " + page)
            val pr = User.getLovedTracks(usernamep, page, 50, cached, lastfmSession, null)
            pr.pageResults.forEach {
                it.isLoved = true
                it.imageUrlsMap = null
            }
            pr
        }
        return this
    }

    fun getSimilarTracks(artist: String, track: String): LFMRequester {
        toExec = {
            Track.getSimilar(artist, track, Stuff.LAST_KEY, 50)
        }
        return this
    }

    fun getSimilarArtists(artist: String): LFMRequester {
        toExec = {
            Artist.getSimilar(artist, 50, Stuff.LAST_KEY)
        }
        return this
    }

    fun getArtistTopTracks(artist: String): LFMRequester {
        toExec = {
            Artist.getTopTracks(artist, Stuff.LAST_KEY)
        }
        return this
    }
    fun getArtistTopAlbums(artist: String): LFMRequester {
        toExec = {
            Artist.getTopAlbums(artist, Stuff.LAST_KEY)
        }
        return this
    }

    fun getFriendsRecents(username: String): LFMRequester {
        toExec = {
            Pair(username, User.getRecentTracks(username, 1, 1, false, 0, 0, !Main.isOnline, null, Stuff.LAST_KEY))
        }
        return this
    }

    fun getDrawerInfo(): LFMRequester {
        toExec = {
            checkSession()
            Stuff.log(this::getDrawerInfo.name)
            val profile = User.getInfo(lastfmSession)
            val cal = Calendar.getInstance()
            cal.setMidnight()
            val recents = User.getRecentTracks(null, 1, 1,
                    cal.timeInMillis/1000, System.currentTimeMillis()/1000, lastfmSession, null)

            if (profile != null && recents != null)
                DrawerData(
                    todayScrobbles = recents.totalPages,
                    totalScrobbles = profile.playcount,
                    registeredDate = profile.registeredDate?.time ?: 0,
                    profilePicUrl = profile.getWebpImageURL(ImageSize.EXTRALARGE) ?: "",
                ).apply { saveToPref(context) }
            else
                DrawerData.loadFromPref(context)
        }
        return this
    }

    fun getFriends(page: Int, usernamep: String?): LFMRequester {
        toExec = {
            Stuff.log(this::getFriends.name + " " + page)
            checkSession(usernamep)
            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            var pr: PaginatedResult<User>
            try {
                pr = User.getFriends(username, page, 30, !Main.isOnline, null, Stuff.LAST_KEY)
            } catch (e: NullPointerException) {
                val url = URL("https://www.last.fm/user/$username/following?page=$page")
                var urlConnection: HttpURLConnection? = null
                val users = mutableListOf<User>()
                try {
                    var idx = 0
                    urlConnection = url.openConnection() as HttpURLConnection
                    urlConnection.setRequestProperty("Accept-Encoding", "gzip")
                    urlConnection.instanceFollowRedirects = false

                    if (urlConnection.responseCode != 200)
                        idx = -1
                    val resp = slurp(urlConnection, 1024)
                    if (resp == "")
                        idx = -1
                    if (idx > -1)
                        idx = resp.indexOf("<ul class=\"user-list\">", 50000)
                    var idx2: Int
                    if (idx > -1) {
                        do {
                            idx = resp.indexOf("  link-block-target", idx)
                            if (idx > -1)
                                idx = resp.indexOf(">", idx + 1)
                            if (idx > -1) {
                                idx += 1
                                idx2 = resp.indexOf("<", idx)
                                val uname = resp.substring(idx, idx2)
                                idx = resp.indexOf("<img", idx2)
                                idx = resp.indexOf("\"", idx)
                                idx2 = resp.indexOf("\"", idx + 1)
                                val imageUrl = resp.substring(idx + 1, idx2)
                                val user = User(uname, "https://www.last.fm/user/$uname")
                                user.imageURL = imageUrl
                                users.add(user)
                                idx = idx2
                            }
                        } while (idx > -1)

                    }
                } catch (e: Exception) {
                } finally {
                    urlConnection?.disconnect()
                }
                val totalPages = if (users.isEmpty())
                    page
                else
                    10
                pr = PaginatedResult(page, totalPages, users.size, users, null)
            }
            pr
        }
        return this
    }

    fun getCharts(type: Int, period: Period, page: Int, usernamep: String?, limit: Int = 50): LFMRequester {
        toExec = {
            Stuff.log(this::getCharts.name + " " + page)
            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            val pr = when (type) {
                Stuff.TYPE_ARTISTS -> User.getTopArtists(username, period, limit, page, Stuff.LAST_KEY)
                Stuff.TYPE_ALBUMS -> User.getTopAlbums(username, period, limit, page, Stuff.LAST_KEY)
                else -> User.getTopTracks(username, period, limit, page, Stuff.LAST_KEY)
            }
            pr
        }
        return this
    }

    fun getWeeklyChartsList(usernamep: String?, scrobblingSincep: Long): LFMRequester {
        toExec = {
            Stuff.log(this::getWeeklyChartsList.name)
            val username: String
            var scrobblingSince = scrobblingSincep
            if (usernamep == null) {
                username = lastfmUsername ?: throw Exception("Login required")
                scrobblingSince = context
                        .getSharedPreferences(Stuff.ACTIVITY_PREFS, MODE_PRIVATE)
                        ?.getLong(Stuff.PREF_ACTIVITY_SCROBBLING_SINCE, 0) ?: 0
            } else {
                username = usernamep
                if (scrobblingSincep == 0L)
                    scrobblingSince = User.getInfo(username, Stuff.LAST_KEY)?.registeredDate?.time ?: 0
            }

            User.getWeeklyChartListAsCharts(username, Stuff.LAST_KEY)
                    .filter { it.to.time > scrobblingSince }
                    .reversed()
        }
        return this
    }

    fun getWeeklyCharts(type: Int, from: Long, to: Long, usernamep: String?): LFMRequester {
        toExec = {
            Stuff.log(this::getWeeklyCharts.name)
            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            val chart = when (type) {
                Stuff.TYPE_ARTISTS -> User.getWeeklyArtistChart(username, from.toString(), to.toString(), -1, Stuff.LAST_KEY)
                Stuff.TYPE_ALBUMS -> User.getWeeklyAlbumChart(username, from.toString(), to.toString(), -1, Stuff.LAST_KEY)
                else -> User.getWeeklyTrackChart(username, from.toString(), to.toString(), -1, Stuff.LAST_KEY)
            }
            PaginatedResult(1, 1, chart.entries.size, chart.entries, username)
        }
        return this
    }

    fun getDigest(period: Period): LFMRequester {
        toExec = {
            val digestArr = mutableListOf<String>()
            var artists: PaginatedResult<Artist>? = null
            var albums: PaginatedResult<Album>? = null
            var tracks: PaginatedResult<Track>? = null

            val es = Executors.newCachedThreadPool()
            es.execute {
                try {
                    artists = LFMRequester(context)
                            .skipContentProvider()
                            .getCharts(Stuff.TYPE_ARTISTS, period, 1, null, 3)
                            .toExec() as? PaginatedResult<Artist>?
                } catch (e: CallException) {}
            }
            es.execute {
                try {
                    albums = LFMRequester(context)
                            .skipContentProvider()
                            .getCharts(Stuff.TYPE_ALBUMS, period, 1, null, 3)
                            .toExec() as? PaginatedResult<Album>?
                } catch (e: CallException) {}
            }
            es.execute {
                try {
                    tracks = LFMRequester(context)
                            .skipContentProvider()
                            .getCharts(Stuff.TYPE_TRACKS, period, 1, null, 3)
                            .toExec() as? PaginatedResult<Track>?
                } catch (e: CallException) {}
            }
            es.shutdown()
            es.awaitTermination(30, TimeUnit.SECONDS)

            artists?.let {
                if (!it.isEmpty) {
                    digestArr += context.getString(R.string.top_artists) + ":"
                    digestArr += it.pageResults.joinToString { it.name }
                }
            }

            albums?.let {
                if (!it.isEmpty) {
                    digestArr += context.getString(R.string.top_albums) + ":"
                    digestArr += it.pageResults.joinToString { it.name }
                }
            }

            tracks?.let {
                if (!it.isEmpty) {
                    digestArr += context.getString(R.string.top_tracks) + ":"
                    digestArr += it.pageResults.joinToString { it.name }
                }
            }

            digestArr
        }
        return this
    }

    fun getRandom(type: Int, totalp: Int, rnd: Random): LFMRequester {
        toExec = {
            Stuff.log(this::getRandom.name + " " + type + " " + totalp)
            checkSession()
            var page = 1
            var total = totalp
            if (total == -1) {
                total = when (type) {
                    Stuff.TYPE_TRACKS -> User.getRecentTracks(null, 1, 1, false, lastfmSession, null)?.totalPages
                            ?: 0
                    Stuff.TYPE_LOVES -> User.getLovedTracks(null, 1, 1, lastfmSession, null)?.totalPages
                            ?: 0
                    else -> 0
                }
            }
            var track: Track? = null
            if (total > 0) {
                page = rnd.nextInt(1, total + 1)
                track = when (type) {
                    Stuff.TYPE_TRACKS -> User.getRecentTracks(null, page, 1, true, lastfmSession, null)?.pageResults?.last()
                    //if there's a now playing, it returns two items
                    Stuff.TYPE_LOVES -> {
                        val t = User.getLovedTracks(null, page, 1, lastfmSession, null)?.pageResults?.last()
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
        return this
    }

    fun getSearches(term:String): LFMRequester {
        toExec = {
            Stuff.log(this::getSearches.name)
            val artists = mutableListOf<Artist>()
            val albums = mutableListOf<Album>()
            val tracks = mutableListOf<Track>()
            val es = Executors.newCachedThreadPool()
            es.execute {
                try {
                    artists += Artist.search(term, Stuff.LAST_KEY)
                } catch (e: Exception) {}
            }
            es.execute {
                try {
                    albums += Album.search(term, Stuff.LAST_KEY)
                } catch (e: Exception) {}
            }
            es.execute {
                try {
                    tracks += Track.search(term, Stuff.LAST_KEY)
                } catch (e: Exception) {}
            }
            es.shutdown()
            es.awaitTermination(20, TimeUnit.SECONDS)
            SearchVM.SearchResults(term, artists, albums, tracks)
        }
        return this
    }

    fun getScrobbleCounts(periods: List<ChartsOverviewFragment.ScrobbleCount>, usernamep: String?): LFMRequester {
        toExec = {
            checkSession(usernamep)
            Stuff.log(this::getScrobbleCounts.name)
            val es = Executors.newCachedThreadPool()
            periods.forEach {
                es.execute {
                    try {
                        val pr = User.getRecentTracks(usernamep, 1, 1, false, it.from / 1000, it.to / 1000, false, lastfmSession, null)
                        it.count = pr.total
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            try {
                es.shutdown()
                es.awaitTermination(30, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                es.shutdownNow()
                Thread.currentThread().interrupt()
            }
            periods
        }
        return this
    }

    fun getTrackInfo(track: Track, pos: Int): LFMRequester {
        toExec = {
            val info = try {
                Track.getInfo(track.artist, track.name, Stuff.LAST_KEY)
            } catch (e: Exception){
                null
            }
            Pair(pos, info)
        }
        return this
    }

    fun getArtistInfo(artist: Artist, pos: Int): LFMRequester {
        toExec = {
            val info = try {
                val info = getArtistInfoSpotify(artist.name)
                if (info?.name.equals(artist.name, ignoreCase = true))
                    info
                else
                    null
            } catch (e:Exception){
                null
            }
            Pair(pos, info)
        }
        return this
    }

    fun getAlbumInfo(album: Album, pos: Int): LFMRequester {
        toExec = {
            val info = try {
                Album.getInfo(album.artist, album.name, Stuff.LAST_KEY)
            } catch (e:Exception){
                null
            }
            Pair(pos, info)
        }
        return this
    }

    fun getTagInfo(tag: String): LFMRequester {
        toExec = {
            Tag.getInfo(tag, Stuff.LAST_KEY) to
//                        Tag.getSimilar(tag, Stuff.LAST_KEY) //this doesn't work anymore
                    null
        }
        return this
    }

    fun getUserTagsForEntry(entry: MusicEntry): LFMRequester {
        toExec = {
            val list = when(entry) {
                is Artist -> Artist.getTags(entry.name, lastfmSession)
                is Album -> Album.getTags(entry.artist, entry.name, lastfmSession)
                is Track -> Track.getTags(entry.artist, entry.name, lastfmSession)
                else -> throw RuntimeException("invalid type")
            }
            list.toMutableSet()
        }
        return this
    }

    fun addUserTagsForEntry(entry: MusicEntry, tags: String): LFMRequester {
        toExec = {
            when(entry) {
                is Artist -> Artist.addTags(entry.name, tags, lastfmSession)
                is Album -> Album.addTags(entry.artist, entry.name, tags, lastfmSession)
                is Track -> Track.addTags(entry.artist, entry.name, tags, lastfmSession)
                else -> throw RuntimeException("invalid type")
            }
        }
        return this
    }

    fun deleteUserTagsForEntry(entry: MusicEntry, tag: String): LFMRequester {
        toExec = {
            when(entry) {
                is Artist -> Artist.removeTag(entry.name, tag, lastfmSession)
                is Album -> Album.removeTag(entry.artist, tag, entry.name, lastfmSession)
                is Track -> Track.removeTag(entry.artist, tag, entry.name, lastfmSession)
                else -> throw RuntimeException("invalid type")
            }
        }
        return this
    }

    fun getInfo(artist: String, albump: String?, track: String?, usernamep: String?,
             activity: Activity, mld: MutableLiveData<Pair<String, MusicEntry?>>): LFMRequester {
        toExec = {
            Stuff.log(this::getInfo.name)
            val username = usernamep ?: lastfmUsername ?: throw Exception("Login required")
            var album = albump
            var albumArtist: String? = null
            if (!track.isNullOrEmpty()) {
                val t = try {
                    Track.getInfo(artist, track, null, username, null, Stuff.LAST_KEY)
                } catch (e: Exception) {
                    null
                }
                if (albump == null)
                    album = t?.album
                albumArtist = t?.albumArtist
                activity.runOnUiThread {
                    mld.value = NLService.B_TRACK to t
                }
            }
            val es = Executors.newCachedThreadPool()
            var a: Artist? = null
            var aa: Artist? = null
            var al: Album? = null
            es.execute {
                a = try {
                    Artist.getInfo(artist, null, username, true, Stuff.LAST_KEY)
                } catch (e: Exception) {
                    null
                }
            }
            if (!albumArtist.isNullOrEmpty() && albumArtist.lowercase() != artist.lowercase()) {
                es.execute {
                    aa = try {
                        Artist.getInfo(albumArtist, null, username, true, Stuff.LAST_KEY)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            es.execute {
                al = try {
                    if (!album.isNullOrEmpty())
                        Album.getInfo(albumArtist ?: artist, album, username, Stuff.LAST_KEY)
                    else
                        null
                } catch (e: Exception) {
                    null
                }
            }
            es.shutdown()
            es.awaitTermination(20, TimeUnit.SECONDS)
            activity.runOnUiThread {
                mld.value = NLService.B_ARTIST to a
                if (aa != null)
                    mld.value = NLService.B_ALBUM_ARTIST to aa
                mld.value = NLService.B_ALBUM to al
            }
            null
        }
        return this
    }


    fun getListenerTrend(url: String): LFMRequester {
        toExec = {
            val monthlyPlayCounts = mutableListOf(0, 0, 0, 0, 0)
            if (Main.isOnline && url != "") {
                var urlConnection: HttpURLConnection? = null
                try {
                    urlConnection = URL(url).openConnection() as HttpURLConnection
                    urlConnection.setRequestProperty("Accept-Encoding", "gzip")

                    if (urlConnection.responseCode != 200)
                        throw IOException()
                    val resp = slurp(urlConnection, 1024)
                    if (resp == "")
                        throw IOException()

                    var idx = resp.indexOf("charts/listener-trend", 200000)
                    var idx2: Int
                    var days = 0
                    val daily = arrayListOf<Int>()
                    if (idx > -1) {
                        val stop1 = "data-value=\""
                        do {
                            idx = resp.indexOf(stop1, idx)
                            if (idx > -1) {
                                idx += stop1.length
                                idx2 = resp.indexOf("\"", idx)
                                val value = resp.substring(idx, idx2).toInt()
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
                } catch (e: Exception) {
                } finally {
                    urlConnection?.disconnect()
                }
            }
            monthlyPlayCounts
        }
        return this
    }

    fun scrobble(nowPlaying: Boolean, scrobbleData: ScrobbleData, hash: Int, ignoredArtist: String? = null): LFMRequester {
        toExec = fun() {
            checkSession()
            Stuff.log(this::scrobble.name + " " +
                    (if (nowPlaying) "np" else "submit")
                    + " " + scrobbleData.artist + " - " + scrobbleData.track)
            val scrobbleResults = mutableMapOf<@StringRes Int, ScrobbleResult>()
            var savedAsPending = false

            if (scrobbleData.duration < 30)
                scrobbleData.duration = -1 //default
            val serviceIdToKeys by lazy { getServiceIdToKeys() }

            if (nowPlaying) {
                // some players put the previous song and then switch to the current song in like 150ms
                // potentially wasting an api call. sleep and throw interrupt in that case
                Thread.sleep(Stuff.META_WAIT)
                if (Thread.interrupted())
                    throw InterruptedException()

                var track: Track? = null
                var correctedArtist: String? = null

                val editsDao = PanoDb.getDb(context).getSimpleEditsDao()
                var edit = editsDao.find(scrobbleData.artist, scrobbleData.album, scrobbleData.track)
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

                if (prefs.getBoolean(Stuff.PREF_PRO_STATUS, false)) {
                    if (PanoDb.getDb(context)
                            .getBlockedMetadataDao()
                            .isBlocked(scrobbleData, ignoredArtist)) {
                        val i = Intent(NLService.iCANCEL).apply {
                            putExtra(NLService.B_HASH, hash)
                        }
                        context.sendBroadcast(i)
                        return
                    }
                }

                if (scrobbleData.artist.isNullOrBlank() || scrobbleData.track.isNullOrBlank()) {
                    val b = scrobbleData.toBundle().apply {
                        putInt(NLService.B_HASH, hash)
                        putBoolean(NLService.B_FORCEABLE, ignoredArtist == null)
                        putString(NLService.B_ERR_MSG, context.getString(R.string.parse_error))
                    }
                    val i = Intent(NLService.iBAD_META).apply {
                        putExtras(b)
                    }
                    context.sendBroadcast(i)
                    return
                } else if (edit != null || regexEdits.values.sum() > 0) {
                    val b = scrobbleData.toBundle().apply {
                        putInt(NLService.B_HASH, hash)
                    }
                    val i = Intent(NLService.iMETA_UPDATE).apply {
                        putExtras(b)
                    }

                    context.sendBroadcast(i)
                }

                if (Main.isOnline) {
                    val (lastScrobbleData, lastTime) = lastNp
                    lastNp = scrobbleData to System.currentTimeMillis()

                    if (
                        lastScrobbleData.artist == scrobbleData.artist &&
                        lastScrobbleData.album == scrobbleData.album &&
                        lastScrobbleData.albumArtist == scrobbleData.albumArtist &&
                        lastScrobbleData.track == scrobbleData.track &&
                        System.currentTimeMillis() - lastTime < Stuff.TRACK_INFO_VALIDITY
                    ) {
                        if (System.currentTimeMillis() - lastTime < 1000)
                            Timber.tag(Stuff.TAG).w(Exception("Possible duplicate scrobble"))
                        track = getValidTrack(scrobbleData.artist, scrobbleData.track, lastfmUsername!!, cacheOnly= true)
                    } else if (validArtistsCache[scrobbleData.artist] == "") {
                    } else
                        track = getValidTrack(scrobbleData.artist, scrobbleData.track, lastfmUsername!!)
                        //works even if the username is wrong
                    if (Thread.interrupted())
                        throw InterruptedException()
                    if (track != null) {
                        if (scrobbleData.album == "") {
                            scrobbleData.artist = track.artist
                            if (track.album != null)
                                scrobbleData.album = track.album
                            if (track.albumArtist != null)
                                scrobbleData.albumArtist = track.albumArtist
                            scrobbleData.track = track.name
                        } else if (!track.albumArtist.isNullOrEmpty() &&
                                prefs.getBoolean(Stuff.PREF_FETCH_AA, false) &&
                                scrobbleData.album.equals(track.album, ignoreCase = true) &&
                                (scrobbleData.albumArtist.isNullOrEmpty() || scrobbleData.artist == scrobbleData.albumArtist))
                            scrobbleData.albumArtist = track.albumArtist
                    }
                    correctedArtist =
                            if (track != null && track.listeners >= Stuff.MIN_LISTENER_COUNT)
                                track.artist
                            else
                                getValidArtist(scrobbleData.artist, prefs.getStringSet(Stuff.PREF_ALLOWED_ARTISTS, null))
                    if (correctedArtist != null && scrobbleData.album == "")
                        scrobbleData.artist = correctedArtist
                }
                edit = if (track != null)
                            editsDao.find(track.artist, scrobbleData.album, track.name)
                        else
                            editsDao.find(scrobbleData.artist, scrobbleData.album, scrobbleData.track)
                if (edit != null) {
                    scrobbleData.artist = edit.artist
                    scrobbleData.album = edit.album
                    if (edit.albumArtist.isNotBlank())
                        scrobbleData.albumArtist = edit.albumArtist
                    scrobbleData.track = edit.track
                    track = getValidTrack(scrobbleData.artist, scrobbleData.track, lastfmUsername!!)
                }
                if (edit != null || track != null) {
                    val b = scrobbleData.toBundle().apply {
                        putInt(NLService.B_HASH, hash)
                        if (track != null) {
                            putBoolean(NLService.B_USER_LOVED, track.isLoved)
                            putInt(NLService.B_USER_PLAY_COUNT, track.userPlaycount)
                        }
                    }
                    val i = Intent(NLService.iMETA_UPDATE).apply {
                        putExtras(b)
                    }

                    context.sendBroadcast(i)
                }
                if (Main.isOnline) {
                    if (correctedArtist != null || edit != null) {
                        if (prefs.getBoolean(Stuff.PREF_NOW_PLAYING, true)) {
                            serviceIdToKeys[R.string.lastfm]?.let {
                                scrobbleResults[R.string.lastfm] = getScrobbleResult(scrobbleData, lastfmSession!!, true)
                            }
                            serviceIdToKeys[R.string.librefm]?.let {
                                val librefmSession: Session = Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                                        Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                                scrobbleResults[R.string.librefm] = getScrobbleResult(scrobbleData, librefmSession, true)
                            }

                            serviceIdToKeys[R.string.gnufm]?.let {
                                val gnufmSession: Session = Session.createCustomRootSession(
                                        prefs.getString(Stuff.PREF_GNUFM_ROOT, null) + "2.0/",
                                        Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                                scrobbleResults[R.string.gnufm] = getScrobbleResult(scrobbleData, gnufmSession, true)
                            }

                            serviceIdToKeys[R.string.listenbrainz]?.let {
                                scrobbleResults[R.string.listenbrainz] =
                                        ListenBrainz(it)
                                                .updateNowPlaying(scrobbleData)
                            }
                            serviceIdToKeys[R.string.custom_listenbrainz]?.let {
                                scrobbleResults[R.string.custom_listenbrainz] =
                                        ListenBrainz(it)
                                                .setApiRoot(prefs.getString(Stuff.PREF_LB_CUSTOM_ROOT, null))
                                                .updateNowPlaying(scrobbleData)
                            }
                        }
                    } else {
                        // unrecognized artist
                        val b = scrobbleData.toBundle().apply {
                            putInt(NLService.B_HASH, hash)
                            putBoolean(NLService.B_FORCEABLE, ignoredArtist == null)
                        }
                        val i = Intent(NLService.iBAD_META).apply {
                            putExtras(b)
                        }
                        context.sendBroadcast(i)
                    }
                }
            } else {
                if (Main.isOnline) {
                    serviceIdToKeys[R.string.lastfm]?.let {
                        scrobbleResults[R.string.lastfm] = getScrobbleResult(scrobbleData, lastfmSession!!, false)
                    }

                    serviceIdToKeys[R.string.librefm]?.let {
                        val librefmSession: Session = Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                                Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                        scrobbleResults[R.string.librefm] = getScrobbleResult(scrobbleData, librefmSession, false)
                    }

                    serviceIdToKeys[R.string.gnufm]?.let {
                        val gnufmSession: Session = Session.createCustomRootSession(
                                prefs.getString(Stuff.PREF_GNUFM_ROOT, null) + "2.0/",
                                Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                        scrobbleResults[R.string.gnufm] = getScrobbleResult(scrobbleData, gnufmSession, false)
                    }

                    serviceIdToKeys[R.string.listenbrainz]?.let {
                        scrobbleResults[R.string.listenbrainz] =
                                ListenBrainz(it)
                                        .scrobble(scrobbleData)
                    }

                    serviceIdToKeys[R.string.custom_listenbrainz]?.let {
                        scrobbleResults[R.string.custom_listenbrainz] =
                                ListenBrainz(it)
                                        .setApiRoot(prefs.getString(Stuff.PREF_LB_CUSTOM_ROOT, null))
                                        .scrobble(scrobbleData)
                    }

                }
                if (scrobbleResults.isEmpty() ||
                        scrobbleResults.values.any { !it.isSuccessful }) {
                    val dao = PanoDb.getDb(context).getScrobblesDao()
                    val entry = PendingScrobble()
                    entry.artist = scrobbleData.artist
                    entry.album = scrobbleData.album
                    entry.track = scrobbleData.track
                    if (scrobbleData.albumArtist != null)
                        entry.albumArtist = scrobbleData.albumArtist
                    entry.timestamp = scrobbleData.timestamp.toLong() * 1000
                    entry.duration = scrobbleData.duration.toLong() * 1000

                    if (scrobbleResults.isEmpty())
                        serviceIdToKeys.keys.forEach { key ->
                            entry.state = entry.state or (1 shl Stuff.SERVICE_BIT_POS[key]!!)
                        }
                    else
                        scrobbleResults.forEach { (key, result) ->
                            if (!result.isSuccessful) {
                                entry.state = entry.state or (1 shl Stuff.SERVICE_BIT_POS[key]!!)
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
                    val i = Intent(NLService.iOTHER_ERR).apply {
                        putExtra(NLService.B_ERR_MSG, failedText)
                        putExtra(NLService.B_PENDING, savedAsPending)
                        putExtra(NLService.B_HASH, hash)
                    }
                    context.sendBroadcast(i)
                }

            } catch (e: NullPointerException) {
            }
        }
        return this
    }

    fun loveOrUnlove(love: Boolean, artist: String, track: String, callback: ((Boolean) -> Unit)? = null): LFMRequester {
        toExec = {
            checkSession()
            Stuff.log(this::loveOrUnlove.name+ " " + love)
            var submittedAll = true
            val serviceIdToKeys = getServiceIdToKeys(true)

            val dao = PanoDb.getDb(context).getLovesDao()
            val pl = dao.find(artist, track)
            if (pl != null){
                if (pl.shouldLove == !love) {
                    pl.shouldLove = love
                    serviceIdToKeys.keys.forEach { key ->
                        pl.state = pl.state or (1 shl Stuff.SERVICE_BIT_POS[key]!!)
                    }
                    dao.update(pl)
                }
                submittedAll = false
            } else {
                val results = mutableMapOf<@StringRes Int, Result>()

                serviceIdToKeys[R.string.lastfm]?.let {
                    results[R.string.lastfm] = try {
                        if (love)
                            Track.love(artist, track, lastfmSession)
                        else
                            Track.unlove(artist, track, lastfmSession)
                    } catch (e: CallException) {
                        ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                    }
                }
                serviceIdToKeys[R.string.librefm]?.let {
                    val librefmSession: Session = Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                            Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                    results[R.string.librefm] = try {
                        if (love)
                            Track.love(artist, track, librefmSession)
                        else
                            Track.unlove(artist, track, librefmSession)
                    } catch (e: CallException) {
                        ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                    }
                }
                serviceIdToKeys[R.string.gnufm]?.let {
                    val gnufmSession: Session = Session.createCustomRootSession(
                            prefs.getString(Stuff.PREF_GNUFM_ROOT, null)+"2.0/",
                            Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                    results[R.string.gnufm] = try {
                        if (love)
                            Track.love(artist, track, gnufmSession)
                        else
                            Track.unlove(artist, track, gnufmSession)
                    } catch (e: CallException) {
                        ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                    }
                }
                if (results.values.any { !it.isSuccessful }) {
                    val entry = PendingLove()
                    entry.artist = artist
                    entry.track = track
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
            callback?.invoke(submittedAll)
            null
        }
        return this
    }

    fun delete(track: Track, callback: ((Boolean) -> Unit)?): LFMRequester {
        toExec = {
            val serviceIdToKeys = getServiceIdToKeys(true)
            val unscrobbler = LastfmUnscrobbler(context)
            val success = unscrobbler.checkCsrf(lastfmUsername!!) &&
                    unscrobbler.unscrobble(track.artist, track.name, track.playedWhen.time)
            callback!!.invoke(success)

            serviceIdToKeys[R.string.librefm]?.let {
                val librefmSession: Session = Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                        Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                Library.removeScrobble(track.artist, track.name, track.playedWhen.time/1000, librefmSession)
            }

            serviceIdToKeys[R.string.gnufm]?.let {
                val gnufmSession: Session = Session.createCustomRootSession(
                        prefs.getString(Stuff.PREF_GNUFM_ROOT, null)+"2.0/",
                        Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                Library.removeScrobble(track.artist, track.name, track.playedWhen.time/1000, gnufmSession)
            }
            null
        }
        return this
    }

    fun doAuth(@StringRes type: Int, token: String?): LFMRequester {
        toExec = {
            if (!token.isNullOrEmpty()) {
                when (type) {
                    R.string.lastfm -> {
                        val lastfmSession = Authenticator.getSession(null, token, Stuff.LAST_KEY, Stuff.LAST_SECRET)
                        if (lastfmSession != null) {
                            prefs.putString(Stuff.PREF_LASTFM_USERNAME, lastfmSession.username)
                            prefs.putString(Stuff.PREF_LASTFM_SESS_KEY, lastfmSession.key)
                        }
                    }
                    R.string.librefm -> {
                        val librefmSession = Authenticator.getSession(Stuff.LIBREFM_API_ROOT,
                                token, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY)
                        if (librefmSession != null) {
                            prefs.putString(Stuff.PREF_LIBREFM_USERNAME, librefmSession.username)
                            prefs.putString(Stuff.PREF_LIBREFM_SESS_KEY, librefmSession.key)
                        }
                    }
                    R.string.gnufm -> {
                        val gnufmSession = Authenticator.getSession(
                                prefs.getString(Stuff.PREF_GNUFM_ROOT, null) + "2.0/",
                                token, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY)
                        if (gnufmSession != null) {
                            prefs.putString(Stuff.PREF_GNUFM_USERNAME, gnufmSession.username)
                            prefs.putString(Stuff.PREF_GNUFM_SESS_KEY, gnufmSession.key)
                        }
                    }
                }
                val intent = Intent(NLService.iSESS_CHANGED)
                context.sendBroadcast(intent)
            }
            null
        }
        return this
    }

    fun tryInvoke(): Any? {
        try {
            return toExec()
        } catch(e: CallException){
            //ignore
        } catch(e: InterruptedIOException){
            //ignore
        } catch(e: InterruptedException){
            //ignore
        } catch (e: Exception) {
            e.printStackTrace()
            return "err: "+ e.message
        }
        // adb shell am start -W -a android.intent.action.VIEW -d "pscrobble://auth?token=hohoho" com.arn.scrobble
        return null
    }

    private fun getScrobbleResult(scrobbleData: ScrobbleData, session: Session, nowPlaying: Boolean): ScrobbleResult {
        if (Thread.interrupted() && nowPlaying)
            throw InterruptedException()
        return try {
            if (nowPlaying)
                Track.updateNowPlaying(scrobbleData, session)
            else
                Track.scrobble(scrobbleData, session)
        } catch (e: CallException) {
            if (Thread.interrupted() && nowPlaying)
                throw InterruptedException()
//     SocketTimeoutException extends InterruptedIOException
            e.printStackTrace()
            ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
        }
    }

    fun getServiceIdToKeys(lfmOnly: Boolean = false): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        var sessKey = ""
        if (!prefs.getBoolean(Stuff.PREF_LASTFM_DISABLE, false) && lastfmSessKey != null)
            map[R.string.lastfm] = lastfmSessKey!!
        if (prefs.getString(Stuff.PREF_LIBREFM_SESS_KEY, null)?.also { sessKey = it } != null)
            map[R.string.librefm] = sessKey
        if (prefs.getString(Stuff.PREF_GNUFM_SESS_KEY, null)?.also { sessKey = it } != null)
            map[R.string.gnufm] = sessKey
        if (!lfmOnly) {
            if (prefs.getString(Stuff.PREF_LISTENBRAINZ_TOKEN, null)?.also { sessKey = it } != null)
                map[R.string.listenbrainz] = sessKey
            if (prefs.getString(Stuff.PREF_LB_CUSTOM_TOKEN, null)?.also { sessKey = it } != null)
                map[R.string.custom_listenbrainz] = sessKey
        }
        return map
    }

    fun skipContentProvider(): LFMRequester {
        skipCP = true
        return this
    }

    fun asAsyncTask(mld: MutableLiveData<*>? = null): MyAsyncTask {
        val at = MyAsyncTask(this, mld as MutableLiveData<in Any?>?)
        at.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        return at
    }

    fun asSerialAsyncTask(mld: MutableLiveData<*>? = null): MyAsyncTask {
        val at = MyAsyncTask(this, mld as MutableLiveData<in Any?>?)
        at.execute()
        return at
    }

    class MyAsyncTask(private val requester: LFMRequester, private val mld: MutableLiveData<in Any?>? = null): AsyncTask<Unit, Unit, Any?>() {

        override fun doInBackground(vararg p0: Unit?): Any? = requester.tryInvoke()

        override fun onPostExecute(res: Any?) {
            val context = requester.contextWr.get()
            if (context!= null && res is String)
                Stuff.toast(context, res)
            else
                mld?.value = res
        }
    }

    companion object {

        private val validArtistsCache = LruCache<String, String>(10)
        private val validTracksCache = LruCache<Pair<String, String>, Pair<Track?, Long>>(10)

        private var lastNp = ScrobbleData() to 0L

        private var lastNpInfoTime = 0L
        private var lastNpInfoCount = 0

        private fun slurp(urlConnection: HttpURLConnection, bufferSize: Int): String {
            val buffer = CharArray(bufferSize)
            val out = StringBuilder()
            var res = ""
            try {
                val ir = if ("gzip" == urlConnection.contentEncoding) {
                    InputStreamReader(GZIPInputStream(urlConnection.inputStream), "UTF-8")
                } else {
                    InputStreamReader(urlConnection.inputStream, "UTF-8")
                }
                ir.use { `in` ->
                    while (true) {
                        val rsz = `in`.read(buffer, 0, buffer.size)
                        if (rsz < 0)
                            break
                        out.append(buffer, 0, rsz)
                    }
                }
                res = out.toString()
            } catch (ex: InterruptedException){
            } catch (ex: InterruptedIOException){
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            return res
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
            val result: Result = Caller.getInstance().call(Stuff.LIBREFM_API_ROOT, "artist.getInfo",
                    "", mapOf("artist" to artist, "autocorrect" to "1"))
            return ResponseBuilder.buildItem(result, Artist::class.java)
        }

        fun getArtistInfoSpotify(artist: String):Artist? {
            if (Tokens.SPOTIFY_ARTIST_INFO_SERVER.isEmpty() || Tokens.SPOTIFY_ARTIST_INFO_KEY.isEmpty())
                return null
            val result: Result = Caller.getInstance().call(Tokens.SPOTIFY_ARTIST_INFO_SERVER, "artist.getInfo.spotify",
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

class DualPref(private val skipCp: Boolean, context: Context?) {

    private lateinit var sPref: SharedPreferences
    private lateinit var mPref: MultiPreferences

    init {
        if (skipCp)
            sPref = PreferenceManager.getDefaultSharedPreferences(context)
        else
            mPref = MultiPreferences(context!!)
    }

    fun getBoolean(key: String, default: Boolean): Boolean {
        return if (skipCp)
            sPref.getBoolean(key, default)
        else
            mPref.getBoolean(key, default)
    }

    fun putBoolean(key: String, value: Boolean) {
        return if (skipCp)
            sPref.edit().putBoolean(key, value).apply()
        else
            mPref.putBoolean(key, value)
    }

    fun getStringSet(key: String, default: Set<String>?): Set<String>? {
        return if (skipCp)
            sPref.getStringSet(key, default)
        else
            mPref.getStringSet(key, default)
    }

    fun getString(key: String, default: String?): String? {
        return if (skipCp)
            sPref.getString(key, default)
        else
            mPref.getString(key, default)
    }

    fun putString(key: String, value: String) {
        return if (skipCp)
            sPref.edit().putString(key, value).apply()
        else
            mPref.putString(key, value)
    }
}
