package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.crashreporter.CrashReporterConfig
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
import com.arn.scrobble.main.ScrobblerState
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchField
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.ui.accountTypeStringRes
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.also_available_on
import pano_scrobbler.composeapp.generated.resources.android
import pano_scrobbler.composeapp.generated.resources.artist_image
import pano_scrobbler.composeapp.generated.resources.auto
import pano_scrobbler.composeapp.generated.resources.automation
import pano_scrobbler.composeapp.generated.resources.cache
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
import pano_scrobbler.composeapp.generated.resources.pref_about
import pano_scrobbler.composeapp.generated.resources.pref_auto_detect
import pano_scrobbler.composeapp.generated.resources.pref_blocked_metadata
import pano_scrobbler.composeapp.generated.resources.pref_check_updates
import pano_scrobbler.composeapp.generated.resources.pref_crashlytics_enabled
import pano_scrobbler.composeapp.generated.resources.pref_delay
import pano_scrobbler.composeapp.generated.resources.pref_delay_mins
import pano_scrobbler.composeapp.generated.resources.pref_delay_per
import pano_scrobbler.composeapp.generated.resources.pref_enabled_apps_summary
import pano_scrobbler.composeapp.generated.resources.pref_export
import pano_scrobbler.composeapp.generated.resources.pref_export_desc
import pano_scrobbler.composeapp.generated.resources.pref_fetch_missing_album
import pano_scrobbler.composeapp.generated.resources.pref_first_day_of_week
import pano_scrobbler.composeapp.generated.resources.pref_imexport
import pano_scrobbler.composeapp.generated.resources.pref_import
import pano_scrobbler.composeapp.generated.resources.pref_link_heart_button_rating
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
import pano_scrobbler.composeapp.generated.resources.proxy
import pano_scrobbler.composeapp.generated.resources.regex_rules
import pano_scrobbler.composeapp.generated.resources.scrobble_services
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.simple_edits
import pano_scrobbler.composeapp.generated.resources.spotify
import pano_scrobbler.composeapp.generated.resources.system
import pano_scrobbler.composeapp.generated.resources.when_not_using
import java.util.Calendar
import java.util.Locale


