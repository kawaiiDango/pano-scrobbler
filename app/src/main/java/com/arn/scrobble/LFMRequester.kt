package com.arn.scrobble

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.LruCache
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.pending.db.PendingLove
import com.arn.scrobble.pending.db.PendingScrobble
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.pref.MultiPreferences
import de.umass.lastfm.*
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import java.io.IOException
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.zip.GZIPInputStream


/**
 * Created by arn on 18-03-2017.
 */

class LFMRequester(var command: String, vararg args: String) {
    private lateinit var prefs: DualPref
    private var skipCP = false
    private var callback: ((success:Boolean) -> Unit)? = null
    private var args = mutableListOf(*args)
    var isLoading = false

    fun inBackground(context: Context?): Any? {
        context ?: return null

        isLoading = true
        Stuff.log("loadInBackground $command $args")
        prefs = DualPref(skipCP, context)

        try {
            var reAuthNeeded = false
            var lastfmSession: Session? = null
            val caller = Stuff.initCaller(context)

            if (command == Stuff.GET_RECENTS_CACHED || command == Stuff.GET_LOVES_CACHED) {
                command = command.replace("_cached","")
                caller.cache.expirationPolicy = LFMCachePolicy(false)
            } else
                caller.cache.expirationPolicy = LFMCachePolicy(Main.isOnline)

            val lastfmSessKey: String? = prefs.getString(Stuff.PREF_LASTFM_SESS_KEY, null)
            val lastfmUsername: String? by lazy { prefs.getString(Stuff.PREF_LASTFM_USERNAME, null) }

            if (lastfmSessKey != null)
                lastfmSession = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey)
            else
                reAuthNeeded = true

            fun getServiceIdToKeys(lfmOnly: Boolean = false): Map<Int, String> {
                val map = mutableMapOf<Int, String>()
                var sessKey = ""
                if (!prefs.getBoolean(Stuff.PREF_LASTFM_DISABLE, false) && lastfmSessKey != null)
                    map[R.string.lastfm] = lastfmSessKey
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

            //takes up to 16ms till here
            if (!reAuthNeeded) {
                when (command) {
                    Stuff.LASTFM_SESS_AUTH -> return null
                    Stuff.GET_RECENTS -> {
                        val tracks = User.getRecentTracks(null, Integer.parseInt(args[0]), 20, true, lastfmSession, null)
//                        if (tracks.username != null && tracks.username != lastfmUsername)
//                            prefs.putString(Stuff.PREF_LASTFM_USERNAME, tracks.username)
                        return tracks
                    }
                    Stuff.GET_LOVES -> {
                        val pr = User.getLovedTracks(null, Integer.parseInt(args[0]), 20, lastfmSession, null)
                        pr.pageResults.forEach {
                            it.isLoved = true
                            it.imageUrlsMap.clear()
                        }
                        return pr
                    }
                    Stuff.GET_FRIENDS_RECENTS ->
                        // args[0] = username
                        return Pair(args[0], User.getRecentTracks(args[0], 1, 1, false, null, Stuff.LAST_KEY))

                    //for love: command = tag, args[0] = artist, args[1] = song,
                    Stuff.LOVE, Stuff.UNLOVE -> {
                        val love = command == Stuff.LOVE
                        var submittedAll = true
                        val serviceIdToKeys = getServiceIdToKeys(true)

                        val dao = PendingScrobblesDb.getDb(context).getLovesDao()
                        val pl = dao.find(args[0], args[1])
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
                                        Track.love(args[0], args[1], lastfmSession)
                                    else
                                        Track.unlove(args[0], args[1], lastfmSession)
                                } catch (e: CallException) {
                                    ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                                }
                            }
                            serviceIdToKeys[R.string.librefm]?.let {
                                val librefmSession: Session = Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                                        Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                                results[R.string.librefm] = try {
                                    if (love)
                                        Track.love(args[0], args[1], librefmSession)
                                    else
                                        Track.unlove(args[0], args[1], librefmSession)
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
                                        Track.love(args[0], args[1], gnufmSession)
                                    else
                                        Track.unlove(args[0], args[1], gnufmSession)
                                } catch (e: CallException) {
                                    ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                                }
                            }
                            if (results.values.any { !it.isSuccessful }) {
                                val entry = PendingLove()
                                entry.artist = args[0]
                                entry.track = args[1]
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
                    }
                    // args[2] = limit
                    Stuff.GET_SIMILAR -> return Track.getSimilar(args[0], args[1], Stuff.LAST_KEY, args[2].toInt())
                    Stuff.DELETE -> {
                        val serviceIdToKeys = getServiceIdToKeys(true)
                        val unscrobbler = LastfmUnscrobbler(context)
                        val success = unscrobbler.checkCsrf(lastfmUsername!!) &&
                                unscrobbler.unscrobble(args[0],args[1],args[2].toLong())
                        callback!!.invoke(success)

                        serviceIdToKeys[R.string.librefm]?.let {
                            val librefmSession: Session = Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                                    Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                            Library.removeScrobble(args[0],args[1],args[2].toLong()/1000, librefmSession)
                        }

                        serviceIdToKeys[R.string.gnufm]?.let {
                            val gnufmSession: Session = Session.createCustomRootSession(
                                    prefs.getString(Stuff.PREF_GNUFM_ROOT, null)+"2.0/",
                                    Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, it)
                            Library.removeScrobble(args[0],args[1],args[2].toLong()/1000, gnufmSession)
                        }
                    }
                    Stuff.GET_DRAWER_INFO -> {
                        val profile = User.getInfo(lastfmSession)
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        val recents = User.getRecentTracks(null, 1, 1,
                                cal.timeInMillis/1000, System.currentTimeMillis()/1000, lastfmSession, null)

                        val actPref = context.getSharedPreferences(Stuff.ACTIVITY_PREFS, MODE_PRIVATE)
                        actPref.edit()
                                .putInt(Stuff.PREF_ACTIVITY_NUM_SCROBBLES, recents?.totalPages ?: 0)
                                .putString(Stuff.PREF_ACTIVITY_PROFILE_PIC, profile?.getWebpImageURL(ImageSize.EXTRALARGE) ?: "")
                                .apply()

                        val intent = Intent(NLService.iDRAWER_UPDATE)
                        context.sendBroadcast(intent)
                        return null
                    }
                    Stuff.GET_FRIENDS -> {
                        val limit = if (args.size > 1) args[1].toInt() else 30
                        var pr:PaginatedResult<User>
                        try {
                            pr = User.getFriends(lastfmUsername, Integer.parseInt(args[0]), limit, null, Stuff.LAST_KEY)
                        } catch (e:NullPointerException){
                            val url = URL("https://www.last.fm/user/$lastfmUsername/following?page="+args[0])
                            var urlConnection:HttpURLConnection? = null
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
                                        if(idx > -1)
                                            idx= resp.indexOf(">", idx+1)
                                        if(idx > -1){
                                            idx += 1
                                            idx2 = resp.indexOf("<", idx)
                                            val uname = resp.substring(idx, idx2)
                                            idx= resp.indexOf("<img", idx2)
                                            idx= resp.indexOf("\"", idx)
                                            idx2= resp.indexOf("\"", idx+1)
                                            val imageUrl = resp.substring(idx+1,idx2)
                                            val user = User(uname, "https://www.last.fm/user/$uname")
                                            user.imageURL = imageUrl
                                            users.add(user)
                                            idx = idx2
                                        }
                                    }while(idx > -1)

                                }
                            } catch(e:Exception){
                            } finally {
                                urlConnection?.disconnect()
                            }
                            val totalPages = if (users.isEmpty())
                                args[0].toInt()
                            else
                                10
                            pr = PaginatedResult(args[0].toInt(),totalPages, users, null)
                        }
                        return pr
                    }
                    //args[0] = artist, args[1] = track, args[2] = pos
                    Stuff.GET_INFO -> {
                        val info = try {
                            Track.getInfo(args[0], args[1], Stuff.LAST_KEY)
                        } catch (e:Exception){
                            null
                        }
                        return if(info != null)
                            Pair(args[2].toInt(), info)
                        else
                            null
                    }
                    Stuff.GET_HERO_INFO -> {
                        if (!Main.isOnline || args[0] == "")
                            return null
                        //args[0] = page url, args[1] = api large image url
                        val url = URL(args[0])
                        var urlConnection:HttpURLConnection? = null
                        val scrapped = mutableListOf<String?>()
                        try {
                            urlConnection = url.openConnection() as HttpURLConnection
                            urlConnection.setRequestProperty("Accept-Encoding", "gzip")

                            if (urlConnection.responseCode != 200)
                                return null
                            val resp = slurp(urlConnection, 1024)
                            if (resp == "")
                                return null
                            //0
                            var idx = resp.indexOf("charts/listener-trend",200000)
                            var idx2: Int
                            var days = 0
                            val daily = arrayListOf<Int>()
                            val monthly = arrayOf(0,0,0,0,0)
                            if (idx > -1) {
                                val stop1 = "data-value=\""
                                do {
                                    idx = resp.indexOf(stop1, idx)
                                    if(idx > -1){
                                        idx += stop1.length
                                        idx2 = resp.indexOf("\"", idx)
                                        val value = resp.substring(idx, idx2).toInt()
                                        daily.add(value)
                                    }
                                }while(idx > -1)
                                for(i in daily.size-1 downTo 0){
                                    monthly[4-days/30] += daily[i]
                                    days++
                                    if(days/30 >4)
                                        break
                                }
                                scrapped.add(monthly.joinToString())
                            } else
                                scrapped.add(null)

                            /*
                            //1
                            idx = resp.indexOf("id=\"header-expanded-image\"")
                            var img = args[1]
                            if (idx > -1) {
                                idx = resp.indexOf("src=", idx) + 5
                                val idx2 = resp.indexOf("\"", idx)
                                img = resp.substring(idx, idx2)
                                img.replace("/ar0/","/300x300/")
                            }
                            scrapped.add(img)
                            */
                        } catch (e:Exception){
                            if(scrapped.isEmpty())
                                scrapped.add(null)
                        } finally {
                            urlConnection?.disconnect()
                        }
                        return scrapped
                    }

