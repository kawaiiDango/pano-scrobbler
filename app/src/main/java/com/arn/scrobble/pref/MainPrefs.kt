package com.arn.scrobble.pref

import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.arn.scrobble.MetadataUtils
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.isChannelEnabled
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.friends.UserSerializable
import com.arn.scrobble.search.SearchResultsAdapter
import com.arn.scrobble.themes.ColorPatchUtils
import com.frybits.harmony.getHarmonySharedPreferences
import de.umass.lastfm.Period
import hu.autsoft.krate.Krate
import hu.autsoft.krate.booleanPref
import hu.autsoft.krate.default.withDefault
import hu.autsoft.krate.intPref
import hu.autsoft.krate.kotlinx.kotlinxPref
import hu.autsoft.krate.longPref
import hu.autsoft.krate.stringPref
import hu.autsoft.krate.stringSetPref

class MainPrefs(context: Context) : Krate {

    override val sharedPreferences = context.getHarmonySharedPreferences(NAME)

    private val nm by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    var scrobblerEnabled by booleanPref(PREF_MASTER).withDefault(true)
    var allowedPackages by stringSetPref(PREF_ALLOWED_PACKAGES).withDefault(setOf())
    var blockedPackages by stringSetPref(PREF_BLOCKED_PACKAGES).withDefault(setOf())

    private var _autoDetectApps by booleanPref(PREF_AUTO_DETECT).withDefault(true)
    val autoDetectApps
        get() = if (Stuff.isTv || !nm.isChannelEnabled(sharedPreferences, CHANNEL_NOTI_NEW_APP))
            false
        else
            _autoDetectApps

    private var _delaySecs by intPref(PREF_DELAY_SECS).withDefault(PREF_DELAY_SECS_DEFAULT)
    val delaySecs
        get() = _delaySecs.coerceIn(PREF_DELAY_SECS_MIN, PREF_DELAY_SECS_MAX)

    private var _delayPercent by intPref(PREF_DELAY_PER).withDefault(PREF_DELAY_PER_DEFAULT)
    val delayPercent
        get() = _delayPercent.coerceIn(PREF_DELAY_PER_MIN, PREF_DELAY_PER_MAX)

    var scrobbleSpotifyRemote by booleanPref(PREF_SCROBBLE_SPOTIFY_REMOTE).withDefault(false)
    var allowedArtists by stringSetPref(PREF_ALLOWED_ARTISTS).withDefault(setOf())
    var lastfmDisabled by booleanPref(PREF_LASTFM_DISABLE).withDefault(false)
    var submitNowPlaying by booleanPref(PREF_NOW_PLAYING).withDefault(true)
    var fetchAlbumArtist by booleanPref(PREF_FETCH_AA).withDefault(false)
    var fetchAlbum by booleanPref(PREF_FETCH_ALBUM).withDefault(false)
    var searchInSource by booleanPref(PREF_SEARCH_IN_SOURCE).withDefault(false)
    var crashlyticsEnabled by booleanPref(PREF_CRASHLYTICS_ENABLED).withDefault(true)
    var searchType by kotlinxPref<SearchResultsAdapter.SearchType>(PREF_SEARCH_TYPE).withDefault(
        SearchResultsAdapter.SearchType.GLOBAL
    )