@Composable
fun PrefsScreen(
    onNavigate: (PanoRoute) -> Unit,
    scrobblerStateFlow: StateFlow<ScrobblerState>,
    modifier: Modifier = Modifier,
) {
    val onNavigateToBilling = { onNavigate(PanoRoute.Billing) }

    val mainPrefs = remember { PlatformStuff.mainPrefs }
    val scrobblerState by scrobblerStateFlow.collectAsStateWithLifecycle()

    val scrobblerEnabled by mainPrefs.data.collectAsStateWithInitialValue { it.scrobblerEnabled }
    val allowedPackages by mainPrefs.data.collectAsStateWithInitialValue { it.allowedPackages }
    val scrobbleSpotifyRemoteP by mainPrefs.data.collectAsStateWithInitialValue { it.scrobbleSpotifyRemoteP }
    val autoDetectApps by mainPrefs.data.collectAsStateWithInitialValue { it.autoDetectApps }
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
    val proxyHostPort by Requesters.proxyHostPort.collectAsStateWithLifecycle()
    val scrobblableLabels by
    mainPrefs.data.collectAsStateWithInitialValue { p -> p.scrobbleAccounts.associate { it.type to it.user.name } }
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
    var crashReporterEnabled by remember { mutableStateOf(CrashReporterConfig.isEnabled) }
    val isLicenseValid = LocalLicenseValidState.current

    val maybeActivity = getActivityOrNull()

    var searchTerm by rememberSaveable { mutableStateOf("") }
    val searchActive = searchTerm.isNotBlank()
    val keysToTitleRes = remember { mutableMapOf<String, TitleStringResource>() }
    var filteredKeys by remember { mutableStateOf(setOf<String>()) }
    var localeChanged by remember { mutableStateOf(false) }

    LaunchedEffect(searchTerm, localeChanged) {
        delay(500)

        if (searchActive) {
            val fk = mutableSetOf<String>()
            var prevHeaderKey: String? = null

            for ((k, v) in keysToTitleRes) {
                val isHeader = k.startsWith("header_")

                if (isHeader) {
                    prevHeaderKey = k
                } else {
                    // init if needed

                    if (v.string == null || localeChanged) {
                        v.string = if (v.formatRes == null)
                            getString(v.res)
                        else {
                            getString(
                                v.res,
                                getString(v.formatRes)
                            )
                        }
                    }

                    if (v.string?.contains(searchTerm, ignoreCase = true) == true) {
                        if (prevHeaderKey != null) {
                            fk += prevHeaderKey
                            prevHeaderKey = null
                        }

                        fk += k
                    }
                }
            }

            localeChanged = false
            filteredKeys = fk
        }
    }

    LaunchedEffect(Unit) {
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
        fun filteredItem(
            key: String,
            titleRes: StringResource,
            formatRes: StringResource? = null,
            content: @Composable (title: String) -> Unit
        ) {
            keysToTitleRes.computeIfAbsent(key) {
                TitleStringResource(titleRes, formatRes)
            }

            if (key in filteredKeys || !searchActive) {
                item(key) {
                    val titleStr = if (formatRes == null)
                        stringResource(titleRes)
                    else
                        stringResource(titleRes, stringResource(formatRes))

                    content(titleStr)
                }
            }
        }

        fun filteredHeader(
            keySuffix: String,
            titleRes: StringResource,
            imageVector: ImageVector,
        ) {
            val key = "header_$keySuffix"

            keysToTitleRes.computeIfAbsent(key) {
                TitleStringResource(titleRes, null)
            }

            if (key in filteredKeys || !searchActive) {
                item(key) {
                    SimpleHeaderItem(
                        text = stringResource(titleRes),
                        icon = imageVector,
                    )
                }
            }
        }

        stickyHeader("search_field") {
            Surface {
                SearchField(
                    searchTerm,
                    onSearchTermChange = { searchTerm = it },
                    modifier = Modifier.padding(horizontal = horizontalOverscanPadding())
                )
            }
        }

        filteredHeader("scrobbling", Res.string.scrobbles, Icons.MusicNote)

        PlatformSpecificPrefs.prefScrobbler(
            ::filteredItem,
            scrobblerEnabled,
            scrobblerState != ScrobblerState.NLSDisabled,
            onNavigate
        )

        PlatformSpecificPrefs.prefQuickSettings(::filteredItem, scrobblerEnabled)

        PlatformSpecificPrefs.prefAutostart(::filteredItem)

        filteredItem(MainPrefs::allowedPackages.name, Res.string.pref_scrobble_from) { title ->
            Column(Modifier.fillMaxWidth()) {
                AppIconsPref(
                    packageNames = allowedPackages,
                    title = title,
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
                Text(
                    text = stringResource(Res.string.pref_enabled_apps_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        vertical = 16.dp,
                        horizontal = horizontalOverscanPadding()
                    )
                )
            }
        }

        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop) {
            filteredItem(MainPrefs::autoDetectApps.name, Res.string.pref_auto_detect) { title ->
                val notiEnabled =
                    remember { PanoNotifications.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_NEW_APP) }

                SwitchPref(
                    text = title,
                    summary = if (!notiEnabled) stringResource(Res.string.notification_channel_blocked) else null,
                    value = notiEnabled && autoDetectApps,
                    enabled = notiEnabled,
                    copyToSave = { copy(autoDetectApps = it) },
                )
            }
        }

        if (!PlatformStuff.isDesktop && !PlatformStuff.isTv) {
            filteredItem(
                MainPrefs::scrobbleSpotifyRemoteP.name,
                Res.string.pref_spotify_remote
            ) { title ->
                SwitchPref(
                    text = title,
                    value = scrobbleSpotifyRemoteP,
                    copyToSave = { copy(scrobbleSpotifyRemote = it) }
                )
            }
        }

        filteredItem(
            MainPrefs::extractFirstArtistPackages.name,
            Res.string.first_artist
        ) { title ->
            AppIconsPref(
                packageNames = extractFirstArtistPackages,
                title = title,
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

        filteredItem(MainPrefs::submitNowPlaying.name, Res.string.pref_now_playing) { title ->
            SwitchPref(
                text = title,
                value = submitNowPlaying,
                copyToSave = { copy(submitNowPlaying = it) }
            )
        }

        filteredItem(MainPrefs::minDurationSecsP.name, Res.string.min_track_duration) { title ->
            SliderPref(
                text = title,
                value = minDurationSecs.toFloat(),
                copyToSave = { copy(minDurationSecs = it) },
                min = MainPrefs.PREF_MIN_DURATON_SECS_MIN,
                max = MainPrefs.PREF_MIN_DURATON_SECS_MAX,
                increments = 5,
                stringRepresentation = { Stuff.humanReadableDuration(it * 1000L) }
            )
        }

        PlatformSpecificPrefs.discordRpc(::filteredItem, onNavigate)

        filteredHeader("delay", Res.string.pref_delay, Icons.HourglassEmpty)

        filteredItem(MainPrefs::delayPercentP.name, Res.string.pref_delay_per) { title ->
            SliderPref(
                text = title,
                value = delayPercent.toFloat(),
                copyToSave = { copy(delayPercent = it) },
                min = MainPrefs.PREF_DELAY_PER_MIN,
                max = MainPrefs.PREF_DELAY_PER_MAX,
                increments = 1,
                stringRepresentation = { "${it}%" }
            )
        }

        filteredItem(MainPrefs::delaySecsP.name, Res.string.pref_delay_mins) { title ->
            SliderPref(
                text = title,
                value = delaySecs.toFloat(),
                copyToSave = { copy(delaySecs = it) },
                min = MainPrefs.PREF_DELAY_SECS_MIN,
                max = MainPrefs.PREF_DELAY_SECS_MAX,
                increments = 5,
                stringRepresentation = { Stuff.humanReadableDuration(it * 1000L) }
            )
        }

        filteredHeader("personalization", Res.string.pref_personalization, Icons.Person)

        filteredItem(MainPrefs::themeName.name, Res.string.pref_themes) { title ->
            TextPref(
                text = title,
                locked = !isLicenseValid,
                onClick = {
                    onNavigate(PanoRoute.ThemeChooser)
                }
            )
        }

        if (PlatformStuff.isDesktop) {
            filteredItem(
                MainPrefs::trayIconTheme.name,
                Res.string.pref_tray_icon_theme
            ) { title ->
                DropdownPref(
                    text = title,
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

        PlatformSpecificPrefs.prefChartsWidget(::filteredItem)

        filteredItem(
            MainPrefs::showScrobbleSources.name,
            Res.string.pref_show_scrobble_sources
        ) { title ->
            SwitchPref(
                text = title,
                summary = stringResource(Res.string.pref_show_scrobble_sources_desc),
                value = showScrobbleSources,
                onNavigateToBilling = onNavigateToBilling.takeIf { !isLicenseValid },
                copyToSave = { copy(showScrobbleSources = it) }
            )
        }

        if (!PlatformStuff.isDesktop) {
            filteredItem(
                MainPrefs::searchInSource.name,
                Res.string.pref_search_in_source
            ) { title ->
                SwitchPref(
                    text = title,
                    summary = stringResource(Res.string.pref_search_in_source_desc),
                    value = searchInSource,
                    onNavigateToBilling = onNavigateToBilling.takeIf { !isLicenseValid },
                    enabled = showScrobbleSources,
                    copyToSave = { copy(searchInSource = it) }
                )
            }
        }

        if (!PlatformStuff.isTv) {
            filteredItem(
                MainPrefs::searchUrlTemplate.name,
                Res.string.pref_search_url_template
            ) { title ->
                TextPref(
                    text = title,
                    summary = searchUrlTemplate,
                    onClick = {
                        onNavigate(PanoRoute.Modal.MediaSearchPref)
                    }
                )
            }
        }

        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop) {
            filteredItem(
                MainPrefs::linkHeartButtonToRating.name,
                Res.string.pref_link_heart_button_rating
            ) { title ->
                SwitchPref(
                    text = title,
                    summary = stringResource(Res.string.pref_search_in_source_desc),
                    value = linkHeartButtonToRating,
                    onNavigateToBilling = onNavigateToBilling.takeIf { !isLicenseValid },
                    copyToSave = { copy(linkHeartButtonToRating = it) }
                )
            }
        }

        filteredItem(
            MainPrefs::firstDayOfWeek.name,
            Res.string.pref_first_day_of_week
        ) { title ->
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
                text = title,
                selectedValue = firstDayOfWeek,
                values = valuesToDays.keys,
                toLabel = { valuesToDays[it] ?: autoString },
                copyToSave = { copy(firstDayOfWeek = it) }
            )
        }

        PlatformSpecificPrefs.prefNotifications(::filteredItem)

        filteredHeader("lists", Res.string.simple_edits, Icons.EditNote)

        filteredItem("simple_edits", Res.string.simple_edits) { title ->
            TextPref(
                text = title + ": " + numSimpleEdits.format(),
                onClick = {
                    onNavigate(PanoRoute.SimpleEdits)
                }
            )
        }

        filteredItem("regex_edits", Res.string.regex_rules) { title ->
            TextPref(
                text = title + ": " + numRegexEdits.format(),
                onClick = {
                    onNavigate(PanoRoute.RegexEdits)
                }
            )
        }

        filteredItem("blocked_metadata", Res.string.pref_blocked_metadata) { title ->
            TextPref(
                text = title + ": " + numBlockedMetadata.format(),
                onClick = {
                    onNavigate(PanoRoute.BlockedMetadatas)
                },
                locked = !isLicenseValid,
            )
        }

        filteredHeader("additional_metatadata", Res.string.external_metadata, Icons.Api)

        filteredItem(MainPrefs::lastfmApiAlways.name, Res.string.lastfm) { title ->
            SwitchPref(
                text = title,
                value = lastfmApiAlways,
                summary = stringResource(
                    Res.string.when_not_using,
                    stringResource(Res.string.lastfm)
                ),
                copyToSave = { copy(lastfmApiAlways = it) }
            )
        }

        filteredItem(
            MainPrefs::fetchAlbum.name,
            Res.string.pref_fetch_missing_album,
            Res.string.cache
        ) { title ->
            SwitchPref(
                text = stringResource(
                    Res.string.pref_fetch_missing_album,
                    stringResource(Res.string.cache) + " & " +
                            stringResource(Res.string.lastfm)
                ),
                value = fetchAlbum,
                copyToSave = { copy(fetchAlbum = it) }
            )
        }

        filteredItem(MainPrefs::spotifyApi.name, Res.string.spotify) { title ->
            SwitchPref(
                text = title,
                summary = stringResource(Res.string.search) + ": " +
                        stringResource(Res.string.artist_image) + ", " +
                        stringResource(Res.string.album_art),
                value = useSpotify,
                copyToSave = { copy(spotifyApi = it) }
            )
        }

        filteredItem(
            MainPrefs::spotifyCountryP.name,
            Res.string.country_for_api,
            Res.string.spotify
        ) { title ->
            val countryCodes = remember { Locale.getISOCountries().toList() }

            DropdownPref(
                text = title,
                selectedValue = spotifyCountryP,
                values = countryCodes,
                toLabel = { it },
                copyToSave = { copy(spotifyCountry = it) },
                enabled = useSpotify
            )
        }

        filteredItem(
            MainPrefs::spotifyArtistSearchApproximate.name,
            Res.string.pref_spotify_artist_search_approximate
        ) { title ->
            SwitchPref(
                text = title,
                value = spotifyArtistSearchApproximate,
                copyToSave = { copy(spotifyArtistSearchApproximate = it) },
                enabled = useSpotify
            )
        }

        PlatformSpecificPrefs.deezerApi(::filteredItem, deezerApi)

        PlatformSpecificPrefs.tidalSteelSeries(::filteredItem, tidalSteelSeries)

        filteredHeader("languages", Res.string.pref_locale, Icons.Translate)

        filteredItem(LocaleUtils::locale.name, Res.string.pref_locale) { title ->
            val autoString = stringResource(Res.string.auto)

            val localesMap = remember(locale) {
                val autoEntry = mapOf("auto" to autoString)
                LocaleUtils.localesMap().let {
                    autoEntry + it
                }
            }

            DropdownPref(
                text = title,
                selectedValue = locale,
                values = localesMap.keys,
                toLabel = { localesMap[it] ?: autoString },
                copyToSave = {
                    val l = it.takeIf { it != "auto" }
                    LocaleUtils.setAppLocale(lang = l, maybeActivity)
                    localeChanged = true
                    this
                }
            )
        }

        filteredItem("translate", Res.string.pref_translate) { title ->
            TextPref(
                text = title,
                onClick = {
                    PlatformStuff.openInBrowser(Stuff.CROWDIN_URL)
                }
            )
        }

        filteredItem("translate_credits", Res.string.pref_translate_credits) { title ->
            TextPref(
                text = title,
                onClick = {
                    onNavigate(PanoRoute.Translators)
                }
            )
        }

        filteredHeader("imexport", Res.string.pref_imexport, Icons.SwapVert)

        filteredItem("export", Res.string.pref_export) { title ->
            TextPref(
                text = title,
                summary = stringResource(Res.string.pref_export_desc),
                onClick = {
                    onNavigate(PanoRoute.Export)
                }
            )
        }

        filteredItem("import", Res.string.pref_import) { title ->
            TextPref(
                text = title,
                onClick = {
                    onNavigate(PanoRoute.Import)
                }
            )
        }

        filteredHeader("services", Res.string.scrobble_services, Icons.Dns)

        AccountType.entries
            .filterNot {
                PlatformStuff.isTv && it == AccountType.FILE
            }
            .forEach { accountType ->
                val (strRes, formatRes) = accountTypeStringRes(accountType)
                filteredItem(
                    accountType.name,
                    strRes,
                    formatRes
                ) { title ->
                    AccountPref(
                        title,
                        type = accountType,
                        usernamesMap = scrobblableLabels,
                        onNavigate = onNavigate
                    )
                }
            }

        filteredItem(key = "delete_account", Res.string.delete_account) { title ->
            TextPref(
                text = title,
                onClick = {
                    onNavigate(PanoRoute.DeleteAccount)
                }
            )
        }

        filteredHeader("misc", Res.string.pref_misc, Icons.MoreHoriz)

        if (!PlatformStuff.isDesktop && !PlatformStuff.isTv) {
            filteredItem(
                MainPrefs::preventDuplicateAmbientScrobbles.name,
                Res.string.pref_prevent_duplicate_ambient_scrobbles
            ) { title ->
                SwitchPref(
                    text = title,
                    value = preventDuplicateAmbientScrobbles,
                    copyToSave = { copy(preventDuplicateAmbientScrobbles = it) }
                )
            }
        }

        filteredItem(MainPrefs::customProxyEnabled.name, Res.string.proxy) { title ->
            TextPref(
                text = title,
                summary = proxyHostPort?.let { (host, port) ->
                    "$host:$port"
                } ?: stringResource(Res.string.system),
                onClick = {
                    onNavigate(PanoRoute.Modal.ProxyPref)
                }
            )
        }

        if (VariantStuff.githubApiUrl != null) {
            filteredItem(MainPrefs::autoUpdates.name, Res.string.pref_notify_updates) { title ->
                SwitchPref(
                    text = title,
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

            filteredItem("check_for_updates", Res.string.pref_check_updates) { title ->
                TextPref(
                    text = updateProgress?.message ?: title,
                    enabled = updateProgress == null,
                    onClick = {
                        if (updateProgress == null)
                            UpdaterWork.schedule(true)
                    }
                )
            }
        }

        PlatformSpecificPrefs.prefPersistentNoti(::filteredItem, notiPersistent)

        if (!PlatformStuff.isTv) {
            filteredItem("automation", Res.string.automation) { title ->
                TextPref(
                    text = title,
                    onClick = {
                        onNavigate(PanoRoute.AutomationInfo)
                    },
                    locked = !isLicenseValid,
                )
            }
        }

        if (CrashReporterConfig.isAvailable) {
            filteredItem(
                "crash_reporter",
                Res.string.pref_crashlytics_enabled
            ) { title ->
                SwitchPref(
                    text = title,
                    value = crashReporterEnabled,
                    copyToSave = {
                        crashReporterEnabled = it
                        CrashReporterConfig.isEnabled = it
                        this
                    }
                )
            }
        }

        filteredHeader("about", Res.string.pref_about, Icons.Info)

        filteredItem(key = "oss_credits", Res.string.pref_oss_credits) { title ->
            TextPref(
                text = title,
                onClick = {
                    onNavigate(PanoRoute.OssCredits)
                }
            )
        }

        filteredItem(key = "privacy_policy", Res.string.pref_privacy_policy) { title ->
            TextPref(
                text = title,
                onClick = {
                    onNavigate(PanoRoute.PrivacyPolicy)
                }
            )
        }

        filteredItem(
            key = "github_link",
            Res.string.also_available_on,
            if (PlatformStuff.isDesktop)
                Res.string.android
            else
                Res.string.desktop
        ) { title ->
            TextPref(
                text = title,
                summary = Stuff.HOMEPAGE_URL + "\n" +
                        ("v" + BuildKonfig.VER_NAME + if (BuildKonfig.DEBUG) " (Debug)" else ""),
                onClick = {
                    onNavigate(PanoRoute.Modal.ShowLink(Stuff.HOMEPAGE_URL))
                }
            )
        }

        if (BuildKonfig.DEBUG) {
            filteredHeader("debug", Res.string.debug_menu, Icons.BugReport)

            filteredItem(MainPrefs::demoModeP.name, Res.string.demo_mode) { title ->
                SwitchPref(
                    text = title,
                    value = demoMode,
                    copyToSave = { copy(demoMode = it) }
                )
            }

            filteredItem("copy_sk", Res.string.copy_sk) { title ->
                val scope = rememberCoroutineScope()

                TextPref(
                    text = title,
                    onClick = {
                        scope.launch {
                            PlatformStuff.mainPrefs.data
                                .map { p ->
                                    p.scrobbleAccounts.firstOrNull {
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

            filteredItem("delete_receipt", Res.string.delete_receipt) { title ->
                val scope = rememberCoroutineScope()

                TextPref(
                    text = title,
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

typealias FilteredItem = (
    key: String,
    titleRes: StringResource,
    formatRes: StringResource?,
    content: @Composable (title: String) -> Unit
) -> Unit

private data class TitleStringResource(
    val res: StringResource,
    val formatRes: StringResource?,
    var string: String? = null
)

