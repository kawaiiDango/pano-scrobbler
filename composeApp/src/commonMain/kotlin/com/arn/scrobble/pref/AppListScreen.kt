package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.arn.scrobble.edits.RegexPreset
import com.arn.scrobble.edits.RegexPresets
import com.arn.scrobble.icons.Apps
import com.arn.scrobble.icons.Delete
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Info
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.icons.PlayCircle
import com.arn.scrobble.icons.Public
import com.arn.scrobble.icons.Settings
import com.arn.scrobble.navigation.FabClickedResult
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.AppIcon
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.ExpandableHeaderItem
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchEffect
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.myCheckableItemColors
import com.arn.scrobble.ui.myTransparentCheckableItemColors
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.ambient_apps
import pano_scrobbler.composeapp.generated.resources.artist_splitting_exceptions
import pano_scrobbler.composeapp.generated.resources.empty_apps_list
import pano_scrobbler.composeapp.generated.resources.first_artist
import pano_scrobbler.composeapp.generated.resources.forget_checked_websites
import pano_scrobbler.composeapp.generated.resources.forget_unchecked_apps
import pano_scrobbler.composeapp.generated.resources.music_players
import pano_scrobbler.composeapp.generated.resources.needs_plugin
import pano_scrobbler.composeapp.generated.resources.other_apps
import pano_scrobbler.composeapp.generated.resources.supports_ambient_apps
import pano_scrobbler.composeapp.generated.resources.websites
import pano_scrobbler.composeapp.generated.resources.websites_desc


