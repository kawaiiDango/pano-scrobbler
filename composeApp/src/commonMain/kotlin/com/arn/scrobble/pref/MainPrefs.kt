package com.arn.scrobble.pref

import androidx.datastore.core.Serializer
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.CookieSerializable
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.SearchType
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.edits.RegexPresets
import com.arn.scrobble.themes.ContrastMode
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.themes.ThemeUtils
import com.arn.scrobble.ui.GridMode
import com.arn.scrobble.ui.SerializableWindowState
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream


@Serializable
data class MainPrefs(
    val scrobblerEnabled: Boolean = true,
    val allowedPackages: Set<String> = emptySet(),
    val blockedPackages: Set<String> = emptySet(),
    val allowedAutomationPackages: Set<String> = emptySet(),
    val seenApps: Map<String, String> = emptyMap(),
    private val autoDetectApps: Boolean = true,
    private val delaySecs: Int = PREF_DELAY_SECS_DEFAULT,
    private val delayPercent: Int = PREF_DELAY_PER_DEFAULT,
    private val minDurationSecs: Int = PREF_MIN_DURATON_SECS_DEFAULT,
    val scrobbleSpotifyRemote: Boolean = false,
    val linkHeartButtonToRating: Boolean = false,
    val preventDuplicateAmbientScrobbles: Boolean = false,
    val submitNowPlaying: Boolean = true,
    val fetchAlbum: Boolean = false,
    val searchInSource: Boolean = false,
    val crashReporterEnabled: Boolean = true,
    val lastSearchType: SearchType = SearchType.GLOBAL,
    val firstDayOfWeek: Int = -1,
    val lastInteractiveTime: Long? = null,
    private val demoMode: Boolean = false,
    val locale: String? = null,
    val showAlbumsInRecents: Boolean = false,
    val showScrobbleSources: Boolean = true,
    val themeName: String = ThemeUtils.defaultTheme.name,
    val themeContrast: ContrastMode = ContrastMode.LOW,
    val themeDynamic: Boolean = false,
    val themeDayNight: DayNightMode = DayNightMode.DARK,
    val appListWasRun: Boolean = !(allowedPackages.isEmpty() && blockedPackages.isEmpty()),
    val lastHomePagerTab: Int = 0,
    val lastChartsPeriodType: TimePeriodType = TimePeriodType.CONTINUOUS,
    val lastChartsLastfmPeriodSelected: TimePeriod = TimePeriod(LastfmPeriod.MONTH),
    val lastChartsCustomPeriod: TimePeriod = TimePeriod(1577836800000L, 1609459200000L), // 2020
    val currentAccountType: AccountType = AccountType.LASTFM,
    val scrobbleAccounts: List<UserAccountSerializable> = emptyList(),
    val drawerData: Map<AccountType, DrawerData> = emptyMap(),
    val lastRandomType: Int = Stuff.TYPE_TRACKS,
    val lastKillCheckTime: Long = -1,
    val userTopTagsFetched: Boolean = false,
    val collageSkipMissing: Boolean = false,
    val collageUsername: Boolean = true,
    val collageText: Boolean = false,
    val collageSize: Int = 3,
    val collageCaptions: Boolean = true,
    val lastFullIndexTime: Long? = null,
    val lastDeltaIndexTime: Long? = null,
    val lastFullIndexedScrobbleTime: Long? = null,
    val lastDeltaIndexedScrobbleTime: Long? = null,
    val gridMode: GridMode = GridMode.GRID,
    val regexLearnt: Boolean = false,
    val regexEditsLearnt: Boolean = false,
    val squarePhotoLearnt: Boolean = false,
    val changelogSeenHashcode: Int? = null,
    val searchHistory: List<String> = emptyList(),
    val tagHistory: List<String> = emptyList(),
    val notificationsOnLockscreen: Boolean = false,
    val notiScrobbling: Boolean = true,
    val notiError: Boolean = true,
    val notiWeeklyDigests: Boolean = true,
    val notiMonthlyDigests: Boolean = true,
    val notiPendingScrobbles: Boolean = true,
    val notiNewApp: Boolean = true,
    val notiPersistent: Boolean = false,
    val digestSeconds: Int? = null,
    val lastReviewPromptTime: Long? = null,
    val lastUpdateCheckTime: Long? = null,
    val checkForUpdates: Boolean = true,
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
    val trayIconTheme: DayNightMode = DayNightMode.SYSTEM,
    val cookies: Map<String, CookieSerializable> = emptyMap(),
    // keep this as a string and not as an enum, in case i delete presets later
    val regexPresets: Set<String> = RegexPresets.defaultPresets.map { it.name }.toSet(),
    val windowState: SerializableWindowState? = null,
) {

    val autoDetectAppsP
        get() = if (!PlatformStuff.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_NEW_APP))
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
        get() = demoMode && PlatformStuff.isDebug

    val lastMaxIndexedScrobbleTime
        get() = lastDeltaIndexedScrobbleTime ?: lastFullIndexedScrobbleTime

    val lastMaxIndexTime
        get() = lastDeltaIndexTime ?: lastFullIndexTime

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

    fun updateFromPublicPrefs(prefs: MainPrefsPublic) = copy(
        scrobblerEnabled = prefs.scrobblerEnabled,
        delaySecs = prefs.delaySecs.coerceIn(PREF_DELAY_SECS_MIN, PREF_DELAY_SECS_MAX),
        delayPercent = prefs.delayPercent.coerceIn(PREF_DELAY_PER_MIN, PREF_DELAY_PER_MAX),
        submitNowPlaying = prefs.submitNowPlaying,
        fetchAlbum = prefs.fetchAlbum,
        autoDetectApps = prefs.autoDetectApps,
        showAlbumsInRecents = prefs.showAlbumsInRecents,
        showScrobbleSources = prefs.showScrobbleSources,
        linkHeartButtonToRating = prefs.linkHeartButtonToRating,
        notificationsOnLockscreen = prefs.notificationsOnLockscreen,
        themeName = prefs.themeName,
        themeContrast = prefs.themeContrast,
        themeDayNight = prefs.themeDayNight,
        searchInSource = prefs.searchInSource,
        scrobbleSpotifyRemote = prefs.scrobbleSpotifyRemote,
        spotifyArtistSearchApproximate = prefs.spotifyArtistSearchApproximate,
        preventDuplicateAmbientScrobbles = prefs.preventDuplicateAmbientScrobbles,
        firstDayOfWeek = prefs.firstDayOfWeek,
        searchUrlTemplate = prefs.searchUrlTemplate,
        allowedPackages = prefs.allowedPackages,
        regexPresets = prefs.regexPresets,
    )

    fun toPublicPrefs() = MainPrefsPublic(
        scrobblerEnabled = this.scrobblerEnabled,
        delaySecs = this.delaySecsP,
        delayPercent = this.delayPercentP,
        minDurationSecs = this.minDurationSecsP,
        submitNowPlaying = this.submitNowPlaying,
        fetchAlbum = this.fetchAlbum,
        autoDetectApps = this.autoDetectAppsP,
        showAlbumsInRecents = this.showAlbumsInRecents,
        showScrobbleSources = this.showScrobbleSources,
        linkHeartButtonToRating = this.linkHeartButtonToRating,
        notificationsOnLockscreen = this.notificationsOnLockscreen,
        themeName = this.themeName,
        themeContrast = this.themeContrast,
        themeDayNight = this.themeDayNight,
        searchInSource = this.searchInSource,
        scrobbleSpotifyRemote = this.scrobbleSpotifyRemote,
        spotifyArtistSearchApproximate = this.spotifyArtistSearchApproximate,
        preventDuplicateAmbientScrobbles = this.preventDuplicateAmbientScrobbles,
        firstDayOfWeek = this.firstDayOfWeek,
        searchUrlTemplate = this.searchUrlTemplate,
        allowedPackages = this.allowedPackages,
        regexPresets = this.regexPresets,
    )

    companion object {
        const val FILE_NAME = "main-prefs.json"

        const val PREF_DELAY_SECS_MIN = 30
        const val PREF_DELAY_SECS_MAX = 360
        const val PREF_DELAY_SECS_DEFAULT = 180
        const val PREF_DELAY_PER_DEFAULT = 50
        const val PREF_DELAY_PER_MIN = 30
        const val PREF_DELAY_PER_MAX = 95
        const val PREF_MIN_DURATON_SECS_DEFAULT = 30
        const val PREF_MIN_DURATON_SECS_MIN = 10
        const val PREF_MIN_DURATON_SECS_MAX = 60
    }
}

