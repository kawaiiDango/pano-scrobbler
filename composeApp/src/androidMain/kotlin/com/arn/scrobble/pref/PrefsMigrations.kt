package com.arn.scrobble.pref

import androidx.datastore.core.DataMigration
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserAccountSerializable
import com.arn.scrobble.api.lastfm.CookieSerializable
import com.arn.scrobble.charts.AllPeriods
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.widget.ChartsWidgetListItem
import com.frybits.harmony.getHarmonySharedPreferences

class MainPrefsMigration5 : DataMigration<MainPrefs> {

    override suspend fun shouldMigrate(currentData: MainPrefs) =
        currentData.version < 5

    override suspend fun migrate(currentData: MainPrefs): MainPrefs {
        // Read old preferences
        val sharedPreferences = AndroidStuff.application.getHarmonySharedPreferences("main")
        val scrobblerEnabled = sharedPreferences.getBoolean("master", true)
        val allowedPackages =
            sharedPreferences.getStringSet("app_whitelist", emptySet()) ?: emptySet()
        val blockedPackages =
            sharedPreferences.getStringSet("app_blacklist", emptySet()) ?: emptySet()
        val autoDetectApps = sharedPreferences.getBoolean("auto_detect", true)
        val delaySecs = sharedPreferences.getInt("delay_secs", 180)
        val delayPercent = sharedPreferences.getInt("delay_per", 50)
        val scrobbleSpotifyRemote = sharedPreferences.getBoolean("scrobble_spotify_remote", false)
        val linkHeartButtonToRating =
            sharedPreferences.getBoolean("link_heart_button_to_rating", false)
        val preventDuplicateAmbientScrobbles =
            sharedPreferences.getBoolean("prevent_duplicate_ambient_scrobbles", false)
        val submitNowPlaying = sharedPreferences.getBoolean("now_playing", true)
        val fetchAlbum = sharedPreferences.getBoolean("fetch_album", false)
        val searchInSource = sharedPreferences.getBoolean("search_in_source", false)
        val crashlyticsEnabled = sharedPreferences.getBoolean("crashlytics_enabled", true)
        val lastInteractiveTime = sharedPreferences.getLong("last_screen_on_time", -1)
        val locale = sharedPreferences.getString("locale", null)
        val showAlbumInRecents = sharedPreferences.getBoolean("show_album", false)
        val showScrobbleSources = sharedPreferences.getBoolean("show_scrobble_sources", true)
        val themeDynamic = sharedPreferences.getBoolean("theme_dynamic", false)
        val themeDayNight =
            sharedPreferences.getInt("theme_day_night", 2) // Default to DayNightMode.DARK
        val appListWasRun = sharedPreferences.getBoolean(
            "app_list_run",
            !(allowedPackages.isEmpty() && blockedPackages.isEmpty())
        )
        val lastHomePagerTab = sharedPreferences.getInt("last_tab", 0)
        val currentAccountIdx = sharedPreferences.getInt("current_user_idx", 0)

        val scrobbleAccountsJson = sharedPreferences.getString("scrobble_accounts", null)
        val scrobbleAccounts = scrobbleAccountsJson?.let {
            val migratedJson =
                if (scrobbleAccountsJson.contains("MALOJA") && !scrobbleAccountsJson.contains("CUSTOM_LISTENBRAINZ")) {
                    scrobbleAccountsJson.replace(Regex("""\{"type":"MALOJA"(.*?"apiRoot":"(.*?)"(.*?))\}""")) { match ->
                        val apiRoot = match.groupValues[2]
                        val newApiRoot = apiRoot.trimEnd('/') + "/apis/listenbrainz/"
                        match.value
                            .replace("\"type\":\"MALOJA\"", "\"type\":\"CUSTOM_LISTENBRAINZ\"")
                            .replace("\"apiRoot\":\"$apiRoot\"", "\"apiRoot\":\"$newApiRoot\"")
                    }
                } else {
                    scrobbleAccountsJson.replace(Regex("""\{[^{}]*"type":"MALOJA"[^{}]*\},?"""), "")
                        .replace(Regex(""",\s*]"""), "]") // Clean up trailing commas
                }

            Stuff.myJson.decodeFromString<List<UserAccountSerializable>>(migratedJson)
        } ?: emptyList()
        val currentAccountType =
            scrobbleAccounts.getOrNull(currentAccountIdx)?.type ?: AccountType.LASTFM
        val drawerDataJson = sharedPreferences.getString("drawer_data", null)
        val drawerData =
            drawerDataJson?.let { Stuff.myJson.decodeFromString<Map<AccountType, DrawerData>>(it) }
                ?: emptyMap()
        val lastRandomType = sharedPreferences.getInt("random_type", Stuff.TYPE_TRACKS)
        val lastKillCheckTime = sharedPreferences.getLong("last_kill_checked", -1)
        val collageSkipMissing = sharedPreferences.getBoolean("collage_skip_missing", false)
        val collageUsername = sharedPreferences.getBoolean("collage_username", true)
        val collageText = sharedPreferences.getBoolean("collage_text", false)
        val collageSize = sharedPreferences.getInt("collage_size", 3)
        val collageCaptions = sharedPreferences.getBoolean("collage_captions", true)
        val lastFullIndexTime =
            sharedPreferences.getLong("last_full_indexed_time", -1).takeIf { it != -1L }
        val lastDeltaIndexTime =
            sharedPreferences.getLong("last_delta_indexed_time", -1).takeIf { it != -1L }
        val lastFullIndexedScrobbleTime =
            sharedPreferences.getLong("last_full_indexed_scrobble_time", -1).takeIf { it != -1L }
        val lastDeltaIndexedScrobbleTime =
            sharedPreferences.getLong("last_delta_indexed_scrobble_time", -1).takeIf { it != -1L }
        val regexLearnt = sharedPreferences.getBoolean("regex_learnt", false)
        val regexEditsLearnt = sharedPreferences.getBoolean("regex_edits_learnt", false)
        val squarePhotoLearnt = sharedPreferences.getBoolean("square_photo_learnt", false)
        val notificationsOnLockscreen = sharedPreferences.getBoolean("lockscreen_noti", false)
        val notiScrobbling = sharedPreferences.getBoolean("noti_scrobbling", true)
        val notiError = sharedPreferences.getBoolean("noti_scrobble_errors", true)
        val notiWeeklyDigests = sharedPreferences.getBoolean("noti_digest_weekly", true)
        val notiMonthlyDigests = sharedPreferences.getBoolean("noti_digest_monthly", true)
        val notiPendingScrobbles = sharedPreferences.getBoolean("noti_pending", true)
        val notiNewApp = sharedPreferences.getBoolean("noti_new_app", true)
        val notiPersistent =
            sharedPreferences.getBoolean("noti_persistent", AndroidStuff.forcePersistentNoti)
        val digestSeconds = sharedPreferences.getInt("digest_seconds", -1).takeIf { it != -1 }
        val lastReviewPromptTime =
            sharedPreferences.getLong("date_firstlaunch", -1).takeIf { it != -1L }
        val lastUpdateCheckTime =
            sharedPreferences.getLong("last_update_check_time", -1).takeIf { it != -1L }
        val hiddenTags = sharedPreferences.getStringSet("hidden_tags", emptySet()) ?: emptySet()
        val spotifyAccessToken =
            sharedPreferences.getString("spotify_access_token", "bad_token") ?: "bad_token"
        val spotifyAccessTokenExpires =
            sharedPreferences.getLong("spotify_access_token_expires", -1)
        val spotifyArtistSearchApproximate =
            sharedPreferences.getBoolean("spotify_artist_search_approximate", false)
        val receipt = sharedPreferences.getString("receipt", null)
        val receiptSignature = sharedPreferences.getString("receipt_signature", null)
        val lastLicenseCheckTime = sharedPreferences.getLong("last_license_check_time", -1)

        val cookiesPrefs = AndroidStuff.application.getHarmonySharedPreferences("LastFmCookies")

        val cookies = cookiesPrefs.all.mapNotNull { (key, value) ->
            value?.let {
                val cookie = Stuff.myJson.decodeFromString<CookieSerializable>(it as String)
                key to cookie
            }
        }.toMap()


        // Create new MainPrefs instance
        val newPrefs = MainPrefs(
            scrobblerEnabled = scrobblerEnabled,
            allowedPackages = allowedPackages,
            blockedPackages = blockedPackages,
            autoDetectApps = autoDetectApps,
            delaySecs = delaySecs,
            delayPercent = delayPercent,
            scrobbleSpotifyRemote = scrobbleSpotifyRemote,
            linkHeartButtonToRating = linkHeartButtonToRating,
            preventDuplicateAmbientScrobbles = preventDuplicateAmbientScrobbles,
            submitNowPlaying = submitNowPlaying,
            fetchAlbum = fetchAlbum,
            searchInSource = searchInSource,
            crashReporterEnabled = crashlyticsEnabled,
            lastInteractiveTime = lastInteractiveTime,
            locale = locale,
            showAlbumsInRecents = showAlbumInRecents,
            showScrobbleSources = showScrobbleSources,
            themeDynamic = themeDynamic,
            themeDayNight = DayNightMode.entries.firstOrNull { it.ordinal == themeDayNight }
                ?: DayNightMode.DARK,
            appListWasRun = appListWasRun,
            lastHomePagerTab = lastHomePagerTab,
            currentAccountType = currentAccountType,
            scrobbleAccounts = scrobbleAccounts,
            drawerData = drawerData,
            lastRandomType = lastRandomType,
            lastKillCheckTime = lastKillCheckTime,
            collageSkipMissing = collageSkipMissing,
            collageUsername = collageUsername,
            collageText = collageText,
            collageSize = collageSize,
            collageCaptions = collageCaptions,
            lastFullIndexTime = lastFullIndexTime,
            lastDeltaIndexTime = lastDeltaIndexTime,
            lastFullIndexedScrobbleTime = lastFullIndexedScrobbleTime,
            lastDeltaIndexedScrobbleTime = lastDeltaIndexedScrobbleTime,
            regexLearnt = regexLearnt,
            regexEditsLearnt = regexEditsLearnt,
            squarePhotoLearnt = squarePhotoLearnt,
            notificationsOnLockscreen = notificationsOnLockscreen,
            notiScrobbling = notiScrobbling,
            notiError = notiError,
            notiWeeklyDigests = notiWeeklyDigests,
            notiMonthlyDigests = notiMonthlyDigests,
            notiPendingScrobbles = notiPendingScrobbles,
            notiNewApp = notiNewApp,
            notiPersistent = notiPersistent,
            digestSeconds = digestSeconds,
            lastReviewPromptTime = lastReviewPromptTime,
            lastUpdateCheckTime = lastUpdateCheckTime,
            version = 5,
            hiddenTags = hiddenTags,
            spotifyAccessToken = spotifyAccessToken,
            spotifyAccessTokenExpires = spotifyAccessTokenExpires,
            spotifyArtistSearchApproximate = spotifyArtistSearchApproximate,
            receipt = receipt,
            receiptSignature = receiptSignature,
            lastLicenseCheckTime = lastLicenseCheckTime,
            cookies = cookies
        )

        return newPrefs
    }