@Composable
fun AppListScreen(
    searchFieldState: TextFieldState,
    isSingleSelect: Boolean,
    saveType: AppListSaveType,
    packagesOverride: Set<String>?,
    preSelectedPackages: Set<String>,
    onSetPackagesSelection: (List<AppItem>, List<AppItem>) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListVM = viewModel { AppListVM(preSelectedPackages, packagesOverride) },
) {
    val appList by viewModel.appList.collectAsStateWithLifecycle()
    val appListFiltered by viewModel.appListFiltered.collectAsStateWithLifecycle()
    val selectedPackages by viewModel.selectedPackages.collectAsStateWithLifecycle()
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()
    val firstRun by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        !it.appListWasRun && it.allowedPackages.isEmpty() && it.blockedPackages.isEmpty()
    }
    var pluginsNeededExpanded by rememberSaveable { mutableStateOf(false) }
    var websitesExpanded by rememberSaveable { mutableStateOf(false) }
    var useFirstArtistChecked by rememberSaveable { mutableStateOf(firstRun) }

    val hostnamesFiltered by if (saveType == AppListSaveType.Scrobbling) {
        viewModel.hostnamesFiltered.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val blockedHostnames by if (saveType == AppListSaveType.Scrobbling) {
        viewModel.blockedHostnames.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptySet()) }
    }

    SearchEffect(searchFieldState) {
        viewModel.setFilter(it)
    }

    ResultEffect<FabClickedResult> {
        if (hasLoaded) {
            val all = (viewModel.appList.value.musicPlayers + viewModel.appList.value.otherApps)
            val (checked, unchecked) = all.partition {
                viewModel.selectedPackages.value.contains(it.appId)
            }

            val checkedAppIdsSet = checked.map { it.appId }.toSet()
            val uncheckedAppIdsSet = unchecked.map { it.appId }.toSet()

            val blockedHostNames = if (
                viewModel.pluginsNeeded.isEmpty() && PlatformStuff.isDesktop
            )
                blockedHostnames
            else
                null

            when (saveType) {
                AppListSaveType.Scrobbling -> {
                    Stuff.appScope.launch {
                        PlatformStuff.mainPrefs.updateData { pref ->
                            pref.copy(
                                allowedPackages = pref.allowedPackages +
                                        checkedAppIdsSet - uncheckedAppIdsSet,
                                blockedPackages = pref.blockedPackages +
                                        uncheckedAppIdsSet - checkedAppIdsSet,
                                extractFirstArtistPackages = if (useFirstArtistChecked)
                                    checkedAppIdsSet - pref.getRegexPresetApps(RegexPreset.parse_title)
                                else
                                    pref.extractFirstArtistPackages,
                                blockedHostnames = blockedHostNames ?: pref.blockedHostnames,
                                appListWasRun = true,
                            )
                        }
                    }
                }

                AppListSaveType.ExtractFirstArtist -> {
                    Stuff.appScope.launch {
                        PlatformStuff.mainPrefs.updateData { pref ->
                            pref.copy(
                                extractFirstArtistPackages = pref.extractFirstArtistPackages +
                                        checkedAppIdsSet - uncheckedAppIdsSet,
                            )
                        }
                    }
                }

                AppListSaveType.Automation -> {
                    Stuff.appScope.launch {
                        PlatformStuff.mainPrefs.updateData { pref ->
                            pref.copy(
                                allowedAutomationPackages = pref.allowedAutomationPackages +
                                        checkedAppIdsSet - uncheckedAppIdsSet,
                            )
                        }
                    }
                }

                is AppListSaveType.RegexPresetApps -> {
                    Stuff.appScope.launch {
                        PlatformStuff.mainPrefs.updateData { pref ->
                            val thisAllowList =
                                pref.getRegexPresetApps(saveType.preset) + checkedAppIdsSet - uncheckedAppIdsSet

                            // Ensure no duplicates across presets. thisAllowList wins

                            val updatedRegexPresetsApps =
                                RegexPresets.hasSettings.associateWith { p ->
                                    if (p == saveType.preset) {
                                        thisAllowList
                                    } else {
                                        pref.getRegexPresetApps(p) - thisAllowList
                                    }
                                }.mapKeys { (k, v) -> k.name }

                            pref.copy(regexPresetsApps = updatedRegexPresetsApps)
                        }
                    }
                }

                AppListSaveType.Callback -> {
                    onSetPackagesSelection(checked, unchecked)
                }
            }
        }

        onBack()
    }

    PanoLazyColumn(
        contentPadding = panoContentPadding(mayHaveBottomFab = true),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        modifier = modifier
    ) {
        fun addItems(
            items: List<AppItem>,
        ) {
            itemsIndexed(
                items = items,
                key = { idx, appItem -> "app_" + appItem.appId }
            ) { idx, appItem ->

                val showAppId =
                    (PlatformStuff.isDesktop && appItem.friendlyLabel != appItem.appId) ||
                            items.getOrNull(idx - 1)?.friendlyLabel.equals(
                                appItem.friendlyLabel,
                                ignoreCase = true
                            ) ||
                            items.getOrNull(idx + 1)?.friendlyLabel.equals(
                                appItem.friendlyLabel,
                                ignoreCase = true
                            )

                AppListItem(
                    appItem = appItem,
                    isSelected = selectedPackages.contains(appItem.appId),
                    isSingleSelect = isSingleSelect,
                    showAppId = showAppId,
                    onToggle = { selected ->
                        if (isSingleSelect) {
                            viewModel.setSingleSelectionAppId(appItem.appId)
                        } else {
                            viewModel.setMultiSelectionAppId(appItem.appId, selected)
                        }
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }

        fun addPlaceholderItems(
            count: Int,
        ) {
            items(
                count,
                key = { "shimmer_$it" }
            ) {
                AppListItem(
                    appItem = null,
                    isSelected = false,
                    isSingleSelect = isSingleSelect,
                    showAppId = false,
                    onToggle = {},
                    modifier = Modifier
                        .shimmerWindowBounds()
                        .animateItem(),
                    forShimmer = true,
                )
            }
        }

        if (PlatformStuff.isDesktop && appListFiltered.musicPlayers.isEmpty() &&
            saveType == AppListSaveType.Scrobbling && hasLoaded && searchFieldState.text.isBlank()
        ) {
            item("header_no_music_players") {
                SimpleHeaderItem(
                    text = stringResource(Res.string.empty_apps_list),
                    icon = Icons.Info,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        if (appListFiltered.musicPlayers.isNotEmpty() || !hasLoaded) {

            if (saveType is AppListSaveType.ExtractFirstArtist) {
                item("header_action") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        ButtonWithIcon(
                            onClick = { onNavigate(PanoRoute.ArtistsWithDelimiters) },
                            text = stringResource(Res.string.artist_splitting_exceptions),
                            icon = Icons.Settings,
                        )
                    }
                }
            } else {
                when (saveType) {
                    is AppListSaveType.Scrobbling if firstRun -> {
                        stickyHeader("header_primary") {
                            Surface(
                                tonalElevation = 4.dp,
                                shape = MaterialTheme.shapes.large,
                                modifier = modifier
                                    .fillMaxWidth()
                            ) {
                                LabeledCheckbox(
                                    text = stringResource(Res.string.first_artist),
                                    checked = useFirstArtistChecked,
                                    onCheckedChange = { useFirstArtistChecked = it }
                                )
                            }
                        }
                    }

                    else -> {
                        item("header_action") {
                            SimpleHeaderItem(
                                text = stringResource(Res.string.music_players),
                                icon = Icons.PlayCircle,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        if (!hasLoaded) {
            addPlaceholderItems(10)
        } else {
            addItems(appListFiltered.musicPlayers)

            if (!PlatformStuff.isDesktop && !PlatformStuff.isTv &&
                saveType == AppListSaveType.Scrobbling && searchFieldState.text.isBlank()
            ) {
                item("notice_ambient_apps") {
                    ListItem(
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Info,
                                contentDescription = null,
                            )
                        },
                        colors = ListItemDefaults.myTransparentCheckableItemColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            style = MaterialTheme.typography.bodyMediumEmphasized,
                            text = stringResource(
                                Res.string.supports_ambient_apps,
                                stringResource(Res.string.ambient_apps),
                            ),
                        )
                    }
                }
            }

            if (appListFiltered.otherApps.isNotEmpty()) {
                item("header_other_apps") {
                    SimpleHeaderItem(
                        text = stringResource(Res.string.other_apps),
                        icon = Icons.Apps, modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                addItems(appListFiltered.otherApps)
            }

            if (saveType == AppListSaveType.Scrobbling && PlatformStuff.isDesktop) {
                if (viewModel.pluginsNeeded.isNotEmpty()) { // windows
                    item("header_plugins_needed") {
                        ExpandableHeaderItem(
                            text = stringResource(Res.string.needs_plugin),
                            icon = Icons.Info,
                            expanded = pluginsNeededExpanded,
                            onToggle = { pluginsNeededExpanded = it },
                        )
                    }

                    if (pluginsNeededExpanded) {
                        items(
                            viewModel.pluginsNeeded,
                            key = { (appName, _) -> "plugin_$appName" }
                        ) { (appName, pluginUrl) ->
                            ListItem(
                                onClick = {
                                    PlatformStuff.openInBrowser(pluginUrl)
                                },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.OpenInBrowser,
                                        contentDescription = null,
                                        modifier = Modifier
                                    )
                                },
                                colors = ListItemDefaults.myTransparentCheckableItemColors(),
                                modifier = Modifier
                                    .fillMaxWidth(),
                            ) {
                                Text(
                                    text = appName,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (appList.musicPlayers.isNotEmpty()) {
                    item("forget_unchecked_apps") {
                        ButtonWithIcon(
                            onClick = {
                                viewModel.forgetUncheckedApps()
                            },
                            icon = Icons.Delete,
                            text = stringResource(Res.string.forget_unchecked_apps),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth()
                        )
                    }
                }

                if (hostnamesFiltered.isNotEmpty()) { // linux
                    item("header_websites") {
                        ExpandableHeaderItem(
                            text = stringResource(Res.string.websites),
                            icon = Icons.Public,
                            expanded = websitesExpanded,
                            onToggle = { websitesExpanded = it },
                        )
                    }

                    if (websitesExpanded) {
                        item("notice_websites") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Info,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Text(
                                    style = MaterialTheme.typography.labelMedium,
                                    text = stringResource(Res.string.websites_desc),
                                )
                            }
                        }

                        items(
                            items = hostnamesFiltered,
                            key = { "hostname_$it" }
                        ) { hostname ->
                            val appItem = AppItem(
                                appId = hostname,
                                label = hostname,
                            )

                            AppListItem(
                                appItem = appItem,
                                isSelected = hostname !in blockedHostnames,
                                isSingleSelect = isSingleSelect,
                                showAppId = false,
                                onToggle = { selected ->
                                    if (isSingleSelect) {
                                        viewModel.setSingleSelectionHostname(hostname)
                                    } else {
                                        viewModel.setMultiSelectionHostname(
                                            hostname,
                                            selected
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }

                        item("forget_checked_websites") {
                            ButtonWithIcon(
                                onClick = {
                                    viewModel.forgetCheckedHostnames()
                                },
                                icon = Icons.Delete,
                                text = stringResource(Res.string.forget_checked_websites),
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .wrapContentWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    appItem: AppItem?,
    isSelected: Boolean,
    isSingleSelect: Boolean,
    showAppId: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    forShimmer: Boolean = false,
) {
    ListItem(
        checked = isSelected,
        enabled = !forShimmer,
        onCheckedChange = onToggle,
        modifier = modifier,
        colors = ListItemDefaults.myCheckableItemColors(),
        supportingContent = appItem?.appId?.takeIf { showAppId }?.let {
            {
                Text(text = it, maxLines = 1)
            }
        },
        trailingContent = {
            if (isSingleSelect)
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
            else
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null
                )
        },
        leadingContent = {
            AppIcon(
                appItem = appItem,
                modifier = Modifier
                    .size(32.dp)
                    .backgroundForShimmer(forShimmer)
            )
        }
    ) {
        Text(
            text = appItem?.friendlyLabel ?: "",
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .backgroundForShimmer(forShimmer)
        )
    }
}

@Serializable
sealed interface AppListSaveType {
    @Serializable
    object Scrobbling : AppListSaveType

    @Serializable
    object ExtractFirstArtist : AppListSaveType

    @Serializable
    object Automation : AppListSaveType

    @Serializable
    class RegexPresetApps(val preset: RegexPreset) : AppListSaveType

    @Serializable
    object Callback : AppListSaveType
}