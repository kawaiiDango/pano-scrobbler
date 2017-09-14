package com.arn.scrobble

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import java.util.regex.Pattern

/**
 * Created by arn on 13-03-2017.
 */

internal object Stuff {
    val NOW_PLAYING = "np"
    val SCROBBLE = "scrobble"
    val CHECK_AUTH = "auth"
    val CHECK_AUTH_SILENT = "authSilent"
    val GET_RECENTS = "recents"
    val GET_RECENTS_CACHED = "recents_cached"
    val LAST_KEY = Tokens.LAST_KEY
    val LAST_SECRET = Tokens.LAST_SECRET
    val TAG = "scrobbler"
    val LOVE = "loved"
    val UNLOVE = "unloved"
    val GET_LOVED = "getloved"
    val HERO_INFO = "heroinfo"
    val IS_ONLINE = "online"
    val APP_WHITELIST = "app_whitelist"
    val APP_BLACKLIST = "app_blacklist"
    val AUTO_DETECT_PREF = "auto_detect"
    val FIRST_RUN_PREF = "first_run"

    val SESS_KEY = "sesskey"
    val USERNAME = "username"

    val RECENTS_REFRESH_INTERVAL: Long = 15 * 1000
    val MAX_APPS = 30
    var timeIt:Long = 0

    val APPS_IGNORE_ARTIST_META = arrayOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.mango",
            "com.google.android.apps.youtube.music",
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev"
    )

    private val seperators = arrayOf(// in priority order
            "—", " ‎– ", "–", " - ", "-", "「", "『", "ー",

            "【", "〖", "〔",
            // ":",
            " \"", " /")
    private val unwantedSeperators = arrayOf("』", "」", "\"", "'", "】", "〗", "〕")

    fun log(s: String) {
        Log.i(TAG, s)
    }

    fun timeIt(s: String) {
        val now = System.currentTimeMillis()
        Log.e(TAG+"_time: ", "["+ (now - timeIt) + "] " + s)
        timeIt = now
    }

    fun toast(c: Context, s: String) {
        try {
            Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            //            Log.i(TAG,"toastErr: "+e.getMessage())
        }

    }

    fun dumpBundle(c: Context, bundle: Bundle?) {
        if (bundle == null)
            Log.i(TAG, "Bundle: null")
        else {
            for (key in bundle.keySet()) {
                val value = bundle.get(key) ?: "null"
                Log.i(TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.javaClass.name))
            }
        }
    }

    fun sanitizeTitle(titleContentOriginal: String): Array<String> {
        //New detection of trackinformation
        //remove (*) and/or [*] to remove unimportant data
        val titleContent = titleContentOriginal.replace(" *\\([^)]*\\) *".toRegex(), " ")
                .replace(" *\\[[^)]*] *".toRegex(), " ")

                //remove HD info
                .replace("\\W* HD( \\W*)?".toRegex(), " ")
                .replace("\\W* HQ( \\W*)?".toRegex(), " ")

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

    fun humanReadableNum(bytes: Long): String {
        val unit = 1000
        if (bytes < unit) return bytes.toString()
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
        val pre = "KMB"[exp - 1] //kilo, million, bilion
        return String.format("%.1f%s", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
    }
}
