package com.arn.scrobble

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.*
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.hardware.input.InputManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.text.Html
import android.text.Spanned
import android.text.format.DateUtils
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.InputDevice
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import coil.request.SuccessResult
import coil.result
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.ShadowDrawerArrowDrawable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import de.umass.lastfm.*
import de.umass.lastfm.scrobble.ScrobbleData
import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.IOException
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.math.*


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val SCROBBLER_PROCESS_NAME = "bgScrobbler"
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
    const val ARG_SHOW_DIALOG = "dialog"
    const val ARG_COUNT = "count"
    const val ARG_DATA = "data"
    const val ARG_PKG = "pkg"
    const val ARG_ACTION = "action"
    const val ARG_TLS_NO_VERIFY = "tls_no_verify"
    const val ARG_ALLOWED_PACKAGES = MainPrefs.PREF_ALLOWED_PACKAGES
    const val TYPE_ARTISTS = 1
    const val TYPE_ALBUMS = 2
    const val TYPE_TRACKS = 3
    const val TYPE_LOVES = 4
    const val TYPE_SC = 5
    const val TYPE_TAG_CLOUD = 6
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
    const val DL_PRO = 37
    const val DIRECT_OPEN_KEY = "directopen"
    const val FRIENDS_RECENTS_DELAY = 800L
    const val CROSSFADE_DURATION = 200
    const val MAX_PATTERNS = 30
    const val MAX_PINNED_FRIENDS = 10
    const val MAX_INDEXED_ITEMS = 4000
    const val MAX_CHARTS_NUM_COLUMNS = 6
    const val MIN_CHARTS_NUM_COLUMNS = 1
    const val PINNED_FRIENDS_CACHE_TIME = 60L * 60 * 24 * 1 * 1000
    const val MIN_ITEMS_TO_SHOW_SEARCH = 7

    const val EXTRA_PINNED = "pinned"
    val DEMO_MODE = BuildConfig.DEBUG && false

    val SERVICE_BIT_POS = mapOf(
        R.string.lastfm to 0,
        R.string.librefm to 1,
        R.string.gnufm to 2,
        R.string.listenbrainz to 3,
        R.string.custom_listenbrainz to 4
    )

    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36"

    const val RECENTS_REFRESH_INTERVAL = 15 * 1000L
    const val NOTI_SCROBBLE_INTERVAL = 5 * 60 * 1000L
    const val CONNECT_TIMEOUT = 20 * 1000L
    const val READ_TIMEOUT = 20 * 1000L
    const val OFFLINE_SCROBBLE_JOB_DELAY = 20 * 1000L
    const val LASTFM_MAX_PAST_SCROBBLE = 14 * 24 * 60 * 60 * 1000L
    const val CRASH_REPORT_INTERVAL = 120 * 60 * 1000L
    const val CHARTS_WIDGET_REFRESH_INTERVAL = 30 * 60 * 1000L
    const val TRACK_INFO_VALIDITY = 5 * 1000L
    const val TRACK_INFO_WINDOW = 60 * 1000L
    const val TRACK_INFO_REQUESTS = 2
    const val META_WAIT = 400L
    const val START_POS_LIMIT = 1500L
    const val PENDING_PURCHASE_NOTIFY_THRESHOLD = 15 * 1000L
    const val MIN_LISTENER_COUNT = 5
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
    const val LASTFM_AUTH_CB_URL =
        "https://www.last.fm/api/auth?api_key=$LAST_KEY&cb=pscrobble://auth/lastfm"
    const val LIBREFM_AUTH_CB_URL =
        "https://www.libre.fm/api/auth?api_key=$LIBREFM_KEY&cb=pscrobble://auth/librefm"

    private var timeIt = 0L

    const val MANUFACTURER_HUAWEI = "huawei"
    const val MANUFACTURER_XIAOMI = "xiaomi"
    const val MANUFACTURER_SAMSUNG = "samsung"
    const val MANUFACTURER_GOOGLE = "google"

    const val CHANNEL_PIXEL_NP =
        "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
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
    const val PACKAGE_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"
    const val PACKAGE_SPOTIFY = "com.spotify.music"
    const val PACKAGE_YOUTUBE_TV = "com.google.android.youtube.tv"
    const val PACKAGE_YMUSIC = "com.kapp.youtube.final"
    const val PACKAGE_SOUNDCLOUD = "com.soundcloud.android"

    val MARKET_URL = "market://details?id=" + BuildConfig.APPLICATION_ID

    val IGNORE_ARTIST_META = setOf(
        "com.google.android.youtube",
        "com.vanced.android.youtube",
        "com.google.android.ogyoutube",
        "com.google.android.apps.youtube.mango",
        PACKAGE_YOUTUBE_TV,
        "com.google.android.youtube.tvkids",
        "com.liskovsoft.smarttubetv.beta",
        "com.liskovsoft.smarttubetv",
        "org.schabi.newpipe",
        PACKAGE_YMUSIC,
        "jp.nicovideo.nicobox",
        PACKAGE_SOUNDCLOUD,

        // radios
        "tunein.player",
        "com.thehouseofcode.radio_nowy_swiat",
    )

    val IGNORE_ARTIST_META_WITH_FALLBACK = setOf(
        PACKAGE_SOUNDCLOUD,
    )

    val needSyntheticStates = arrayOf(
        PACKAGE_BLACKPLAYER,
        PACKAGE_BLACKPLAYEREX,
        PACKAGE_YOUTUBE_MUSIC,
    )

    val STARTUPMGR_INTENTS = listOf(
        //pkg, class
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
        "com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
        "com.asus.mobilemanager" to "com.asus.mobilemanager.MainActivity",
        "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.setting.SleepingAppsActivity",
        "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.setting.AppPowerManagementActivity",
    )

    private var _hasMouse: Boolean? = null

    val forcePersistentNoti by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                Build.MANUFACTURER.lowercase(Locale.ENGLISH) in arrayOf(
            MANUFACTURER_HUAWEI,
            MANUFACTURER_XIAOMI,
            MANUFACTURER_SAMSUNG,
        )
    }

    val updateCurrentOrImmutable: Int
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.FLAG_UPDATE_CURRENT
        }

    val updateCurrentOrMutable: Int
        get() {
            if (Build.VERSION.SDK_INT >= 31)
                return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            return PendingIntent.FLAG_UPDATE_CURRENT
        }

    val Int.dp
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    val Int.sp
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()

    val isWindows11 = Build.BOARD == "windows"

    val countryCodesMap by lazy {
        val countries = hashMapOf<String, String>()
        Locale.getISOCountries().forEach { iso ->
            val l = Locale("en", iso)
            countries[l.getDisplayCountry(l)] = iso
        }
        countries
    }

    fun log(s: String) {
        Timber.tag(TAG).i(s)
    }

    fun timeIt(s: String) {
        val now = System.currentTimeMillis()
        Timber.tag(TAG + "_time").d("[${now - timeIt}] $s")
        timeIt = now
    }

    fun toast(c: Context?, s: String, len: Int = Toast.LENGTH_SHORT) {
        c ?: return
        try {
            Toast.makeText(c, s, len).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun Bundle?.dump(): String {
        this ?: return "null"
        var s = ""
        for (key in keySet().sortedDescending()) {
            val value = get(key) ?: "null"
            s += "$key= $value, "
        }
        return s
    }

    fun exec(command: String): String {
        var resp = ""
        try {
            val process = Runtime.getRuntime().exec(command)
            resp = process.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return resp
    }

    fun setTitle(activity: Activity, @StringRes strId: Int) {
        val title = if (strId == 0)
            null
        else
            activity.getString(strId)
        setTitle(activity, title)
    }

    fun setTitle(activity: Activity, str: String?) {
        activity as MainActivity
        if (activity.isDestroyed || activity.isFinishing)
            return
        if (str == null) { // = clear title
            activity.binding.coordinatorMain.toolbar.title = null
//            activity.window.navigationBarColor = lastColorMutedBlack
        } else {
            activity.binding.coordinatorMain.toolbar.title = str
            activity.binding.coordinatorMain.appBar.setExpanded(expanded = false, animate = true)

            val navbarBgAnimator = ValueAnimator.ofArgb(activity.window.navigationBarColor, 0)
            navbarBgAnimator.duration *= 2
            navbarBgAnimator.addUpdateListener {
                activity.window.navigationBarColor = it.animatedValue as Int
            }
            navbarBgAnimator.start()
        }
        activity.window.navigationBarColor =
            MaterialColors.getColor(activity, android.R.attr.colorBackground, null)
        activity.binding.coordinatorMain.ctl.setContentScrimColor(
            MaterialColors.getColor(
                activity,
                android.R.attr.colorBackground,
                null
            )
        )
        activity.binding.coordinatorMain.toolbar.setArrowColors(
            MaterialColors.getColor(
                activity,
                R.attr.colorPrimary,
                null
            ), Color.TRANSPARENT
        )
    }

    fun Toolbar.setArrowColors(fg: Int, bg: Int) {
        for (i in 0..childCount) {
            val child = getChildAt(i)
            if (child is ImageButton) {
                (child.drawable as ShadowDrawerArrowDrawable).setColors(fg, bg)
                break
            }
        }
    }

    fun BottomSheetDialogFragment.expandIfNeeded() {
        val bottomSheetView =
            dialog!!.window!!.decorView.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (view?.isInTouchMode == false || context!!.hasMouse)
            BottomSheetBehavior.from(bottomSheetView).state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun setCtlHeight(activity: Activity, additionalHeight: Int = 0) {
        activity as MainActivity
        val sHeightPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets =
                windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            val dm = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(dm)
            dm.heightPixels
        }

        val abHeightPx = activity.resources.getDimension(R.dimen.app_bar_height)
        val targetAbHeight: Int
        val lp = activity.binding.coordinatorMain.ctl.layoutParams
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
                    activity.binding.coordinatorMain.ctl.layoutParams = lp
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

    fun getMatColor(
        c: Context,
        seed: Int,
        colorWeight: String? = if (c.resources.getBoolean(R.bool.is_dark))
            "200"
        else
            "500"
    ): Int {
        val colorNamesArray = c.resources.getStringArray(R.array.mdcolor_names)
        val index = abs(seed) % colorNamesArray.size
        val colorName = colorNamesArray[index]

        val colorId =
            c.resources.getIdentifier("mdcolor_${colorName}_$colorWeight", "color", c.packageName)
        return ContextCompat.getColor(c, colorId)
    }

    fun isDark(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    fun humanReadableNum(n: Int): String {
        val k = 1000
        if (n < k) return DecimalFormat("#").format(n) //localise
        val exp = (ln(n.toDouble()) / ln(k.toDouble())).toInt()
        val unit = "KMB"[exp - 1] //kilo, million, billion
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

    fun myRelativeTime(context: Context, date: Date?, longFormat: Boolean = false) =
        myRelativeTime(context, date?.time ?: 0, longFormat)

    fun myRelativeTime(context: Context, millis: Long, longFormat: Boolean = false): CharSequence {
        val diff = System.currentTimeMillis() - millis
        if (millis == 0L || diff <= 60 * 1000)
            return context.getString(R.string.time_just_now)
        return when {
            longFormat && diff >= 24 * 60 * 60 * 1000 ->
                DateUtils.getRelativeTimeSpanString(context, millis, true)

            (longFormat && diff < 24 * 60 * 60 * 1000) ||
                    (!longFormat && diff < 60 * 60 * 1000) ->
                DateUtils.getRelativeTimeSpanString(
                    millis,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                )

            else ->
                DateUtils.getRelativeDateTimeString(
                    context,
                    millis,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                )
        }
    }

    fun getStartupIntent(context: Context): Intent? {
        // https://stackoverflow.com/questions/48166206/how-to-start-power-manager-of-all-android-manufactures-to-enable-background-and/48166241#48166241
        for ((pkg, klass) in STARTUPMGR_INTENTS) {
            val intent = Intent().setComponent(ComponentName(pkg, klass))
            if (context.packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                ) != null
            )
                return intent
        }
        return null
    }

    fun isDkmaNeeded(context: Context): Boolean {
        val packages = STARTUPMGR_INTENTS.map { it.first }.toSet()
        return packages.any {
            try {
                context.packageManager.getApplicationInfo(it, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
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
        setProgressBackgroundColorSchemeColor(
            MaterialColors.getColor(
                this,
                R.attr.colorPrimaryContainer
            )
        )
    }

    fun launchSearchIntent(context: Context, track: Track, pkgName: String?) {
        launchSearchIntent(context, track.name, track.artist, pkgName)
    }

    fun launchSearchIntent(context: Context, artist: String, track: String, pkgName: String?) {
        val prefs = MainPrefs(context)

        if (BuildConfig.DEBUG && isWindows11 && prefs.songSearchUrl.isNotEmpty()) { // open song urls in windows browser for me
            val searchUrl = prefs.songSearchUrl
                .replace("\$artist", artist)
                .replace("\$title", track)
            openInBrowser(context, searchUrl)
            return
        }

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
            putExtra(MediaStore.EXTRA_MEDIA_TITLE, track)
            putExtra(SearchManager.QUERY, "$artist $track")
            if (pkgName != null && prefs.proStatus && prefs.showScrobbleSources && prefs.searchInSource)
                `package` = pkgName
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (pkgName != null) {
                try {
                    intent.`package` = null
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    toast(context, context.getString(R.string.no_player))
                }
            } else
                toast(context, context.getString(R.string.no_player))
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // prevent infinite loop
            if (MainPrefs(context).lastfmLinksEnabled) {
                browserIntent.`package` = getDefaultBrowserPackage(context.packageManager)
            }

            context.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            toast(context, context.getString(R.string.no_browser))
        }
    }

    private fun getDefaultBrowserPackage(packageManager: PackageManager): String? {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"))
        return try {
            val pkgName = packageManager.resolveActivity(
                browserIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )!!
                .activityInfo.packageName

            // returns "android" if no default browser is set
            if ("." in pkgName)
                pkgName
            else
                null
        } catch (e: ActivityNotFoundException) {
            null
        }
    }

    fun getBrowsers(pm: PackageManager): List<ResolveInfo> {
        val browsersIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        return pm.queryIntentActivities(
            browsersIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PackageManager.MATCH_ALL
            else
                0
        )
    }

    fun getBrowsersAsStrings(pm: PackageManager) =
        getBrowsers(pm)
            .map { it.activityInfo.applicationInfo.packageName }
            .toSet()

    fun genHashCode(vararg objects: Any): Int {
        val prime = 31
        var result = 1
        for (o in objects) {
            result = result * prime + o.hashCode()
        }
        return result
    }

    fun FragmentManager.dismissAllDialogFragments() {
        for (fragment in fragments) {
            if (fragment is DialogFragment) {
                fragment.dismissAllowingStateLoss()
            }
            fragment.childFragmentManager.dismissAllDialogFragments()
        }
    }

    fun FragmentManager.popBackStackTill(n: Int) {
        while (backStackEntryCount > n) {
            popBackStackImmediate()
        }
    }

    fun NotificationManager.isNotiEnabled(pref: SharedPreferences, channelId: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isWindows11)
            getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
        else
            pref.getBoolean(channelId, true)

    fun capMaxSatLum(rgb: Int, maxSat: Float, maxLum: Float): Int {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.RGBToHSL(
            Color.red(rgb),
            Color.green(rgb),
            Color.blue(rgb),
            hsl
        )
        hsl[1] = min(hsl[1], maxSat)
        hsl[2] = min(hsl[2], maxLum)
        return ColorUtils.HSLToColor(hsl)
    }

    fun capMinSatLum(rgb: Int, minSat: Float, maxSat: Float, minLum: Float): Int {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.RGBToHSL(
            Color.red(rgb),
            Color.green(rgb),
            Color.blue(rgb),
            hsl
        )
        hsl[1] = hsl[1].coerceIn(minSat, maxSat)
        hsl[2] = max(hsl[2], minLum)
        return ColorUtils.HSLToColor(hsl)
    }

    fun timeToUTC(time: Long) = time + TimeZone.getDefault().getOffset(System.currentTimeMillis())

    fun timeToLocal(time: Long) = time - TimeZone.getDefault().getOffset(System.currentTimeMillis())

    fun getCountryFlag(countryName: String): String {
        val isoCode = countryCodesMap[countryName] ?: return ""
        val flagEmoji = StringBuilder()
        isoCode.forEach {
            val codePoint = 127397 + it.code
            flagEmoji.appendCodePoint(codePoint)
        }
        return flagEmoji.toString()
    }

    fun PopupMenu.showWithIcons(iconTintColor: Int? = null) {
        (menu as? MenuBuilder)?.showIcons(iconTintColor)
        show()
    }

    @SuppressLint("RestrictedApi")
    fun MenuBuilder.showIcons(iconTintColor: Int? = null) {
        setOptionalIconsVisible(true)
        visibleItems.forEach { item ->
            val iconMarginPx =
                context.resources.getDimension(R.dimen.popup_menu_icon_padding).toInt()
            if (item.icon != null) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0)
                } else {
                    item.icon =
                        object : InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0) {
                            override fun getIntrinsicWidth() =
                                intrinsicHeight + iconMarginPx + iconMarginPx
                        }
                }

                if (iconTintColor != null)
                    item.icon.setTint(iconTintColor)
            }
        }
    }

    fun MusicEntry.toBundle() = Bundle().also {
        when (this) {
            is Track -> {
                it.putString(NLService.B_ARTIST, artist)
                if (!album.isNullOrEmpty())
                    it.putString(NLService.B_ALBUM, album)
                it.putString(NLService.B_TRACK, name)
            }
            is Album -> {
                it.putString(NLService.B_ARTIST, artist)
                it.putString(NLService.B_ALBUM, name)
            }
            is Artist -> {
                it.putString(NLService.B_ARTIST, name)
            }
        }
    }

    fun <T : Any> List<T>.toBimap(): BiMap<Int, T> {
        val map = mapIndexed { i, it -> i to it }.toMap()
        return HashBiMap.create(map)
    }

    fun <T : Any> BiMap<Int, T>.firstOrNull() = get(0)

    fun <T : Any> BiMap<Int, T>.lastOrNull() = get(size - 1)


    fun RecyclerView.mySmoothScrollToPosition(
        position: Int,
        padding: Int = 40.dp,
        animate: Boolean = true
    ) {

        val smoothScroller by lazy {
            object : LinearSmoothScroller(context) {
                override fun getHorizontalSnapPreference() = SNAP_TO_ANY
                override fun getVerticalSnapPreference() = SNAP_TO_ANY

                override fun calculateTimeForScrolling(dx: Int): Int {
                    return super.calculateTimeForScrolling(dx).coerceAtLeast(100)
                    // at least 100ms. Looks instant otherwise for some reason
                }

                override fun calculateDtToFit(
                    viewStart: Int,
                    viewEnd: Int,
                    boxStart: Int,
                    boxEnd: Int,
                    snapPreference: Int
                ): Int {

                    val dtStart = boxStart + padding - viewStart
                    if (dtStart > 0) {
                        return dtStart
                    }
                    val dtEnd = boxEnd - padding - viewEnd
                    if (dtEnd < 0) {
                        return dtEnd
                    }
                    return 0
                }
            }
        }

        smoothScroller.targetPosition = position

        if (animate) {
            layoutManager!!.startSmoothScroll(smoothScroller)
        } else {
            // scroll without animation
            // https://stackoverflow.com/a/51233011/1067596

            doOnNextLayout {
                scrollToPosition(position)
            }
        }
    }

    fun Context.attrToThemeId(@AttrRes attributeResId: Int): Int {
        val typedValue = TypedValue()
        if (theme.resolveAttribute(attributeResId, typedValue, true)) {
            return typedValue.data
        }
        throw IllegalArgumentException(resources.getResourceName(attributeResId))
    }

    fun Context.getTintedDrawable(@DrawableRes drawableRes: Int, hash: Int) =
        ContextCompat.getDrawable(this, drawableRes)!!.apply {
            setTint(getMatColor(this@getTintedDrawable, hash))
        }

    fun RecyclerView.ViewHolder.setDragAlpha(dragging: Boolean) {
        itemView.animate().alpha(
            if (dragging) 0.5f
            else 1f
        ).setDuration(150).start()
    }

    fun BottomSheetDialogFragment.scheduleTransition() {
        val viewParent = view?.parent as? ViewGroup ?: return
        val lastRunTime = viewParent.getTag(R.id.time) as? Int ?: 0
        val now = System.currentTimeMillis()
        if (now - lastRunTime < 500) {
            return
        }
        viewParent.setTag(R.id.time, now)

        TransitionManager.beginDelayedTransition(
            viewParent,
            AutoTransition()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .setInterpolator(DecelerateInterpolator())
        )
    }

    fun Snackbar.focusOnTv() {
        if (MainActivity.isTV)
            view.postDelayed({
                view.findViewById<View>(com.google.android.material.R.id.snackbar_action)
                    .requestFocus()
            }, 200)
    }

    fun JobInfo.Builder.scheduleExpeditedCompat(
        js: JobScheduler,
        elseFn: (JobInfo.Builder.() -> Unit)? = null
    ): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setExpedited(true)
        } else {
            elseFn?.invoke(this)
        }
        val jobInfo = build()
        var result = js.schedule(jobInfo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && jobInfo.isExpedited && result == JobScheduler.RESULT_FAILURE) {
            setExpedited(false)
            elseFn?.invoke(this)
            build()
            result = js.schedule(jobInfo)
        }
        return result
    }

    fun getAction(
        icon: Int,
        emoji: String,
        text: String,
        pIntent: PendingIntent
    ): NotificationCompat.Action {
        val emojiText = if (isWindows11)
            "$emoji $text"
        else
            text
        return NotificationCompat.Action(icon, emojiText, pIntent)
    }

    fun stonksIconForDelta(delta: Int?) = when {
        delta == null -> 0
        delta == Int.MAX_VALUE -> R.drawable.vd_stonks_new
        delta in 1..5 -> R.drawable.vd_stonks_up
        delta > 5 -> R.drawable.vd_stonks_up_double
        delta in -1 downTo -5 -> R.drawable.vd_stonks_down
        delta < -5 -> R.drawable.vd_stonks_down_double
        delta == 0 -> R.drawable.vd_stonks_no_change
        else -> 0
    }

    fun Calendar.setMidnight() {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }

    fun ScrobbleData.toBundle() = Bundle().apply {
        putString(NLService.B_ARTIST, artist)
        putString(NLService.B_ALBUM, album)
        putString(NLService.B_TRACK, track)
        putString(NLService.B_ALBUM_ARTIST, albumArtist)
        putLong(NLService.B_TIME, timestamp * 1000L)
        putLong(NLService.B_DURATION, duration * 1000L)
    }

    fun ScrobbleData.copy() = ScrobbleData().also {
        it.artist = artist
        it.track = track
        it.album = album
        it.albumArtist = albumArtist
        it.timestamp = timestamp
        it.duration = duration
        it.isChosenByUser = isChosenByUser
        it.musicBrainzId = musicBrainzId
        it.streamId = streamId
        it.trackNumber = trackNumber
    }

    fun String.isUrlOrDomain(): Boolean {
        // got some internal IOBE, catch everything
        return try {
            toHttpUrlOrNull()?.topPrivateDomain() != null
        } catch (e: Exception) {
            false
        }
                ||
                try {
                    "https://$this".toHttpUrlOrNull()?.topPrivateDomain() != null
                } catch (e: Exception) {
                    false
                }
    }

    fun Fragment.hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }

    fun Fragment.showKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(view, 0)
    }

    val Context.hasMouse: Boolean
        get() {
            if (_hasMouse == null) {
                val inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
                _hasMouse = inputManager.inputDeviceIds.any {
                    val device = inputManager.getInputDevice(it)
                    // for windows 11 wsa
                    device.supportsSource(InputDevice.SOURCE_MOUSE) or
                            device.supportsSource(InputDevice.SOURCE_STYLUS)
                }
            }
            return _hasMouse!!
        }

    fun isScrobblerRunning(context: Context): Boolean {
        val serviceComponent = ComponentName(context, NLService::class.java)
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        var serviceRunning = false
        val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
        if (runningServices == null) {
            log("${this::isScrobblerRunning.name} runningServices is NULL")
            return true //just assume true for now. this throws SecurityException, might not work in future
        }
        for (service in runningServices) {
            if (service.service == serviceComponent) {
                log(
                    "${this::isScrobblerRunning.name}  service - pid: " + service.pid + ", currentPID: " +
                            Process.myPid() + ", clientPackage: " + service.clientPackage + ", clientCount: " +
                            service.clientCount + " process:" + service.process + ", clientLabel: " +
                            if (service.clientLabel == 0) "0" else "(" + context.resources.getString(
                                service.clientLabel
                            ) + ")"
                )
                if (service.process == BuildConfig.APPLICATION_ID + ":${Stuff.SCROBBLER_PROCESS_NAME}" /*&& service.clientCount > 0 */) {
                    serviceRunning = true
                    break
                }
            }
        }
        if (serviceRunning)
            return true

        log("${this::isScrobblerRunning.name} : service not running")
        return false
    }

    fun View.startFadeLoop() {
        clearAnimation()
        val anim = AlphaAnimation(0.5f, 0.9f).apply {
            duration = 500
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
            interpolator = DecelerateInterpolator()
        }
        startAnimation(anim)
    }

    val ImageView.memoryCacheKey
        get() = (this.result as? SuccessResult)?.memoryCacheKey

    fun Track.equalsExt(other: Track?): Boolean {
        return other != null &&
                this.artist == other.artist &&
                this.name == other.name &&
                this.album == other.album &&
                this.playedWhen == other.playedWhen
    }

    // https://stackoverflow.com/a/65046522/1067596
    suspend fun <TInput, TOutput> Iterable<TInput>.mapConcurrently(
        maxConcurrency: Int,
        transform: suspend (TInput) -> TOutput,
    ) = coroutineScope {
        val gate = Semaphore(maxConcurrency)
        this@mapConcurrently.map {
            async {
                gate.withPermit {
                    transform(it)
                }
            }
        }.awaitAll()
    }

    fun <T> RecyclerView.Adapter<*>.autoNotify(
        oldList: List<T>,
        newList: List<T>,
        detectMoves: Boolean = false,
        compareContents: (T, T) -> Boolean = { old, new -> old == new },
        compare: (T, T) -> Boolean,
    ) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                compare(oldList[oldItemPosition], newList[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                compareContents(oldList[oldItemPosition], newList[newItemPosition])

            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size
        }, detectMoves)
        diff.dispatchUpdatesTo(this)
    }

    fun OkHttpClient.Builder.ignoreSslErrors(): OkHttpClient.Builder {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts = Caller.trustAllCerts
        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory

        return sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
    }

    fun TextView.setTextAndAnimate(@StringRes stringRes: Int) =
        setTextAndAnimate(context.getString(stringRes))

    fun TextView.setTextAndAnimate(text: String) {
        val oldText = this.text.toString()
        if (oldText == text) return
        TransitionManager.beginDelayedTransition(parent as ViewGroup)
        this.text = text
    }

    fun Context.copyToClipboard(text: String, toast: Boolean = true) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Pano Scrobbler", text)
        clipboard.setPrimaryClip(clip)
        if (toast)
            toast(this, getString(R.string.copied))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun Context.getScrobblerExitReasons(
        afterTime: Long = -1,
        printAll: Boolean = false
    ): List<ApplicationExitInfo> {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 5)
            if (printAll) {
                exitReasons.forEachIndexed { index, applicationExitInfo ->
                    Timber.tag("exitReasons").w("${index + 1}. $applicationExitInfo")
                }
            }
            exitReasons.filter {
                it.processName == "$packageName:$SCROBBLER_PROCESS_NAME"
                        && it.reason == ApplicationExitInfo.REASON_OTHER
                        && it.timestamp > afterTime
            }
        } catch (e: Exception) {
            emptyList()
        }
        // Caused by java.lang.IllegalArgumentException at getHistoricalProcessExitReasons
        // Comparison method violates its general contract!
        // probably a samsung bug
    }

    fun <K, V> Map<K, V>.getOrDefaultKey(key: K, defaultKey: K) = this[key] ?: this[defaultKey]!!

    fun <T> List<T>.wrappedGet(index: Int) = this[(index + size) % size]
}