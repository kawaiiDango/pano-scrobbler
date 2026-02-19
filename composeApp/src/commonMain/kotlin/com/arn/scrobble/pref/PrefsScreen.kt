package com.arn.scrobble.pref

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.icons.Api
import com.arn.scrobble.icons.BugReport
import com.arn.scrobble.icons.Dns
import com.arn.scrobble.icons.EditNote
import com.arn.scrobble.icons.HourglassEmpty
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Info
import com.arn.scrobble.icons.MoreHoriz
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.icons.Person
import com.arn.scrobble.icons.SwapVert
import com.arn.scrobble.icons.Translate
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.VariantStuff
import com.arn.scrobble.utils.setAppLocale
import com.arn.scrobble.work.CommonWorkState
import com.arn.scrobble.work.DigestWork
import com.arn.scrobble.work.DigestWorker
import com.arn.scrobble.work.UpdaterWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.also_available_on
import pano_scrobbler.composeapp.generated.resources.android
import pano_scrobbler.composeapp.generated.resources.artist_image
import pano_scrobbler.composeapp.generated.resources.auto
import pano_scrobbler.composeapp.generated.resources.automation
import pano_scrobbler.composeapp.generated.resources.copy_sk
import pano_scrobbler.composeapp.generated.resources.country_for_api
import pano_scrobbler.composeapp.generated.resources.dark
import pano_scrobbler.composeapp.generated.resources.debug_menu
import pano_scrobbler.composeapp.generated.resources.delete_account
import pano_scrobbler.composeapp.generated.resources.delete_receipt
import pano_scrobbler.composeapp.generated.resources.demo_mode
import pano_scrobbler.composeapp.generated.resources.desktop
import pano_scrobbler.composeapp.generated.resources.external_metadata
import pano_scrobbler.composeapp.generated.resources.first_artist
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.light
import pano_scrobbler.composeapp.generated.resources.min_track_duration
import pano_scrobbler.composeapp.generated.resources.notification_channel_blocked
import pano_scrobbler.composeapp.generated.resources.num_blocked_metadata
import pano_scrobbler.composeapp.generated.resources.num_simple_edits
import pano_scrobbler.composeapp.generated.resources.pref_about
import pano_scrobbler.composeapp.generated.resources.pref_auto_detect
import pano_scrobbler.composeapp.generated.resources.pref_check_updates
import pano_scrobbler.composeapp.generated.resources.pref_crashlytics_enabled
import pano_scrobbler.composeapp.generated.resources.pref_delay
import pano_scrobbler.composeapp.generated.resources.pref_delay_mins
import pano_scrobbler.composeapp.generated.resources.pref_delay_per
import pano_scrobbler.composeapp.generated.resources.pref_enabled_apps_summary
import pano_scrobbler.composeapp.generated.resources.pref_export
import pano_scrobbler.composeapp.generated.resources.pref_export_desc
import pano_scrobbler.composeapp.generated.resources.pref_fetch_album
import pano_scrobbler.composeapp.generated.resources.pref_first_day_of_week
import pano_scrobbler.composeapp.generated.resources.pref_imexport
import pano_scrobbler.composeapp.generated.resources.pref_import
import pano_scrobbler.composeapp.generated.resources.pref_link_heart_button_rating
import pano_scrobbler.composeapp.generated.resources.pref_lists
import pano_scrobbler.composeapp.generated.resources.pref_locale
import pano_scrobbler.composeapp.generated.resources.pref_misc
import pano_scrobbler.composeapp.generated.resources.pref_notify_updates
import pano_scrobbler.composeapp.generated.resources.pref_now_playing
import pano_scrobbler.composeapp.generated.resources.pref_oss_credits
import pano_scrobbler.composeapp.generated.resources.pref_personalization
import pano_scrobbler.composeapp.generated.resources.pref_prevent_duplicate_ambient_scrobbles
import pano_scrobbler.composeapp.generated.resources.pref_privacy_policy
import pano_scrobbler.composeapp.generated.resources.pref_scrobble_from
import pano_scrobbler.composeapp.generated.resources.pref_search_in_source
import pano_scrobbler.composeapp.generated.resources.pref_search_in_source_desc
import pano_scrobbler.composeapp.generated.resources.pref_search_url_template
import pano_scrobbler.composeapp.generated.resources.pref_show_scrobble_sources
import pano_scrobbler.composeapp.generated.resources.pref_show_scrobble_sources_desc
import pano_scrobbler.composeapp.generated.resources.pref_spotify_artist_search_approximate
import pano_scrobbler.composeapp.generated.resources.pref_spotify_remote
import pano_scrobbler.composeapp.generated.resources.pref_themes
import pano_scrobbler.composeapp.generated.resources.pref_translate
import pano_scrobbler.composeapp.generated.resources.pref_translate_credits
import pano_scrobbler.composeapp.generated.resources.pref_tray_icon_theme
import pano_scrobbler.composeapp.generated.resources.regex_rules
import pano_scrobbler.composeapp.generated.resources.scrobble_services
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.spotify
import pano_scrobbler.composeapp.generated.resources.when_not_using
import java.util.Calendar
import java.util.Locale


