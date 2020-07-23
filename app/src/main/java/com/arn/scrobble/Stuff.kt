package com.arn.scrobble

import android.animation.ValueAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.arn.scrobble.RecentsFragment.Companion.lastColorMutedBlack
import com.arn.scrobble.ui.ShadowDrawerArrowDrawable
import com.arn.scrobble.ui.StatefulAppBar
import com.google.android.material.appbar.CollapsingToolbarLayout
import de.umass.lastfm.Caller
import de.umass.lastfm.cache.FileSystemCache
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*
import java.io.IOException
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*
import java.util.logging.Level
import kotlin.math.ln
import kotlin.math.pow


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val NOW_PLAYING = "np"
    const val SCROBBLE = "scrobble"
    const val LASTFM_SESS_AUTH = "lastfm_auth"
    const val LIBREFM_SESS_AUTH = "librefm_auth"
    const val GNUFM_SESS_AUTH = "gnufm_auth"
    const val GET_RECENTS = "recents"
    const val GET_RECENTS_CACHED = "recents_cached"
    const val GET_LOVES = "loves"
    const val GET_LOVES_CACHED = "loves_cached"
    const val TAG_PAGER = "pager"
    const val TAG_FIRST_THINGS = "first_things"
    const val ARG_URL = "url"
    const val ARG_SAVE_COOKIES = "cookies"
    const val ARG_NOPASS = "nopass"
    const val DELETE = "delete"
    const val GET_SIMILAR = "similar"
    const val TAG_SIMILAR = "similar"
    const val GET_DRAWER_INFO = "profile"
    const val GET_FRIENDS = "friends"
    const val LIBREFM_KEY = "panoScrobbler"
    const val LAST_KEY = Tokens.LAST_KEY
    const val LAST_SECRET = Tokens.LAST_SECRET
    const val TAG = "scrobbler"
    const val DL_SETTINGS = 31
    const val DL_APP_LIST = 32
    const val DL_NOW_PLAYING = 33
    const val DL_MIC = 34
    const val DIRECT_OPEN_KEY = "directopen"
    const val LOVE = "loved"
    const val UNLOVE = "unloved"
    const val GET_FRIENDS_RECENTS = "get_friends_recent"
    const val FRIENDS_RECENTS_DELAY: Long = 800
    const val GET_HERO_INFO = "heroinfo"
    const val GET_INFO = "info"
    const val PREF_MASTER = "master"
    const val PREF_NOTIFICATIONS = "show_notifications"
    const val PREF_WHITELIST = "app_whitelist"
    const val PREF_BLACKLIST = "app_blacklist"
    const val PREF_AUTO_DETECT = "auto_detect"
    const val PREF_DELAY_SECS = "delay_secs"
    const val PREF_DELAY_SECS_DEFAULT = 240
    const val PREF_DELAY_PER = "delay_per"
    const val PREF_DELAY_PER_DEFAULT = 50
    const val PREF_ALLOWED_ARTISTS = "allowed_artists"

    const val PREF_ACTIVITY_FIRST_RUN = "first_run"
    const val PREF_ACTIVITY_GRAPH_DETAILS = "show_graph_details"
    const val PREF_ACTIVITY_LAST_TAB = "last_tab"
    const val PREF_ACTIVITY_NUM_SCROBBLES = "num_scrobbles_cached"
    const val PREF_ACTIVITY_PROFILE_PIC = "profile_cached"
    const val PREF_ACTIVITY_SHARE_SIG = "share_sig"
    const val ACTIVITY_PREFS = "activity_preferences"

    const val PREF_LASTFM_SESS_KEY = "lastfm_sesskey"
    const val PREF_LASTFM_USERNAME = "lastfm_username"
    const val PREF_LIBREFM_USERNAME = "librefm_username"
    const val PREF_LIBREFM_SESS_KEY = "librefm_sesskey"
    const val PREF_LISTENBRAINZ_USERNAME = "listenbrainz_username"
    const val PREF_LISTENBRAINZ_TOKEN = "listenbrainz_token"
    const val PREF_LB_CUSTOM_USERNAME = "lb_username"
    const val PREF_LB_CUSTOM_ROOT = "lb_root"
    const val PREF_LB_CUSTOM_TOKEN = "lb_token"
    const val PREF_GNUFM_USERNAME = "gnufm_username"
    const val PREF_GNUFM_ROOT = "gnufm_root"
    const val PREF_GNUFM_SESS_KEY = "gnufm_sesskey"
    const val PREF_LASTFM_DISABLE = "lastfm_disable"
    const val PREF_NOW_PLAYING = "now_playing"
    const val PREF_ACR_HOST = "acr_host"
    const val PREF_ACR_KEY = "acr_key"
    const val PREF_ACR_SECRET = "acr_secret"
    const val PREF_PIXEL_NP = "pixel_np"
    const val PREF_IMPORT = "import"
    const val PREF_EXPORT = "export"
    const val PREF_INTENTS = "intents"
    val SERVICE_BIT_POS = mapOf(
            R.string.lastfm to 0,
            R.string.librefm to 1,
            R.string.gnufm to 2,
            R.string.listenbrainz to 3,
            R.string.custom_listenbrainz to 4
    )

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"

    const val RECENTS_REFRESH_INTERVAL: Long = 15 * 1000
    const val PIXEL_NP_INTERVAL: Long = 5 * 60 * 1000
    const val CONNECT_TIMEOUT = 20 * 1000
    const val READ_TIMEOUT = 20 * 1000
    const val CANCELLABLE_MSG = 9
    const val OFFLINE_SCROBBLE_JOB_DELAY: Long = 20 * 1000
    const val KEEPALIVE_JOB_INTERVAL: Long = 30 * 60 * 1000
    const val META_WAIT: Long = 500
    const val START_POS_LIMIT: Long = 1500
    const val DEBOUNCE_TIME = 100
    const val MAX_APPS = 30
    const val MIN_LISTENER_COUNT = 7
    const val REQUEST_CODE_EXPORT = 10
    const val REQUEST_CODE_IMPORT = 11
    const val REQUEST_CODE_MIC_PERM = 20
    const val EDITS_NOPE = 0
    const val EDITS_REPLACE_ALL = 1
    const val EDITS_REPLACE_EXISTING = 2
    const val EDITS_KEEP_EXISTING = 3

    const val LASTFM_API_ROOT = "https://ws.audioscrobbler.com/2.0/"
    const val LIBREFM_API_ROOT = "https://libre.fm/2.0/"
    const val LISTENBRAINZ_API_ROOT = "https://api.listenbrainz.org/"

    val NLS_SETTINGS = if (Build.VERSION.SDK_INT > 21)
        ACTION_NOTIFICATION_LISTENER_SETTINGS
    else "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
    const val LASTFM_AUTH_CB_URL = "https://www.last.fm/api/auth?api_key=$LAST_KEY&cb=pscrobble://auth/lastfm"
    const val LIBREFM_AUTH_CB_URL = "https://www.libre.fm/api/auth?api_key=$LIBREFM_KEY&cb=pscrobble://auth/librefm"

    private var timeIt: Long = 0

    val IGNORE_ARTIST_META = arrayOf(
            "com.google.android.youtube",
            "com.vanced.android.youtube",
            "com.bvanced.android.youtube",
            "com.pvanced.android.youtube",
            "com.google.android.ogyoutube",
            "com.google.android.apps.youtube.mango",
            "com.google.android.youtube.tv",
            "org.schabi.newpipe",
            "com.kapp.youtube.final",
            "jp.nicovideo.nicobox"
    )

    const val CHANNEL_PIXEL_NP = "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
    const val PACKAGE_PIXEL_NP = "com.google.intelligence.sense"
    const val PACKAGE_PIXEL_NP_R = "com.google.android.as"
    const val PACKAGE_N7PLAYER = "com.n7mobile.nplayer"
    const val PACKAGE_XIAMI = "fm.xiami.main"
    const val PACKAGE_PANDORA = "com.pandora.android"
    const val PACKAGE_BLACKPLAYER_PREFIX = "com.kodarkooperativet.blackplayer"
    const val PACKAGE_SONOS_PREFIX = "com.sonos.acr"

    private val seperators = arrayOf(// in priority order
            "—", " – ", " –", "– ", " _ ", " - ", " | ", " -", "- ", "「", "『", /*"ー", */" • ",

            "【", "〖", "〔",
            "】", "〗", "』", "」", "〕",
            // ":",
            " \"", " / ", "／")
    private val unwantedSeperators = arrayOf("『", "』", "「", "」", "\"", "'", "【", "】", "〖", "〗", "〔", "〕", "\\|")

    private val metaSpam = arrayOf("downloaded", ".com", ".co.", "www.")
    private val metaUnknown = arrayOf("unknown", "[unknown]", "unknown album", "[unknown album]", "unknown artist", "[unknown artist]")

    val STARTUPMGR_INTENTS = arrayOf( //pkg, class
            "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity",
            "com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity",
            "com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
            "com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
            "com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
            "com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity",
            "com.samsung.android.lool", "com.samsung.android.sm.battery.ui.setting.SleepingAppsActivity"
    )

    val persistentNoti by lazy {
        Build.MANUFACTURER.toLowerCase(Locale.ENGLISH) in arrayOf("huawei", "samsung")
    }

    fun log(s: String) {
        Log.i(TAG, s)
    }

    fun timeIt(s: String) {
        val now = System.currentTimeMillis()
        Log.w(TAG + "_time: ", "[" + (now - timeIt) + "] " + s)
        timeIt = now
    }

    fun toast(c: Context?, s: String, len: Int = Toast.LENGTH_SHORT) {
        c ?: return
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

    fun exec(command: String): String {
        var resp = ""
        try {
            val process = Runtime.getRuntime().exec(command)
            resp = process.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
        }
        return resp
    }

    fun sanitizeTitle(titleContentOriginal: String): Array<String> {
        //New detection of trackinformation
        //remove (*) and/or [*] to remove unimportant data
        val titleContent = titleContentOriginal.replace(" *\\([^)]*?\\) *".toRegex(), " ")
                .replace(" *\\[[^)]*?] *".toRegex(), " ")

                //remove HD info
                .replace("\\W* HD|HQ|4K|MV|M/V|Official Music Video|Music Video|Lyric Video|Official Audio( \\W*)?"
                        .toRegex(RegexOption.IGNORE_CASE)
                        , " ")

//        get remix info
        val remixInfo = "\\([^)]*(?:remix|mix|cover|version|edit|booty?leg)\\)".toRegex(RegexOption.IGNORE_CASE).find(titleContentOriginal)

        var musicInfo: Array<String>? = null
        for (s in seperators) {
            //parsing artist - title
            musicInfo = titleContent.split(s).filter { it.isNotBlank() }.toTypedArray()
            if (s == "／")
                musicInfo.reverse()

//            println("musicInfo= "+musicInfo[0] + (if (musicInfo.size >1) "," + musicInfo[1] else "") + "|" + musicInfo.size)
            //got artist, parsing title - audio (cover) [blah]
            if (musicInfo.size > 1) {
                for (j in 0 until seperators.size - 2) {
                    val splits = musicInfo[1].split(seperators[j]).filter { it.isNotEmpty() }
//                    println("splits= $splits |" + splits.size + "|" + seperators[j])
                    if (splits.size > 1) {
                        musicInfo[1] = splits[0]
                        break
                    }
//                    else if (splits.size == 1)
//                        break
                }
                break
            }
        }


        if (musicInfo == null || musicInfo.size < 2) {
            return arrayOf(titleContent, "")
        }

        //remove ", ', 」, 』 from musicInfo
        val allUnwantedSeperators = "(" + unwantedSeperators.joinToString("|") + ")"
        for (i in musicInfo.indices) {
            musicInfo[i] = musicInfo[i].replace("^\\s*$allUnwantedSeperators|$allUnwantedSeperators\\s*$".toRegex(), " ")
        }

        musicInfo[1] = musicInfo[1].replace("\\.(avi|wmv|mp4|mpeg4|mov|3gpp|flv|webm)$".toRegex(RegexOption.IGNORE_CASE), " ")
                .replace("Full Album".toRegex(RegexOption.IGNORE_CASE), "")
        //Full Album Video
//        println("mi1="+musicInfo[1])
        //move feat. info from artist to
        musicInfo[0] = musicInfo[0].replace(" (ft\\.?) ".toRegex(), " feat. ")
        if (musicInfo[0].contains(" feat.* .*".toRegex(RegexOption.IGNORE_CASE))) {
            val m = " feat.* .*".toRegex(RegexOption.IGNORE_CASE).find(musicInfo[0])
            musicInfo[1] = musicInfo[1].trim() + " " + (m!!.groups[0]?.value ?: "").trim()
            musicInfo[0] = musicInfo[0].replace(" feat.* .*".toRegex(RegexOption.IGNORE_CASE), "")
        }
//        println("mi2="+musicInfo[1])
        //add remix info
        if (remixInfo?.groups?.isNotEmpty() == true) {
            musicInfo[1] = musicInfo[1].trim() + " " + remixInfo.groups[0]?.value
        }

        //delete spaces
        musicInfo[0] = musicInfo[0].trim()
        musicInfo[1] = musicInfo[1].trim()

        return musicInfo
    }

    fun sanitizeAlbum(albumOrig: String): String {
        val albumLower = albumOrig.toLowerCase(Locale.ENGLISH)
        if (metaSpam.any { albumLower.contains(it) } || metaUnknown.any { albumLower == it })
            return ""

        return albumOrig
    }

    fun sanitizeArtist(artistOrig: String): String {
        val splits = artistOrig.split("; ").filter { !it.isBlank() }
        if (splits.isEmpty())
            return ""
        return splits[0]
    }

    fun pixelNPExtractMeta(titleStr: String, formatStr: String):Array<String>? {
        val tpos = formatStr.indexOf("%1\$s")
        val apos = formatStr.indexOf("%2\$s")
        val regex = formatStr.replace("(", "\\(")
                .replace(")", "\\)")
                .replace("%1\$s", "(.*)")
                .replace("%2\$s", "(.*)")
        return try {
            val m = regex.toRegex().find(titleStr)!!
            val g = m.groupValues
            if (g.size != 3)
                throw Exception("group size != 3")
            if (tpos > apos)
                arrayOf(g[1],g[2])
            else
                arrayOf(g[2],g[1])

        } catch (e:Exception) {
            print("err in $titleStr $formatStr")
            null
        }
    }

    fun setTitle(activity: Activity?, strId: Int) {
        activity!!
        val ctl = activity.findViewById<CollapsingToolbarLayout>(R.id.ctl) ?: return
        if (strId == 0) { // = clear title
            ctl.findViewById<Toolbar>(R.id.toolbar).title = null
            activity.window.navigationBarColor = lastColorMutedBlack
        } else {
            ctl.findViewById<Toolbar>(R.id.toolbar).title = activity.getString(strId)
            activity.app_bar.setExpanded(false, true)
            ctl.setContentScrimColor(ContextCompat.getColor(activity, R.color.colorPrimary))
            ctl.setCollapsedTitleTextColor(Color.WHITE)

            val navbarBgAnimator = ValueAnimator.ofArgb(activity.window.navigationBarColor, 0)
            navbarBgAnimator.duration *= 2
            navbarBgAnimator.addUpdateListener {
                activity.window.navigationBarColor = it.animatedValue as Int
            }
            navbarBgAnimator.start()
            for (i in 0..ctl.toolbar.childCount) {
                val child = ctl.toolbar.getChildAt(i)
                if (child is ImageButton) {
                    (child.drawable as ShadowDrawerArrowDrawable).setColors(Color.WHITE, Color.TRANSPARENT)
                    break
                }
            }
        }
    }

    fun setAppBarHeight(activity: Activity, additionalHeight: Int = 0) {
        val sHeightPx: Int
        val dm = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(dm)
        sHeightPx = dm.heightPixels

        val abHeightPx = activity.resources.getDimension(R.dimen.app_bar_height)
        val targetAbHeight: Int
        val lp = activity.app_bar.layoutParams
        val margin = dp2px(65, activity)

        targetAbHeight = if (sHeightPx < abHeightPx + additionalHeight + margin)
            ((sHeightPx - additionalHeight) * 0.6).toInt()
        else
            activity.resources.getDimensionPixelSize(R.dimen.app_bar_height)
        if (targetAbHeight != lp.height) {
            activity.findViewById<StatefulAppBar>(R.id.app_bar).isCollapsed
            activity.findViewById<StatefulAppBar>(R.id.app_bar).isExpanded
            if (!activity.app_bar.isExpanded) {
                lp.height = targetAbHeight
//                activity.app_bar.setExpanded(false, false)
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
        val arrayId = c.resources.getIdentifier("mdcolor_$typeColor", "array", c.packageName)

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

    fun dp2px(dp: Int, c: Context): Int =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), c.resources.displayMetrics).toInt()

    fun sp2px(sp: Int, c: Context): Int =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), c.resources.displayMetrics).toInt()

    fun humanReadableNum(f: Float): String {
        val n = f.toLong()
        val k = 1000
        if (n < k) return DecimalFormat("#").format(n) //localise
        val exp = (ln(n.toDouble()) / ln(k.toDouble())).toInt()
        val unit = "KMB"[exp - 1] //kilo, million, bilion
        val dec = n / k.toDouble().pow(exp.toDouble())

        val decimal = DecimalFormat("#.#").format(dec)
        return decimal + unit
    }

    fun myRelativeTime(context: Context, date: Date?): CharSequence =
            myRelativeTime(context, date?.time ?: 0)

    fun myRelativeTime(context: Context, millis: Long, showTime: Boolean = false): CharSequence {
        if (millis == 0L)
            return context.getString(R.string.time_now)
        val diff = System.currentTimeMillis()-millis
        if(diff <=60*1000)
            return context.getString(R.string.time_now_long)
        var relDate = DateUtils.getRelativeTimeSpanString(
                    millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL) as String

        if(showTime && diff>24*3600*1000){
            val df = DateFormat.getTimeInstance(DateFormat.SHORT)
            relDate = relDate + ", "+ df.format(Date(millis))
        }
        return relDate
    }

    fun drawableToBitmap(drawable: Drawable, width: Int, height: Int, forceDraw: Boolean = false): Bitmap {
        if (!forceDraw && drawable is BitmapDrawable && !drawable.bitmap.isRecycled)
            return drawable.bitmap

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable is PictureDrawable)
            canvas.drawPicture(drawable.picture)
        else {
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
        }
        return bitmap
    }

    fun isMiui(context: Context?): Boolean {
        return try {
            if (context != null) {
                val pm = context.packageManager
                pm.getPackageInfo("com.miui.securitycenter", PackageManager.GET_META_DATA)
                true
            } else
                false
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getStartupIntent(context: Context): Intent? {
        // https://stackoverflow.com/questions/48166206/how-to-start-power-manager-of-all-android-manufactures-to-enable-background-and/48166241#48166241
        for (i in STARTUPMGR_INTENTS.indices step 2) {
            val intent = Intent().setComponent(ComponentName(STARTUPMGR_INTENTS[i], STARTUPMGR_INTENTS[i+1]))
            if (context.packageManager.resolveActivity(intent,
                            PackageManager.MATCH_DEFAULT_ONLY) != null)
                return intent
        }
        return null
    }

    fun nowPlayingAnim(np: ImageView, isNowPlaying: Boolean) {
        if (isNowPlaying) {
            np.visibility = View.VISIBLE
            var anim = np.drawable
            if (anim !is AnimatedVectorDrawableCompat || anim !is AnimatedVectorDrawable) {
                np.setImageResource(R.drawable.avd_eq)
                anim = np.drawable
            }
            if (anim is AnimatedVectorDrawableCompat? && anim?.isRunning != true) {
                anim?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        if (drawable?.isVisible == true)
                            np.post {
                                (np.drawable as? AnimatedVectorDrawableCompat)?.start()
                            }
                    }
                })
                anim?.start()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && anim is AnimatedVectorDrawable && !anim.isRunning) {
                anim.registerAnimationCallback(object : Animatable2.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        if (drawable?.isVisible == true)
                            (drawable as? AnimatedVectorDrawable)?.start()
                    }
                })
                anim.start()
            }
        } else {
            np.visibility = View.GONE
            np.setImageDrawable(null)
        }
    }

    fun setProgressCircleColor(swl: SwipeRefreshLayout) {
        swl.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryLight)
        swl.setProgressBackgroundColorSchemeResource(R.color.darkBg)
    }

    fun launchSearchIntent(artist: String, track: String, context: Context) {
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
        intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
        intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, track)
        intent.putExtra(SearchManager.QUERY, "$artist - $track")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException){
            toast(context, context.getString(R.string.no_player))
        }
    }

    fun openInBrowser(url: String, context: Context?, source: View? = null, startX: Int = 10, startY: Int = 10) {
        context ?: return
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        var bundle: Bundle? = null
        if (source != null) {
            bundle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityOptions.makeClipRevealAnimation(source, startX, startY, 10, 10)
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

    fun getBrowsers(pm: PackageManager): MutableList<ResolveInfo> {
        val browsersIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        return pm.queryIntentActivities(browsersIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PackageManager.MATCH_ALL
                else
                    0
        )
    }

    fun getBrowsersAsStrings(pm: PackageManager): MutableList<String> {
        val browserPackages = mutableListOf<String>()
        getBrowsers(pm)
                .forEach {
                    browserPackages += it.activityInfo.applicationInfo.packageName
                }
        return browserPackages
    }

    fun initCaller(context: Context): Caller {
        val caller = Caller.getInstance()
        if (caller.userAgent != USER_AGENT) { // static instance not inited
            caller.userAgent = USER_AGENT
            caller.logger.level = Level.WARNING
            val fsCache = FileSystemCache(context.cacheDir)
            caller.cache = fsCache
        }
        return caller
    }

    fun genHashCode(vararg strings: String): Int {
        val prime = 31
        var result = 1
        for (s in strings) {
            result = result * prime + s.hashCode()
        }
        return result
    }
}