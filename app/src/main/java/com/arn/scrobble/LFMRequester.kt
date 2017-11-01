package com.arn.scrobble

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import android.os.AsyncTask
import android.os.Handler
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
import java.net.HttpURLConnection
import java.net.URL


/**
 * Created by arn on 18-03-2017.
 */

class LFMRequester constructor(val context: Context, private val handler: Handler? = null) : AsyncTask<String, Any, Any?>() {
    lateinit private var prefs: SharedPreferences
    lateinit private var command: String
    private var subCommand: String? = null

    override fun doInBackground(vararg s: String): Any? {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        command = s[0]
        val isNetworkAvailable = Stuff.isNetworkAvailable(context)
        try {
            Stuff.log("doInBackground $command isNetworkAvailable $isNetworkAvailable")
            var reAuthNeeded = false
            var session: Session? = null
            val caller = Caller.getInstance()
            caller.userAgent = Stuff.USER_AGENT
            val fsCache = FileSystemCache(context.cacheDir)
            if (command == Stuff.GET_RECENTS_CACHED)
                fsCache.expirationPolicy = LFMCachePolicy(false)
            else
                fsCache.expirationPolicy = LFMCachePolicy(isNetworkAvailable)
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

            if (!reAuthNeeded) {
                when (command) {
                    Stuff.AUTH_FROM_TOKEN -> return null
                    Stuff.GET_RECENTS_CACHED -> {
                        return User.getRecentTracks(username, 1, 15, true, Stuff.LAST_KEY)
                    }
                    Stuff.GET_RECENTS -> {
                        handler?.obtainMessage(0, Pair(Stuff.IS_ONLINE, isNetworkAvailable))
                                ?.sendToTarget()
                        subCommand = s[1]
                        return User.getRecentTracks(username, Integer.parseInt(subCommand), 15, true, Stuff.LAST_KEY)
                    }
                    Stuff.GET_FRIENDS_RECENTS -> {
                        subCommand = s[2]
                        return User.getRecentTracks(s[1], 1, 1, false, Stuff.LAST_KEY)
                    }
                    //for love: command = tag, s[1] = artist, s[2] = song,
                    Stuff.LOVE -> return Track.love(s[1], s[2], session)
                    Stuff.UNLOVE -> return Track.unlove(s[1], s[2], session)
                    Stuff.GET_DRAWER_INFO -> {
                        val profile = User.getInfo(session)
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        val recents = User.getRecentTracks(username, 1, 1, cal.timeInMillis/1000, 0, Stuff.LAST_KEY)

                        prefs.edit().putInt(Stuff.NUM_SCROBBLES_PREF, recents?.totalPages ?: 0)
                                .putString(Stuff.PROFILE_PIC_PREF, profile?.getImageURL(ImageSize.EXTRALARGE) ?: "")
                                .apply()
                        return null
                    }
                    Stuff.GET_FRIENDS -> {
                        handler?.obtainMessage(0, Pair(Stuff.IS_ONLINE, isNetworkAvailable))
                                ?.sendToTarget()
                        subCommand = s[1]
                        return User.getFriends(username, true, Integer.parseInt(subCommand), 20, Stuff.LAST_KEY)
                    }
                    Stuff.HERO_INFO -> {
                        handler?.obtainMessage(0, Pair(Stuff.IS_ONLINE, isNetworkAvailable))
                                ?.sendToTarget()
                        if (!isNetworkAvailable)
                            return null
                        //s[1] = page url, s[2] = api large image url
                        val url = URL(s[1])
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
                            var img = s[2]
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

                //for scrobble: command = tag, s[1] = artist, s[2] = album, s[3] = song, s[4] = time, s[5] = duration
                val scrobbleData = ScrobbleData()
                scrobbleData.artist = s[1]
                scrobbleData.album = s[2]
                scrobbleData.track = s[3]
                scrobbleData.timestamp = (s[4].toLong()/1000).toInt() // in secs
                scrobbleData.duration = (s[5].toLong()/1000).toInt() // in secs

                when(command) {
                    Stuff.NOW_PLAYING -> {
                        if (isNetworkAvailable){
                            val hash = s[1].hashCode() + s[3].hashCode()
                            val corrected = getCorrectedData(s[1], s[3])
                            if (corrected != null) {
                                scrobbleData.artist = corrected.first
                                scrobbleData.track = corrected.second
                                scrobbleResult = Track.updateNowPlaying(scrobbleData, session)
                            } else {
                                (handler as NLService.ScrobbleHandler).remove(hash)
                                handler.notification(context.getString(R.string.invalid_artist), s[1], context.getString(R.string.not_scrobling), NLService.NOTI_ERR_ICON)
                            }
                        }
                    }
                    Stuff.SCROBBLE -> {
                        if (isNetworkAvailable)
                            scrobbleResult = Track.scrobble(scrobbleData, session)
                        else {
                            val dao = PendingScrobblesDb.getDb(context).getDao()
                            val entry = PendingScrobble()
                            entry.artist = scrobbleData.artist
                            entry.album = scrobbleData.album
                            entry.track = scrobbleData.track
                            entry.timestamp = s[4].toLong()
                            entry.duration = s[5].toLong()
                            dao.insert(entry)
                            OfflineScrobbleJob.checkAndSchedule(context, true)
                        }
                    }
                }
                try {
                    if (scrobbleResult?.isSuccessful == false) {

                        (handler as NLService.ScrobbleHandler)
                                .notification(context.getString(R.string.network_error), s[1] + " " + s[3], context.getString(R.string.not_scrobling), android.R.drawable.stat_notify_error)
                    } else if(scrobbleResult?.isSuccessful == true && scrobbleResult.isIgnored) {
                        val hash = s[1].hashCode() + s[3].hashCode()
                        val handler = handler as NLService.ScrobbleHandler
                        handler.remove(hash)
                        val artistTrunc = if (s[1].length > 12) s[1].substring(0, 12) else s[1]
                        handler.notification(s[3], context.getString(R.string.scrobble_ignored, artistTrunc), context.getString(R.string.not_scrobling), R.drawable.ic_transparent)

                    }
                } catch (e: NullPointerException) {
                    publishProgress(command + ": NullPointerException")
                }
            } else if (command == Stuff.AUTH_FROM_TOKEN){
                subCommand = if (s.size == 2) s[1] else null
                val token = subCommand
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
                Stuff.log("command: $command")
                reAuth()
            }
        } catch(e: CallException){
            //ignore
        } catch(e: InterruptedIOException){
            //ignore
        } catch (e: Exception) {
            e.printStackTrace()
            publishProgress("err: "+ e.cause)
        }

        // adb shell am start -W -a android.intent.action.VIEW -d "pscrobble://auth?token=hohoho" com.arn.scrobble
        return null
    }

    private fun reAuth() {
//        token = Authenticator.getToken(Stuff.LAST_KEY)
        publishProgress(context.getString(R.string.please_authorize))
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .remove(Stuff.SESS_KEY)
                .apply()
        Stuff.openInBrowser(Stuff.AUTH_CB_URL, context)
    }

    override fun onProgressUpdate(vararg values: Any) {
        super.onProgressUpdate(*values)
        val res = values[0]
        if (res is String) //usually err msg
            Stuff.toast(context, values[0].toString())
    }

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
        } else if (command == Stuff.HERO_INFO && res is MutableList<*>) {
//            val s = res as MutableList<String?>
            handler?.obtainMessage(0, Pair(command, res))?.sendToTarget()
        }
    }

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