                    Stuff.NOW_PLAYING, Stuff.SCROBBLE -> {
                        val scrobbleResults = mutableMapOf<@StringRes Int, ScrobbleResult>()

                        //for scrobble: command = tag, args[0] = artist, args[1] = album, args[2] = title, args[3] = albumArtist, args[4] = time, args[5] = duration, args[6] = hash
                        val scrobbleData = ScrobbleData()
                        scrobbleData.artist = args[0]
                        scrobbleData.album = args[1]
                        scrobbleData.track = args[2]
                        scrobbleData.albumArtist = args[3]
                        scrobbleData.timestamp = (args[4].toLong()/1000).toInt() // in secs
                        scrobbleData.duration = (args[5].toLong()/1000).toInt() // in secs
                        val hash = args[6].toInt()
                        var savedAsPending = false

                        if (scrobbleData.duration < 30)
                            scrobbleData.duration = -1 //default
                        val serviceIdToKeys by lazy { getServiceIdToKeys() }

                        fun getScrobbleResult(scrobbleData: ScrobbleData, session: Session, nowPlaying: Boolean): ScrobbleResult {
                            if (Thread.interrupted())
                                throw InterruptedException()
                            return try {
                                if (nowPlaying)
                                    Track.updateNowPlaying(scrobbleData, session)
                                else
                                    Track.scrobble(scrobbleData, session)
                            } catch (e: CallException) {
                                if (e.cause is InterruptedIOException)
                                    throw e.cause as InterruptedIOException
                                ScrobbleResult.createHttp200OKResult(0, e.cause?.message, "")
                            }
                        }

                        when(command) {
                            Stuff.NOW_PLAYING -> {
                                var track: Track? = null
                                var correctedArtist: String? = null
                                if (Thread.interrupted())
                                    throw InterruptedException()

                                if (Main.isOnline) {
                                    try {
                                        track = Track.getInfo(args[0], args[2], null, lastfmUsername, null, Stuff.LAST_KEY)
                                        //works even if the username is wrong
                                        } catch (e: CallException) { }
                                    if (Thread.interrupted())
                                        throw InterruptedException()
                                    if (track != null && args[1] == "") {
                                        scrobbleData.artist = track.artist
                                        if (track.album != null)
                                            scrobbleData.album = track.album
                                        if (track.albumArtist != null)
                                            scrobbleData.albumArtist = track.albumArtist
                                        scrobbleData.track = track.name
                                    }
                                    correctedArtist =
                                            if (track != null && track.listeners >= Stuff.MIN_LISTENER_COUNT / 2)
                                                track.artist
                                            else
                                                getValidArtist(args[0], prefs.getStringSet(Stuff.PREF_ALLOWED_ARTISTS, null))
                                    if (correctedArtist != null && args[1] == "")
                                        scrobbleData.artist = correctedArtist
                                }
                                val dao = PendingScrobblesDb.getDb(context).getEditsDao()
                                val edit =
                                        try {
                                            dao.find(scrobbleData.artist.hashCode().toString() +
                                                    scrobbleData.album.hashCode().toString() + scrobbleData.track.hashCode().toString())
                                        } catch (e: Exception) {
                                            null
                                        }
                                if (edit != null) {
                                    scrobbleData.artist = edit.artist
                                    scrobbleData.album = edit.album
                                    if (edit.albumArtist.isNotBlank())
                                        scrobbleData.albumArtist = edit.albumArtist
                                    scrobbleData.track = edit.track
                                    try {
                                        track = Track.getInfo(scrobbleData.artist, scrobbleData.track, null, lastfmUsername, null, Stuff.LAST_KEY)
                                    } catch (e: CallException) { }
                                }
                                if (edit != null || track!= null) {
                                    val i = Intent(NLService.iMETA_UPDATE)
                                    i.putExtra(NLService.B_ARTIST, scrobbleData.artist)
                                    i.putExtra(NLService.B_ALBUM, scrobbleData.album)
                                    i.putExtra(NLService.B_ALBUM_ARTIST, scrobbleData.albumArtist)
                                    i.putExtra(NLService.B_TITLE, scrobbleData.track)
                                    i.putExtra(NLService.B_HASH, hash)
                                    if (track != null) {
                                        i.putExtra(NLService.B_USER_LOVED, track.isLoved)
                                        i.putExtra(NLService.B_USER_PLAY_COUNT, track.userPlaycount)
                                    }
                                    context.sendBroadcast(i)
                                }
                                if (Main.isOnline){
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
                                                        prefs.getString(Stuff.PREF_GNUFM_ROOT, null)+"2.0/",
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
                                        //no such artist
                                        val i = Intent(NLService.iBAD_META)
                                            i.putExtra(NLService.B_ARTIST, args[0])
                                            i.putExtra(NLService.B_ALBUM, args[1])
                                            i.putExtra(NLService.B_TITLE, args[2])
                                            i.putExtra(NLService.B_ALBUM_ARTIST, args[3])
                                            i.putExtra(NLService.B_TIME, args[4].toLong())
                                            i.putExtra(NLService.B_HASH, hash)
                                            context.sendBroadcast(i)
                                    }
                                }
                            }
                            Stuff.SCROBBLE -> {
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
                                                prefs.getString(Stuff.PREF_GNUFM_ROOT, null)+"2.0/",
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
                                if (!Main.isOnline ||
                                        scrobbleResults.values.any { !it.isSuccessful } ){
                                    val dao = PendingScrobblesDb.getDb(context).getScrobblesDao()
                                    val entry = PendingScrobble()
                                    entry.artist = scrobbleData.artist
                                    entry.album = scrobbleData.album
                                    entry.track = scrobbleData.track
                                    if (scrobbleData.albumArtist != null)
                                        entry.albumArtist = scrobbleData.albumArtist
                                    entry.timestamp = args[4].toLong()
                                    entry.duration = args[5].toLong()

                                    if (!Main.isOnline)
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
                        }
                        try {
                            val failedTextLines = mutableListOf<String>()
                            scrobbleResults.forEach { (key, result) ->
                                if (!result.isSuccessful) {
                                    val errMsg = scrobbleResults[key]?.errorMessage
                                            ?: context.getString(R.string.network_error)
                                    failedTextLines += "<b>" + context.getString(key) + ":</b> $errMsg"
                                } else if (result.isSuccessful && result.isIgnored) {
                                    failedTextLines += "<b>" + context.getString(key) + ":</b> " + context.getString(R.string.scrobble_ignored, args[0])
                                }
                            }
                            if (failedTextLines.isNotEmpty()){
                                val failedText = failedTextLines.joinToString("<br>\n")
                                Stuff.log("failedText= $failedText")
                                val i = Intent(NLService.iOTHER_ERR)
                                i.putExtra(NLService.B_ERR_MSG, failedText)
                                i.putExtra(NLService.B_PENDING, savedAsPending)
                                i.putExtra(NLService.B_HASH, hash)
                                context.sendBroadcast(i)
                            }

                        } catch (e: NullPointerException) {
                            return "$command: NullPointerException"
                        }
                    }
                }
            } //end !reauthNeeded (for lastfm)

            when (command) {
                Stuff.LASTFM_SESS_AUTH -> {
                    val token = if (args.size == 1) args[0] else null
                    if (!token.isNullOrBlank()) {
                        lastfmSession = Authenticator.getSession(null, token, Stuff.LAST_KEY, Stuff.LAST_SECRET)
                        if (lastfmSession != null) {
                            prefs.putString(Stuff.PREF_LASTFM_USERNAME, lastfmSession.username)
                            prefs.putString(Stuff.PREF_LASTFM_SESS_KEY, lastfmSession.key)
                            val intent = Intent(NLService.iSESS_CHANGED)
                            context.sendBroadcast(intent)
                        }
                    }
                }
                Stuff.LIBREFM_SESS_AUTH -> {
                    val token = if (args.size == 1) args[0] else null
                    if (!token.isNullOrBlank()) {
                        val librefmSession = Authenticator.getSession(Stuff.LIBREFM_API_ROOT,
                                token, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY)
                        if (librefmSession != null) {
                            prefs.putString(Stuff.PREF_LIBREFM_USERNAME, librefmSession.username)
                            prefs.putString(Stuff.PREF_LIBREFM_SESS_KEY, librefmSession.key)
                            val intent = Intent(NLService.iSESS_CHANGED)
                            context.sendBroadcast(intent)
                        }
                    }
                }
                Stuff.GNUFM_SESS_AUTH -> {
                    val token = if (args.size == 1) args[0] else null
                    if (!token.isNullOrBlank()) {
                        val gnufmSession = Authenticator.getSession(
                                prefs.getString(Stuff.PREF_GNUFM_ROOT,null)+"2.0/",
                                token, Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY)
                        if (gnufmSession != null) {
                            prefs.putString(Stuff.PREF_GNUFM_USERNAME, gnufmSession.username)
                            prefs.putString(Stuff.PREF_GNUFM_SESS_KEY, gnufmSession.key)
                            val intent = Intent(NLService.iSESS_CHANGED)
                            context.sendBroadcast(intent)
                        }
                    }
                }
                Stuff.GET_RECENTS -> {}
                else -> {
                    if (reAuthNeeded) {
                        reAuth(context)
                        return context.getString(R.string.please_authorize)
                    }
                }
            }
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

    private fun reAuth(context: Context) {
        MultiPreferences(context).remove(Stuff.PREF_LASTFM_SESS_KEY)
        Stuff.openInBrowser(Stuff.LASTFM_AUTH_CB_URL, context)
    }

    fun skipContentProvider(): LFMRequester {
        skipCP = true
        return this
    }

    fun addCallback(callback: (success:Boolean)->Unit): LFMRequester {
        this.callback = callback
        return this
    }

    fun asAsyncTask(context: Context, mld: MutableLiveData<*>? = null): MyAsyncTask {
        val at = MyAsyncTask(this, context, mld as MutableLiveData<in Any?>?)
        at.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        return at
    }

    fun asSerialAsyncTask(context: Context, mld: MutableLiveData<*>? = null): MyAsyncTask {
        val at = MyAsyncTask(this, context, mld as MutableLiveData<in Any?>?)
        at.execute()
        return at
    }

    class MyAsyncTask(private val requester: LFMRequester, context: Context, private val mld: MutableLiveData<in Any?>? = null): AsyncTask<Unit, Unit, Any?>() {
        private var contextWr = WeakReference(context.applicationContext)

        override fun doInBackground(vararg p0: Unit?): Any? = requester.inBackground(contextWr.get())

        override fun onPostExecute(res: Any?) {
            requester.isLoading = false
            val context = contextWr.get()
            if (context!= null && res is String)
                Stuff.toast(context, res)
            else
                mld?.value = res
        }
    }

    companion object {

        private val validArtistsCache = LruCache<String, Boolean>(10)

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


        fun getValidArtist(artist:String, set:Set<String>? = null): String? {
            val valid = set?.contains(artist) == true
            if (valid || validArtistsCache[artist] == true)
                return artist
            else if (validArtistsCache[artist] == null) {
                var artistInfo: Artist?
                var errCode = Caller.getInstance().lastError?.errorCode
                //6 = artist not found on lastfm, 7 = invalid resource specified on librefm
                if (errCode != null && errCode != 6 && errCode != 7)
                    artistInfo = getArtistInfoLibreFM(artist)
                else {
                    artistInfo = Artist.getInfo(artist, false, Stuff.LAST_KEY)
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
                        validArtistsCache.put(artistInfo.name, true)
                        return artistInfo.name
                    } else
                        validArtistsCache.put(artist, false)
                } else
                    validArtistsCache.put(artist, false)
            }
            return null
        }

        fun getArtistInfoLibreFM(artist: String):Artist? {
            val result: Result = Caller.getInstance().call(Stuff.LIBREFM_API_ROOT, "artist.getInfo",
                    "", mapOf("artist" to artist))
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