    var locale by stringPref(PREF_LOCALE)
    var showAlbumInRecents by booleanPref(PREF_SHOW_RECENTS_ALBUM).withDefault(false)
    var showScrobbleSources by booleanPref(PREF_SHOW_SCROBBLE_SOURCES).withDefault(true)
    var themePrimary by stringPref(PREF_THEME_PRIMARY).withDefault(ColorPatchUtils.primaryDefault)
    var themeSecondary by stringPref(PREF_THEME_SECONDARY).withDefault(ColorPatchUtils.secondaryDefault)
    var themeRandom by booleanPref(PREF_THEME_RANDOM).withDefault(false)
    var themeTintBackground by booleanPref(PREF_THEME_TINT_BG).withDefault(true)
    var themeDynamic by booleanPref(PREF_THEME_DYNAMIC).withDefault(false)
    var themeDayNight by intPref(PREF_THEME_DAY_NIGHT).withDefault(AppCompatDelegate.MODE_NIGHT_YES)
    var appListWasRun by booleanPref(PREF_ACTIVITY_APP_LIST_WAS_RUN).withDefault(
        !(allowedPackages.isEmpty() && blockedPackages.isEmpty())
    )
    var lastHomePagerTab by intPref(PREF_ACTIVITY_LAST_TAB).withDefault(0)
    var lastChartsPeriodType by stringPref(PREF_ACTIVITY_LAST_CHARTS_PERIOD_TYPE).withDefault(
        TimePeriodType.CONTINUOUS.name
    )
    var lastChartsPeriodSelectedJson by kotlinxPref<TimePeriod>(
        PREF_ACTIVITY_LAST_CHARTS_PERIOD_SELECTED
    ).withDefault(TimePeriod(context, Period.ONE_MONTH))
    var currentUser by kotlinxPref<UserAccountSerializable>(PREF_CURRENT_USER)
    var scrobbleAccounts by kotlinxPref<List<UserAccountSerializable>>(PREF_SCROBBLE_ACCOUNTS).withDefault(listOf())
    var scrobblesTodayCached by intPref(PREF_ACTIVITY_TODAY_SCROBBLES).withDefault(0)
    var scrobblesTotalCached by intPref(PREF_ACTIVITY_TOTAL_SCROBBLES).withDefault(0)
    var scrobblingSince by longPref(PREF_ACTIVITY_SCROBBLING_SINCE).withDefault(0)
    var lastRandomType by intPref(PREF_ACTIVITY_LAST_RANDOM_TYPE).withDefault(Stuff.TYPE_TRACKS)
    var lastKillCheckTime by longPref(PREF_ACTIVITY_LAST_KILL_CHECK_TIME).withDefault(-1)
    var profilePicUrlCached by stringPref(PREF_ACTIVITY_PROFILE_PIC)
    var userTopTagsFetched by booleanPref(PREF_ACTIVITY_USER_TAG_HISTORY_FETCHED).withDefault(
        false
    )
    var collageSkipMissing by booleanPref(PREF_COLLAGE_SKIP_MISSING).withDefault(false)
    var collageUsername by booleanPref(PREF_COLLAGE_USERNAME).withDefault(true)
    var collageText by booleanPref(PREF_COLLAGE_TEXT).withDefault(false)
    var collageSize by intPref(PREF_COLLAGE_SIZE).withDefault(3)
    var collageCaptions by booleanPref(PREF_COLLAGE_CAPTIONS).withDefault(true)

    var lastFullIndexTime by longPref(PREF_ACTIVITY_LAST_FULL_INDEXED_TIME)
    var lastDeltaIndexTime by longPref(PREF_ACTIVITY_LAST_DELTA_INDEXED_TIME)
    var lastFullIndexedScrobbleTime by longPref(PREF_ACTIVITY_LAST_FULL_INDEXED_SCROBBLE_TIME)
    var lastDeltaIndexedScrobbleTime by longPref(PREF_ACTIVITY_LAST_DELTA_INDEXED_SCROBBLE_TIME)

    val lastMaxIndexedScrobbleTime
        get() = lastDeltaIndexedScrobbleTime ?: lastFullIndexedScrobbleTime
    val lastMaxIndexTime get() = lastDeltaIndexTime ?: lastFullIndexTime

    var gridColumnsToAdd by intPref(PREF_ACTIVITY_GRID_COLUMNS_TO_ADD).withDefault(0)
    var gridSingleColumn by booleanPref(PREF_ACTIVITY_GRID_SINGLE_COLUMN).withDefault(false)
    var scrubLearnt by booleanPref(PREF_ACTIVITY_SCRUB_LEARNT).withDefault(false)
    var longPressLearnt by booleanPref(PREF_ACTIVITY_LONG_PRESS_LEARNT).withDefault(false)
    var regexEditsLearnt by booleanPref(PREF_ACTIVITY_REGEX_EDITS_LEARNT).withDefault(false)
    var reorderFriendsLearnt by booleanPref(PREF_ACTIVITY_REORDER_FRIENDS_LEARNT).withDefault(false)
    var gridPinchLearnt by booleanPref(PREF_ACTIVITY_GRID_PINCH_LEARNT).withDefault(false)

