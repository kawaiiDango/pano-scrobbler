package com.arn.scrobble.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.postString
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.cache.CacheStrategy
import com.arn.scrobble.billing.BillingClientData
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.updates.UpdateAction
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.URLParserException
import io.ktor.http.maxAge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.PaddingOption
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt


/**
 * Created by arn on 13-03-2017.
 */

object Stuff {
    const val SCROBBLER_PROCESS_NAME = "bgScrobbler"
    const val DEEPLINK_SCHEME = "pano-scrobbler"
    const val ARG_TAB = "tab"
    const val PRO_PRODUCT_ID = "pscrobbler_pro"
    const val TYPE_ALL = 0
    const val TYPE_ARTISTS = 1
    const val TYPE_ALBUMS = 2
    const val TYPE_TRACKS = 3
    const val TYPE_ALBUM_ARTISTS = 4
    const val TYPE_LOVES = 5
    const val LIBREFM_KEY = "panoScrobbler"
    const val FRIENDS_RECENTS_DELAY = 800L
    const val MAX_PATTERNS = 50
    const val MAX_PINNED_FRIENDS = 10
    const val MAX_INDEXED_ITEMS = 10000
    const val PINNED_FRIENDS_CACHE_TIME = 60L * 60 * 24 * 1 * 1000
    const val MIN_ITEMS_TO_SHOW_SEARCH = 7
    const val TIME_2002 = 1009823400000L // Jan 1 2002
    const val SPOTIFY_SEARCH_URL = "spotify://search/\$query"
    const val APPLE_MUSIC_SEARCH_URL = "https://music.apple.com/search?term=\$query"
    const val DEEZER_SEARCH_URL = "https://deezer.com/search/\$query"
    const val TIDAL_SEARCH_URL = "https://tidal.com/search?q=\$query"
    const val YT_MUSIC_SEARCH_URL = "https://music.youtube.com/search?q=\$query"
    const val BANDCAMP_SEARCH_URL = "https://bandcamp.com/search?q=\$query"
    const val GENIUS_SEARCH_URL = "https://genius.com/search?q=\$query"
    const val DEFAULT_SEARCH_URL = SPOTIFY_SEARCH_URL

    const val EXTRA_PINNED = "pinned"

    const val RECENTS_REFRESH_INTERVAL = 30 * 1000L
    const val FRIENDS_REFRESH_INTERVAL = 60 * 1000L
    const val LASTFM_MAX_PAST_SCROBBLE = 14 * 24 * 60 * 60 * 1000L
    const val FULL_INDEX_ALLOWED_INTERVAL = 24 * 60 * 60 * 1000L
    const val CHARTS_WIDGET_REFRESH_INTERVAL_HOURS = 6
    const val META_WAIT = 1000L
    const val MAX_HISTORY_ITEMS = 20
    const val DEFAULT_PAGE_SIZE = 100
    const val SCROBBLE_SOURCE_THRESHOLD = 1000L

    const val LASTFM_API_ROOT = "https://ws.audioscrobbler.com/2.0/"
    const val LIBREFM_API_ROOT = "https://libre.fm/2.0/"
    const val LISTENBRAINZ_API_ROOT = "https://api.listenbrainz.org/"
    const val LASTFM_URL = "https://www.last.fm/"

    const val MANUFACTURER_HUAWEI = "huawei"
    const val MANUFACTURER_XIAOMI = "xiaomi"
    const val MANUFACTURER_SAMSUNG = "samsung"
    const val MANUFACTURER_ONEPLUS = "oneplus"
    const val MANUFACTURER_OPPO = "oppo"
    const val MANUFACTURER_MEIZU = "meizu"
    const val MANUFACTURER_VIVO = "vivo"

