package com.arn.scrobble.pref

import android.content.Context
import com.arn.scrobble.Stuff
import com.arn.scrobble.themes.ColorPatchUtils
import com.frybits.harmony.getHarmonySharedPreferences
import hu.autsoft.krate.*
import hu.autsoft.krate.default.withDefault

class MainPrefs(context: Context) : Krate {

    override val sharedPreferences = context.getHarmonySharedPreferences(NAME)

    var scrobblerEnabled by booleanPref(PREF_MASTER).withDefault(true)
    var allowedPackages by stringSetPref(PREF_ALLOWED_PACKAGES).withDefault(setOf())
    var blockedPackages by stringSetPref(PREF_BLOCKED_PACKAGES).withDefault(setOf())
    var autoDetectApps by booleanPref(PREF_AUTO_DETECT).withDefault(true)
    var delaySecs by intPref(PREF_DELAY_SECS).withDefault(240)
    var delayPercent by intPref(PREF_DELAY_PER).withDefault(50)
    var allowedArtists by stringSetPref(PREF_ALLOWED_ARTISTS).withDefault(setOf())
    var lastfmDisabled by booleanPref(PREF_LASTFM_DISABLE).withDefault(false)
    var submitNowPlaying by booleanPref(PREF_NOW_PLAYING).withDefault(true)
    var pixelNowPlaying by booleanPref(PREF_PIXEL_NP).withDefault(true)
    var fetchAlbumArtist by booleanPref(PREF_FETCH_AA).withDefault(false)
    var searchInSource by booleanPref(PREF_SEARCH_IN_SOURCE).withDefault(false)

    var locale by stringPref(PREF_LOCALE)
    var showAlbumInRecents by booleanPref(PREF_SHOW_RECENTS_ALBUM).withDefault(false)
    var showScrobbleSources by booleanPref(PREF_SHOW_SCROBBLE_SOURCES).withDefault(true)
    var themePrimary by stringPref(PREF_THEME_PRIMARY).withDefault(ColorPatchUtils.primaryDefault)
    var themeSecondary by stringPref(PREF_THEME_SECONDARY).withDefault(ColorPatchUtils.secondaryDefault)
    var themeRandom by booleanPref(PREF_THEME_RANDOM).withDefault(false)
    var themePaletteBackground by booleanPref(PREF_THEME_PALETTE_BG).withDefault(true)
    var themeTintBackground by booleanPref(PREF_THEME_TINT_BG).withDefault(true)
    var themeDynamic by booleanPref(PREF_THEME_DYNAMIC).withDefault(false)
    var firstRun by booleanPref(PREF_ACTIVITY_FIRST_RUN).withDefault(true)
    var heroGraphDetails by booleanPref(PREF_ACTIVITY_GRAPH_DETAILS).withDefault(true)
    var lastHomePagerTab by intPref(PREF_ACTIVITY_LAST_TAB).withDefault(0)
    var lastChartsPeriod by intPref(PREF_ACTIVITY_LAST_CHARTS_PERIOD).withDefault(1)
    var lastChartsWeekTo by longPref(PREF_ACTIVITY_LAST_CHARTS_WEEK_TO).withDefault(0)
    var lastChartsWeekFrom by longPref(PREF_ACTIVITY_LAST_CHARTS_WEEK_FROM).withDefault(0)
    var scrobblesTodayCached by intPref(PREF_ACTIVITY_TODAY_SCROBBLES).withDefault(0)
    var scrobblesTotalCached by intPref(PREF_ACTIVITY_TOTAL_SCROBBLES).withDefault(0)
    var scrobblingSince by longPref(PREF_ACTIVITY_SCROBBLING_SINCE).withDefault(0)
    var lastRandomType by intPref(PREF_ACTIVITY_LAST_RANDOM_TYPE).withDefault(Stuff.TYPE_TRACKS)
    var profilePicUrlCached by stringPref(PREF_ACTIVITY_PROFILE_PIC)
    var userTopTagsFetched by booleanPref(PREF_ACTIVITY_USER_TAG_HISTORY_FETCHED).withDefault(
        false
    )
    var scrubLearnt by booleanPref(PREF_ACTIVITY_SCRUB_LEARNT).withDefault(false)
    var longPressLearnt by booleanPref(PREF_ACTIVITY_LONG_PRESS_LEARNT).withDefault(false)
    var regexEditsLearnt by booleanPref(PREF_ACTIVITY_REGEX_EDITS_LEARNT).withDefault(false)

    var searchHistory by stringSetPref(PREF_ACTIVITY_SEARCH_HISTORY)
    var tagHistory by stringSetPref(PREF_ACTIVITY_TAG_HISTORY)