@Composable
fun PrefsScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onNavigateToBilling = { onNavigate(PanoRoute.Billing) }

    val mainPrefs = remember { PlatformStuff.mainPrefs }
    val nlsEnabled = remember { PlatformStuff.isNotificationListenerEnabled() }

    val scrobblerEnabled by mainPrefs.data.collectAsStateWithInitialValue { it.scrobblerEnabled }
    val allowedPackages by mainPrefs.data.collectAsStateWithInitialValue { it.allowedPackages }
    val scrobbleSpotifyRemoteP by mainPrefs.data.collectAsStateWithInitialValue { it.scrobbleSpotifyRemoteP }
    val autoDetectApps by mainPrefs.data.collectAsStateWithInitialValue { it.autoDetectAppsP }
    val delayPercent by mainPrefs.data.collectAsStateWithInitialValue { it.delayPercentP }
    val delaySecs by mainPrefs.data.collectAsStateWithInitialValue { it.delaySecsP }
    val minDurationSecs by mainPrefs.data.collectAsStateWithInitialValue { it.minDurationSecsP }
    val showScrobbleSources by mainPrefs.data.collectAsStateWithInitialValue { it.showScrobbleSources }
    val searchInSource by mainPrefs.data.collectAsStateWithInitialValue { it.searchInSource }
    val searchUrlTemplate by mainPrefs.data.collectAsStateWithInitialValue { p ->
        p.searchUrlTemplate.takeIf { !p.usePlayFromSearchP }
    }
    val linkHeartButtonToRating by mainPrefs.data.collectAsStateWithInitialValue { it.linkHeartButtonToRating }
    val firstDayOfWeek by mainPrefs.data.collectAsStateWithInitialValue { it.firstDayOfWeek }
    val locale by LocaleUtils.locale.collectAsStateWithLifecycle()
    val lastfmApiAlways by mainPrefs.data.collectAsStateWithInitialValue { it.lastfmApiAlways }
    val fetchAlbum by mainPrefs.data.collectAsStateWithInitialValue { it.fetchAlbum }
    val tidalSteelSeries by mainPrefs.data.collectAsStateWithInitialValue { it.tidalSteelSeriesApi }
    val spotifyArtistSearchApproximate by
    mainPrefs.data.collectAsStateWithInitialValue { it.spotifyArtistSearchApproximate }
    val preventDuplicateAmbientScrobbles by
    mainPrefs.data.collectAsStateWithInitialValue { it.preventDuplicateAmbientScrobbles }
    val submitNowPlaying by
    mainPrefs.data.collectAsStateWithInitialValue { it.submitNowPlaying }
    val trayIconTheme by
    mainPrefs.data.collectAsStateWithInitialValue { it.trayIconTheme }
    val notiPersistent by
    mainPrefs.data.collectAsStateWithInitialValue { it.notiPersistent }
    val checkForUpdates by
    mainPrefs.data.collectAsStateWithInitialValue { it.autoUpdates }
    val useSpotify by
    mainPrefs.data.collectAsStateWithInitialValue { it.spotifyApi }
    val spotifyCountryP by
    mainPrefs.data.collectAsStateWithInitialValue { it.spotifyCountryP }
    val deezerApi by
    mainPrefs.data.collectAsStateWithInitialValue { it.deezerApi }
    val extractFirstArtistPackages by
    mainPrefs.data.collectAsStateWithInitialValue { it.extractFirstArtistPackages }
    val demoMode by mainPrefs.data.collectAsStateWithInitialValue { it.demoModeP }
    var isAddedToStartup by remember { mutableStateOf(false) }
    val scrobblableLabels by
    mainPrefs.data.collectAsStateWithInitialValue { it.scrobbleAccounts.associate { it.type to it.user.name } }
    val updateProgress by remember {
        UpdaterWork.getProgress().filter { it.state == CommonWorkState.RUNNING }
    }.collectAsStateWithLifecycle(null)

    val numSimpleEdits by if (Stuff.isInDemoMode) {
        remember { mutableIntStateOf(1000) }
    } else {
        PanoDb.db.getSimpleEditsDao().count().collectAsStateWithLifecycle(0)
    }
    val numRegexEdits by if (Stuff.isInDemoMode) {
        remember { mutableIntStateOf(30) }
    } else {
        PanoDb.db.getRegexEditsDao().count().collectAsStateWithLifecycle(0)
    }
    val numBlockedMetadata by if (Stuff.isInDemoMode) {
        remember { mutableIntStateOf(1000) }
    } else {
        PanoDb.db.getBlockedMetadataDao().count().collectAsStateWithLifecycle(0)
    }
    var crashReporterEnabled by remember {
        mutableStateOf(
            VariantStuff.crashReporter.isEnabled
        )
    }
    val isLicenseValid = LocalLicenseValidState.current

    val maybeActivity = getActivityOrNull()

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            isAddedToStartup = PlatformSpecificPrefs.isAddedToStartup()
        }

        if (!PlatformStuff.isDesktop && !PlatformStuff.isTv) {
            snapshotFlow { firstDayOfWeek }
                .drop(1) // only for changes
                .collect {
                    val (nextWeek, nextMonth) = DigestWorker.nextWeekAndMonth()

                    DigestWork.schedule(
                        nextWeek,
                        nextMonth,
                    )
                }
        }
    }

    PanoLazyColumn(modifier = modifier) {
        PlatformSpecificPrefs.prefScrobbler(this, scrobblerEnabled, nlsEnabled, onNavigate)

        PlatformSpecificPrefs.prefQuickSettings(this, scrobblerEnabled)

        PlatformSpecificPrefs.addToStartup(this, isAddedToStartup) {
            isAddedToStartup = it
        }

        stickyHeader("scrobbling_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.scrobbles),
                icon = Icons.MusicNote
            )
        }

        item(MainPrefs::allowedPackages.name) {
            AppIconsPref(
                packageNames = allowedPackages,
                title = stringResource(Res.string.pref_scrobble_from),
                onClick = {
                    onNavigate(
                        PanoRoute.AppList(
                            saveType = AppListSaveType.Scrobbling,
                            preSelectedPackages = allowedPackages.toList(),
                            isSingleSelect = false,
                        )
                    )
                }
            )
        }

        item(key = "choose_apps_notice") {
            Text(
                text = stringResource(Res.string.pref_enabled_apps_summary),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(
                    vertical = 16.dp,
                    horizontal = horizontalOverscanPadding()
                )
            )
        }

        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop) {
            item(MainPrefs::autoDetectAppsP.name) {
                val notiEnabled =
                    remember { PanoNotifications.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_NEW_APP) }

                SwitchPref(
                    text = stringResource(Res.string.pref_auto_detect),
                    summary = if (!notiEnabled) stringResource(Res.string.notification_channel_blocked) else null,
                    value = autoDetectApps,
                    enabled = notiEnabled,
                    copyToSave = { copy(autoDetectApps = it) },
                )
            }
        }

        if (!PlatformStuff.isDesktop && !PlatformStuff.isTv) {
            item(MainPrefs::scrobbleSpotifyRemoteP.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_spotify_remote),
                    value = scrobbleSpotifyRemoteP,
                    copyToSave = { copy(scrobbleSpotifyRemote = it) }
                )
            }
        }

        item(MainPrefs::extractFirstArtistPackages.name) {
            AppIconsPref(
                packageNames = extractFirstArtistPackages,
                title = stringResource(Res.string.first_artist),
                enabled = (allowedPackages.size + extractFirstArtistPackages.size) > 0,
                onClick = {
                    onNavigate(
                        PanoRoute.AppList(
                            saveType = AppListSaveType.ExtractFirstArtist,
                            packagesOverride = (allowedPackages union extractFirstArtistPackages).toList(),
                            preSelectedPackages = extractFirstArtistPackages.toList(),
                            isSingleSelect = false,
                        )
                    )
                }
            )
        }

        item(MainPrefs::submitNowPlaying.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_now_playing),
                value = submitNowPlaying,
                copyToSave = { copy(submitNowPlaying = it) }
            )
        }

        item(MainPrefs::minDurationSecsP.name) {
            SliderPref(
                text = stringResource(Res.string.min_track_duration),
                value = minDurationSecs.toFloat(),
                copyToSave = { copy(minDurationSecs = it) },
                min = MainPrefs.PREF_MIN_DURATON_SECS_MIN,
                max = MainPrefs.PREF_MIN_DURATON_SECS_MAX,
                increments = 5,
                stringRepresentation = { Stuff.humanReadableDuration(it * 1000L) }
            )
        }

        PlatformSpecificPrefs.discordRpc(this, onNavigate)

        stickyHeader("pref_delay_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_delay),
                icon = Icons.HourglassEmpty
            )
        }

        item(MainPrefs::delayPercentP.name) {
            SliderPref(
                text = stringResource(Res.string.pref_delay_per),
                value = delayPercent.toFloat(),
                copyToSave = { copy(delayPercent = it) },
                min = MainPrefs.PREF_DELAY_PER_MIN,
                max = MainPrefs.PREF_DELAY_PER_MAX,
                increments = 1,
                stringRepresentation = { "${it}%" }
            )
        }

        item(MainPrefs::delaySecsP.name) {
            SliderPref(
                text = stringResource(Res.string.pref_delay_mins),
                value = delaySecs.toFloat(),
                copyToSave = { copy(delaySecs = it) },
                min = MainPrefs.PREF_DELAY_SECS_MIN,
                max = MainPrefs.PREF_DELAY_SECS_MAX,
                increments = 5,
                stringRepresentation = { Stuff.humanReadableDuration(it * 1000L) }
            )
        }

        stickyHeader("personalization_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_personalization),
                icon = Icons.Person
            )
        }

        item(MainPrefs::themeName.name) {
            TextPref(
                text = stringResource(Res.string.pref_themes),
                locked = !isLicenseValid,
                onClick = {
                    onNavigate(PanoRoute.ThemeChooser)
                }
            )
        }

        if (PlatformStuff.isDesktop) {
            item(MainPrefs::trayIconTheme.name) {
                DropdownPref(
                    text = stringResource(Res.string.pref_tray_icon_theme),
                    selectedValue = trayIconTheme,
                    values = DayNightMode.entries,
                    toLabel = {
                        stringResource(
                            when (it) {
                                DayNightMode.SYSTEM -> Res.string.auto
                                DayNightMode.LIGHT -> Res.string.light
                                DayNightMode.DARK -> Res.string.dark
                            }
                        )
                    },
                    copyToSave = { copy(trayIconTheme = it) }
                )
            }
        }

        PlatformSpecificPrefs.prefChartsWidget(this)