    const val PACKAGE_TV_SETTINGS = "com.android.tv.settings"
    const val ACTIVITY_TV_SETTINGS = "com.android.tv.settings.device.apps.AppsActivity"
    const val CHANNEL_PIXEL_NP =
        "com.google.intelligence.sense.ambientmusic.MusicNotificationChannel"
    const val PACKAGE_PIXEL_NP = "com.google.intelligence.sense"
    const val PACKAGE_PIXEL_NP_R = "com.google.android.as"
    const val PACKAGE_PIXEL_NP_AMM = "com.kieronquinn.app.pixelambientmusic"
    const val PACKAGE_SHAZAM = "com.shazam.android"
    const val PACKAGE_AUDILE = "com.mrsep.musicrecognizer"
    const val CHANNEL_SHAZAM = "notification_shazam_match_v1" //"auto_shazam_v2"
    const val CHANNEL_SHAZAM2 = "notification_shazam_foreground_match_v2"
    const val AUDILE_METADATA_KEY_TRACK_TITLE = "com.mrsep.musicrecognizer.track_metadata.title"
    const val AUDILE_METADATA_KEY_TRACK_ARTIST = "com.mrsep.musicrecognizer.track_metadata.artist"
    const val AUDILE_METADATA_KEY_TRACK_ALBUM = "com.mrsep.musicrecognizer.track_metadata.album"
    const val AUDILE_METADATA_KEY_TRACK_DURATION =
        "com.mrsep.musicrecognizer.track_metadata.duration"
    const val AUDILE_METADATA_KEY_TRACK_SAMPLE_TIMESTAMP =
        "com.mrsep.musicrecognizer.track_metadata.sample_timestamp"

    //    const val NOTIFICATION_TAG_SHAZAM = "NOTIFICATION_SHAZAM_RESULTS" //"auto_shazam_v2"
    const val PACKAGE_PANDORA = "com.pandora.android"
    const val PACKAGE_SONOS = "com.sonos.acr"
    const val PACKAGE_SONOS2 = "com.sonos.acr2"
    const val PACKAGE_DIFM = "com.audioaddict.di"
    const val PACKAGE_PODCAST_ADDICT = "com.bambuna.podcastaddict"
    const val PACKAGE_HUAWEI_MUSIC = "com.android.mediacenter"
    const val PACKAGE_SPOTIFY = "com.spotify.music"
    const val PACKAGE_DEEZER = "deezer.android.app"
    const val PACKAGE_DEEZER_TV = "deezer.android.tv"
    const val PACKAGE_YOUTUBE_TV = "com.google.android.youtube.tv"
    const val PACKAGE_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music"
    const val PACKAGE_YMUSIC = "com.kapp.youtube.final"
    const val PACKAGE_SOUNDCLOUD = "com.soundcloud.android"
    const val PACKAGE_ECHO = "dev.brahmkshatriya.echo.nightly"
    const val PACKAGE_METROLIST = "com.metrolist.music"
    const val PACKAGE_OTO_MUSIC = "com.piyush.music"
    const val PACKAGE_PI_MUSIC = "com.Project100Pi.themusicplayer"
    const val PACKAGE_SYMFONIUM = "app.symfonik.music.player"
    const val PACKAGE_PLEXAMP = "tv.plex.labs.plexamp"
    const val PACKAGE_NICOBOX = "jp.nicovideo.nicobox"
    const val PACKAGE_YANDEX_MUSIC = "ru.yandex.music"
    const val PACKAGE_YAMAHA_MUSIC_CAST = "com.yamaha.av.musiccastcontroller"
    const val PACKAGE_NEWPIPE = "org.schabi.newpipe"
    const val PACKAGE_NINTENDO_MUSIC = "com.nintendo.znba"
    const val PACKAGE_APPLE_MUSIC = "com.apple.android.music"
    const val PACKAGE_APPLE_MUSIC_CLASSICAL = "com.apple.android.music.classical"
    const val PACKAGE_TIDAL = "com.aspiro.tidal"
    const val PACKAGE_OMNIA = "com.rhmsoft.omnia"
    const val PACKAGE_APPLE_MUSIC_WIN_EXE = "AppleMusic.exe"
    const val PACKAGE_APPLE_MUSIC_WIN_STORE = "AppleInc.AppleMusicWin_nzyj5cx40ttqa!App"
    const val HOST_APPLE_MUSIC = "music.apple.com"
    const val HOST_YOUTUBE_MUSIC = "music.youtube.com"
    const val PACKAGE_KDE_CONNECT_LINUX = "org.mpris.MediaPlayer2.kdeconnect"
    const val PACKAGE_FIREFOX_WIN = "308046B0AF4A39CB"
    const val PACKAGE_DEEZER_WIN = "com.deezer.deezer-desktop"
    const val PACKAGE_DEEZER_WIN_EXE = "Deezer.exe"
    const val PACKAGE_DEEZER_WIN_STORE = "Deezer.62021768415AF_q7m17pa7q8kj0!Deezer.Music"
    const val PACKAGE_TIDAL_WIN_EXE = "TIDAL.exe"
    const val PACKAGE_TIDAL_WIN = "com.squirrel.TIDAL.TIDAL"
    const val PACKAGE_TIDAL_WIN_STORE = "WiMPMusic.27241E05630EA_kn85bz84x7te4!TIDAL"

