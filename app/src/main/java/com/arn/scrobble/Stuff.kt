package com.arn.scrobble

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.SearchManager
import android.app.UiModeManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.media.MediaMetadata
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.Process
import android.provider.MediaStore
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.text.format.DateUtils
import android.view.InputDevice
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.UiUtils.toast
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.Track
import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber
import java.io.IOException
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ln
import kotlin.math.pow


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val SCROBBLER_PROCESS_NAME = "bgScrobbler"
    const val DEEPLINK_PROTOCOL_NAME = "pscrobbler"
    const val TAG_HOME_PAGER = "home_pager"
    const val TAG_CHART_PAGER = "chart_pager"
    const val TAG_FIRST_THINGS = "first_things"
    const val TAG_INFO_FROM_WIDGET = "info_widget"
    const val ARG_URL = "url"
    const val ARG_SAVE_COOKIES = "cookies"
    const val ARG_NOPASS = "nopass"
    const val ARG_TYPE = "type"
    const val ARG_TAG = "tag"
    const val ARG_SHOW_DIALOG = "dialog"
    const val ARG_COUNT = "count"
    const val ARG_PKG = "pkg"
    const val ARG_ACTION = "action"
    const val ARG_TLS_NO_VERIFY = "tls_no_verify"
    const val ARG_DISABLE_FRAGMENT_NAVIGATION = "disable_fragment_navigation"
    const val ARG_ALLOWED_PACKAGES = MainPrefs.PREF_ALLOWED_PACKAGES
    const val TYPE_ALL = 0
    const val TYPE_ARTISTS = 1
    const val TYPE_ALBUMS = 2
    const val TYPE_TRACKS = 3
    const val TYPE_LOVES = 4
    const val TYPE_SC = 5
    const val TYPE_TAG_CLOUD = 6
    const val NP_ID = -5
    const val LIBREFM_KEY = "panoScrobbler"
    val LAST_KEY = Tokens.LAST_KEY
    val LAST_SECRET = Tokens.LAST_SECRET
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
    const val FETCH_TRACK_INFO = false

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
    const val OFFLINE_SCROBBLE_JOB_DELAY = 20 * 1000L
    const val LASTFM_MAX_PAST_SCROBBLE = 14 * 24 * 60 * 60 * 1000L
    const val CHARTS_WIDGET_REFRESH_INTERVAL = 60 * 60 * 1000L
    const val LASTFM_JAVA_CACHE_SIZE = 30 * 1024 * 1024
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
    val LASTFM_AUTH_CB_URL =
        "https://www.last.fm/api/auth?api_key=$LAST_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/lastfm"
    const val LIBREFM_AUTH_CB_URL =
        "https://www.libre.fm/api/auth?api_key=$LIBREFM_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/librefm"

    private var timeIt = 0L

    const val MANUFACTURER_HUAWEI = "huawei"
    const val MANUFACTURER_XIAOMI = "xiaomi"
    const val MANUFACTURER_SAMSUNG = "samsung"
    const val MANUFACTURER_GOOGLE = "google"

    const val CHANNEL_PIXEL_NP =
        "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
    const val PACKAGE_PIXEL_NP = "com.google.intelligence.sense"
    const val PACKAGE_PIXEL_NP_R = "com.google.android.as"
    const val PACKAGE_PIXEL_NP_AMM = "com.kieronquinn.app.pixelambientmusic"
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
    const val PACKAGE_NICOBOX = "jp.nicovideo.nicobox"

    const val MARKET_URL = "market://details?id=" + BuildConfig.APPLICATION_ID

    val IGNORE_ARTIST_META = setOf(
        "com.google.android.youtube",
        "com.vanced.android.youtube",
        "com.google.android.ogyoutube",
        "com.google.android.apps.youtube.mango",
        PACKAGE_YOUTUBE_TV,
        "com.google.android.youtube.tvkids",
        "com.liskovsoft.smarttubetv.beta",
        "com.liskovsoft.smarttubetv",
        "app.revanced.android.youtube",
        "org.schabi.newpipe",
        PACKAGE_YMUSIC,
        PACKAGE_NICOBOX,
        PACKAGE_SOUNDCLOUD,

        // radios
        "tunein.player",
    )

    val IGNORE_ARTIST_META_WITH_FALLBACK = setOf(
        PACKAGE_SOUNDCLOUD,
    )

    val needSyntheticStates = setOf(
        PACKAGE_BLACKPLAYER,
        PACKAGE_BLACKPLAYEREX,
//        PACKAGE_YOUTUBE_MUSIC,
    )
    val PACKAGES_PIXEL_NP = setOf(
        PACKAGE_PIXEL_NP,
        PACKAGE_PIXEL_NP_R,
        PACKAGE_PIXEL_NP_AMM,
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

    var isOnline = true

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            return PendingIntent.FLAG_UPDATE_CURRENT
        }

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

    fun isDkmaNeeded(): Boolean {
        val packages = STARTUPMGR_INTENTS.map { it.first }.toSet()
        return packages.any {
            try {
                App.context.packageManager.getApplicationInfo(it, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            App.context.packageManager.getPackageInfo(packageName, 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getDefaultBrowserPackage(packageManager: PackageManager): String? {
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

    fun launchSearchIntent(musicEntry: MusicEntry, pkgName: String?) {
        var searchQueryFirst = ""
        var searchQuerySecond = ""

        when (musicEntry) {
            is Artist -> {
                searchQueryFirst = musicEntry.name
            }

            is Album -> {
                searchQueryFirst = musicEntry.artist
                searchQuerySecond = musicEntry.name
            }

            is Track -> {
                searchQueryFirst = musicEntry.artist
                searchQuerySecond = musicEntry.name
            }
        }

        launchSearchIntent(searchQueryFirst, searchQuerySecond, pkgName)
    }

    private fun launchSearchIntent(
        artist: String,
        track: String,
        pkgName: String?
    ) {
        if (artist.isEmpty() && track.isEmpty())
            return

        val prefs = MainPrefs(App.context)

        if (BuildConfig.DEBUG && isWindows11 && prefs.songSearchUrl.isNotEmpty()) { // open song urls in windows browser for me
            val searchUrl = prefs.songSearchUrl
                .replace("\$artist", artist)
                .replace("\$title", track)
            openInBrowser(searchUrl)
            return
        }

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (artist.isNotEmpty())
                putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist)
            if (track.isNotEmpty())
                putExtra(MediaStore.EXTRA_MEDIA_TITLE, track)

            val query = when {
                artist.isEmpty() -> track
                track.isEmpty() -> artist
                else -> "$artist $track"
            }
            putExtra(SearchManager.QUERY, query)

            if (pkgName != null && prefs.proStatus && prefs.searchInSource)
                `package` = pkgName
        }
        try {
            App.context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (pkgName != null) {
                try {
                    intent.`package` = null
                    App.context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    App.context.toast(R.string.no_player)
                }
            } else
                App.context.toast(R.string.no_player)
        }
    }

    fun openInBrowser(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // prevent infinite loop
            if (MainPrefs(App.context).lastfmLinksEnabled) {
                browserIntent.`package` = getDefaultBrowserPackage(App.context.packageManager)
            }

            App.context.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            App.context.toast(R.string.no_browser)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    fun savePictureQ(
        internalStorageUri: Uri,
        displayName: String,
        mimeType: String,
    ) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        var uri: Uri? = null

        runCatching {
            with(App.context.contentResolver) {
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                    uri = it // Keep uri reference so it can be removed on failure
                    openOutputStream(it)?.use { ostream ->
                        openInputStream(internalStorageUri)?.use { istream ->
                            istream.copyTo(ostream)
                        }
                    } ?: throw IOException("Failed to open output stream.")

                } ?: throw IOException("Failed to create new MediaStore record.")
            }
        }.getOrElse {
            uri?.let { orphanUri ->
                // Don't leave an orphan entry in the MediaStore
                App.context.contentResolver.delete(orphanUri, null, null)
            }

            throw it
        }
    }

    fun NotificationManager.isChannelEnabled(pref: SharedPreferences, channelId: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isWindows11) {
            areNotificationsEnabled() &&
                    getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            pref.getBoolean(channelId, true)
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

    fun getNotificationAction(
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

    fun String.isUrlOrDomain(): Boolean {
        // got some internal IOBE, catch everything
        // .topPrivateDomain() reads the big public suffix file every time and causes ANRs
        return try {
            toHttpUrl().host.contains('.')
        } catch (e: Exception) {
            try {
                "https://$this".toHttpUrl().host.contains('.')
            } catch (e: Exception) {
                false
            }
        }
    }

    fun isScrobblerRunning(): Boolean {
        val serviceComponent = ComponentName(App.context, NLService::class.java)
        val manager = ContextCompat.getSystemService(
            App.context,
            ActivityManager::class.java
        ) as ActivityManager
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
                            if (service.clientLabel == 0) "0" else "(" + App.context.resources.getString(
                                service.clientLabel
                            ) + ")"
                )
                if (service.process == BuildConfig.APPLICATION_ID + ":${SCROBBLER_PROCESS_NAME}" /*&& service.clientCount > 0 */) {
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

    fun Track.equalsExt(other: Track?): Boolean {
        return other != null &&
                this.artist == other.artist &&
                this.name == other.name &&
                this.album == other.album &&
                this.playedWhen == other.playedWhen
    }

    fun MediaMetadata.dump() {
        val data = keySet().joinToString(separator = "\n") {
            var value: String? = getString(it)
            if (value == null)
                value = getLong(it).toString()
            if (value == "0")
                value = getBitmap(it)?.toString()
            if (value == null)
                value = getRating(it)?.toString()
            "$it: $value"
        }
        log("MediaMetadata\n$data")
    }

    fun Intent.putSingle(parcelable: Parcelable): Intent {
        putExtra(parcelable::class.qualifiedName, parcelable)
        return this
    }

    fun Bundle.putSingle(parcelable: Parcelable): Bundle {
        putParcelable(parcelable::class.qualifiedName, parcelable)
        return this
    }

    inline fun <reified T : Parcelable> Intent.getSingle() =
        getParcelableExtra<T>(T::class.qualifiedName)

    inline fun <reified T : Parcelable> Bundle.getSingle() =
        getParcelable<T>(T::class.qualifiedName)

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

    fun Context.copyToClipboard(text: String, toast: Boolean = true) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Pano Scrobbler", text)
        clipboard.setPrimaryClip(clip)
        if (toast)
            toast(R.string.copied)
    }

    fun String.similarity(s: String): Double {
        var longer = this
        var shorter = s
        if (this.length < s.length) { // longer should always have greater length
            longer = s
            shorter = this
        }
        val longerLength = longer.length
        return if (longerLength == 0) {
            1.0 /* both strings are zero length */
        } else (longerLength - editDistance(longer, shorter)) / longerLength.toDouble()
    }

    // Example implementation of the Levenshtein Edit Distance
// See http://rosettacode.org/wiki/Levenshtein_distance#Java
    private fun editDistance(s1: String, s2: String): Int {
        val s1 = s1.lowercase(Locale.getDefault())
        val s2 = s2.lowercase(Locale.getDefault())
        val costs = IntArray(s2.length + 1)
        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) {
                    costs[j] = j
                } else {
                    if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1[i - 1] != s2[j - 1])
                            newValue = Math.min(
                                Math.min(newValue, lastValue),
                                costs[j]
                            ) + 1
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
            }
            if (i > 0)
                costs[s2.length] = lastValue
        }
        return costs[s2.length]
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getScrobblerExitReasons(
        afterTime: Long = -1,
        printAll: Boolean = false
    ): List<ApplicationExitInfo> {
        return try {
            val activityManager =
                ContextCompat.getSystemService(App.context, ActivityManager::class.java)!!
            val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 30)

            exitReasons.filter {
                it.processName == "${App.context.packageName}:$SCROBBLER_PROCESS_NAME"
//                        && it.reason == ApplicationExitInfo.REASON_OTHER
                        && it.timestamp > afterTime
            }.also {
                if (printAll) {
                    it.take(5).forEachIndexed { index, applicationExitInfo ->
                        Timber.tag("exitReasons").w("${index + 1}. $applicationExitInfo")
                    }
                }
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

    val hasMouse by lazy {
        val inputManager = ContextCompat.getSystemService(App.context, InputManager::class.java)!!
        inputManager.inputDeviceIds.any {
            val device = inputManager.getInputDevice(it)
            // for windows 11 wsa
            device.supportsSource(InputDevice.SOURCE_MOUSE) or
                    device.supportsSource(InputDevice.SOURCE_STYLUS)
        }
    }

    val isTv by lazy {
        val uiModeManager = ContextCompat.getSystemService(App.context, UiModeManager::class.java)!!
        uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}

@Keep
// Useful for force logging to crashlytics in debug builds
class ForceLogException(override val message: String?) : Exception(message)