//        item(MainPrefs::showAlbumsInRecents.name) {
//            SwitchPref(
//                text = stringResource(Res.string.pref_show_albums),
//                summary = stringResource(Res.string.pref_show_albums_desc),
//                value = showAlbumsInRecents,
//                copyToSave = { copy(showAlbumsInRecents = it) }
//            )
//        }

        item(MainPrefs::showScrobbleSources.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_show_scrobble_sources),
                summary = stringResource(Res.string.pref_show_scrobble_sources_desc),
                value = showScrobbleSources,
                onNavigateToBilling = onNavigateToBilling.takeIf { !isLicenseValid },
                copyToSave = { copy(showScrobbleSources = it) }
            )
        }

        if (!PlatformStuff.isDesktop) {
            item(MainPrefs::searchInSource.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_search_in_source),
                    summary = stringResource(Res.string.pref_search_in_source_desc),
                    value = searchInSource,
                    onNavigateToBilling = onNavigateToBilling.takeIf { !isLicenseValid },
                    enabled = showScrobbleSources,
                    copyToSave = { copy(searchInSource = it) }
                )
            }
        }

        if (!PlatformStuff.isTv) {
            item(MainPrefs::searchUrlTemplate.name) {
                TextPref(
                    text = stringResource(Res.string.pref_search_url_template),
                    summary = searchUrlTemplate,
                    onClick = {
                        onNavigate(PanoRoute.Modal.MediaSearchPref)
                    }
                )
            }
        }

        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop) {
            item(MainPrefs::linkHeartButtonToRating.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_link_heart_button_rating),
                    summary = stringResource(Res.string.pref_search_in_source_desc),
                    value = linkHeartButtonToRating,
                    onNavigateToBilling = onNavigateToBilling.takeIf { !isLicenseValid },
                    copyToSave = { copy(linkHeartButtonToRating = it) }
                )
            }
        }

        item(MainPrefs::firstDayOfWeek.name) {
            val autoString = stringResource(Res.string.auto)

            val valuesToDays = remember {
                val cal = Calendar.getInstance()

                var autoDayName = ""

                val days = cal.getDisplayNames(
                    Calendar.DAY_OF_WEEK,
                    Calendar.LONG,
                    Locale.getDefault()
                )!!
                    .map { (k, v) ->
                        if (v == cal.firstDayOfWeek)
                            autoDayName = k
                        v to k
                    }
                    .toMap()

                (days + (-1 to "$autoString: $autoDayName")).toSortedMap()
            }

            DropdownPref(
                text = stringResource(Res.string.pref_first_day_of_week),
                selectedValue = firstDayOfWeek,
                values = valuesToDays.keys,
                toLabel = { valuesToDays[it] ?: autoString },
                copyToSave = { copy(firstDayOfWeek = it) }
            )
        }

        PlatformSpecificPrefs.prefNotifications(this)

        stickyHeader("lists_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_lists),
                icon = Icons.EditNote
            )
        }

        item("simple_edits") {
            TextPref(
                text = pluralStringResource(
                    Res.plurals.num_simple_edits,
                    numSimpleEdits,
                    numSimpleEdits
                ),
                onClick = {
                    onNavigate(PanoRoute.SimpleEdits)
                }
            )
        }

        item("regex_edits") {
            TextPref(
                text = "(${numRegexEdits.format()}) " + stringResource(Res.string.regex_rules),
                onClick = {
                    onNavigate(PanoRoute.RegexEdits)
                }
            )
        }

        item("blocked_metadata") {
            TextPref(
                text = pluralStringResource(
                    Res.plurals.num_blocked_metadata,
                    numBlockedMetadata,
                    numBlockedMetadata
                ),
                onClick = {
                    onNavigate(PanoRoute.BlockedMetadatas)
                },
                locked = !isLicenseValid,
            )
        }

        stickyHeader("languages_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_locale),
                icon = Icons.Translate
            )
        }

        item(LocaleUtils::locale.name) {
            val autoString = stringResource(Res.string.auto)

            val localesMap = remember(locale) {
                val autoEntry = mapOf("auto" to autoString)
                LocaleUtils.localesMap().let {
                    autoEntry + it
                }
            }

            DropdownPref(
                text = stringResource(Res.string.pref_locale),
                selectedValue = locale,
                values = localesMap.keys,
                toLabel = { localesMap[it] ?: autoString },
                copyToSave = {
                    val l = it.takeIf { it != "auto" }
                    LocaleUtils.setAppLocale(lang = l, maybeActivity)
                    this
                }
            )
        }

        item("translate") {

            TextPref(
                text = stringResource(Res.string.pref_translate),
                onClick = {
                    PlatformStuff.openInBrowser(Stuff.CROWDIN_URL)
                }
            )
        }

        item("translate_credits") {
            TextPref(
                text = stringResource(Res.string.pref_translate_credits),
                onClick = {
                    onNavigate(PanoRoute.Translators)
                }
            )
        }

        stickyHeader("additional_metatadata_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.external_metadata),
                icon = Icons.Api
            )
        }

        item(MainPrefs::lastfmApiAlways.name) {
            SwitchPref(
                text = stringResource(Res.string.lastfm),
                value = lastfmApiAlways,
                summary = stringResource(
                    Res.string.when_not_using,
                    stringResource(Res.string.lastfm)
                ),
                copyToSave = { copy(lastfmApiAlways = it) }
            )
        }

        item(MainPrefs::fetchAlbum.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_fetch_album),
                value = fetchAlbum,
                copyToSave = { copy(fetchAlbum = it) }
            )
        }

        item(MainPrefs::spotifyApi.name) {
            SwitchPref(
                text = stringResource(Res.string.spotify),
                summary = stringResource(Res.string.search) + ": " +
                        stringResource(Res.string.artist_image) + ", " +
                        stringResource(Res.string.album_art),
                value = useSpotify,
                copyToSave = { copy(spotifyApi = it) }
            )
        }

        item(MainPrefs::spotifyCountryP.name) {
            val countryCodes = remember { Locale.getISOCountries().toList() }

            DropdownPref(
                text = stringResource(
                    Res.string.country_for_api,
                    stringResource(Res.string.spotify)
                ),
                selectedValue = spotifyCountryP,
                values = countryCodes,
                toLabel = { it },
                copyToSave = { copy(spotifyCountry = it) },
                enabled = useSpotify
            )
        }

        item(MainPrefs::spotifyArtistSearchApproximate.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_spotify_artist_search_approximate),
                value = spotifyArtistSearchApproximate,
                copyToSave = { copy(spotifyArtistSearchApproximate = it) },
                enabled = useSpotify
            )
        }

        PlatformSpecificPrefs.deezerApi(this, deezerApi)

        PlatformSpecificPrefs.tidalSteelSeries(this, tidalSteelSeries)

        stickyHeader("misc_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_misc),
                icon = Icons.MoreHoriz
            )
        }

        if (!PlatformStuff.isDesktop && !PlatformStuff.isTv) {
            item(MainPrefs::preventDuplicateAmbientScrobbles.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_prevent_duplicate_ambient_scrobbles),
                    value = preventDuplicateAmbientScrobbles,
                    copyToSave = { copy(preventDuplicateAmbientScrobbles = it) }
                )
            }
        }

        if (!PlatformStuff.noUpdateCheck) {
            item(MainPrefs::autoUpdates.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_notify_updates),
                    value = checkForUpdates,
                    copyToSave = {
                        if (!it)
                            UpdaterWork.cancel()
                        else
                            UpdaterWork.schedule(true)

                        copy(autoUpdates = it)
                    }
                )
            }

            item("check_for_updates") {
                TextPref(
                    text = updateProgress?.message ?: stringResource(Res.string.pref_check_updates),
                    enabled = updateProgress == null,
                    onClick = {
                        if (updateProgress == null)
                            UpdaterWork.schedule(true)
                    }
                )
            }
        }

        PlatformSpecificPrefs.prefPersistentNoti(this, notiPersistent)

        if (!PlatformStuff.isTv) {
            item("automation") {
                TextPref(
                    text = stringResource(Res.string.automation),
                    onClick = {
                        onNavigate(PanoRoute.AutomationInfo)
                    },
                    locked = !isLicenseValid,
                )
            }
        }

        if (VariantStuff.crashReporter.isAvailable) {
            item(VariantStuff::crashReporter.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_crashlytics_enabled),
                    value = crashReporterEnabled,
                    copyToSave = {
                        crashReporterEnabled = it
                        VariantStuff.crashReporter.isEnabled = it
                        this
                    }
                )
            }
        }

        stickyHeader("imexport_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_imexport),
                icon = Icons.SwapVert
            )
        }

        item("export") {
            TextPref(
                text = stringResource(Res.string.pref_export),
                summary = stringResource(Res.string.pref_export_desc),
                onClick = {
                    onNavigate(PanoRoute.Export)
                }
            )
        }

        item("import") {
            TextPref(
                text = stringResource(Res.string.pref_import),
                onClick = {
                    onNavigate(PanoRoute.Import)
                }
            )
        }

        stickyHeader("services_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.scrobble_services),
                icon = Icons.Dns
            )
        }

        AccountType.entries
            .filterNot {
                PlatformStuff.isTv && it == AccountType.FILE
            }
            .forEach { accountType ->
                item(accountType.name) {
                    AccountPref(
                        type = accountType,
                        usernamesMap = scrobblableLabels,
                        onNavigate = onNavigate
                    )
                }
            }

        item(key = "delete_account") {
            TextPref(
                text = stringResource(Res.string.delete_account),
                onClick = {
                    onNavigate(PanoRoute.DeleteAccount)
                }
            )
        }

        stickyHeader("about_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_about),
                icon = Icons.Info
            )
        }

        item(key = "oss_credits") {
            TextPref(
                text = stringResource(Res.string.pref_oss_credits),
                onClick = {
                    onNavigate(PanoRoute.OssCredits)
                }
            )
        }

        item(key = "privacy_policy") {
            TextPref(
                text = stringResource(Res.string.pref_privacy_policy),
                onClick = {
                    onNavigate(PanoRoute.PrivacyPolicy)
                }
            )
        }

        item(key = "github_link") {
            TextPref(
                text = stringResource(
                    Res.string.also_available_on,
                    if (PlatformStuff.isDesktop)
                        stringResource(Res.string.android)
                    else
                        stringResource(Res.string.desktop)
                ),
                summary = Stuff.HOMEPAGE_URL,
                onClick = {
                    onNavigate(PanoRoute.Modal.ShowLink(Stuff.HOMEPAGE_URL))
                }
            )
        }

        item(key = "version") {
            Text(
                text = ("v" + BuildKonfig.VER_NAME + if (BuildKonfig.DEBUG) " (Debug)" else ""),
                modifier = Modifier.padding(
                    vertical = 16.dp,
                    horizontal = horizontalOverscanPadding()
                )
            )
        }

        if (BuildKonfig.DEBUG) {
            stickyHeader("debug_header") {
                SimpleHeaderItem(
                    text = stringResource(Res.string.debug_menu),
                    icon = Icons.BugReport
                )
            }

            item(MainPrefs::demoModeP.name) {
                SwitchPref(
                    text = stringResource(Res.string.demo_mode),
                    value = demoMode,
                    copyToSave = { copy(demoMode = it) }
                )
            }

            item("copy_sk") {
                TextPref(
                    text = stringResource(Res.string.copy_sk),
                    onClick = {
                        scope.launch {
                            PlatformStuff.mainPrefs.data
                                .map {
                                    it.scrobbleAccounts.firstOrNull {
                                        it.type == AccountType.LASTFM
                                    }?.authKey
                                }.first()
                                ?.let {
                                    PlatformStuff.copyToClipboard(it)
                                }
                        }
                    }
                )
            }

            item("delete_receipt") {
                TextPref(
                    text = stringResource(Res.string.delete_receipt),
                    onClick = {
                        scope.launch {
                            mainPrefs.updateData {
                                it.copy(receipt = null, receiptSignature = null)
                            }
                        }
                    }
                )
            }
        }
    }
}

