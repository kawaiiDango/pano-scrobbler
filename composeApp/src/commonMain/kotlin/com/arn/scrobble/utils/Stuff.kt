package com.arn.scrobble.utils

import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.Tokens
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.CacheStrategy
import com.arn.scrobble.billing.BillingClientData
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.PanoSnackbarVisuals
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLParserException
import io.ktor.http.maxAge
import io.ktor.http.takeFrom
import io.ktor.util.encodeBase64
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val SCROBBLER_PROCESS_NAME = "bgScrobbler"
    const val DEEPLINK_PROTOCOL_NAME = "pano-scrobbler"
    const val DEEPLINK_BASE_PATH = "$DEEPLINK_PROTOCOL_NAME://screen"
    const val ARG_TAB = "tab"
    const val PRO_PRODUCT_ID = "pscrobbler_pro"
    const val TYPE_ALL = 0
    const val TYPE_ARTISTS = 1
    const val TYPE_ALBUMS = 2
    const val TYPE_TRACKS = 3
    const val TYPE_ALBUM_ARTISTS = 4
    const val TYPE_LOVES = 5
    const val LIBREFM_KEY = "panoScrobbler"
    val LAST_KEY = Tokens.LAST_KEY
    val LAST_SECRET = Tokens.LAST_SECRET
    const val FRIENDS_RECENTS_DELAY = 800L
    const val MAX_PATTERNS = 30
    const val MAX_PINNED_FRIENDS = 10
    const val MAX_INDEXED_ITEMS = 10000
    const val PINNED_FRIENDS_CACHE_TIME = 60L * 60 * 24 * 1 * 1000
    const val MIN_ITEMS_TO_SHOW_SEARCH = 7
    const val TIME_2002 = 1009823400000L // Jan 1 2002

    const val EXTRA_PINNED = "pinned"
    const val EXTRA_EVENT = "event"

    const val RECENTS_REFRESH_INTERVAL = 40 * 1000L
    const val NOTI_SCROBBLE_INTERVAL = 5 * 60 * 1000L
    const val LASTFM_MAX_PAST_SCROBBLE = 14 * 24 * 60 * 60 * 1000L
    const val FULL_INDEX_ALLOWED_INTERVAL = 24 * 60 * 60 * 1000L
    const val CHARTS_WIDGET_REFRESH_INTERVAL = 60 * 60 * 1000L
    const val TRACK_INFO_WINDOW = 3 * 60 * 1000L
    const val TRACK_INFO_REQUESTS = 2
    const val META_WAIT = 400L
    const val SCROBBLE_FROM_MIC_DELAY = 3 * 1000L
    const val MIN_LISTENER_COUNT = 5
    const val MAX_HISTORY_ITEMS = 20
    const val DEFAULT_PAGE_SIZE = 100
    const val GRID_MIN_SIZE = 170
    const val SCROBBLE_SOURCE_THRESHOLD = 1000L

    const val LASTFM_API_ROOT = "https://ws.audioscrobbler.com/2.0/"
    const val LIBREFM_API_ROOT = "https://libre.fm/2.0/"
    const val LISTENBRAINZ_API_ROOT = "https://api.listenbrainz.org/"

    val LASTFM_AUTH_CB_URL =
        "https://www.last.fm/api/auth?api_key=$LAST_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/lastfm"
    const val LIBREFM_AUTH_CB_URL =
        "https://www.libre.fm/api/auth?api_key=$LIBREFM_KEY&cb=$DEEPLINK_PROTOCOL_NAME://auth/librefm"

    const val MANUFACTURER_HUAWEI = "huawei"
    const val MANUFACTURER_XIAOMI = "xiaomi"
    const val MANUFACTURER_SAMSUNG = "samsung"

    const val PACKAGE_TV_SETTINGS = "com.android.tv.settings"
    const val ACTIVITY_TV_SETTINGS = "com.android.tv.settings.device.apps.AppsActivity"
    const val CHANNEL_PIXEL_NP =
        "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
    const val PACKAGE_PIXEL_NP = "com.google.intelligence.sense"
    const val PACKAGE_PIXEL_NP_R = "com.google.android.as"
    const val PACKAGE_PIXEL_NP_AMM = "com.kieronquinn.app.pixelambientmusic"
    const val PACKAGE_SHAZAM = "com.shazam.android"
    const val CHANNEL_SHAZAM = "notification_shazam_match_v1" //"auto_shazam_v2"

    //    const val NOTIFICATION_TAG_SHAZAM = "NOTIFICATION_SHAZAM_RESULTS" //"auto_shazam_v2"
    const val PACKAGE_XIAMI = "fm.xiami.main"
    const val PACKAGE_PANDORA = "com.pandora.android"
    const val PACKAGE_SONOS = "com.sonos.acr"
    const val PACKAGE_SONOS2 = "com.sonos.acr2"
    const val PACKAGE_DIFM = "com.audioaddict.di"
    const val PACKAGE_PODCAST_ADDICT = "com.bambuna.podcastaddict"
    const val PACKAGE_HUAWEI_MUSIC = "com.android.mediacenter"
    const val PACKAGE_SPOTIFY = "com.spotify.music"
    const val PACKAGE_YOUTUBE_TV = "com.google.android.youtube.tv"
    const val PACKAGE_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"
    const val PACKAGE_YMUSIC = "com.kapp.youtube.final"
    const val PACKAGE_SOUNDCLOUD = "com.soundcloud.android"
    const val PACKAGE_OTO_MUSIC = "com.piyush.music"
    const val PACKAGE_PI_MUSIC = "com.Project100Pi.themusicplayer"
    const val PACKAGE_SYMFONIUM = "app.symfonik.music.player"
    const val PACKAGE_PLEXAMP = "tv.plex.labs.plexamp"
    const val PACKAGE_BANDCAMP = "com.bandcamp.android"
    const val PACKAGE_NICOBOX = "jp.nicovideo.nicobox"
    const val PACKAGE_YANDEX_MUSIC = "ru.yandex.music"
    const val PACKAGE_YAMAHA_MUSIC_CAST = "com.yamaha.av.musiccastcontroller"
    const val PACKAGE_NEWPIPE = "org.schabi.newpipe"
    const val PACKAGE_NINTENDO_MUSIC = "com.nintendo.znba"
    const val ARTIST_NINTENDO_MUSIC = "Nintendo Co., Ltd."

    const val CHANNEL_NOTI_SCROBBLING = "noti_scrobbling"
    const val CHANNEL_NOTI_SCR_ERR = "noti_scrobble_errors"
    const val CHANNEL_NOTI_NEW_APP = "noti_new_app"
    const val CHANNEL_NOTI_PENDING = "noti_pending_scrobbles"
    const val CHANNEL_NOTI_DIGEST_WEEKLY = "noti_digest_weekly"
    const val CHANNEL_NOTI_DIGEST_MONTHLY = "noti_digest_monthly"
    const val CHANNEL_NOTI_PERSISTENT = "noti_persistent"
    const val CHANNEL_TEST_SCROBBLE_FROM_NOTI = "test_scrobble_from_noti"

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
        "app.rvx.android.youtube",

        PACKAGE_YOUTUBE_MUSIC,
        "com.vanced.android.apps.youtube.music",
        "app.revanced.android.apps.youtube.music",

        PACKAGE_NEWPIPE,
        PACKAGE_YMUSIC,
        PACKAGE_NICOBOX,
        PACKAGE_SOUNDCLOUD,

        // radios
        "tunein.player",
    )

    val IGNORE_ARTIST_META_WITH_FALLBACK = setOf(
        PACKAGE_SOUNDCLOUD,
        PACKAGE_NICOBOX,
        PACKAGE_YMUSIC,
        PACKAGE_NEWPIPE,
        PACKAGE_YOUTUBE_MUSIC,
        "com.vanced.android.apps.youtube.music",
        "app.revanced.android.apps.youtube.music",
    )

    val BLOCKED_MEDIA_SESSION_TAGS = mapOf(
        "*" to listOf("CastMediaSession"),
        PACKAGE_YAMAHA_MUSIC_CAST to listOf("NotificationService"),
        // my test app
        "com.example.myapplication.sessiontest" to listOf("androidx.media3.session.id.demo_session_id 124"),
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

    val disallowedWebviewUrls = listOf(
        "https://www.last.fm/join",
        "https://www.last.fm/settings/lostpassword",
        "https://libre.fm/register.php",
        "https://libre.fm/reset.php",
    )

    var isRunningInTest = false

    var isOnline = true

    val isInDemoMode get() = mainPrefsInitialValue.demoModeP

    val countryCodesMap by lazy {
        val countries = hashMapOf<String, String>()
        Locale.getISOCountries().forEach { iso ->
            val l = Locale("en", iso)
            countries[l.getDisplayCountry(l)] = iso
        }
        countries
    }

    val myJson by lazy {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    private val numberFormat by lazy {
        NumberFormat.getInstance()
    }

    var mainPrefsInitialValue = MainPrefs()

    val billingClientData by lazy {
        BillingClientData(
            proProductId = PRO_PRODUCT_ID,
            appName = BuildKonfig.APP_NAME,
            publicKeyBase64 = if (PlatformStuff.isNonPlayBuild)
                Tokens.LICENSE_PUBLIC_KEY_BASE64
            else
                Tokens.PLAY_BILLING_PUBLIC_KEY_BASE64,
            httpClient = Requesters.genericKtorClient,
            serverUrl = Tokens.LICENSE_CHECKING_SERVER,
            lastcheckTime = PlatformStuff.mainPrefs.data.map { it.lastLicenseCheckTime },
            deviceIdentifier = PlatformStuff.getDeviceIdentifier(),
            setLastcheckTime = {
                PlatformStuff.mainPrefs.updateData { it.copy(lastLicenseCheckTime = it.lastLicenseCheckTime) }
            },
            receipt = PlatformStuff.mainPrefs.data.map { it.receipt to it.receiptSignature },
            setReceipt = { r, s ->
                PlatformStuff.mainPrefs.updateData { it.copy(receipt = r, receiptSignature = s) }
            }
        )
    }


    val globalExceptionFlow by lazy { MutableSharedFlow<Throwable>() }

    val globalSnackbarFlow by lazy { MutableSharedFlow<PanoSnackbarVisuals>() }

    val browserPackages = mutableSetOf<String>()

    fun Number.format() = numberFormat.format(this)!!

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

    @Composable
    fun <T> Flow<MainPrefs>.collectAsStateWithInitialValue(
        mapBlock: (MainPrefs) -> T,
    ) = mapLatest {
//        mainPrefsInitialValue = it
        mapBlock(it)
    }.collectAsStateWithLifecycle(mapBlock(mainPrefsInitialValue))


    fun humanReadableNum(n: Int): String {
        val k = 1000
        if (n < k) return DecimalFormat("#").format(n) //localise
        val exp = (ln(n.toDouble()) / ln(k.toDouble())).toInt()
        val unit = "KMB"[exp - 1] //kilo, million, billion
        val dec = n / k.toDouble().pow(exp.toDouble())

        val decimal = DecimalFormat(if (dec >= 100) "#" else "#.#").format(dec)
        return decimal + unit
    }

    fun humanReadableDuration(millis: Long): String {
        val secs = millis / 1000
        val s = secs % 60
        val m = (secs / 60) % 60
        val h = secs / 3600
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    fun getFileNameDateSuffix(): String {
        val cal = Calendar.getInstance()
        return "" + cal[Calendar.YEAR] + "_" + (cal[Calendar.MONTH] + 1) + "_" + cal[Calendar.DATE] + "_" + cal[Calendar.HOUR_OF_DAY] + "_" + cal[Calendar.MINUTE]
    }

    fun Long.timeToUTC() = this + TimeZone.getDefault().getOffset(System.currentTimeMillis())

    fun Long.timeToLocal() = this - TimeZone.getDefault().getOffset(System.currentTimeMillis())

    fun getCountryFlag(countryName: String): String {
        val isoCode = countryCodesMap[countryName] ?: return ""
        val flagEmoji = StringBuilder()
        isoCode.forEach {
            val codePoint = 127397 + it.code
            flagEmoji.appendCodePoint(codePoint)
        }
        return flagEmoji.toString()
    }

    fun <T : Any> List<T>.toInverseMap() = mapIndexed { i, it -> it to i }.toMap()

    fun isLoggedIn() = Scrobblables.current.value != null


    fun Calendar.setMidnight() {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }

    suspend fun Calendar.setUserFirstDayOfWeek(): Calendar {
        val firstDayOfWeek = PlatformStuff.mainPrefs.data.map { it.firstDayOfWeek }.first()
        if (firstDayOfWeek >= Calendar.SUNDAY)
            this.firstDayOfWeek = firstDayOfWeek
        // else auto
        return this
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


    fun isValidUrl(url: String): Boolean {
        return try {
            URLBuilder().takeFrom(url)
            true
        } catch (e: URLParserException) {
            false
        }
    }

    fun HttpRequestBuilder.cacheStrategy(cacheStrategy: CacheStrategy) {
        when (cacheStrategy) {
            CacheStrategy.CACHE_FIRST -> {}
            CacheStrategy.CACHE_ONLY_INCLUDE_EXPIRED -> header(
                HttpHeaders.CacheControl, "only-if-cached, max-stale=${Int.MAX_VALUE}",
            )

            CacheStrategy.NETWORK_ONLY -> header(HttpHeaders.CacheControl, "no-cache")
            CacheStrategy.CACHE_FIRST_ONE_DAY -> maxAge(TimeUnit.DAYS.toSeconds(1).toInt())
            CacheStrategy.CACHE_FIRST_ONE_WEEK -> maxAge(TimeUnit.DAYS.toSeconds(7).toInt())
        }
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

    fun Painter.toImageBitmap(
        density: Density = Density(1f),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        size: Size = intrinsicSize,
        config: ImageBitmapConfig = ImageBitmapConfig.Argb8888,
    ): ImageBitmap {
        val image = ImageBitmap(
            width = size.width.roundToInt(),
            height = size.height.roundToInt(),
            config = config
        )
        val canvas = Canvas(image)
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = size
        ) {
            draw(size = this.size)
        }
        return image
    }

    suspend fun addTestCreds(serviceStr: String, username: String, sk: String): Boolean {
        val type = try {
            AccountType.valueOf(serviceStr.uppercase())
        } catch (e: IllegalArgumentException) {
            return false
        }

        Scrobblables.add(
            UserAccountSerializable(
                type,
                UserCached(
                    username,
                    "https://last.fm/user/$username",
                    username,
                    "",
                    -1,
                ),
                sk
            )
        )

        return true
    }

    fun <K, V> Map<K, V>.getOrDefaultKey(key: K, defaultKey: K) = this[key] ?: this[defaultKey]!!

    fun <T> List<T>.wrappedGet(index: Int) = this[(index + size) % size]

    fun formatBigHyphen(artist: String, title: String) = "$artist — $title"
}


fun String.sha256() =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .encodeBase64()

@Keep
// Useful for force logging to crashlytics in debug builds
class ForceLogException(override val message: String?) : Exception(message)