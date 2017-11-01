package com.arn.scrobble

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Animatable2
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.text.DecimalFormat
import java.util.*
import java.util.regex.Pattern


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val NOW_PLAYING = "np"
    const val SCROBBLE = "scrobble"
    const val AUTH_FROM_TOKEN = "auth"
    const val GET_RECENTS = "recents"
    const val GET_RECENTS_CACHED = "recents_cached"
    const val RELOAD_LIST_DATA = "list_refresh"
    const val PROFILE_PIC_PREF = "profile_cached"
    const val GET_DRAWER_INFO = "profile"
    const val GET_FRIENDS = "friends"
    const val LAST_KEY = Tokens.LAST_KEY
    const val LAST_SECRET = Tokens.LAST_SECRET
    const val TAG = "scrobbler"
    const val DL_SETTINGS = 31
    const val DL_APP_LIST = 32
    const val DEEP_LINK_KEY = "deeplink"
    const val LOVE = "loved"
    const val UNLOVE = "unloved"
    const val GET_FRIENDS_RECENTS = "get_friends_recent"
    const val NEED_FRIENDS_RECENTS = "need_friends_recent"
    const val FRIENDS_RECENTS_DELAY:Long = 1500
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
    const val NUM_SCROBBLES_PREF = "num_scrobbles_cached"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"

    const val RECENTS_REFRESH_INTERVAL: Long = 15 * 1000
    const val OFFLINE_SCROBBLE_JOB_DELAY: Long = 15 * 1000
    const val META_WAIT: Long = 500
    const val DEBOUNCE_TIME = 100
    const val MAX_APPS = 30
    const val MIN_LISTENER_COUNT = 7

    val AUTH_CB_URL = "https://www.last.fm/api/auth?api_key=${Stuff.LAST_KEY}&cb=pscrobble://auth"

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
            "—"," ‎– ", "–"," \\| ", " - ", "-", "「", "『", "ー", " • ",

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

    fun exec(command:String): String {
        val resp = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec(command)
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line:String? = ""
            do {
                resp.append(line+ "\n")
                line = bufferedReader.readLine()
            } while (line != null)

        } catch (e: IOException) {
        }
        return resp.toString()
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
                    if (splits.size == 1)
                        musicInfo[1] = splits[0]
                    else
                        break
                    //                    log(null, splits[0] + "|" + splits.length);
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
        val ctl = activity.findViewById<CollapsingToolbarLayout>(R.id.ctl)
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
        val k = 1000
        if (n < k) return n.toString()
        val exp = (Math.log(n.toDouble()) / Math.log(k.toDouble())).toInt()
        val unit = "KMB"[exp - 1] //kilo, million, bilion
        val dec = n / Math.pow(k.toDouble(), exp.toDouble())

        val decimal = DecimalFormat("#.#").format(dec)
        return decimal + unit
    }

    fun isNetworkAvailable(c: Context): Boolean {
        val connectivityManager = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
    fun myRelativeTime(date: Date?): CharSequence {
        var relDate:CharSequence = "   now"
        if(date != null)
            relDate = DateUtils.getRelativeTimeSpanString(
                date.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
        if (relDate[0] == '0')
            return "just now"
        return relDate
    }

    fun drawableToBitmap(drawable: Drawable, forceDraw:Boolean = false): Bitmap {
        if (!forceDraw && drawable is BitmapDrawable && !drawable.bitmap.isRecycled) {
            return drawable.bitmap
        } else if (drawable is PictureDrawable) {
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                    drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawPicture(drawable.picture)
            return bitmap
        }
        var width = drawable.intrinsicWidth
        width = if (width > 0) width else 1
        var height = drawable.intrinsicHeight
        height = if (height > 0) height else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun nowPlayingAnim(np: ImageView, isNowPlaying:Boolean){
        if (isNowPlaying) {
            np.visibility = View.VISIBLE
            val anim = np.drawable
            if (anim is AnimatedVectorDrawableCompat && !anim.isRunning) {
                anim.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        drawable as AnimatedVectorDrawableCompat?
                        drawable?.unregisterAnimationCallback(this)
                        if (drawable != null && drawable.isVisible) {
                            val newAnim = AnimatedVectorDrawableCompat.create(np.context, R.drawable.avd_eq)
                            np.setImageDrawable(newAnim)
                            newAnim?.start()
                            newAnim?.registerAnimationCallback(this)
                        }
                    }
                })
                anim.start()
            } else if (Build.VERSION.SDK_INT >= 23 && anim is AnimatedVectorDrawable && !anim.isRunning) {
                anim.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        if (drawable != null && drawable.isVisible)
                            (drawable as AnimatedVectorDrawable).start()
                    }
                })
                anim.start()
            }
        } else
            np.visibility = View.GONE
    }

    fun openInBrowser(url:String, context:Context){
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    }
}
