package com.arn.scrobble

import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.app.Fragment
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.preference.PreferenceManager
import android.support.design.widget.CollapsingToolbarLayout
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import kotlinx.android.synthetic.main.coordinator_main.*
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.text.DecimalFormat
import java.util.*


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val NOW_PLAYING = "np"
    const val SCROBBLE = "scrobble"
    const val AUTH_FROM_TOKEN = "auth"
    const val GET_RECENTS = "recents"
    const val GET_SIMILAR = "similar"
    const val GET_RECENTS_CACHED = "recents_cached"
    const val RELOAD_LIST_DATA = "list_refresh"
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
    const val FRIENDS_RECENTS_DELAY:Long = 800
    const val HERO_INFO = "heroinfo"
    const val PREF_MASTER = "master"
    const val PREF_WHITELIST = "app_whitelist"
    const val PREF_BLACKLIST = "app_blacklist"
    const val PREF_SEARCH_URL = "search_url"
    const val PREF_AUTO_DETECT = "auto_detect"
    const val PREF_FIRST_RUN = "first_run"
    const val PREF_GRAPH_DETAILS = "show_graph_details"
    const val PREF_OFFLINE_SCROBBLE = "offline_scrobble"
    const val PREF_NUM_SCROBBLES = "num_scrobbles_cached"
    const val PREF_PROFILE_PIC = "profile_cached"
    const val ARGS_SUMMARY_VIEW = "summary"

    const val SESS_KEY = "sesskey"
    const val USERNAME = "username"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"

    const val RECENTS_REFRESH_INTERVAL: Long = 15 * 1000
    const val CANCELLABLE_MSG = 9
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

    val IGNORE_LEGAGY_META = arrayOf(
            "com.n7mobile.nplayer"
    )

    private val seperators = arrayOf(// in priority order
            "—"," ‎– ", "–", " - "," \\| ", "-", "「", "『", "ー", " • ",

            "【", "〖", "〔",
            "】", "〗","』", "」", "〕",
            // ":",
            " \"", " /")
    private val unwantedSeperators = arrayOf("『", "』","「", "」", "\"", "'", "【", "】", "〖", "〗", "〔", "〕", "\\|")

    private val metaSpam = arrayOf("downloaded")

    fun log(s: String) {
        Log.i(TAG, s)
    }

    fun timeIt(s: String) {
        val now = System.currentTimeMillis()
        Log.w(TAG+"_time: ", "["+ (now - timeIt) + "] " + s)
        timeIt = now
    }

    fun toast(c: Context, s: String, len:Int = Toast.LENGTH_SHORT) {
        try {
            Toast.makeText(c, s, len).show()
        } catch (e: Exception) {
        }
    }

    fun bundleDump(bundle: Bundle?): String {
        bundle ?: return "null"
        var s = ""
        for (key in bundle.keySet().sortedDescending()) {
            val value = bundle.get(key) ?: "null"
            s += String.format("%s= %s, ", key, value.toString())
        }
        return s
    }

    fun exec(command:String): String {
        var resp = ""
        try {
            val process = Runtime.getRuntime().exec(command)
            resp = process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
        }
        return resp
    }

    fun sanitizeTitle(titleContentOriginal: String): Array<String> {
        //New detection of trackinformation
        //remove (*) and/or [*] to remove unimportant data
        val titleContent = titleContentOriginal.replace(" *\\([^)]*\\) *".toRegex(), " ")
                .replace(" *\\[[^)]*] *".toRegex(), " ")

                //remove HD info
                .replace("\\W* HD|HQ|4K|MV|Official Music Video|Music Video|Lyric Video|Official Audio( \\W*)?"
                                .toRegex(RegexOption.IGNORE_CASE)
                , " ")

//        get remix info
        val remixInfo = "\\([^)]*(?:remix|mix|cover|version|edit|booty?leg)\\)".toRegex(RegexOption.IGNORE_CASE).find(titleContentOriginal)

        var musicInfo: Array<String>? = null
        for (s in seperators) {
            //parsing artist - title
            musicInfo = titleContent.split(s.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

//            println("musicInfo= "+musicInfo[0] + (if (musicInfo.size >1) "," + musicInfo[1] else "") + "|" + musicInfo.size)
            //got artist, parsing title - audio (cover) [blah]
            if (musicInfo.size > 1 && musicInfo[0] != "") {
                for (j in 0 until seperators.size - 2) {
                    val splits = musicInfo[1].split(seperators[j].toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                    println("splits= "+splits[0] + (if (splits.size >1) "," + splits[1] else "") + "|" + splits.size)
                    if (splits.size == 1)
                        musicInfo[1] = splits[0]
                    else
                        break
                }
                break
            }
        }


        if (musicInfo == null || musicInfo.size == 1) {
            return arrayOf(titleContent, "")
        }

        //remove ", ', 」, 』 from musicInfo
        for (i in musicInfo.indices) {
            for (s in unwantedSeperators)
                musicInfo[i] = musicInfo[i].replace("^\\s*$s|$s\\s*$".toRegex(), " ")
        }

        musicInfo[1] = musicInfo[1].replace("\\.(avi|wmv|mp4|mpeg4|mov|3gpp|flv|webm)$".toRegex(RegexOption.IGNORE_CASE), " ")
                .replace("Full Album".toRegex(RegexOption.IGNORE_CASE), "")
        //Full Album Video

        //move feat. info from artist to
        musicInfo[0] = musicInfo[0].replace(" (ft\\.?) ".toRegex(), " feat. ")
        if (musicInfo[0].contains(" feat.* .*".toRegex(RegexOption.IGNORE_CASE))) {
            val m = " feat.* .*".toRegex(RegexOption.IGNORE_CASE).find(musicInfo[0])
            musicInfo[1] = musicInfo[1] + (m!!.groups[0]?.value ?: "").trim()
            musicInfo[0] = musicInfo[0].replace(" feat.* .*".toRegex(RegexOption.IGNORE_CASE), "")
        }

        //add remix info
        if (remixInfo?.groups?.isNotEmpty() == true) {
            musicInfo[1] = musicInfo[1].trim() + " " + remixInfo.groups[0]?.value
        }

        //delete spaces
        musicInfo[0] = musicInfo[0].replace("^\\s\\s*", "").replace("\\s\\s*$", "").trim()
        musicInfo[1] = musicInfo[1].replace("^\\s\\s*", "").replace("\\s\\s*$", "").trim()

        return musicInfo
    }

    fun sanitizeAlbum(albumOrig:String): String {
        if (albumOrig.contains("unknown", true) &&
                albumOrig.length <= "unknown".length + 4)
            return ""
        //url
        val splits = albumOrig.split(' ')
        splits.forEach {
            try {
                if (it.matches(".*\\w+\\.\\w+.*".toRegex())){
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

    fun sanitizeArtist(artistOrig: String): String{
        val splits = artistOrig.split('|')
        return splits[0]
    }

    fun setTitle(activity:Activity, strId: Int){
        val ctl = activity.findViewById<CollapsingToolbarLayout>(R.id.ctl)
        if (strId == 0) { // = clear title
            ctl.title = null
        } else {
            ctl.title = activity.getString(strId)
//        ctl.tag = activity.getString(strId)
            ctl.setContentScrimColor(ContextCompat.getColor(activity, R.color.colorPrimary))
            ctl.setCollapsedTitleTextColor(Color.WHITE)
        }
    }

    fun setAppBarHeight(activity: Activity, additionalHeight: Int = 0){

        val sHeightPx: Int
        val dm = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(dm)
        sHeightPx = dm.heightPixels

        val abHeightPx = activity.resources.getDimension(R.dimen.app_bar_height)
        val targetAbHeight: Int
        val lp = activity.app_bar.layoutParams

        if (sHeightPx < abHeightPx + additionalHeight + Stuff.dp2px(40, activity))
            targetAbHeight = activity.resources.getDimensionPixelSize(R.dimen.app_bar_summary_height)
        else
            targetAbHeight = activity.resources.getDimensionPixelSize(R.dimen.app_bar_height)
        if (targetAbHeight != lp.height) {
            if (activity.app_bar.isCollapsed) {
                lp.height = targetAbHeight
                activity.app_bar.setExpanded(false, false)
            } else {
                val start = lp.height
                val anim = ValueAnimator.ofInt(start, targetAbHeight)
                anim.addUpdateListener { valueAnimator ->
                    lp.height = valueAnimator.animatedValue as Int
                    activity.app_bar.layoutParams = lp
                }
                anim.interpolator = DecelerateInterpolator()
                anim.duration = 300
                anim.start()

            }
        }
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

    fun dp2px (dp: Int, c: Context): Int =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), c.resources.displayMetrics).toInt()

    fun sp2px (sp: Int, c: Context): Int =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), c.resources.displayMetrics).toInt()

    fun humanReadableNum(n: Long): String {
        val k = 1000
        if (n < k) return n.toString()
        val exp = (Math.log(n.toDouble()) / Math.log(k.toDouble())).toInt()
        val unit = "KMB"[exp - 1] //kilo, million, bilion
        val dec = n / Math.pow(k.toDouble(), exp.toDouble())

        val decimal = DecimalFormat("#.#").format(dec)
        return decimal + unit
    }

    fun getOnlineStatus(context: Context): Boolean{
        //directly using context creates leaks in MM
        val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val ni = manager.activeNetworkInfo
        return ni?.isConnected == true
    }

    fun myRelativeTime(context:Context, date: Date?): CharSequence =
            myRelativeTime(context, date?.time ?: 0)

    fun myRelativeTime(context:Context, millis:Long): CharSequence {
        var relDate:CharSequence = "   " + context.getString(R.string.time_now)
        if(millis != 0L)
            relDate = DateUtils.getRelativeTimeSpanString(
                millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL)
        if (relDate[0] == '0')
            return context.getString(R.string.time_now_long)
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

    fun isMiui(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("com.miui.securitycenter", PackageManager.GET_META_DATA)
            true
//                (Class.forName("android.os.SystemProperties")
//                        .getMethod("get", String::class.java)
//                        .invoke(null, "ro.miui.ui.version.code") as String? ?: "") != ""
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
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
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && anim is AnimatedVectorDrawable && !anim.isRunning) {
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

    fun setProgressCircleColor(swl: SwipeRefreshLayout){
        swl.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary)
        swl.setProgressBackgroundColorSchemeResource(R.color.darkBg)
    }
//
    fun openSearchURL(query:String, view: View, context: Context) {
        var url =
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(PREF_SEARCH_URL, context.getString(R.string.search_site_default))

        try {
            url += URLEncoder.encode(query, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            Stuff.toast(context, context.getString(R.string.failed_encode_url))
        }
        openInBrowser(url, context, view)
    }

    fun openInBrowser(url:String, context:Context, source:View? = null, startX:Int = 10, startY:Int = 10){
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        var bundle:Bundle? = null
        if (source != null){
            bundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                ActivityOptions.makeClipRevealAnimation(source, startX,startY, 10, 10)
                        .toBundle()
            } else
                ActivityOptions.makeScaleUpAnimation(source, startX, startY, 100, 100)
                        .toBundle()
        }
        try {
            context.startActivity(browserIntent, bundle)
        } catch (e: ActivityNotFoundException) {
            toast(context, context.getString(R.string.no_browser))
        }
    }

    class TimedRefresh(fragment: Fragment, private val loaderId: Int): Runnable{
        private val fragmentWr: WeakReference<Fragment> = WeakReference(fragment)

        override fun run() {
            val fragment = fragmentWr.get()
            if (fragment != null && fragment.isAdded)
                fragment.loaderManager.getLoader<Any>(loaderId)?.forceLoad()
        }
    }
}

fun Array<String>.toArgsBundle(key: String = "args"): Bundle {
    val b = Bundle()
    b.putStringArray(key, this)
    return b
}