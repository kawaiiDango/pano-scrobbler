package com.arn.scrobble.pref

import android.annotation.TargetApi
import android.app.LocaleManager
import android.app.PendingIntent
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.MasterSwitchQS
import com.arn.scrobble.NLService
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.LocaleUtils.setLocaleCompat
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.copyToClipboard
import com.arn.scrobble.utils.Stuff.isChannelEnabled
import com.arn.scrobble.utils.UiUtils.toast
import com.arn.scrobble.widget.ChartsWidgetConfigActivity
import com.arn.scrobble.widget.ChartsWidgetProvider
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale


@Composable
fun PrefsScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val onNavigateToBilling = { onNavigate(PanoRoute.Billing) }

    val mainPrefs = remember { PlatformStuff.mainPrefs }
    val context = LocalContext.current
    val nlsEnabled = remember { Stuff.isNotificationListenerEnabled() }

    val autoString = stringResource(R.string.auto)

    // visible prefs
    val mainPrefsData by mainPrefs.data.collectAsStateWithLifecycle(null)

    if (mainPrefsData == null) return

    val scrobblerEnabled by
    remember { derivedStateOf { mainPrefsData!!.scrobblerEnabled } }
    val allowedPackages by
    remember { derivedStateOf { mainPrefsData!!.allowedPackages } }
    val scrobbleSpotifyRemote by
    remember { derivedStateOf { mainPrefsData!!.scrobbleSpotifyRemote } }
    val autoDetectApps by
    remember { derivedStateOf { mainPrefsData!!.autoDetectAppsP } }
    val delayPercent by remember { derivedStateOf { mainPrefsData!!.delayPercentP } }
    val delaySecs by remember { derivedStateOf { mainPrefsData!!.delaySecsP } }
    val showAlbumsInRecents by
    remember { derivedStateOf { mainPrefsData!!.showAlbumsInRecents } }
    val showScrobbleSources by
    remember { derivedStateOf { mainPrefsData!!.showScrobbleSources } }
    val searchInSource by remember { derivedStateOf { mainPrefsData!!.searchInSource } }
    val linkHeartButtonToRating by
    remember { derivedStateOf { mainPrefsData!!.linkHeartButtonToRating } }
    val firstDayOfWeek by remember { derivedStateOf { mainPrefsData!!.firstDayOfWeek } }
    val locale by remember { derivedStateOf { mainPrefsData!!.locale } }
    val fetchAlbum by remember { derivedStateOf { mainPrefsData!!.fetchAlbum } }
    val spotifyArtistSearchApproximate by
    remember { derivedStateOf { mainPrefsData!!.spotifyArtistSearchApproximate } }
    val preventDuplicateAmbientScrobbles by
    remember { derivedStateOf { mainPrefsData!!.preventDuplicateAmbientScrobbles } }
    val submitNowPlaying by
    remember { derivedStateOf { mainPrefsData!!.submitNowPlaying } }
    val crashReporterEnabled by
    remember { derivedStateOf { mainPrefsData!!.crashReporterEnabled } }
    val demoMode by remember { derivedStateOf { mainPrefsData!!.demoModeP } }
    val scrobblableLabels by remember {
        derivedStateOf {
            mainPrefsData!!.scrobbleAccounts.associate { it.type to it.user.name }
        }
    }

    val numSimpleEdits by PanoDb.db.getSimpleEditsDao().count().collectAsStateWithLifecycle(0)
    val numRegexEdits by PanoDb.db.getRegexEditsDao().count().collectAsStateWithLifecycle(0)
    val numBlockedMetadata by PanoDb.db.getBlockedMetadataDao().count()
        .collectAsStateWithLifecycle(0)


    var showIntentsDescDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(modifier = modifier, contentPadding = panoContentPadding()) {
        item(MainPrefs::scrobblerEnabled.name) {
            SwitchPref(
                text = stringResource(R.string.pref_master),
                summary = stringResource(if (nlsEnabled) R.string.pref_offline_info else R.string.grant_notification_access),
                value = scrobblerEnabled,
                enabled = nlsEnabled,
                copyToSave = { copy(scrobblerEnabled = it) }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Stuff.isTv) {
            item("master_qs_add") {
                val scrobblerEnabledText =
                    stringResource(if (scrobblerEnabled) R.string.scrobbler_on else R.string.scrobbler_off)

                TextPref(
                    text = stringResource(R.string.pref_master_qs_add),
                    onClick = {
                        val statusBarManager =
                            ContextCompat.getSystemService(
                                context,
                                StatusBarManager::class.java
                            )
                                ?: return@TextPref
                        statusBarManager.requestAddTileService(
                            ComponentName(context, MasterSwitchQS::class.java),
                            scrobblerEnabledText,
                            Icon.createWithResource(context, R.drawable.vd_noti),
                            context.mainExecutor
                        ) { result ->
                            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED)
                                context.toast(R.string.pref_master_qs_already_addded)
                        }
                    }
                )
            }
        }

        stickyHeader("apps_header") {
            SimpleHeaderItem(
                text = stringResource(R.string.choose_apps),
                icon = Icons.Outlined.Apps
            )
        }

        item(MainPrefs::allowedPackages.name) {
            AppIconsPref(
                packageNames = allowedPackages,
                onClick = {
                    onNavigate(
                        PanoRoute.AppList(
                            preSelectedPackages = emptyList(),
                            hasPreSelection = false,
                            isSingleSelect = false,
                        )
                    )
                }
            )
        }

        item(MainPrefs::scrobbleSpotifyRemote.name) {
            SwitchPref(
                text = stringResource(R.string.pref_spotify_remote),
                value = scrobbleSpotifyRemote,
                copyToSave = { copy(scrobbleSpotifyRemote = it) }
            )
        }

        if (!Stuff.isTv) {
            item(MainPrefs::autoDetectAppsP.name) {
                val notiEnabled =
                    remember { PlatformStuff.notificationManager.isChannelEnabled(Stuff.CHANNEL_NOTI_NEW_APP) }

                SwitchPref(
                    text = stringResource(R.string.pref_auto_detect),
                    summary = if (!notiEnabled) stringResource(R.string.notification_channel_blocked) else null,
                    value = autoDetectApps,
                    enabled = notiEnabled,
                    copyToSave = { copy(autoDetectApps = it) },
                )
            }
        }

        stickyHeader("pref_delay_header") {
            SimpleHeaderItem(
                text = stringResource(R.string.pref_delay),
                icon = Icons.Outlined.HourglassEmpty
            )
        }

        item(MainPrefs::delayPercentP.name) {
            if (delayPercent > 0) {
                SliderPref(
                    text = stringResource(R.string.pref_delay_per),
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
                    text = stringResource(R.string.pref_delay_mins),
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
                text = stringResource(R.string.pref_personalization),
                icon = Icons.Outlined.Person
            )
        }

        item(MainPrefs::themeName.name) {
            TextPref(
                text = stringResource(R.string.pref_themes),
                onNavigateToBilling = onNavigateToBilling,
                onClick = {
                    onNavigate(PanoRoute.ThemeChooser)
                }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Stuff.isTv) {
            item("widget") {
                TextPref(
                    text = stringResource(R.string.pref_widget_charts),
                    onClick = {
                        requestPinWidget(context)
                    }
                )
            }
        }

//        item(MainPrefs::showAlbumsInRecents.name) {
//            SwitchPref(
//                text = stringResource(R.string.pref_show_albums),
//                summary = stringResource(R.string.pref_show_albums_desc),
//                value = showAlbumsInRecents,
//                copyToSave = { copy(showAlbumsInRecents = it) }
//            )
//        }

        item(MainPrefs::showScrobbleSources.name) {
            SwitchPref(
                text = stringResource(R.string.pref_show_scrobble_sources),
                summary = stringResource(R.string.pref_show_scrobble_sources_desc),
                value = showScrobbleSources,
                onNavigateToBilling = onNavigateToBilling,
                copyToSave = { copy(showScrobbleSources = it) }
            )
        }

        item(MainPrefs::searchInSource.name) {
            SwitchPref(
                text = stringResource(R.string.pref_search_in_source),
                summary = stringResource(R.string.pref_search_in_source_desc),
                value = searchInSource,
                onNavigateToBilling = onNavigateToBilling,
                enabled = showScrobbleSources,
                copyToSave = { copy(searchInSource = it) }
            )
        }

        if (!Stuff.isTv) {
            item(MainPrefs::linkHeartButtonToRating.name) {
                SwitchPref(
                    text = stringResource(R.string.pref_link_heart_button_rating),
                    summary = stringResource(R.string.pref_search_in_source_desc),
                    value = linkHeartButtonToRating,
                    onNavigateToBilling = onNavigateToBilling,
                    copyToSave = { copy(linkHeartButtonToRating = it) }
                )
            }
        }

        item(MainPrefs::firstDayOfWeek.name) {
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
                text = stringResource(R.string.pref_first_day_of_week),
                selectedValue = firstDayOfWeek,
                values = valuesToDays.keys,
                toLabel = { valuesToDays[it] ?: autoString },
                copyToSave = { copy(firstDayOfWeek = it) }
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !Stuff.isTv) {
            item("notifications") {
                TextPref(
                    text = stringResource(R.string.pref_noti),
                    onClick = {
                        launchNotificationsActivity(context)
                    }
                )
            }
        }

        stickyHeader("lists_header") {
            SimpleHeaderItem(
                text = stringResource(R.string.pref_lists),
                icon = Icons.AutoMirrored.Outlined.List
            )
        }

        item("simple_edits") {
            TextPref(
                text = pluralStringResource(
                    R.plurals.num_simple_edits,
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
                    R.plurals.num_regex_edits,
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
                    R.plurals.num_blocked_metadata,
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
                text = stringResource(R.string.pref_locale),
                icon = Icons.Outlined.Translate
            )
        }

        item(MainPrefs::locale.name) {
            val currentLocale = remember { getCurrentLocale(context, locale) }

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
                text = stringResource(R.string.pref_locale),
                selectedValue = currentLocale,
                values = localesMap.keys,
                toLabel = { localesMap[it] ?: autoString },
                copyToSave = {
                    context.setLocaleCompat(langp = it, force = true)
                    copy(locale = it.takeIf { it != "auto" })
                }
            )
        }

        item("translate") {
            val crowdinLink = stringResource(R.string.crowdin_link)

            TextPref(
                text = stringResource(R.string.pref_translate),
                onClick = {
                    Stuff.openInBrowser(crowdinLink)
                }
            )
        }

        item("translate_credits") {
            TextPref(
                text = stringResource(R.string.pref_translate_credits),
                onClick = {
                    onNavigate(PanoRoute.Translators)
                }
            )
        }

        stickyHeader("misc_header") {
            SimpleHeaderItem(
                text = stringResource(R.string.pref_misc),
                icon = Icons.Outlined.MoreHoriz
            )
        }

        item(MainPrefs::fetchAlbum.name) {
            SwitchPref(
                text = stringResource(R.string.pref_fetch_album),
                value = fetchAlbum,
                copyToSave = { copy(fetchAlbum = it) }
            )
        }

        item(MainPrefs::spotifyArtistSearchApproximate.name) {
            SwitchPref(
                text = stringResource(R.string.pref_spotify_artist_search_approximate),
                value = spotifyArtistSearchApproximate,
                copyToSave = { copy(spotifyArtistSearchApproximate = it) }
            )
        }

        item(MainPrefs::preventDuplicateAmbientScrobbles.name) {
            SwitchPref(
                text = stringResource(R.string.pref_prevent_duplicate_ambient_scrobbles),
                value = preventDuplicateAmbientScrobbles,
                copyToSave = { copy(preventDuplicateAmbientScrobbles = it) }
            )
        }

        item(MainPrefs::submitNowPlaying.name) {
            SwitchPref(
                text = stringResource(R.string.pref_now_playing),
                value = submitNowPlaying,
                copyToSave = { copy(submitNowPlaying = it) }
            )
        }

        if (!Stuff.isTv) {
            item("intents") {
                TextPref(
                    text = stringResource(R.string.pref_intents),
                    onClick = {
                        showIntentsDescDialog = true
                    }
                )

                if (showIntentsDescDialog)
                    IntentsDescDialog(
                        onDismissRequest = { showIntentsDescDialog = false },
                    )
            }
        }

        if (!ExtrasConsts.isNonPlayBuild) {
            item(MainPrefs::crashReporterEnabled.name) {
                SwitchPref(
                    text = stringResource(R.string.pref_crashlytics_enabled),
                    value = crashReporterEnabled,
                    copyToSave = {
                        CrashReporter.setEnabled(it)
                        copy(crashReporterEnabled = it)
                    }
                )
            }
        }

        stickyHeader("imexport_header") {
            SimpleHeaderItem(
                text = stringResource(R.string.pref_imexport),
                icon = Icons.Outlined.SwapVert
            )
        }

        item("export") {
            TextPref(
                text = stringResource(R.string.pref_export),
                summary = stringResource(R.string.pref_export_desc),
                onClick = {
                    onNavigate(PanoRoute.Export)
                }
            )
        }

        item("import") {
            TextPref(
                text = stringResource(R.string.pref_import),
                onClick = {
                    onNavigate(PanoRoute.Import)
                }
            )
        }

        stickyHeader("services_header") {
            SimpleHeaderItem(
                text = stringResource(R.string.scrobble_services),
                icon = Icons.Outlined.Dns
            )
        }

        AccountType.entries
            .filterNot {
                Stuff.isTv && it == AccountType.FILE
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
                text = stringResource(R.string.delete_account),
                onClick = {
                    onNavigate(PanoRoute.DeleteAccount)
                }
            )
        }

        stickyHeader("about_header") {
            SimpleHeaderItem(
                text = stringResource(R.string.pref_about),
                icon = Icons.Outlined.Info
            )
        }

        item(key = "oss_credits") {
            TextPref(
                text = stringResource(R.string.pref_oss_credits),
                onClick = {
                    onNavigate(PanoRoute.OssCredits)
                }
            )
        }

        item(key = "privacy_policy") {
            val privacyPolicyLink = stringResource(R.string.privacy_policy_link)

            TextPref(
                text = stringResource(R.string.pref_privacy_policy),
                onClick = {
                    onNavigate(PanoRoute.WebView(privacyPolicyLink))
                }
            )
        }

        item(key = "github_link") {
            val githubLink = stringResource(R.string.github_link)

            TextPref(
                text = "v " + BuildConfig.VERSION_NAME,
                summary = githubLink,
                onClick = {
                    Stuff.openInBrowser(githubLink)
                }
            )
        }

        if (BuildConfig.DEBUG) {
            stickyHeader("debug_header") {
                SimpleHeaderItem(
                    text = stringResource(R.string.debug_menu),
                    icon = Icons.Outlined.BugReport
                )
            }

            item(MainPrefs::demoModeP.name) {
                SwitchPref(
                    text = stringResource(R.string.demo_mode),
                    value = demoMode,
                    copyToSave = { copy(demoMode = it) }
                )
            }

            item("copy_sk") {
                TextPref(
                    text = stringResource(R.string.copy_sk),
                    onClick = {
                        Scrobblables.all.value.firstOrNull {
                            it.userAccount.type ==
                                    AccountType.LASTFM
                        }?.userAccount?.authKey?.let {
                            context.copyToClipboard(it)
                        }
                    }
                )
            }

            item("delete_receipt") {
                TextPref(
                    text = stringResource(R.string.delete_receipt),
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

@TargetApi(Build.VERSION_CODES.O)
private fun requestPinWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val pi = PendingIntent.getActivity(
            context,
            30,
            Intent(context, ChartsWidgetConfigActivity::class.java)
                .apply { putExtra(Stuff.EXTRA_PINNED, true) },
            Stuff.updateCurrentOrMutable
        )

        val myProvider =
            ComponentName(context, ChartsWidgetProvider::class.java)
        appWidgetManager.requestPinAppWidget(myProvider, null, pi)
    }
}

private fun getCurrentLocale(context: Context, localePref: String?): String {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        localePref ?: "auto"
    } else {
        context
            .getSystemService(LocaleManager::class.java)
            .applicationLocales
            .takeIf { it.size() == 1 }
            ?.get(0)
            ?.let {
                if (it.toLanguageTag() in LocaleUtils.localesSet)
                    it.toLanguageTag()
                else if (it.language in LocaleUtils.localesSet)
                    it.language
                else
                    null
            } ?: "auto"
    }
}

@TargetApi(Build.VERSION_CODES.O)
private fun launchNotificationsActivity(context: Context) {
    val intent = Intent().apply {
        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntentsDescDialog(
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        val items = remember {
            listOf(
                NLService.iSCROBBLER_ON,
                NLService.iSCROBBLER_OFF,
                NLService.iLOVE,
                NLService.iUNLOVE,
                NLService.iCANCEL,
            )
        }

        val context = LocalContext.current

        items.forEach { item ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.copyToClipboard(item)
                    }
                    .padding(16.dp)
            )
        }
    }
}