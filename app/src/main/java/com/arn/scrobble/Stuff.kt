package com.arn.scrobble

import android.animation.ValueAnimator
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.text.Html
import android.text.Spanned
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.arn.scrobble.ui.ShadowDrawerArrowDrawable
import com.google.android.material.color.MaterialColors
import de.umass.lastfm.Caller
import de.umass.lastfm.cache.FileSystemCache
import java.io.IOException
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import java.util.logging.Level
import kotlin.math.ln
import kotlin.math.pow


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val TAG_HOME_PAGER = "home_pager"
    const val TAG_CHART_PAGER = "chart_pager"
    const val TAG_FIRST_THINGS = "first_things"
    const val TAG_INFO_FROM_WIDGET = "info_widget"
    const val ARG_URL = "url"
    const val ARG_SAVE_COOKIES = "cookies"
    const val ARG_NOPASS = "nopass"
    const val ARG_USERNAME = "username"
    const val ARG_REGISTERED_TIME = "registered"
    const val ARG_TYPE = "type"
    const val ARG_TAG = "tag"
    const val TYPE_ARTISTS = 1
    const val TYPE_ALBUMS = 2
    const val TYPE_TRACKS = 3
    const val TYPE_LOVES = 4
    const val TYPE_SC = 5
    const val NP_ID = -5
    const val LIBREFM_KEY = "panoScrobbler"
    const val LAST_KEY = Tokens.LAST_KEY
    const val LAST_SECRET = Tokens.LAST_SECRET
    const val TAG = "scrobbler"
    const val DL_SETTINGS = 31
    const val DL_APP_LIST = 32
    const val DL_RECENTS = 33
    const val DL_MIC = 34
    const val DL_SEARCH = 35
    const val DL_CHARTS = 36
    const val DIRECT_OPEN_KEY = "directopen"
    const val FRIENDS_RECENTS_DELAY: Long = 800

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
    const val PREF_LOCKSCREEN_NOTI = "lockscreen_noti"
    const val PREF_IMPORT = "import"
    const val PREF_EXPORT = "export"
    const val PREF_INTENTS = "intents"
    const val PREF_FETCH_AA = "fetch_album_artist"
    const val PREF_DIGEST_WEEKLY = "digest_weekly"
    const val PREF_DIGEST_MONTHLY = "digest_monthly"
    const val PREF_SHOW_RECENTS_ALBUM = "show_album"
    const val PREF_THEME_PRIMARY = "theme_primary"
    const val PREF_THEME_SECONDARY = "theme_secondary"
    const val PREF_THEME_BACKGROUND = "theme_background"
    const val PREF_THEME_RANDOM = "theme_random"
    const val PREF_THEME_SAME_TONE = "theme_same_tone"
    const val PREF_PRO_STATUS = "pro_status"
    const val PREF_PRO_SKU_JSON = "pro_sku_json"

    const val PREF_ACTIVITY_FIRST_RUN = "first_run"
    const val PREF_ACTIVITY_GRAPH_DETAILS = "show_graph_details"
    const val PREF_ACTIVITY_LAST_TAB = "last_tab"
    const val PREF_ACTIVITY_LAST_CHARTS_PERIOD = "last_charts_period"
    const val PREF_ACTIVITY_LAST_CHARTS_WEEK_TO = "last_charts_week_to"
    const val PREF_ACTIVITY_LAST_CHARTS_WEEK_FROM = "last_charts_week_from"
    const val PREF_ACTIVITY_TODAY_SCROBBLES = "today_scrobbles_cached"
    const val PREF_ACTIVITY_TOTAL_SCROBBLES = "total_scrobbles_cached"
    const val PREF_ACTIVITY_SCROBBLING_SINCE = "scrobbling_since"
    const val PREF_ACTIVITY_LAST_RANDOM_TYPE = "random_type"
    const val PREF_ACTIVITY_PROFILE_PIC = "profile_cached"
    const val PREF_ACTIVITY_SEARCH_HISTORY = "search_history"
    const val PREF_ACTIVITY_TAG_HISTORY = "tag_history"
    const val PREF_ACTIVITY_SCRUB_LEARNT = "scrub_learnt"
    const val PREF_ACTIVITY_LONG_PRESS_LEARNT = "long_press_learnt"
    const val ACTIVITY_PREFS = "activity_preferences"

    const val WIDGET_PREFS = "widget_preferences"
    const val PREF_WIDGET_TAB = "tab"
    const val PREF_WIDGET_BG_ALPHA = "bg_alpha"
    const val PREF_WIDGET_DARK = "dark"
    const val PREF_WIDGET_PERIOD = "period"
    const val PREF_WIDGET_SHADOW = "shadow"
    const val PREF_WIDGET_LAST_UPDATED = "last_updated"

    const val EXTRA_PINNED = "pinned"

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
    const val OFFLINE_SCROBBLE_JOB_DELAY: Long = 20 * 1000
    const val KEEPALIVE_JOB_INTERVAL: Long = 30 * 60 * 1000
    const val LASTFM_MAX_PAST_SCROBBLE: Long = 14 * 24 * 60 * 60 * 1000
    const val META_WAIT: Long = 500
    const val START_POS_LIMIT: Long = 1500
    const val PENDING_PURCHASE_NOTIFY_THRESHOLD: Long = 15 * 1000
    const val MIN_LISTENER_COUNT = 5
    const val REQUEST_CODE_EXPORT = 10
    const val REQUEST_CODE_IMPORT = 11
    const val REQUEST_CODE_MIC_PERM = 20
    const val EDITS_NOPE = 0
    const val EDITS_REPLACE_ALL = 1
    const val EDITS_REPLACE_EXISTING = 2
    const val EDITS_KEEP_EXISTING = 3

    const val LOADING_ALPHA = 0.7f

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

    const val MANUFACTURER_HUAWEI = "huawei"
    const val MANUFACTURER_SAMSUNG = "samsung"

    const val CHANNEL_PIXEL_NP = "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
    const val PACKAGE_PIXEL_NP = "com.google.intelligence.sense"
    const val PACKAGE_PIXEL_NP_R = "com.google.android.as"
    const val PACKAGE_SHAZAM = "com.shazam.android"
    const val CHANNEL_SHAZAM = "auto_shazam_v2"
    const val PACKAGE_XIAMI = "fm.xiami.main"
    const val PACKAGE_PANDORA = "com.pandora.android"
    const val PACKAGE_BLACKPLAYER = "com.musicplayer.blackplayerfree"
    const val PACKAGE_BLACKPLAYEREX = "com.kodarkooperativet.blackplayerex"
    const val PACKAGE_SONOS = "com.sonos.acr"
    const val PACKAGE_SONOS2 = "com.sonos.acr2"
    const val PACKAGE_DIFM = "com.audioaddict.di"
    const val PACKAGE_PODCAST_ADDICT = "com.bambuna.podcastaddict"
    const val PACKAGE_HUAWEI_MUSIC = "com.android.mediacenter"
    const val PACKAGE_NETEASE_MUSIC = "com.netease.cloudmusic"

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
        Build.MANUFACTURER.toLowerCase(Locale.ENGLISH) in arrayOf(MANUFACTURER_HUAWEI, MANUFACTURER_SAMSUNG)
    }

    val updateCurrentOrImmutable: Int
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.FLAG_UPDATE_CURRENT
        }

    val updateCurrentOrMutable: Int
        get() {
            //TODO: uncomment when targeting S
//            if (Build.VERSION.SDK_INT >= 31)
//                return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            return PendingIntent.FLAG_UPDATE_CURRENT
        }

    val Int.dp
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val Int.sp
        get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(),
            Resources.getSystem().displayMetrics).toInt()

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

    fun Bundle?.dump(): String {
        this ?: return "null"
        var s = ""
        for (key in keySet().sortedDescending()) {
            val value = get(key) ?: "null"
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

    fun setTitle(activity: Activity?, @StringRes strId: Int) {
        val title = if (strId == 0)
            null
        else
            activity?.getString(strId)
        setTitle(activity, title)
    }

    fun setTitle(activity: Activity?, str: String?) {
        activity as Main
        if (activity.isDestroyed || activity.isFinishing)
            return
        if (str == null) { // = clear title
            activity.binding.coordinatorMain.toolbar.title = null
//            activity.window.navigationBarColor = lastColorMutedBlack
        } else {
            activity.binding.coordinatorMain.toolbar.title = str
            activity.binding.coordinatorMain.appBar.setExpanded(false, true)

            val navbarBgAnimator = ValueAnimator.ofArgb(activity.window.navigationBarColor, 0)
            navbarBgAnimator.duration *= 2
            navbarBgAnimator.addUpdateListener {
                activity.window.navigationBarColor = it.animatedValue as Int
            }
            navbarBgAnimator.start()
        }
        activity.window.navigationBarColor = MaterialColors.getColor(activity, android.R.attr.colorBackground, null)
        activity.binding.coordinatorMain.ctl.setContentScrimColor(MaterialColors.getColor(activity, android.R.attr.colorBackground, null))
        activity.binding.coordinatorMain.toolbar.setArrowColors(MaterialColors.getColor(activity, R.attr.colorPrimary, null), Color.TRANSPARENT)
    }

    fun Toolbar.setArrowColors(fg: Int, bg: Int) {
        for (i in 0..childCount) {
            val child = getChildAt(i)
            if (child is ImageButton) {
                (child.drawable as ShadowDrawerArrowDrawable).
                setColors(fg, bg)
                break
            }
        }
    }

    fun setAppBarHeight(activity: Activity, additionalHeight: Int = 0) {
        activity as Main
        val sHeightPx: Int
        val dm = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(dm)
        sHeightPx = dm.heightPixels

        val abHeightPx = activity.resources.getDimension(R.dimen.app_bar_height)
        val targetAbHeight: Int
        val lp = activity.binding.coordinatorMain.appBar.layoutParams
        val margin = 65.dp

        targetAbHeight = if (sHeightPx < abHeightPx + additionalHeight + margin)
            ((sHeightPx - additionalHeight) * 0.6).toInt()
        else
            activity.resources.getDimensionPixelSize(R.dimen.app_bar_height)
        if (targetAbHeight != lp.height) {
            if (!activity.binding.coordinatorMain.appBar.isExpanded) {
                lp.height = targetAbHeight
//                activity.app_bar.setExpanded(false, false)
            } else {
                val start = lp.height
                val anim = ValueAnimator.ofInt(start, targetAbHeight)
                anim.addUpdateListener { valueAnimator ->
                    lp.height = valueAnimator.animatedValue as Int
                    activity.binding.coordinatorMain.appBar.layoutParams = lp
                }
                anim.interpolator = DecelerateInterpolator()
                anim.duration = 300
                anim.start()

            }
        }
    }

    fun getColoredTitle(context: Context, title: String): Spanned {
        val colorAccent = MaterialColors.getColor(context, R.attr.colorPrimary, null)
        val hex = "#" + Integer.toHexString(colorAccent).substring(2)
        return Html.fromHtml("<font color=\"$hex\">$title</font>")
    }

    fun getMatColor(c: Context, hash: Long, typeColor: String = "500"): Int {
        var hash = hash
        var returnColor = Color.BLACK
        val arrayId = c.resources.getIdentifier("mdcolor_$typeColor", "array", c.packageName)

        if (arrayId != 0) {
            val colors = c.resources.obtainTypedArray(arrayId)
            if (hash < 0)
                hash = -hash
            val index = if (hash.toInt() == 0)
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

    fun humanReadableNum(n: Int): String {
        val k = 1000
        if (n < k) return DecimalFormat("#").format(n) //localise
        val exp = (ln(n.toDouble()) / ln(k.toDouble())).toInt()
        val unit = "KMB"[exp - 1] //kilo, million, bilion
        val dec = n / k.toDouble().pow(exp.toDouble())

        val decimal = DecimalFormat(if (dec >= 100) "#" else "#.#").format(dec)
        return decimal + unit
    }

    fun humanReadableDuration(secs: Int): String {
        val s = secs % 60
        val m = (secs / 60) % 60
        val h = secs / 3600
        val str = StringBuilder()
        val nf = NumberFormat.getInstance()
        nf.minimumIntegerDigits = 2
        if (h > 0)
            str.append(nf.format(h))
                    .append(':')
        str.append(nf.format(m))
                .append(':')
                .append(nf.format(s))
        return str.toString()
    }

    fun myRelativeTime(context: Context, date: Date?) =
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

    fun SwipeRefreshLayout.setProgressCircleColors() {
        setColorSchemeColors(
                MaterialColors.getColor(this, R.attr.colorPrimary),
                MaterialColors.getColor(this, R.attr.colorSecondary)
        )
        setProgressBackgroundColorSchemeColor(MaterialColors.getColor(this, android.R.attr.colorBackground))
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

    fun getBrowsers(pm: PackageManager): List<ResolveInfo> {
        val browsersIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        return pm.queryIntentActivities(browsersIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PackageManager.MATCH_ALL
                else
                    0
        ).filter { it.activityInfo.applicationInfo.packageName != PACKAGE_NETEASE_MUSIC }
    }

    fun getBrowsersAsStrings(pm: PackageManager): MutableList<String> {
        val browserPackages = mutableListOf<String>()
        getBrowsers(pm)
                .forEach {
                    browserPackages += it.activityInfo.applicationInfo.packageName
                }
        return browserPackages
    }

    @Synchronized
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

    fun genHashCode(vararg objects: Any): Int {
        val prime = 31
        var result = 1
        for (o in objects) {
            result = result * prime + o.hashCode()
        }
        return result
    }

    fun Calendar.setMidnight() {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }

    fun scheduleDigests(context: Context) {
        val weeklyIntent = PendingIntent.getBroadcast(context, 20,
                Intent(NLService.iDIGEST_WEEKLY), updateCurrentOrImmutable)

        val monthlyIntent = PendingIntent.getBroadcast(context, 21,
                Intent(NLService.iDIGEST_MONTHLY), updateCurrentOrImmutable)

        val cal = Calendar.getInstance()
        cal.setMidnight()

        cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        val nextWeek = cal.timeInMillis

        cal.timeInMillis = System.currentTimeMillis()
        cal.setMidnight()

        cal[Calendar.DAY_OF_MONTH] = 1
        cal.add(Calendar.MONTH, 1)
        val nextMonth = cal.timeInMillis

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, nextWeek, weeklyIntent)
        alarmManager.set(AlarmManager.RTC, nextMonth, monthlyIntent)

        /*
        if (BuildConfig.DEBUG) {
            val dailyIntent = PendingIntent.getBroadcast(context, 22,
                    Intent(NLService.iDIGEST_WEEKLY), updateCurrentOrImmutable)

            cal.timeInMillis = System.currentTimeMillis()
            cal.setMidnight()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val nextDay = cal.timeInMillis
            alarmManager.set(AlarmManager.RTC, nextDay, dailyIntent)
        }
        */
    }

    fun getWidgetPrefName(name: String, appWidgetId: Int) = "${name}_$appWidgetId"

    fun <K, V> Map<K, V>.getOrDefaultKey(key: K, defaultKey: K) = this[key] ?: this[defaultKey]!!
}