object MainPrefsSerializer : Serializer<MainPrefs> {
    override val defaultValue = MainPrefs()

    override suspend fun readFrom(input: InputStream) =
        try {
            Stuff.myJson.decodeFromStream<MainPrefs>(input)
        } catch (exception: SerializationException) {
            defaultValue
        }

    override suspend fun writeTo(
        t: MainPrefs,
        output: OutputStream,
    ) = Stuff.myJson.encodeToStream(t, output)
}


@Serializable
data class MainPrefsPublic(
    val scrobblerEnabled: Boolean,
    val delaySecs: Int,
    val delayPercent: Int,
    val minDurationSecs: Int = MainPrefs.PREF_MIN_DURATON_SECS_DEFAULT,
    val submitNowPlaying: Boolean,
    val fetchAlbum: Boolean,
    val autoDetectApps: Boolean,
    val showAlbumsInRecents: Boolean,
    val showScrobbleSources: Boolean,
    val linkHeartButtonToRating: Boolean,
    val notificationsOnLockscreen: Boolean,
    val themeName: String,
    val themeContrast: ContrastMode,
    val themeDayNight: DayNightMode,
    val searchInSource: Boolean,
    val scrobbleSpotifyRemote: Boolean,
    val spotifyArtistSearchApproximate: Boolean,
    val preventDuplicateAmbientScrobbles: Boolean,
    val firstDayOfWeek: Int,
    val searchUrlTemplate: String = Stuff.DEFAULT_SEARCH_URL,
    val regexPresets: Set<String> = RegexPresets.defaultPresets.map { it.name }.toSet(),
    val allowedPackages: Set<String>,
)
