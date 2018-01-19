package com.arn.scrobble

import android.content.AsyncTaskLoader
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import java.util.Calendar
import android.preference.PreferenceManager
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.PendingScrobblesDb
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


/**
 * Created by arn on 18-03-2017.
 */

class LFMRequester constructor(context: Context, var command: String, vararg args: String) : AsyncTaskLoader<Any?>(context) {
    lateinit private var prefs: SharedPreferences
    var args = arrayListOf<String>()
    var isLoading = false

    init {
        args.forEach { this.args.add(it) }
    }

    override fun loadInBackground(): Any? {
        isLoading = true
//        Stuff.timeIt("loadInBackground "+ command)
        Stuff.log("loadInBackground $command $args")
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        try {
            var reAuthNeeded = false
            var session: Session? = null
            val caller = Caller.getInstance()
            caller.userAgent = Stuff.USER_AGENT
            val fsCache = FileSystemCache(context.cacheDir)

            if (command == Stuff.GET_RECENTS_CACHED) {
                command = Stuff.GET_RECENTS
                fsCache.expirationPolicy = LFMCachePolicy(false)
            } else
                fsCache.expirationPolicy = LFMCachePolicy(Main.isOnline)
            caller.cache = fsCache
//            caller.isDebugMode = false

            val key: String = prefs.getString(Stuff.SESS_KEY, "")
            var username: String? = prefs.getString(Stuff.USERNAME, null)

            if (key.length > 5)
                session = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, key)
            else
                reAuthNeeded = true

            if (session == null || username == null)
                reAuthNeeded = true
//            Stuff.timeIt("!reAuthNeeded " + command)
            if (!reAuthNeeded) {
                when (command) {
                    Stuff.AUTH_FROM_TOKEN -> return null
                    Stuff.GET_RECENTS -> {
                        return User.getRecentTracks(username, Integer.parseInt(args[0]), 15, true, Stuff.LAST_KEY)
                    }
                    Stuff.GET_FRIENDS_RECENTS -> {
                        // args[1] = position
                        return User.getRecentTracks(args[0], 1, 1, false, Stuff.LAST_KEY)
                    }
                    //for love: command = tag, args[1] = artist, args[2] = song,
                    Stuff.LOVE -> return Track.love(args[0], args[1], session)
                    Stuff.UNLOVE -> return Track.unlove(args[0], args[1], session)
                    Stuff.GET_SIMILAR -> return Track.getSimilar(args[0], args[1], Stuff.LAST_KEY, 6)
                    Stuff.GET_DRAWER_INFO -> {
                        val profile = User.getInfo(session)
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        val recents = User.getRecentTracks(username, 1, 1, cal.timeInMillis/1000, 0, Stuff.LAST_KEY)

                        prefs.edit().putInt(Stuff.PREF_NUM_SCROBBLES, recents?.totalPages ?: 0)
                                .putString(Stuff.PREF_PROFILE_PIC, profile?.getImageURL(ImageSize.EXTRALARGE) ?: "")
                                .apply()
                        return null
                    }
                    Stuff.GET_FRIENDS -> {
                        val limit = if (args.size > 1) args[1].toInt() else 30
                        return try {
                            User.getFriends(username, true, Integer.parseInt(args[0]), limit, Stuff.LAST_KEY)
                        } catch (e:NullPointerException){
                            PaginatedResult<User>(1,0, listOf())
                        }
                    }
                    Stuff.HERO_INFO -> {
                        if (!Main.isOnline)
                            return null
                        //args[1] = page url, args[2] = api large image url
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
                            var idx = resp.indexOf("id=\"header-expanded-image\"")
                            var img = args[1]
                            var idx2: Int
                            if (idx > -1) {
                                idx = resp.indexOf("src=", idx) + 5
                                idx2 = resp.indexOf("\"", idx)
                                img = resp.substring(idx, idx2)
                            }
//                            if (img == "")
                                //TODO: get artist image
                            scrapped.add(img)
                            //1
                            idx = resp.indexOf("charts/sparkline")
                            if (idx > -1) {
                                idx = resp.indexOf("[", idx) + 1
                                idx2 = resp.indexOf("]", idx)
                                val chart = resp.substring(idx, idx2)
                                scrapped.add(chart)
                            } else
                                scrapped.add(null)
                        } finally {
                            urlConnection?.disconnect()
                        }
                        return scrapped
                    }
                }

                var scrobbleResult: ScrobbleResult? = null

                //for scrobble: command = tag, args[1] = artist, args[2] = album, args[3] = song, args[4] = time, args[5] = duration
                val scrobbleData = ScrobbleData()
                scrobbleData.artist = args[0]
                scrobbleData.album = args[1]
                scrobbleData.track = args[2]
                scrobbleData.timestamp = (args[3].toLong()/1000).toInt() // in secs
                scrobbleData.duration = (args[4].toLong()/1000).toInt() // in secs

                if (scrobbleData.duration < 5)
                    scrobbleData.duration = -1 //default

                when(command) {
                    Stuff.NOW_PLAYING -> {
                        if (NLService.isOnline){
                            val hash = args[0].hashCode() + args[2].hashCode()
                            val corrected = getCorrectedData(args[0], args[2])
                            if (corrected != null) {
                                scrobbleData.artist = corrected.first
                                scrobbleData.track = corrected.second
                                scrobbleResult = Track.updateNowPlaying(scrobbleData, session)
                            } else {
                                notifyFailed(args[0], context.getString(R.string.invalid_artist),
                                        context.getString(R.string.not_scrobling), NLService.NOTI_ERR_ICON, hash)
                            }
                        }
                    }
                    Stuff.SCROBBLE -> {
                        var couldntConnect = false
                        if (NLService.isOnline) {
                            try {
                                scrobbleResult = Track.scrobble(scrobbleData, session)
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
                            entry.timestamp = args[3].toLong()
                            entry.duration = args[4].toLong()
                            dao.insert(entry)
                            OfflineScrobbleJob.checkAndSchedule(context, true)
                        }
                    }
                }
                try {
                    val hash = args[0].hashCode() + args[2].hashCode()
                    if (scrobbleResult?.isSuccessful == false) {
                        notifyFailed(args[0] + " " + args[2], context.getString(R.string.network_error), context.getString(R.string.not_scrobling),
                                android.R.drawable.stat_notify_error, hash)
                    } else if(scrobbleResult?.isSuccessful == true && scrobbleResult.isIgnored) {
                        val artistTrunc = if (args[0].length > 12) args[0].substring(0, 12) else args[0]
                        notifyFailed(args[2], context.getString(R.string.scrobble_ignored, artistTrunc), context.getString(R.string.not_scrobling),
                                NLService.NOTI_ERR_ICON, hash)
                    }
                } catch (e: NullPointerException) {
                    return command + ": NullPointerException"
                }
            } else if (command == Stuff.AUTH_FROM_TOKEN){
                val token = if (args.size == 1) args[0] else null
                if (token != null && token.length > 5) {
                    session = Authenticator.getSession(token, Stuff.LAST_KEY, Stuff.LAST_SECRET)
                    if (session != null) {
                        username = session.username
                        prefs.edit()
                                .putString(Stuff.USERNAME, username)
                                .putString(Stuff.SESS_KEY, session.key)
                                .apply()
                    }
                }
            } else if (command != Stuff.GET_RECENTS ) {
                reAuth()
                return context.getString(R.string.please_authorize)
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

    override fun deliverResult(data: Any?) {
        isLoading = false
        if (data is String) {
            Stuff.toast(context, data.toString()) //error msgs
            super.deliverResult(null)
        } else {
            super.deliverResult(data)
//            Stuff.timeIt("deliverResult")
        }
    }

    private fun notifyFailed(title1: String, title2: String?, state: String, iconId: Int, hash: Int){
        val i = Intent(NLService.iNOTIFY_FAILED)
        i.putExtra("title1", title1)
        i.putExtra("title2", title2)
        i.putExtra("state", state)
        i.putExtra("iconId", iconId)
        i.putExtra("hash", hash)
        context.sendBroadcast(i)
    }

    fun inAsyncTask(){
        MyAsyncTask(this).execute()
    }
    
    private fun reAuth() {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(Stuff.SESS_KEY)
                .apply()
        Stuff.openInBrowser(Stuff.AUTH_CB_URL, context)
    }

    class MyAsyncTask(loader: LFMRequester): AsyncTask<Unit, Unit, Any?>() {
        private var loaderWr: WeakReference<LFMRequester> = WeakReference(loader)

        override fun doInBackground(vararg p0: Unit?): Any? = loaderWr.get()?.loadInBackground()

        override fun onPostExecute(res: Any?) {
            val loader = loaderWr.get()
            if (loader!= null && res is String)
                Stuff.toast(loader.context, res)
        }
    }
/*
    override fun onPostExecute(res: Any?) {
        //do stuff
        if (res is PaginatedResult<*>) {
            if (command == Stuff.GET_FRIENDS_RECENTS)
                handler?.obtainMessage(0, Pair(command,Pair(subCommand, res)))?.sendToTarget() //subCommand = grid position
            else
                handler?.obtainMessage(0, Pair(command,res))?.sendToTarget()
        } else if (res is Result) {
            if (!res.isSuccessful) {
                if (res.errorMessage != null)
                    Stuff.toast(context, command + ": " + res.errorMessage)
                else
                    Stuff.toast(context, command+ " failed")
            }
        } else if (command == Stuff.HERO_INFO && res is MutableList<*> ||
                command == Stuff.GET_SIMILAR && res is ArrayList<*>) {
//            val s = res as MutableList<String?>
            handler?.obtainMessage(0, Pair(command, res))?.sendToTarget()
        } else if (res is String){ // error msg
            Stuff.toast(context, res)
        }
    }
*/
    companion object {
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

            return out.toString()
        }

        fun getCorrectedData(artist:String, track: String): Pair<String, String>? {
            val artistInfo = Artist.getInfo(artist, true, Stuff.LAST_KEY)
            return if (artistInfo != null && artistInfo.name?.trim() != "" && artistInfo.listeners >= Stuff.MIN_LISTENER_COUNT)
                Pair(artistInfo.name, track)
            else
                null
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
}//    private static ArrayList<Integer> scrobbledHashes= new ArrayList<>();
