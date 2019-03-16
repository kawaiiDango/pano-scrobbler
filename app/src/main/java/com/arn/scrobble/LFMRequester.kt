package com.arn.scrobble

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.util.LruCache
import androidx.lifecycle.MutableLiveData
import com.arn.scrobble.pending.PendingScrJob
import com.arn.scrobble.pending.db.PendingScrobble
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.pref.MultiPreferences
import de.umass.lastfm.*
import de.umass.lastfm.cache.FileSystemCache
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


/**
 * Created by arn on 18-03-2017.
 */

class LFMRequester(var command: String, vararg args: String) {
    private lateinit var prefs: DualPref
    private var skipCP = false
    private var callback: ((success:Boolean) -> Unit)? = null
    private var args = mutableListOf(*args)
    var isLoading = false

    private fun inBackground(context: Context?): Any? {
        context ?: return null

        isLoading = true
        Stuff.timeIt("loadInBackground $command")
        Stuff.log("loadInBackground $command $args")
        prefs = DualPref(skipCP, context)

        try {
            var reAuthNeeded = false
            var lastfmSession: Session? = null
            val caller = Caller.getInstance()
            if (caller.userAgent != Stuff.USER_AGENT) { // static instance not inited
                caller.userAgent = Stuff.USER_AGENT
                caller.isDebugMode = false
                val fsCache = FileSystemCache(context.cacheDir)
                caller.cache = fsCache
            }

            if (command == Stuff.GET_RECENTS_CACHED) {
                command = Stuff.GET_RECENTS
                caller.cache.expirationPolicy = LFMCachePolicy(false)
            } else
                caller.cache.expirationPolicy = LFMCachePolicy(Main.isOnline)

            val lastfmSessKey: String? = prefs.getString(Stuff.PREF_LASTFM_SESS_KEY, null)
            var lastfmUsername: String? = prefs.getString(Stuff.PREF_LASTFM_USERNAME, null)

            if (!lastfmSessKey.isNullOrBlank())
                lastfmSession = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey)

            if (lastfmSession == null || lastfmUsername == null)
                reAuthNeeded = true

            Stuff.timeIt("!reAuthNeeded $command")
            if (!reAuthNeeded) {
                when (command) {
                    Stuff.LASTFM_SESS_AUTH -> return null
                    Stuff.GET_RECENTS -> {
                        return User.getRecentTracks(lastfmUsername, Integer.parseInt(args[0]), 15, true, lastfmSessKey, Stuff.LAST_KEY)
                    }
                    Stuff.GET_FRIENDS_RECENTS ->
                        // args[0] = username
                        return Pair(args[0], User.getRecentTracks(args[0], 1, 1, false, null, Stuff.LAST_KEY))

                    //for love: command = tag, args[0] = artist, args[1] = song,
                    Stuff.LOVE -> return Track.love(args[0], args[1], lastfmSession)
                    Stuff.UNLOVE -> return Track.unlove(args[0], args[1], lastfmSession)
                    // args[2] = limit
                    Stuff.GET_SIMILAR -> return Track.getSimilar(args[0], args[1], Stuff.LAST_KEY, args[2].toInt())
                    Stuff.DELETE -> {
                        val unscrobbler = LastfmUnscrobbler(context)
                        val success = unscrobbler.checkCsrf(lastfmUsername!!) &&
                                unscrobbler.unscrobble(args[0],args[2],args[3].toLong())
                        callback!!.invoke(success)
                    }
                    Stuff.GET_DRAWER_INFO -> {
                        val profile = User.getInfo(lastfmSession)
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        val recents = User.getRecentTracks(lastfmUsername, 1, 1,
                                cal.timeInMillis/1000, System.currentTimeMillis()/1000, lastfmSessKey, Stuff.LAST_KEY)

                        val actPref = context.getSharedPreferences(Stuff.ACTIVITY_PREFS, MODE_PRIVATE)
                        actPref.edit()
                                .putInt(Stuff.PREF_ACTIVITY_NUM_SCROBBLES, recents?.totalPages ?: 0)
                                .putString(Stuff.PREF_ACTIVITY_PROFILE_PIC, profile?.getImageURL(ImageSize.EXTRALARGE) ?: "")
                                .apply()

                        val intent = Intent(NLService.iDRAWER_UPDATE)
                        context.sendBroadcast(intent)
                        return null
                    }
                    Stuff.GET_FRIENDS -> {
                        val limit = if (args.size > 1) args[1].toInt() else 30
                        return try {
                            User.getFriends(lastfmUsername, true, Integer.parseInt(args[0]), limit, Stuff.LAST_KEY)
                        } catch (e:NullPointerException){
                            PaginatedResult<User>(1,0, listOf())
                        }
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
                            if (urlConnection.responseCode != 200)
                                return null
                            val resp = slurp(urlConnection.inputStream, 1024)
                            if (resp == "")
                                return null
                            //0
                            var idx = resp.indexOf("charts/sparkline")
                            val idx2: Int
                            if (idx > -1) {
                                idx = resp.indexOf("[", idx) + 1
                                idx2 = resp.indexOf("]", idx)
                                val chart = resp.substring(idx, idx2)
                                scrapped.add(chart)
                            } else
                                scrapped.add(null)

                            /*
                            //1
                            idx = resp.indexOf("id=\"header-expanded-image\"")
                            var img = args[1]
                            if (idx > -1) {
                                idx = resp.indexOf("src=", idx) + 5
                                idx2 = resp.indexOf("\"", idx)
                                img = resp.substring(idx, idx2)
                            }
                            scrapped.add(img)
                            */
                        } finally {
                            urlConnection?.disconnect()
                        }
                        return scrapped
                    }

                    Stuff.NOW_PLAYING, Stuff.SCROBBLE -> {
                        val scrobbleResults = mutableMapOf<String, ScrobbleResult>()

                        //for scrobble: command = tag, args[0] = artist, args[1] = album, args[2] = title, args[3] = albumArtist, args[4] = time, args[5] = duration
                        val scrobbleData = ScrobbleData()
                        scrobbleData.artist = args[0]
                        scrobbleData.album = args[1]
                        scrobbleData.track = args[2]
                        if (args[3] != "" && args[3] != args[0])
                            scrobbleData.albumArtist = args[3]
                        scrobbleData.timestamp = (args[4].toLong()/1000).toInt() // in secs
                        scrobbleData.duration = (args[5].toLong()/1000).toInt() // in secs

                        if (scrobbleData.duration < 30)
                            scrobbleData.duration = -1 //default


                        val librefmSessKey: String? = prefs.getString(Stuff.PREF_LIBREFM_SESS_KEY, null)
                        val librefmSession: Session? =
                            if (!librefmSessKey.isNullOrBlank())
                                Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                                        Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, librefmSessKey)
                            else
                                null

                        when(command) {
                            Stuff.NOW_PLAYING -> {
                                if (NLService.isOnline){
                                    val hash = args[0].hashCode() + args[2].hashCode()

                                    val track =
                                            try {
                                                Track.getInfo(args[0], args[2], null, lastfmUsername, Tokens.LAST_KEY)
                                            } catch (e: CallException){
                                                null
                                            }
                                    val corrected =
                                            if (track != null && track.listeners >= Stuff.MIN_LISTENER_COUNT/2)
                                                Pair(args[0], args[2])
                                            else
                                                getValidArtist(args[0], args[2])
                                    if (track != null) {
                                        val i = Intent(NLService.iMETA_UPDATE)
                                        i.putExtra(NLService.B_ARTIST, args[0])
                                        if (args[1] == "")
                                            i.putExtra(NLService.B_ALBUM, track.album ?: "")
                                        else
                                            i.putExtra(NLService.B_ALBUM, args[1])
                                        i.putExtra(NLService.B_TITLE, track.name)
                                        i.putExtra(NLService.B_USER_LOVED, track.isLoved)
                                        i.putExtra(NLService.B_USER_PLAY_COUNT, track.userPlaycount)
                                        context.sendBroadcast(i)
                                    }
                                    if (corrected != null) {
                                        scrobbleData.artist = corrected.first
                                        scrobbleData.track = corrected.second
                                        if (!prefs.getBoolean(Stuff.PREF_LASTFM_DISABLE, false))
                                            scrobbleResults[context.getString(R.string.lastfm)] = Track.updateNowPlaying(scrobbleData, lastfmSession)

                                        if (librefmSession != null)
                                            scrobbleResults[context.getString(R.string.lastfm)] = Track.updateNowPlaying(scrobbleData, librefmSession)

                                        if (prefs.getString(Stuff.PREF_LISTENBRAINZ_USERNAME, null) != null)
                                            scrobbleResults[context.getString(R.string.listenbrainz)] =
                                                    ListenBrainz(prefs.getString(Stuff.PREF_LISTENBRAINZ_TOKEN, null))
                                                    .updateNowPlaying(scrobbleData)
                                        if (prefs.getString(Stuff.PREF_LB_CUSTOM_USERNAME, null) != null)
                                            scrobbleResults[context.getString(R.string.custom_listenbrainz)] =
                                                    ListenBrainz(prefs.getString(Stuff.PREF_LB_CUSTOM_TOKEN, null))
                                                    .setApiRoot(prefs.getString(Stuff.PREF_LB_CUSTOM_ROOT, null))
                                                    .updateNowPlaying(scrobbleData)
                                    } else {
                                        //no such artist
                                        val i = Intent(NLService.iBAD_META)
                                            i.putExtra(NLService.B_ARTIST, args[0])
                                            i.putExtra(NLService.B_ALBUM, args[1])
                                            i.putExtra(NLService.B_TITLE, args[2])
                                            i.putExtra(NLService.B_TIME, args[4].toLong())
                                            i.putExtra(NLService.B_HASH, hash)
                                            context.sendBroadcast(i)
                                    }
                                }
                            }
                            Stuff.SCROBBLE -> {
                                var couldntConnect = false
                                if (NLService.isOnline) {
                                    try {
                                        if (!prefs.getBoolean(Stuff.PREF_LASTFM_DISABLE, false))
                                            scrobbleResults[context.getString(R.string.lastfm)] = Track.scrobble(scrobbleData, lastfmSession)

                                        if (librefmSession != null)
                                            scrobbleResults[context.getString(R.string.lastfm)] = Track.scrobble(scrobbleData, librefmSession)

                                        Stuff.log("scrobbleResults:$scrobbleResults")

                                        if (prefs.getString(Stuff.PREF_LISTENBRAINZ_USERNAME, null) != null)
                                            scrobbleResults[context.getString(R.string.listenbrainz)] =
                                                    ListenBrainz(prefs.getString(Stuff.PREF_LISTENBRAINZ_TOKEN, null))
                                                    .scrobble(scrobbleData)

                                        if (prefs.getString(Stuff.PREF_LB_CUSTOM_USERNAME, null) != null)
                                            scrobbleResults[context.getString(R.string.custom_listenbrainz)] =
                                                    ListenBrainz(prefs.getString(Stuff.PREF_LB_CUSTOM_TOKEN, null))
                                                    .setApiRoot(prefs.getString(Stuff.PREF_LB_CUSTOM_ROOT, null))
                                                    .scrobble(scrobbleData)

                                    } catch (e: CallException){
                                        Stuff.log("CallException: $e")
                                        couldntConnect = true
                                    }
                                }
                                if (couldntConnect || !NLService.isOnline){
                                    val dao = PendingScrobblesDb.getDb(context).getDao()
                                    val entry = PendingScrobble()
                                    entry.artist = scrobbleData.artist
                                    entry.album = scrobbleData.album
                                    entry.track = scrobbleData.track
                                    if (scrobbleData.albumArtist != null)
                                        entry.albumArtist = scrobbleData.albumArtist
                                    entry.timestamp = args[4].toLong()
                                    entry.duration = args[5].toLong()
                                    dao.insert(entry)
                                    PendingScrJob.checkAndSchedule(context)
                                }
                            }
                        }
                        try {
                            val hash = args[0].hashCode() + args[2].hashCode()

                            var failedText = ""
                            scrobbleResults.keys.forEach { key ->
                                if (scrobbleResults[key]?.isSuccessful == false) {
                                    val errMsg = scrobbleResults[key]?.errorMessage ?: context.getString(R.string.network_error)
                                    failedText += "$key: $errMsg\n"
                                } else if (scrobbleResults[key]?.isSuccessful == true && scrobbleResults[key]?.isIgnored == true) {
                                    failedText += key + ": " +context.getString(R.string.scrobble_ignored, args[0]) + "\n"
                                }
                                if (failedText != ""){
                                    val i = Intent(NLService.iOTHER_ERR)
                                    i.putExtra(NLService.B_ERR_MSG, failedText)
                                    i.putExtra(NLService.B_HASH, hash)
                                    context.sendBroadcast(i)
                                }
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
                            lastfmUsername = lastfmSession.username
                            prefs.putString(Stuff.PREF_LASTFM_USERNAME, lastfmUsername)
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
        } catch (e: Exception) {
            e.printStackTrace()
            return "err: "+ e.cause
        }

        // adb shell am start -W -a android.intent.action.VIEW -d "pscrobble://auth?token=hohoho" com.arn.scrobble
        return null
    }

    fun skipContentProvider(): LFMRequester {
        skipCP = true
        return this
    }

    fun addCallback(callback: (success:Boolean)->Unit): LFMRequester {
        this.callback = callback
        return this
    }

    fun asAsyncTask(context: Context, mld: MutableLiveData<*>? = null): AsyncTask<Unit, Unit, Any?>? =
            MyAsyncTask(this, context, mld as MutableLiveData<in Any?>?).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

    fun asSerialAsyncTask(context: Context, mld: MutableLiveData<*>? = null): AsyncTask<Unit, Unit, Any?>? =
            MyAsyncTask(this, context, mld as MutableLiveData<in Any?>?).execute()

    class MyAsyncTask(private val requester: LFMRequester, context: Context, private val mld: MutableLiveData<in Any?>? = null): AsyncTask<Unit, Unit, Any?>() {
        private var contextWr = WeakReference(context)

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

        private fun slurp(`is`: InputStream, bufferSize: Int): String {
            val buffer = CharArray(bufferSize)
            val out = StringBuilder()
            try {
                InputStreamReader(`is`, "UTF-8").use { `in` ->
                    while (true) {
                        val rsz = `in`.read(buffer, 0, buffer.size)
                        if (rsz < 0)
                            break
                        out.append(buffer, 0, rsz)
                    }
                }
            } catch (ex: InterruptedException){
                return ""
            } catch (ex: InterruptedIOException){
                return ""
            }
            catch (ex: IOException) {
                ex.printStackTrace()
                return ""
            }
            `is`.close()
            return out.toString()
        }

        fun reAuth(context: Context) {
            MultiPreferences(context).remove(Stuff.PREF_LASTFM_SESS_KEY)
            Stuff.openInBrowser(Stuff.LASTFM_AUTH_CB_URL, context)
        }

        fun getValidArtist(artist:String, track: String, threshold: Int = Stuff.MIN_LISTENER_COUNT):
                Pair<String, String>? {
            if (validArtistsCache[artist] == null) {
                val artistInfo = Artist.getInfo(artist, true, Stuff.LAST_KEY)
                Stuff.log("artistInfo: $artistInfo")
                //nw err throws an exception
                if (artistInfo!= null && artistInfo.name?.trim() != ""){
                    if(artistInfo.listeners >= threshold) {
                        validArtistsCache.put(artist, true)
                        return Pair(artist, track)
                    } else
                        validArtistsCache.put(artist, false)
                }

            } else if (validArtistsCache[artist])
                return Pair(artist, track)
            return null
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
