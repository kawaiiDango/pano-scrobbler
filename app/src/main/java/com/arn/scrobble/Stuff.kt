package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.regex.Pattern


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val NOW_PLAYING = "np"
    const val SCROBBLE = "scrobble"
    const val CHECK_AUTH = "auth"
    const val CHECK_AUTH_SILENT = "authSilent"
    const val GET_RECENTS = "recents"
    const val GET_RECENTS_CACHED = "recents_cached"
    const val LAST_KEY = Tokens.LAST_KEY
    const val LAST_SECRET = Tokens.LAST_SECRET
    const val TAG = "scrobbler"
    const val DL_SETTINGS = 31
    const val DL_APP_LIST = 32
    const val DEEP_LINK_KEY = "deeplink"
    const val LOVE = "loved"
    const val UNLOVE = "unloved"
    const val GET_LOVED = "getloved"
    const val HERO_INFO = "heroinfo"
    const val IS_ONLINE = "online"
    const val APP_WHITELIST = "app_whitelist"
    const val APP_BLACKLIST = "app_blacklist"
    const val AUTO_DETECT_PREF = "auto_detect"
    const val FIRST_RUN_PREF = "first_run"
    const val GRAPH_DETAILS_PREF = "show_graph_details"
    const val OFFLINE_SCROBBLE_PREF = "offline_scrobble"

    const val SESS_KEY = "sesskey"
    const val USERNAME = "username"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"

    const val RECENTS_REFRESH_INTERVAL: Long = 15 * 1000
    const val OFFLINE_SCROBBLE_JOB_DELAY: Long = 15 * 1000
    const val META_WAIT: Long = 800
    const val DEBOUNCE_TIME = 100
    const val MAX_APPS = 30
    private var timeIt:Long = 0

    val APPS_IGNORE_ARTIST_META = arrayOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.mango",
            "com.google.android.apps.youtube.music",
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev"
    )

    private val seperators = arrayOf(// in priority order
            "—"," ‎– ", "–"," \\| ", " - ", "-", "「", "『", "ー",

            "【", "〖", "〔",
            // ":",
            " \"", " /")
    private val unwantedSeperators = arrayOf("』", "」", "\"", "'", "】", "〗", "〕")

    private val metaSpam = arrayOf("downloaded")

    fun log(s: String) {
        Log.i(TAG, s)
    }

    fun timeIt(s: String) {
        val now = System.currentTimeMillis()
        Log.e(TAG+"_time: ", "["+ (now - timeIt) + "] " + s)
        timeIt = now
    }

    fun toast(c: Context, s: String, len:Int = Toast.LENGTH_SHORT) {
        try {
            Toast.makeText(c, s, len).show()
        } catch (e: Exception) {
        }
    }

    fun bundleDump(bundle: Bundle?): String {
        if (bundle == null)
            return "null"
        else {
            var s = ""
            for (key in bundle.keySet().sortedDescending()) {
                val value = bundle.get(key) ?: "null"
                s += String.format("%s= %s, ", key, value.toString())
            }
            return s
        }
    }

    fun getLogcat(): String {
        val log = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line:String? = ""
            do {
                log.append(line+ "\n")
                line = bufferedReader.readLine()
            } while (line != null)

        } catch (e: IOException) {
        }
        return log.toString()
    }

    fun sanitizeTitle(titleContentOriginal: String): Array<String> {
        //New detection of trackinformation
        //remove (*) and/or [*] to remove unimportant data
        val titleContent = titleContentOriginal.replace(" *\\([^)]*\\) *".toRegex(), " ")
                .replace(" *\\[[^)]*] *".toRegex(), " ")

                //remove HD info
                .replace("\\W* HD( \\W*)?".toRegex(), " ")
                .replace("\\W* HQ( \\W*)?".toRegex(), " ")
                .replace("\\W* Music Video( \\W*)?".toRegex(), " ")

        var r = Pattern.compile("\\([^)]*(?:remix|mix|cover|version|edit|booty?leg)\\)")
        //get remix info
        val remixInfo = r.matcher(titleContentOriginal)

        var musicInfo: Array<String>? = null
        for (s in seperators) {
            musicInfo = titleContent.split(s.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            //            log(null ,"********"+s + "|"+musicInfo[0]);
            if (musicInfo.size > 1) {
                for (j in 0 until seperators.size - 2) {
                    val splits = musicInfo[1].split(seperators[j].toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    musicInfo[1] = splits[0]
                    //                    log(null, splits[0] + "|" + splits.length);
                    if (splits.size > 1)
                        break
                }
                break
            }
        }


        if (musicInfo == null || musicInfo.size == 1) {
            return arrayOf(titleContent, "")
            //            feedback = "notFound";
        }

        //remove ", ', 」, 』 from musicInfo
        for (i in musicInfo.indices) {
            for (s in unwantedSeperators)
                musicInfo[i] = musicInfo[i].replace("^\\s*$s|$s\\s*$".toRegex(), " ")
        }

        musicInfo[1] = musicInfo[1].replace("\\.(avi|wmv|mp4|mpeg4|mov|3gpp|flv|webm)$", " ")
                .replace("Full Album".toRegex(), "")
        //Full Album Video

        //move feat. info from artist to
        musicInfo[0] = musicInfo[0].replace(" (ft\\.?) ".toRegex(), " feat. ")
        if (musicInfo[0].matches(" feat.* .*".toRegex())) {
            r = Pattern.compile(" feat.* .*")
            val m = r.matcher(musicInfo[0])
            musicInfo[1] = musicInfo[1] + m.group()
            musicInfo[0] = musicInfo[0].replace(" feat.* .*".toRegex(), "")
        }

        //add remix info
        if (remixInfo.find()) {
            musicInfo[1] += " " + remixInfo.group(0)
        }

        //delete spaces
        musicInfo[0] = musicInfo[0].replace("^\\s\\s*", "").replace("\\s\\s*$", "").trim()
        musicInfo[1] = musicInfo[1].replace("^\\s\\s*", "").replace("\\s\\s*$", "").trim()

        return musicInfo
    }

    fun sanitizeAlbum(albumOrig:String): String {
        //url
        val splits = albumOrig.split(' ')
        splits.forEach {
            try {
                if (it.contains('.')){
                    if (it.contains(':'))
                        URL(it)
                    else
                        URL("http://"+it)
                    return ""
                }
            } catch (e: MalformedURLException){
            }
        }

        if (metaSpam.any {albumOrig.contains(it)})
            return ""

        return albumOrig
    }

    fun setTitle(activity:Activity, strId: Int){
        val ctl = activity.findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout)
        ctl.title = activity.getString(strId)
//        ctl.tag = activity.getString(strId)
        ctl.setContentScrimColor(ContextCompat.getColor(activity, R.color.colorPrimary))
        ctl.setCollapsedTitleTextColor(Color.WHITE)
    }

    fun getMatColor(c: Context, typeColor: String, hash: Long = 0): Int {
        var hash = hash
        var returnColor = Color.BLACK
        val arrayId = c.resources.getIdentifier("mdcolor_" + typeColor, "array", c.packageName)

        if (arrayId != 0) {
            val colors = c.resources.obtainTypedArray(arrayId)
            val index: Int
            if (hash < 0)
                hash = -hash
            index = if (hash.toInt() == 0)
                (Math.random() * colors.length()).toInt()
            else
                (hash % colors.length()).toInt()
            returnColor = colors.getColor(index, Color.BLACK)
            colors.recycle()
        }
        return returnColor
    }

    fun isDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    fun dp2px (dp: Int, c: Context): Int {
       return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), c.resources.displayMetrics).toInt()
    }

    fun humanReadableNum(n: Long): String {
        val unit = 1000
        if (n < unit) return n.toString()
        val exp = (Math.log(n.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMB"[exp - 1] //kilo, million, bilion
        return String.format("%.1f%s", n / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }

    fun isNetworkAvailable(c: Context): Boolean {
        val connectivityManager = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
    fun myRelativeTime(date: Date?): CharSequence {
        var relDate:CharSequence = "just now"
        if(date != null)
            relDate = DateUtils.getRelativeTimeSpanString(
                date.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        if (relDate[0] == '0')
            return "just now"
        return relDate
    }
}
