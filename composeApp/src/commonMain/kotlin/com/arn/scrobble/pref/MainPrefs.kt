@file:OptIn(ExperimentalSerializationApi::class)

package com.arn.scrobble.pref

import androidx.datastore.core.DataMigration
import androidx.datastore.core.Serializer
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.CookieSerializable
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.SearchType
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.edits.RegexPreset
import com.arn.scrobble.edits.RegexPresets
import com.arn.scrobble.themes.ContrastMode
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.themes.ThemeUtils
import com.arn.scrobble.ui.GridMode
import com.arn.scrobble.ui.SerializableWindowState
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.getSystemCountryCode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration.Companion.days


@Serializable
data class MainPrefs(
    val scrobblerEnabled: Boolean = true,
    val allowedPackages: Set<String> = emptySet(),
    val blockedPackages: Set<String> = emptySet(),
    val allowedAutomationPackages: Set<String> = emptySet(),
    val seenApps: Map<String, String> = emptyMap(),
    private val regexPresetsApps: Map<String, Set<String>> = mapOf(
        RegexPreset.parse_title.name to Stuff.DEFAULT_IGNORE_ARTIST_META_WITHOUT_FALLBACK,
        RegexPreset.parse_title_with_fallback.name to Stuff.DEFAULT_IGNORE_ARTIST_META_WITH_FALLBACK
    ),
    private val autoDetectApps: Boolean = true,
    private val delaySecs: Int = PREF_DELAY_SECS_DEFAULT,
    private val delayPercent: Int = PREF_DELAY_PER_DEFAULT,
    private val minDurationSecs: Int = PREF_MIN_DURATON_SECS_DEFAULT,
    private val scrobbleSpotifyRemote: Boolean = false,
    val linkHeartButtonToRating: Boolean = false,
    val preventDuplicateAmbientScrobbles: Boolean = false,
    val submitNowPlaying: Boolean = true,
    val fetchAlbum: Boolean = false,
    val searchInSource: Boolean = false,
    val lastSearchType: SearchType = SearchType.GLOBAL,
    val firstDayOfWeek: Int = -1,
    private val demoMode: Boolean = false,
    val showScrobbleSources: Boolean = true,
    val themeName: String = ThemeUtils.defaultThemeName,
    val themeContrast: ContrastMode = ContrastMode.LOW,
    val themeDynamic: Boolean = false,
    val themeRandom: Boolean = false,
    val themeDayNight: DayNightMode = DayNightMode.DARK,
    val appListWasRun: Boolean = false,
    val lastHomePagerTab: Int = 0,
    val lastChartsPeriodType: TimePeriodType = TimePeriodType.CONTINUOUS,
    val lastChartsLastfmPeriod: TimePeriod = TimePeriod(LastfmPeriod.MONTH),
    val lastChartsListenBrainzPeriod: TimePeriod = TimePeriod(LastfmPeriod.OVERALL),
    val lastChartsCustomPeriod: TimePeriod = TimePeriod(1767225600000, 1798761600000), // 2026
    val currentAccountType: AccountType = AccountType.LASTFM,
    val scrobbleAccounts: List<UserAccountSerializable> = emptyList(),
    val drawerData: Map<AccountType, DrawerData> = emptyMap(),
    val randomType: Int = Stuff.TYPE_TRACKS,
    val userTopTagsFetched: Boolean = false,
    val collageSkipMissing: Boolean = false,
    val collageUsername: Boolean = true,
    val collageText: Boolean = false,
    val collageSize: Int = 3,
    val collageCaptions: Boolean = true,
    val collageBorders: Boolean = true,
    val lastFullIndexTime: Long? = null,
    val lastDeltaIndexTime: Long? = null,
    val lastFullIndexedScrobbleTime: Long? = null,
    val lastDeltaIndexedScrobbleTime: Long? = null,
    val gridMode: GridMode = GridMode.GRID,
    val regexLearnt: Boolean = false,
    val otherPlatformsLearnt: Boolean = false,
    val squarePhotoLearnt: Boolean = false,
    val spotifyConsentLearnt: Boolean = false,
    val changelogSeenHashcode: Int? = null,
    val searchHistory: List<String> = emptyList(),
    val tagHistory: List<String> = emptyList(),
    val notiWeeklyDigests: Boolean = true,
    val notiMonthlyDigests: Boolean = true,
    val notiPersistent: Boolean = false,
    val digestSeconds: Int? = null,
    val lastReviewPromptTime: Long? = null,
    val lastUpdateCheckTime: Long? = null,
    val autoUpdates: Boolean = true,
    val version: Int = 0,
    val hiddenTags: Set<String> = emptySet(),
    val pinnedFriends: Map<AccountType, List<UserCached>> = emptyMap(),
    val spotifyAccessToken: String = "bad_token",
    val spotifyAccessTokenExpires: Long = -1,
    val spotifyArtistSearchApproximate: Boolean = false,
    val receipt: String? = null,
    val receiptSignature: String? = null,
    val lastLicenseCheckTime: Long = -1,
    val searchUrlTemplate: String = Stuff.DEFAULT_SEARCH_URL,
    private val usePlayFromSearch: Boolean = true,
    val trayIconTheme: DayNightMode = DayNightMode.SYSTEM,
    val cookies: Map<String, CookieSerializable> = emptyMap(),
    // keep this as a string and not as an enum, in case I delete presets later
    val regexPresets: Set<String> = RegexPresets.defaultPresets.map { it.name }.toSet(),
    val windowState: SerializableWindowState? = null,
    private val itunesCountry: String? = null,
    val spotifyApi: Boolean = false,
    private val spotifyCountry: String? = null,
    val tidalSteelSeriesApi: Boolean = true,
    val deezerApi: Boolean = true,
    val lastfmApiAlways: Boolean = false,
    private val logToFileOnAndroidSince: Long = -1,
    val extractFirstArtistPackages: Set<String> = emptySet(),
    val discordRpc: DiscordRpcSettings = DiscordRpcSettings(),
) {

    @Serializable
    data class DiscordRpcSettings(
        val enabled: Boolean = false,
        val statusLine: Int = 2,
        val albumArt: Boolean = true,
        val albumArtFromNowPlaying: Boolean = true,
        val line1Format: String = $$"$title",
        val line2Format: String = $$"$artist",
        val line3Format: String = $$"$album",
        val nameFormat: String = $$"$mediaPlayer",
        val showPausedForSecs: Int = 60,
        val detailsUrl: Boolean = true,
    )


    @Serializable
    data class Public(
        @JsonNames("master")
        val scrobblerEnabled: Boolean = defaultMainPrefs.scrobblerEnabled,
        @JsonNames("delay_secs")
        val delaySecs: Int = PREF_DELAY_SECS_DEFAULT,
        @JsonNames("delay_per")
        val delayPercent: Int = defaultMainPrefs.delayPercent,
        val minDurationSecs: Int = defaultMainPrefs.minDurationSecs,
        @JsonNames("now_playing")
        val submitNowPlaying: Boolean = defaultMainPrefs.submitNowPlaying,
        @JsonNames("fetch_album")
        val fetchAlbum: Boolean = defaultMainPrefs.fetchAlbum,
        @JsonNames("auto_detect")
        val autoDetectApps: Boolean = defaultMainPrefs.autoDetectApps,
        @JsonNames("show_scrobble_sources")
        val showScrobbleSources: Boolean = defaultMainPrefs.showScrobbleSources,
        @JsonNames("link_heart_button_to_rating")
        val linkHeartButtonToRating: Boolean = defaultMainPrefs.linkHeartButtonToRating,
        val themeName: String = defaultMainPrefs.themeName,
        val themeContrast: ContrastMode = defaultMainPrefs.themeContrast,
        val themeRandom: Boolean = defaultMainPrefs.themeRandom,
        val themeDayNight: DayNightMode = defaultMainPrefs.themeDayNight,
        @JsonNames("search_in_source")
        val searchInSource: Boolean = defaultMainPrefs.searchInSource,
        @JsonNames("scrobble_spotify_remote")
        val scrobbleSpotifyRemote: Boolean = defaultMainPrefs.scrobbleSpotifyRemote,
        @JsonNames("spotify_artist_search_approximate")
        val spotifyArtistSearchApproximate: Boolean = defaultMainPrefs.spotifyArtistSearchApproximate,
        @JsonNames("prevent_duplicate_ambient_scrobbles")
        val preventDuplicateAmbientScrobbles: Boolean = defaultMainPrefs.preventDuplicateAmbientScrobbles,
        val firstDayOfWeek: Int = defaultMainPrefs.firstDayOfWeek,
        val searchUrlTemplate: String = defaultMainPrefs.searchUrlTemplate,
        val usePlayFromSearch: Boolean = defaultMainPrefs.usePlayFromSearch,
        val regexPresets: Set<String> = defaultMainPrefs.regexPresets,
        val extractFirstArtistPackages: Set<String> = defaultMainPrefs.extractFirstArtistPackages,
        @JsonNames("app_whitelist")
        val allowedPackages: Set<String> = defaultMainPrefs.allowedPackages,
        val regexPresetsApps: Map<String, Set<String>> = defaultMainPrefs.regexPresetsApps,
        val spotifyApi: Boolean = defaultMainPrefs.spotifyApi,
        val spotifyCountry: String? = defaultMainPrefs.spotifyCountry,
        val tidalSteelSeriesApi: Boolean = defaultMainPrefs.tidalSteelSeriesApi,
        val deezerApi: Boolean = defaultMainPrefs.deezerApi,
        val lastfmApiAlways: Boolean = defaultMainPrefs.lastfmApiAlways,
        val discordRpc: DiscordRpcSettings = defaultMainPrefs.discordRpc,
    )

    val autoDetectAppsP
        get() = if (!PanoNotifications.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_NEW_APP))
            false
        else
            autoDetectApps

    val delaySecsP
        get() = delaySecs.coerceIn(PREF_DELAY_SECS_MIN, PREF_DELAY_SECS_MAX)

    val delayPercentP
        get() = delayPercent.coerceIn(PREF_DELAY_PER_MIN, PREF_DELAY_PER_MAX)

    val minDurationSecsP
        get() = minDurationSecs.coerceIn(PREF_MIN_DURATON_SECS_MIN, PREF_MIN_DURATON_SECS_MAX)

    val demoModeP
        get() = demoMode && BuildKonfig.DEBUG

    val lastMaxIndexedScrobbleTime
        get() = lastDeltaIndexedScrobbleTime ?: lastFullIndexedScrobbleTime

    val lastMaxIndexTime
        get() = lastDeltaIndexTime ?: lastFullIndexTime

    val spotifyCountryP
        get() = spotifyCountry ?: LocaleUtils.getSystemCountryCode()

    val itunesCountryP
        get() = itunesCountry ?: LocaleUtils.getSystemCountryCode()

    val scrobbleSpotifyRemoteP
        get() = !PlatformStuff.isTv && scrobbleSpotifyRemote

    val usePlayFromSearchP
        get() = PlatformStuff.isTv || !PlatformStuff.isDesktop && usePlayFromSearch

    val currentAccount
        get() = scrobbleAccounts.firstOrNull { it.type == currentAccountType }

    val logToFileOnAndroid
        get() = (System.currentTimeMillis() - logToFileOnAndroidSince) <= 15.days.inWholeMilliseconds

    fun allowOrBlockAppCopied(appId: String, allow: Boolean): MainPrefs {
        //create copies
        val aSet = allowedPackages.toMutableSet()
        val bSet = blockedPackages.toMutableSet()

        if (allow)
            aSet += appId
        else
            bSet += appId
        bSet.removeAll(aSet) // allowlist takes over blocklist for conflicts

        return copy(
            allowedPackages = aSet,
            blockedPackages = bSet
        )
    }

    fun getRegexPresetApps(regexPreset: RegexPreset): Set<String> =
        regexPresetsApps.getOrDefault(regexPreset.name, emptySet())

    fun updateFromPublicPrefs(prefs: Public) = copy(
        scrobblerEnabled = prefs.scrobblerEnabled,
        delaySecs = prefs.delaySecs.coerceIn(PREF_DELAY_SECS_MIN, PREF_DELAY_SECS_MAX),
        delayPercent = prefs.delayPercent.coerceIn(PREF_DELAY_PER_MIN, PREF_DELAY_PER_MAX),
        submitNowPlaying = prefs.submitNowPlaying,
        fetchAlbum = prefs.fetchAlbum,
        autoDetectApps = prefs.autoDetectApps,
        showScrobbleSources = prefs.showScrobbleSources,
        linkHeartButtonToRating = prefs.linkHeartButtonToRating,
        themeName = prefs.themeName,
        themeContrast = prefs.themeContrast,
        themeRandom = prefs.themeRandom,
        themeDayNight = prefs.themeDayNight,
        searchInSource = prefs.searchInSource,
        scrobbleSpotifyRemote = prefs.scrobbleSpotifyRemote,
        spotifyArtistSearchApproximate = prefs.spotifyArtistSearchApproximate,
        preventDuplicateAmbientScrobbles = prefs.preventDuplicateAmbientScrobbles,
        firstDayOfWeek = prefs.firstDayOfWeek,
        searchUrlTemplate = prefs.searchUrlTemplate,
        allowedPackages = prefs.allowedPackages,
        regexPresets = prefs.regexPresets,
        extractFirstArtistPackages = prefs.extractFirstArtistPackages,
        regexPresetsApps = prefs.regexPresetsApps,
        discordRpc = prefs.discordRpc,
        spotifyApi = prefs.spotifyApi,
        spotifyCountry = prefs.spotifyCountry,
        tidalSteelSeriesApi = prefs.tidalSteelSeriesApi,
        deezerApi = prefs.deezerApi,
        lastfmApiAlways = prefs.lastfmApiAlways,
    )

    fun toPublicPrefs() = Public(
        scrobblerEnabled = scrobblerEnabled,
        delaySecs = delaySecsP,
        delayPercent = delayPercentP,
        minDurationSecs = minDurationSecsP,
        submitNowPlaying = submitNowPlaying,
        fetchAlbum = fetchAlbum,
        autoDetectApps = autoDetectAppsP,
        showScrobbleSources = showScrobbleSources,
        linkHeartButtonToRating = linkHeartButtonToRating,
        themeName = themeName,
        themeContrast = themeContrast,
        themeRandom = themeRandom,
        themeDayNight = themeDayNight,
        searchInSource = searchInSource,
        scrobbleSpotifyRemote = scrobbleSpotifyRemote,
        spotifyArtistSearchApproximate = spotifyArtistSearchApproximate,
        preventDuplicateAmbientScrobbles = preventDuplicateAmbientScrobbles,
        firstDayOfWeek = firstDayOfWeek,
        searchUrlTemplate = searchUrlTemplate,
        allowedPackages = allowedPackages,
        regexPresets = regexPresets,
        extractFirstArtistPackages = extractFirstArtistPackages,
        regexPresetsApps = regexPresetsApps,
        discordRpc = discordRpc,
        spotifyApi = spotifyApi,
        spotifyCountry = spotifyCountry,
        tidalSteelSeriesApi = tidalSteelSeriesApi,
        deezerApi = deezerApi,
        lastfmApiAlways = lastfmApiAlways,
    )


    companion object {
        private val defaultMainPrefs = MainPrefs()

        val dataStoreSerializer = object : Serializer<MainPrefs> {
            override val defaultValue = defaultMainPrefs

            override suspend fun readFrom(input: InputStream) =
                try {
                    Stuff.myJson.decodeFromStream<MainPrefs>(input)
                } catch (e: SerializationException) {
                    Logger.e(e) { "MainPrefs deserialization error" }
                    defaultValue
                }

            override suspend fun writeTo(
                t: MainPrefs,
                output: OutputStream,
            ) = Stuff.myJson.encodeToStream(t, output)
        }

        const val FILE_NAME = "main-prefs.json"
        const val PREF_DELAY_SECS_MIN = 30
        const val PREF_DELAY_SECS_MAX = 360
        private const val PREF_DELAY_SECS_DEFAULT = 180
        private const val PREF_DELAY_PER_DEFAULT = 50
        const val PREF_DELAY_PER_MIN = 30
        const val PREF_DELAY_PER_MAX = 95
        private const val PREF_MIN_DURATON_SECS_DEFAULT = 30
        const val PREF_MIN_DURATON_SECS_MIN = 10
        const val PREF_MIN_DURATON_SECS_MAX = 60

        fun migrations() = listOf<DataMigration<MainPrefs>>(
            MainPrefsMigration6(),
        )
    }
}