package com.arn.scrobble.pref

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.getCurrentLocale
import com.arn.scrobble.utils.setAppLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.auto
import pano_scrobbler.composeapp.generated.resources.choose_apps
import pano_scrobbler.composeapp.generated.resources.copy_sk
import pano_scrobbler.composeapp.generated.resources.crowdin_link
import pano_scrobbler.composeapp.generated.resources.debug_menu
import pano_scrobbler.composeapp.generated.resources.delete_account
import pano_scrobbler.composeapp.generated.resources.delete_receipt
import pano_scrobbler.composeapp.generated.resources.demo_mode
import pano_scrobbler.composeapp.generated.resources.github_link
import pano_scrobbler.composeapp.generated.resources.grant_notification_access
import pano_scrobbler.composeapp.generated.resources.notification_channel_blocked
import pano_scrobbler.composeapp.generated.resources.num_blocked_metadata
import pano_scrobbler.composeapp.generated.resources.num_regex_edits
import pano_scrobbler.composeapp.generated.resources.num_simple_edits
import pano_scrobbler.composeapp.generated.resources.pref_about
import pano_scrobbler.composeapp.generated.resources.pref_auto_detect
import pano_scrobbler.composeapp.generated.resources.pref_check_updates
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
import pano_scrobbler.composeapp.generated.resources.pref_master
import pano_scrobbler.composeapp.generated.resources.pref_misc
import pano_scrobbler.composeapp.generated.resources.pref_native_file_picker
import pano_scrobbler.composeapp.generated.resources.pref_now_playing
import pano_scrobbler.composeapp.generated.resources.pref_offline_info
import pano_scrobbler.composeapp.generated.resources.pref_oss_credits
import pano_scrobbler.composeapp.generated.resources.pref_personalization
import pano_scrobbler.composeapp.generated.resources.pref_prevent_duplicate_ambient_scrobbles
import pano_scrobbler.composeapp.generated.resources.pref_privacy_policy
import pano_scrobbler.composeapp.generated.resources.pref_search_in_source
import pano_scrobbler.composeapp.generated.resources.pref_search_in_source_desc
import pano_scrobbler.composeapp.generated.resources.pref_search_url_template
import pano_scrobbler.composeapp.generated.resources.pref_search_url_template_desc
import pano_scrobbler.composeapp.generated.resources.pref_show_scrobble_sources
import pano_scrobbler.composeapp.generated.resources.pref_show_scrobble_sources_desc
import pano_scrobbler.composeapp.generated.resources.pref_spotify_artist_search_approximate
import pano_scrobbler.composeapp.generated.resources.pref_spotify_remote
import pano_scrobbler.composeapp.generated.resources.pref_themes
import pano_scrobbler.composeapp.generated.resources.pref_translate
import pano_scrobbler.composeapp.generated.resources.pref_translate_credits
import pano_scrobbler.composeapp.generated.resources.privacy_policy_link
import pano_scrobbler.composeapp.generated.resources.scrobble_services
import java.util.Calendar
import java.util.Locale


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PrefsScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onNavigateToBilling = { onNavigate(PanoRoute.Billing) }

    val mainPrefs = remember { PlatformStuff.mainPrefs }
    val nlsEnabled = remember { PlatformStuff.isNotificationListenerEnabled() }

    // visible prefs
    val mainPrefsData by mainPrefs.data.collectAsStateWithInitialValue { it }

    val scrobblerEnabled by
    remember { derivedStateOf { mainPrefsData.scrobblerEnabled } }
    val allowedPackages by
    remember { derivedStateOf { mainPrefsData.allowedPackages } }
    val allowedAutomationPackages by
    remember { derivedStateOf { mainPrefsData.allowedAutomationPackages } }
    val seenAppsMap by
    remember { derivedStateOf { mainPrefsData.seenApps.associate { it.appId to it.friendlyLabel } } }
    val scrobbleSpotifyRemote by
    remember { derivedStateOf { mainPrefsData.scrobbleSpotifyRemote } }
    val autoDetectApps by
    remember { derivedStateOf { mainPrefsData.autoDetectAppsP } }
    val delayPercent by remember { derivedStateOf { mainPrefsData.delayPercentP } }
    val delaySecs by remember { derivedStateOf { mainPrefsData.delaySecsP } }
    val showScrobbleSources by
    remember { derivedStateOf { mainPrefsData.showScrobbleSources } }
    val searchInSource by remember { derivedStateOf { mainPrefsData.searchInSource } }
    val searchUrlTemplate by remember { derivedStateOf { mainPrefsData.searchUrlTemplate } }
    val linkHeartButtonToRating by
    remember { derivedStateOf { mainPrefsData.linkHeartButtonToRating } }
    val firstDayOfWeek by remember { derivedStateOf { mainPrefsData.firstDayOfWeek } }
    val locale by remember { derivedStateOf { mainPrefsData.locale } }
    val fetchAlbum by remember { derivedStateOf { mainPrefsData.fetchAlbum } }
    val spotifyArtistSearchApproximate by
    remember { derivedStateOf { mainPrefsData.spotifyArtistSearchApproximate } }
    val preventDuplicateAmbientScrobbles by
    remember { derivedStateOf { mainPrefsData.preventDuplicateAmbientScrobbles } }
    val submitNowPlaying by
    remember { derivedStateOf { mainPrefsData.submitNowPlaying } }
    val useNativeFilePicker by
    remember { derivedStateOf { mainPrefsData.useNativeFilePicker } }
    val notiPersistent by
    remember { derivedStateOf { mainPrefsData.notiPersistent } }
    val checkForUpdates by
    remember { derivedStateOf { mainPrefsData.checkForUpdates } }
    val crashReporterEnabled by
    remember { derivedStateOf { mainPrefsData.crashReporterEnabled } }
    val demoMode by remember { derivedStateOf { mainPrefsData.demoModeP } }
    var isAddedToStartup by remember { mutableStateOf(false) }
    val scrobblableLabels by remember {
        derivedStateOf {
            mainPrefsData.scrobbleAccounts.associate { it.type to it.user.name }
        }
    }

    val numSimpleEdits by PanoDb.db.getSimpleEditsDao().count().collectAsStateWithLifecycle(0)
    val numRegexEdits by PanoDb.db.getRegexEditsDao().count().collectAsStateWithLifecycle(0)
    val numBlockedMetadata by PanoDb.db.getBlockedMetadataDao().count()
        .collectAsStateWithLifecycle(0)

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            isAddedToStartup = isAddedToStartup()
        }
    }



    PanoLazyColumn(modifier = modifier) {
        item(MainPrefs::scrobblerEnabled.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_master),
                summary = stringResource(if (nlsEnabled) Res.string.pref_offline_info else Res.string.grant_notification_access),
                value = scrobblerEnabled,
                enabled = nlsEnabled,
                copyToSave = { copy(scrobblerEnabled = it) }
            )
        }

        prefQuickSettings(this, scrobblerEnabled)

        addToStartup(this, isAddedToStartup) {
            isAddedToStartup = it
        }

        stickyHeader("apps_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.choose_apps),
                icon = Icons.Outlined.Apps
            )
        }

        item(MainPrefs::allowedPackages.name) {
            AppIconsPref(
                packageNames = allowedPackages,
                seenAppsMap = seenAppsMap,
                summary = stringResource(Res.string.pref_enabled_apps_summary),
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

        if (!PlatformStuff.isDesktop) {
            item(MainPrefs::scrobbleSpotifyRemote.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_spotify_remote),
                    value = scrobbleSpotifyRemote,
                    copyToSave = { copy(scrobbleSpotifyRemote = it) }
                )
            }
        }

        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop) {
            item(MainPrefs::autoDetectAppsP.name) {
                val notiEnabled =
                    remember { PlatformStuff.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_NEW_APP) }

                SwitchPref(
                    text = stringResource(Res.string.pref_auto_detect),
                    summary = if (!notiEnabled && !PlatformStuff.isDesktop) stringResource(Res.string.notification_channel_blocked) else null,
                    value = autoDetectApps,
                    enabled = notiEnabled,
                    copyToSave = { copy(autoDetectApps = it) },
                )
            }
        }

        stickyHeader("pref_delay_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_delay),
                icon = Icons.Outlined.HourglassEmpty
            )
        }

        item(MainPrefs::delayPercentP.name) {
            if (delayPercent > 0) {
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
        }

        item(MainPrefs::delaySecsP.name) {
            if (delaySecs > 0) {
                SliderPref(
                    text = stringResource(Res.string.pref_delay_mins),
                    value = delaySecs.toFloat(),
                    copyToSave = { copy(delaySecs = it) },
                    min = MainPrefs.PREF_DELAY_SECS_MIN,
                    max = MainPrefs.PREF_DELAY_SECS_MAX,
                    increments = 10,
                    stringRepresentation = { Stuff.humanReadableDuration(it * 1000L) }
                )
            }
        }

        stickyHeader("personalization_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_personalization),
                icon = Icons.Outlined.Person
            )
        }

        item(MainPrefs::themeName.name) {
            TextPref(
                text = stringResource(Res.string.pref_themes),
                onNavigateToBilling = onNavigateToBilling,
                onClick = {
                    onNavigate(PanoRoute.ThemeChooser)
                }
            )
        }

        prefChartsWidget(this)

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
                onNavigateToBilling = onNavigateToBilling,
                copyToSave = { copy(showScrobbleSources = it) }
            )
        }

        if (!PlatformStuff.isDesktop) {
            item(MainPrefs::searchInSource.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_search_in_source),
                    summary = stringResource(Res.string.pref_search_in_source_desc),
                    value = searchInSource,
                    onNavigateToBilling = onNavigateToBilling,
                    enabled = showScrobbleSources,
                    copyToSave = { copy(searchInSource = it) }
                )
            }
        }

        if (PlatformStuff.isDesktop) {
            item(MainPrefs::searchUrlTemplate.name) {
                TextFieldDialogPref(
                    text = stringResource(Res.string.pref_search_url_template),
                    hint = stringResource(Res.string.pref_search_url_template_desc),
                    value = searchUrlTemplate,
                    validate = { it.contains("\$query") },
                    copyToSave = { copy(searchUrlTemplate = it) }
                )
            }
        }

        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop) {
            item(MainPrefs::linkHeartButtonToRating.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_link_heart_button_rating),
                    summary = stringResource(Res.string.pref_search_in_source_desc),
                    value = linkHeartButtonToRating,
                    onNavigateToBilling = onNavigateToBilling,
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

        prefNotifications(this)

        stickyHeader("lists_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_lists),
                icon = Icons.AutoMirrored.Outlined.List
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
                text = pluralStringResource(
                    Res.plurals.num_regex_edits,
                    numRegexEdits,
                    numRegexEdits
                ),
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
                }
            )
        }

        stickyHeader("languages_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_locale),
                icon = Icons.Outlined.Translate
            )
        }

        item(MainPrefs::locale.name) {
            val autoString = stringResource(Res.string.auto)
            val currentLocale = remember(locale) { getCurrentLocale(locale) }

            val localesMap = remember {
                val autoEntry = mapOf("auto" to autoString)
                LocaleUtils.localesSet.associateWith {
                    val localeObj = Locale.forLanguageTag(it)
                    val displayLanguage = localeObj.displayLanguage

                    val suffix = when (localeObj.language) {
                        in LocaleUtils.showScriptSet -> " (${localeObj.displayScript})"
                        in LocaleUtils.showCountrySet -> localeObj.displayCountry
                            .ifEmpty { null }
                            ?.let { " ($it)" } ?: ""

                        else -> ""
                    }

                    displayLanguage + suffix
                }.let { autoEntry + it }
            }

            DropdownPref(
                text = stringResource(Res.string.pref_locale),
                selectedValue = currentLocale,
                values = localesMap.keys,
                toLabel = { localesMap[it] ?: autoString },
                copyToSave = {
                    setAppLocale(lang = it, force = true)
                    copy(locale = it.takeIf { it != "auto" })
                }
            )
        }

        item("translate") {
            val crowdinLink = stringResource(Res.string.crowdin_link)

            TextPref(
                text = stringResource(Res.string.pref_translate),
                onClick = {
                    PlatformStuff.openInBrowser(crowdinLink)
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

        stickyHeader("misc_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_misc),
                icon = Icons.Outlined.MoreHoriz
            )
        }

        item(MainPrefs::fetchAlbum.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_fetch_album),
                value = fetchAlbum,
                copyToSave = { copy(fetchAlbum = it) }
            )
        }

        item(MainPrefs::spotifyArtistSearchApproximate.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_spotify_artist_search_approximate),
                value = spotifyArtistSearchApproximate,
                copyToSave = { copy(spotifyArtistSearchApproximate = it) }
            )
        }

        if (!PlatformStuff.isDesktop) {
            item(MainPrefs::preventDuplicateAmbientScrobbles.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_prevent_duplicate_ambient_scrobbles),
                    value = preventDuplicateAmbientScrobbles,
                    copyToSave = { copy(preventDuplicateAmbientScrobbles = it) }
                )
            }
        }

        item(MainPrefs::submitNowPlaying.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_now_playing),
                value = submitNowPlaying,
                copyToSave = { copy(submitNowPlaying = it) }
            )
        }

        if (PlatformStuff.isDesktop) {
            item(MainPrefs::useNativeFilePicker.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_native_file_picker),
                    value = useNativeFilePicker,
                    copyToSave = { copy(useNativeFilePicker = it) }
                )
            }
        }

        if (PlatformStuff.isNonPlayBuild) {
            item(MainPrefs::checkForUpdates.name) {
                SwitchPref(
                    text = stringResource(Res.string.pref_check_updates),
                    value = checkForUpdates,
                    copyToSave = { copy(checkForUpdates = it) }
                )
            }
        }

        prefPersistentNoti(this, notiPersistent)

        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop) {
            prefAutomation(this)

            item(MainPrefs::allowedAutomationPackages.name) {
                AppIconsPref(
                    packageNames = allowedAutomationPackages,
                    seenAppsMap = seenAppsMap,
                    onClick = {
                        onNavigate(
                            PanoRoute.AppList(
                                saveType = AppListSaveType.Automation,
                                preSelectedPackages = allowedAutomationPackages.toList(),
                                isSingleSelect = false,
                            )
                        )
                    }
                )
            }

        }

        prefCrashReporter(this, crashReporterEnabled)

        stickyHeader("imexport_header") {
            SimpleHeaderItem(
                text = stringResource(Res.string.pref_imexport),
                icon = Icons.Outlined.SwapVert
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
                icon = Icons.Outlined.Dns
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
                icon = Icons.Outlined.Info
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
            val privacyPolicyLink = stringResource(Res.string.privacy_policy_link)

            TextPref(
                text = stringResource(Res.string.pref_privacy_policy),
                onClick = {
                    if (PlatformStuff.isDesktop)
                        PlatformStuff.openInBrowser(privacyPolicyLink)
                    else
                        onNavigate(PanoRoute.WebView(privacyPolicyLink))
                }
            )
        }

        item(key = "github_link") {
            val githubLink = stringResource(Res.string.github_link)

            TextPref(
                text = "v " + BuildKonfig.VER_NAME,
                summary = githubLink,
                onClick = {
                    PlatformStuff.openInBrowser(githubLink)
                }
            )
        }

        if (PlatformStuff.isDebug) {
            stickyHeader("debug_header") {
                SimpleHeaderItem(
                    text = stringResource(Res.string.debug_menu),
                    icon = Icons.Outlined.BugReport
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
                        Scrobblables.all.value.firstOrNull {
                            it.userAccount.type ==
                                    AccountType.LASTFM
                        }?.userAccount?.authKey?.let {
                            PlatformStuff.copyToClipboard(it)
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

expect fun prefAutomation(listScope: LazyListScope)

expect fun prefNotifications(listScope: LazyListScope)

expect fun prefCrashReporter(listScope: LazyListScope, crashReporterEnabled: Boolean)

expect fun prefQuickSettings(listScope: LazyListScope, scrobblerEnabled: Boolean)

expect fun prefPersistentNoti(listScope: LazyListScope, notiEnabled: Boolean)

expect fun prefChartsWidget(listScope: LazyListScope)

expect fun addToStartup(
    listScope: LazyListScope,
    isAdded: Boolean,
    onAddedChanged: (Boolean) -> Unit,
)

expect suspend fun isAddedToStartup(): Boolean