    const val METADATA_KEY_AM_ARTIST_ID = "com.apple.android.music.playback.metadata.ARTIST_ID"
    const val METADATA_KEY_YOUTUBE_WIDTH =
        "com.google.android.youtube.MEDIA_METADATA_VIDEO_WIDTH_PX"
    const val METADATA_KEY_YOUTUBE_HEIGHT =
        "com.google.android.youtube.MEDIA_METADATA_VIDEO_HEIGHT_PX"
    const val ARTIST_NINTENDO_MUSIC = "Nintendo Co., Ltd."

    const val CHANNEL_NOTI_SCROBBLING = "noti_scrobbling"
    const val CHANNEL_NOTI_SCR_ERR = "noti_scrobble_errors"
    const val CHANNEL_NOTI_NEW_APP = "noti_new_app"
    const val CHANNEL_NOTI_DIGEST_WEEKLY = "noti_digest_weekly"
    const val CHANNEL_NOTI_DIGEST_MONTHLY = "noti_digest_monthly"
    const val CHANNEL_NOTI_FG_SERVICE = "noti_fg_service"
    const val CHANNEL_NOTI_UPDATER = "noti_updater"
    const val GROUP_NOTI_SCROBBLES = "group_scrobbles"
    const val GROUP_NOTI_DIGESTS = "group_digests"
    const val GROUP_NOTI_UPDATER = "group_updater"
    const val GROUP_NOTI_FG_SERVICE = "group_fg_service"
    const val CHANNEL_TEST_SCROBBLE_FROM_NOTI = "test_scrobble_from_noti"

    val CHANNELS_AUDILE = setOf(
        "com.mrsep.musicrecognizer.result",
        "com.mrsep.musicrecognizer.foreground_result",
        "com.mrsep.musicrecognizer.enqueued_result",
    )

    const val TV_URL = "https://kawaiidango.github.io/pano-scrobbler/tv"
    const val HOMEPAGE_URL = "https://kawaiidango.github.io"
    const val REPO_URL = "https://github.com/kawaiiDango/pano-scrobbler"
    const val CROWDIN_URL = "https://crowdin.com/project/pscrobbler"
    const val PRIVACY_POLICY_URL = "https://kawaiidango.github.io/pano-scrobbler/privacy-policy"
    const val FAQ_URL = "https://kawaiidango.github.io/pano-scrobbler/faq"

    const val DISCORD_CLIENT_ID = "1299386213114970172"
    const val EMBEDDED_SERVER_KS = "Jjs5awQQB0YjN10vKCsWPC8AXW0"

    val DEFAULT_IGNORE_ARTIST_META_WITHOUT_FALLBACK = setOf(
        "com.google.android.youtube",
        "com.vanced.android.youtube",
        "com.google.android.apps.youtube.mango",
        PACKAGE_YOUTUBE_TV,
        "com.google.android.youtube.tvkids",
        "com.liskovsoft.smarttubetv.beta",
        "com.liskovsoft.smarttubetv",
        "app.revanced.android.youtube",
        "app.rvx.android.youtube",
        "app.morphe.android.youtube",
    )

    val DEFAULT_IGNORE_ARTIST_META_WITH_FALLBACK = setOf(
        PACKAGE_SOUNDCLOUD,
        PACKAGE_NICOBOX,
        PACKAGE_YMUSIC,
        PACKAGE_NEWPIPE,
        PACKAGE_YOUTUBE_MUSIC,
        "com.vanced.android.apps.youtube.music",
        "app.revanced.android.apps.youtube.music",
        "app.morphe.android.apps.youtube.music",
    )

    val IGNORE_DURATION = setOf(
        "com.ilv.vradio",
        "com.bbc.sounds",
    )

    val PACKAGES_PIXEL_NP = setOf(
        PACKAGE_PIXEL_NP,
        PACKAGE_PIXEL_NP_R,
        PACKAGE_PIXEL_NP_AMM,
    )

    val disallowedWebviewUrls = listOf(
        "https://www.last.fm/join",
        "https://www.last.fm/settings/lostpassword",
        "https://libre.fm/register.php",
        "https://libre.fm/reset.php",
    )

    val mprisUrlSubdomains = setOf(
        HOST_APPLE_MUSIC,
        HOST_YOUTUBE_MUSIC,
    )

    var isRunningInTest = false