    override suspend fun cleanUp() {

    }
}

class WidgetPrefsMigration1 : DataMigration<WidgetPrefs> {
    private val sharedPreferences by lazy { AndroidStuff.application.getHarmonySharedPreferences("widget") }

    override suspend fun shouldMigrate(currentData: WidgetPrefs) =
        currentData.version < 1

    override suspend fun migrate(currentData: WidgetPrefs): WidgetPrefs {
        val widgets = mutableMapOf<Int, SpecificWidgetPrefs>()
        val chartsData =
            mutableMapOf<AllPeriods, Map<Int, List<ChartsWidgetListItem>>>()

        sharedPreferences.all.forEach { (key, value) ->
            val parts = key.split("_")
            if (parts.size == 2) {
                val widgetId = parts[1].toIntOrNull()
                if (widgetId != null) {
                    val specificPrefs = widgets.getOrPut(widgetId) { SpecificWidgetPrefs() }
                    when (parts[0]) {
                        "tab" -> widgets[widgetId] = specificPrefs.copy(tab = value as Int)
                        "bg_alpha" -> widgets[widgetId] =
                            specificPrefs.copy(bgAlpha = value as Float)

                        "shadow" -> widgets[widgetId] =
                            specificPrefs.copy(shadow = value as Boolean)
                    }
                }
            }
        }

        return WidgetPrefs(
            widgets = widgets,
            chartsData = chartsData,
            version = 1
        )
    }

    override suspend fun cleanUp() {

    }
}