    var searchHistory by stringSetPref(PREF_ACTIVITY_SEARCH_HISTORY)
    var tagHistory by stringSetPref(PREF_ACTIVITY_TAG_HISTORY)

    var notiLockscreen by booleanPref(PREF_LOCKSCREEN_NOTI).withDefault(false)
    var notiScrobbling by booleanPref(CHANNEL_NOTI_SCROBBLING).withDefault(true)
    var notiError by booleanPref(CHANNEL_NOTI_SCR_ERR).withDefault(true)
    var notiWeeklyDigests by booleanPref(CHANNEL_NOTI_DIGEST_WEEKLY).withDefault(true)
    var notiMonthlyDigests by booleanPref(CHANNEL_NOTI_DIGEST_MONTHLY).withDefault(true)
    var notiPendingScrobbles by booleanPref(CHANNEL_NOTI_PENDING).withDefault(true)
    var notiNewApp by booleanPref(CHANNEL_NOTI_NEW_APP).withDefault(true)
    var notiPersistent by booleanPref(CHANNEL_NOTI_PERSISTENT).withDefault(Stuff.forcePersistentNoti)

    var lastfmUsername by stringPref(PREF_LASTFM_USERNAME)
    var lastfmSessKey by stringPref(PREF_LASTFM_SESS_KEY)
    var librefmUsername by stringPref(PREF_LIBREFM_USERNAME)
    var librefmSessKey by stringPref(PREF_LIBREFM_SESS_KEY)
    var listenbrainzUsername by stringPref(PREF_LISTENBRAINZ_USERNAME)
    var listenbrainzToken by stringPref(PREF_LISTENBRAINZ_TOKEN)
    var customListenbrainzUsername by stringPref(PREF_LB_CUSTOM_USERNAME)
    var customListenbrainzToken by stringPref(PREF_LB_CUSTOM_TOKEN)
    var customListenbrainzRoot by stringPref(PREF_LB_CUSTOM_ROOT).withDefault(Stuff.LISTENBRAINZ_API_ROOT)
    var customListenbrainzTlsNoVerify by booleanPref(PREF_LB_CUSTOM_TLS_NO_VERIFY).withDefault(false)
    var gnufmUsername by stringPref(PREF_GNUFM_USERNAME)
    var gnufmSessKey by stringPref(PREF_GNUFM_SESS_KEY)
    var gnufmRoot by this.stringPref(PREF_GNUFM_ROOT).withDefault("https://")
    var gnufmTlsNoVerify by booleanPref(PREF_GNUFM_TLS_NO_VERIFY).withDefault(false)
    var acrcloudHost by stringPref(PREF_ACR_HOST)
    var acrcloudKey by stringPref(PREF_ACR_KEY)
    var acrcloudSecret by stringPref(PREF_ACR_SECRET)

    var proStatus by booleanPref(PREF_PRO_STATUS).withDefault(false)
    var digestSeconds by intPref(PREF_DIGEST_SECONDS)
    var scrobbleCount by intPref(PREF_SCROBBLE_COUNT).withDefault(0)
    var firstLaunchTime by longPref(PREF_FIRST_LAUNCHED)
    var lastUpdateCheckTime by longPref(PREF_LAST_UPDATE_CHECK_TIME)
    var checkForUpdates by booleanPref(PREF_CHECK_FOR_UPDATES)
    var dontAskForRating by booleanPref(PREF_DONT_ASK_FOR_RATING).withDefault(false)
    var prefVersion by intPref(PREF_VERSION).withDefault(0)
    var lastfmLinksEnabled by booleanPref(PREF_ENABLE_LASTFM_LINKS).withDefault(false)
    var hiddenTags by stringSetPref(PREF_ACTIVITY_HIDDEN_TAGS).withDefault(MetadataUtils.tagSpam)
    var pinnedFriendsJson by kotlinxPref<List<UserSerializable>>(PREF_ACTIVITY_PINNED_FRIENDS)
        .withDefault(emptyList())
    var touhouCircles by stringPref(PREF_TOUHOU_CIRCLES).withDefault("")
    // we want 401 and not 400
    var spotifyAccessToken by stringPref(PREF_SPOTIFY_ACCESS_TOKEN).withDefault("qwertyuiopasdfghjklzxcvbnm")
    var spotifyAccessTokenExpires by longPref(PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES).withDefault(-1)