    val isInDemoMode get() = mainPrefsCachedValue.demoModeP

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
            coerceInputValues = true
        }
    }

    private val myBase64 by lazy {
        Base64.withPadding(PaddingOption.ABSENT)
    }

    private val numberFormat by lazy {
        NumberFormat.getInstance()
    }

    private var mainPrefsCachedValue = MainPrefs()

    val billingClientData by lazy {
        BillingClientData(
            proProductId = PRO_PRODUCT_ID,
            appName = BuildKonfig.APP_NAME,
            httpPost = { url, body ->
                Requesters.baseKtorClient.postString(url, body)
            },
            lastCheckTime = PlatformStuff.mainPrefs.data.map { it.lastLicenseCheckTime },
            deviceIdentifier = { PlatformStuff.getDeviceIdentifier() },
            setLastcheckTime = { time ->
                PlatformStuff.mainPrefs.updateData { it.copy(lastLicenseCheckTime = time) }
            },
            receipt = PlatformStuff.mainPrefs.data.map { it.receipt to it.receiptSignature },
            setReceipt = { r, s ->
                PlatformStuff.mainPrefs.updateData { it.copy(receipt = r, receiptSignature = s) }
            }
        )
    }


    val globalExceptionFlow by lazy { MutableSharedFlow<Throwable>(extraBufferCapacity = 1) }

    val globalSnackbarFlow by lazy { MutableSharedFlow<PanoSnackbarVisuals>(extraBufferCapacity = 1) }

    val globalUpdateAction by lazy { MutableStateFlow<UpdateAction?>(null) }

    fun Number.format() = numberFormat.format(this)!!

    suspend fun initializeMainPrefsCache(): MainPrefs {
        return if (mainPrefsCachedValue.version == 0)
            PlatformStuff.mainPrefs.data.first().also { mainPrefsCachedValue = it }
        else
            mainPrefsCachedValue
    }

    private fun <T> Flow<MainPrefs>.mapWithCache(
        mapBlock: (MainPrefs) -> T,
    ) = map {
        mainPrefsCachedValue = it
        mapBlock(it)
    }

    fun <T> Flow<MainPrefs>.stateInWithCache(
        scope: CoroutineScope,
        mapBlock: (MainPrefs) -> T,
    ) = mapWithCache(mapBlock)
        .stateIn(
            scope,
            started = SharingStarted.Eagerly,
            initialValue = mapBlock(mainPrefsCachedValue)
        )


    @Composable
    fun <T> Flow<MainPrefs>.collectAsStateWithInitialValue(
        mapBlock: (MainPrefs) -> T,
    ) = remember {
        mapWithCache(mapBlock)
    }.collectAsStateWithLifecycle(mapBlock(mainPrefsCachedValue))

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
        return "" + cal[Calendar.YEAR] + "_" + (cal[Calendar.MONTH] + 1) + "_" + cal[Calendar.DATE] +
                "_" + cal[Calendar.HOUR_OF_DAY] + "_" + cal[Calendar.MINUTE] + "_" + cal[Calendar.SECOND]
    }

    fun xorWithKey(dataB64: String, keyBytes: String): String {
        val data = myBase64.decode(dataB64)
        val keyBytes = keyBytes.toByteArray()
        return xorWithKeyBytes(data, keyBytes).decodeToString()
    }

    fun xorWithKeyBytes(data: ByteArray, keyBytes: ByteArray): ByteArray {
        require(keyBytes.isNotEmpty()) { "Key bytes must not be empty" }
        val out = ByteArray(data.size)
        val klen = keyBytes.size
        for (i in data.indices) {
            val a = data[i].toInt() and 0xFF
            val b = keyBytes[i % klen].toInt() and 0xFF
            out[i] = (a xor b).toByte()
        }
        return out
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

    fun Calendar.setMidnight() {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }

    fun isValidUrl(url: String): Boolean {
        return try {
            URLBuilder(url)
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
        darkTint: Boolean,
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
            draw(
                size = this.size,
                colorFilter = ColorFilter.tint(
                    color = if (darkTint) Color.Black else Color.White,
                    blendMode = BlendMode.SrcIn
                )
            )
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

    fun formatBigHyphen(artist: String, title: String) = "$artist — $title"

    fun sha256Truncated(str: String) =
        MessageDigest.getInstance("SHA-256")
            .digest(str.toByteArray())
            .take(6)
            .toByteArray()
            .let {
                myBase64.encode(it)
            }
}

val Throwable.redactedMessage: String
    get() {
        var m = this.localizedMessage ?: this.message ?: return this.toString()

        // urls
        m = m.replace("https?://\\S+".toRegex(), "<url>")

        return m
    }