    var notiLockscreen by booleanPref(PREF_LOCKSCREEN_NOTI).withDefault(false)
    var notiScrobbling by booleanPref(CHANNEL_NOTI_SCROBBLING).withDefault(true)
    var notiError by booleanPref(CHANNEL_NOTI_SCR_ERR).withDefault(true)
    var notiWeeklyDigests by booleanPref(CHANNEL_NOTI_DIGEST_WEEKLY).withDefault(true)
    var notiMonthlyDigests by booleanPref(CHANNEL_NOTI_DIGEST_MONTHLY).withDefault(true)
    var notiPendingScrobbles by booleanPref(CHANNEL_NOTI_PENDING).withDefault(true)
    var notiNewApp by booleanPref(CHANNEL_NOTI_NEW_APP).withDefault(true)
    var notiPersistent by booleanPref(CHANNEL_NOTI_PERSISTENT).withDefault(false)

    var lastfmUsername by stringPref(PREF_LASTFM_USERNAME)
    var lastfmSessKey by stringPref(PREF_LASTFM_SESS_KEY)
    var librefmUsername by stringPref(PREF_LIBREFM_USERNAME)
    var librefmSessKey by stringPref(PREF_LIBREFM_SESS_KEY)
    var listenbrainzUsername by stringPref(PREF_LISTENBRAINZ_USERNAME)
    var listenbrainzToken by stringPref(PREF_LISTENBRAINZ_TOKEN)
    var customListenbrainzUsername by stringPref(PREF_LB_CUSTOM_USERNAME)
    var customListenbrainzToken by stringPref(PREF_LB_CUSTOM_TOKEN)
    var customListenbrainzRoot by stringPref(PREF_LB_CUSTOM_ROOT).withDefault(Stuff.LISTENBRAINZ_API_ROOT)
    var gnufmUsername by stringPref(PREF_GNUFM_USERNAME)
    var gnufmSessKey by stringPref(PREF_GNUFM_SESS_KEY)
    var gnufmRoot by this.stringPref(PREF_GNUFM_ROOT).withDefault("https://")
    var acrcloudHost by stringPref(PREF_ACR_HOST)
    var acrcloudKey by stringPref(PREF_ACR_KEY)
    var acrcloudSecret by stringPref(PREF_ACR_SECRET)

    var proStatus by booleanPref(PREF_PRO_STATUS).withDefault(false)
    var proSkuJson by stringPref(PREF_PRO_SKU_JSON)
    var digestSeconds by intPref(PREF_DIGEST_SECONDS) //nullable
    var scrobbleCount by intPref(PREF_SCROBBLE_COUNT).withDefault(0)
    var firstLaunchTime by longPref(PREF_FIRST_LAUNCHED) //nullable
    var dontAskForRating by booleanPref(PREF_DONT_ASK_FOR_RATING).withDefault(false)
    var prefVersion by intPref(PREF_VERSION).withDefault(0)

    companion object {
        const val NAME = "main"
        const val PREF_MASTER = "master"
        const val PREF_ALLOWED_PACKAGES = "app_whitelist"
        const val PREF_BLOCKED_PACKAGES = "app_blacklist"
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
        const val PREF_SHOW_SCROBBLE_SOURCES = "show_scrobble_sources"
        const val PREF_SHOW_RECENTS_ALBUM = "show_album"
        const val PREF_THEME_PRIMARY = "theme_primary"
        const val PREF_THEME_SECONDARY = "theme_secondary"
        const val PREF_THEME_BACKGROUND = "theme_background"
        const val PREF_THEME_RANDOM = "theme_random"
        const val PREF_THEME_PALETTE_BG = "palette_bg"
        const val PREF_THEME_TINT_BG = "theme_tint_bg"
        const val PREF_THEME_DYNAMIC = "theme_dynamic"
        const val PREF_PRO_STATUS = "pro_status"
        const val PREF_PRO_SKU_JSON = "pro_sku_json"
        const val PREF_DIGEST_SECONDS = "digest_seconds"
        const val PREF_LOCALE = "locale"
        const val PREF_SEARCH_IN_SOURCE = "search_in_source"

        const val CHANNEL_NOTI_SCROBBLING = "noti_scrobbling"
        const val CHANNEL_NOTI_SCR_ERR = "noti_scrobble_errors"
        const val CHANNEL_NOTI_NEW_APP = "noti_new_app"
        const val CHANNEL_NOTI_PENDING = "noti_pending_scrobbles"
        const val CHANNEL_NOTI_DIGEST_WEEKLY = "noti_digest_weekly"
        const val CHANNEL_NOTI_DIGEST_MONTHLY = "noti_digest_monthly"
        const val CHANNEL_NOTI_PERSISTENT = "noti_persistent"

        const val ACTIVITY_PREFS_NAME = "activity_preferences"
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
        const val PREF_ACTIVITY_REGEX_EDITS_LEARNT = "regex_edits_learnt"
        const val PREF_ACTIVITY_USER_TAG_HISTORY_FETCHED = "user_tag_history_fetched"

        const val PREF_SCROBBLE_COUNT = "scrobble_count"
        const val PREF_FIRST_LAUNCHED = "date_firstlaunch"
        const val PREF_DONT_ASK_FOR_RATING = "dontshowagain"
        const val PREF_VERSION = "version"
    }
}