    var songSearchUrl by stringPref(PREF_ACTIVITY_SONG_SEARCH_URL).withDefault("https://www.youtube.com/results?search_query=\$artist+\$title")

    companion object {
        const val NAME = "main"
        const val PREF_MASTER = "master"
        const val PREF_ALLOWED_PACKAGES = "app_whitelist"
        const val PREF_BLOCKED_PACKAGES = "app_blacklist"
        const val PREF_AUTO_DETECT = "auto_detect"
        const val PREF_DELAY_SECS = "delay_secs"
        const val PREF_DELAY_SECS_MIN = 30
        const val PREF_DELAY_SECS_MAX = 360
        const val PREF_DELAY_SECS_DEFAULT = 180
        const val PREF_DELAY_PER = "delay_per"
        const val PREF_DELAY_PER_DEFAULT = 50
        const val PREF_DELAY_PER_MIN = 30
        const val PREF_DELAY_PER_MAX = 95
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
        const val PREF_LB_CUSTOM_TLS_NO_VERIFY = "lb_tls_no_verify"
        const val PREF_GNUFM_USERNAME = "gnufm_username"
        const val PREF_GNUFM_ROOT = "gnufm_root"
        const val PREF_GNUFM_SESS_KEY = "gnufm_sesskey"
        const val PREF_GNUFM_TLS_NO_VERIFY = "gnufm_tls_no_verify"
        const val PREF_LASTFM_DISABLE = "lastfm_disable"
        const val PREF_NOW_PLAYING = "now_playing"
        const val PREF_ACR_HOST = "acr_host"
        const val PREF_ACR_KEY = "acr_key"
        const val PREF_ACR_SECRET = "acr_secret"
        const val PREF_LOCKSCREEN_NOTI = "lockscreen_noti"
        const val PREF_IMPORT = "import"
        const val PREF_EXPORT = "export"
        const val PREF_INTENTS = "intents"
        const val PREF_FETCH_AA = "fetch_album_artist"
        const val PREF_FETCH_ALBUM = "fetch_album"
        const val PREF_SHOW_SCROBBLE_SOURCES = "show_scrobble_sources"
        const val PREF_SHOW_RECENTS_ALBUM = "show_album"
        const val PREF_THEME_PRIMARY = "theme_primary"
        const val PREF_THEME_SECONDARY = "theme_secondary"
        const val PREF_THEME_BACKGROUND = "theme_background"
        const val PREF_THEME_RANDOM = "theme_random"
        const val PREF_THEME_TINT_BG = "theme_tint_bg"
        const val PREF_THEME_DYNAMIC = "theme_dynamic"
        const val PREF_THEME_DAY_NIGHT = "theme_day_night"
        const val PREF_PRO_STATUS = "pro_status"
        const val PREF_DIGEST_SECONDS = "digest_seconds"
        const val PREF_LOCALE = "locale"
        const val PREF_SEARCH_IN_SOURCE = "search_in_source"
        const val PREF_SEARCH_TYPE = "search_type"
        const val PREF_CRASHLYTICS_ENABLED = "crashlytics_enabled"
        const val PREF_CURRENT_USER = "current_user"
        const val PREF_SCROBBLE_ACCOUNTS = "scrobble_accounts"
        const val PREF_TOUHOU_CIRCLES = "touhou_circles"
        const val PREF_SPOTIFY_ACCESS_TOKEN = "spotify_access_token"
        const val PREF_SPOTIFY_ACCESS_TOKEN_EXPIRES = "spotify_access_token_expires"
        const val PREF_SCROBBLE_SPOTIFY_REMOTE = "scrobble_spotify_remote"

        const val CHANNEL_NOTI_SCROBBLING = "noti_scrobbling"
        const val CHANNEL_NOTI_SCR_ERR = "noti_scrobble_errors"
        const val CHANNEL_NOTI_NEW_APP = "noti_new_app"
        const val CHANNEL_NOTI_PENDING = "noti_pending_scrobbles"
        const val CHANNEL_NOTI_DIGEST_WEEKLY = "noti_digest_weekly"
        const val CHANNEL_NOTI_DIGEST_MONTHLY = "noti_digest_monthly"
        const val CHANNEL_NOTI_PERSISTENT = "noti_persistent"
        const val CHANNEL_NOTI_UPDATE = "noti_update"
        const val CHANNEL_TEST_SCROBBLE_FROM_NOTI = "test_scrobble_from_noti"

        const val ACTIVITY_PREFS_NAME = "activity_preferences"
        const val PREF_ACTIVITY_APP_LIST_WAS_RUN = "app_list_run"
        const val PREF_ACTIVITY_LAST_TAB = "last_tab"
        const val PREF_ACTIVITY_LAST_CHARTS_PERIOD_SELECTED = "last_charts_period_selected"
        const val PREF_ACTIVITY_LAST_CHARTS_PERIOD_TYPE = "last_charts_period_type"
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
        const val PREF_ACTIVITY_REORDER_FRIENDS_LEARNT = "reorder_friends_learnt"
        const val PREF_ACTIVITY_USER_TAG_HISTORY_FETCHED = "user_tag_history_fetched"
        const val PREF_ACTIVITY_SONG_SEARCH_URL = "song_search_url"
        const val PREF_ACTIVITY_HIDDEN_TAGS = "hidden_tags"
        const val PREF_ACTIVITY_PINNED_FRIENDS = "pinned_friends"
        const val PREF_ACTIVITY_LAST_FULL_INDEXED_TIME = "last_full_indexed_time"
        const val PREF_ACTIVITY_LAST_DELTA_INDEXED_TIME = "last_delta_indexed_time"
        const val PREF_ACTIVITY_LAST_FULL_INDEXED_SCROBBLE_TIME = "last_full_indexed_scrobble_time"
        const val PREF_ACTIVITY_LAST_DELTA_INDEXED_SCROBBLE_TIME =
            "last_delta_indexed_scrobble_time"
        const val PREF_ACTIVITY_GRID_COLUMNS_TO_ADD = "grid_columns_to_add"
        const val PREF_ACTIVITY_GRID_SINGLE_COLUMN = "grid_single_column"
        const val PREF_ACTIVITY_GRID_PINCH_LEARNT = "grid_pinch_learnt"
        const val PREF_ACTIVITY_LAST_KILL_CHECK_TIME = "last_kill_checked"

        const val PREF_COLLAGE_SIZE = "collage_size"
        const val PREF_COLLAGE_CAPTIONS = "collage_captions"
        const val PREF_COLLAGE_SKIP_MISSING = "collage_skip_missing"
        const val PREF_COLLAGE_TEXT = "collage_text"
        const val PREF_COLLAGE_USERNAME = "collage_username"

        const val PREF_SCROBBLE_COUNT = "scrobble_count"
        const val PREF_FIRST_LAUNCHED = "date_firstlaunch"
        const val PREF_LAST_UPDATE_CHECK_TIME = "last_update_check_time"
        const val PREF_CHECK_FOR_UPDATES = "check_for_updates"
        const val PREF_DONT_ASK_FOR_RATING = "dontshowagain"
        const val PREF_ENABLE_LASTFM_LINKS = "lastfm_links"
        const val PREF_VERSION = "version"
    }
}