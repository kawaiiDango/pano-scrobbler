package com.arn.scrobble.pref

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.edits.RegexPreset
import com.arn.scrobble.edits.RegexPresets
import com.arn.scrobble.ui.AppIcon
import com.arn.scrobble.ui.ExpandableHeaderItem
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchField
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.music_players
import pano_scrobbler.composeapp.generated.resources.needs_plugin
import pano_scrobbler.composeapp.generated.resources.other_apps


@Composable
fun AppListScreen(
    isSingleSelect: Boolean,
    saveType: AppListSaveType,
    packagesOverride: Set<String>?,
    preSelectedPackages: Set<String>,
    onSetPackagesSelection: (List<AppItem>, List<AppItem>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListVM = viewModel { AppListVM() },
) {
    val appList by viewModel.appList.collectAsStateWithLifecycle()
    val appListFiltered by viewModel.appListFiltered.collectAsStateWithLifecycle()
    val selectedPackages by viewModel.selectedPackages.collectAsStateWithLifecycle()
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()
    var pluginsNeededExpanded by rememberSaveable { mutableStateOf(false) }
    var searchTerm by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (!hasLoaded) {
            viewModel.load(packagesOverride)
        }
    }

    LaunchedEffect(searchTerm) {
        viewModel.setFilter(searchTerm)
    }

    LaunchedEffect(preSelectedPackages) {
        viewModel.setSelectedPackages(preSelectedPackages)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (hasLoaded) {
                val all = (viewModel.appList.value.musicPlayers + viewModel.appList.value.otherApps)
                val (checked, unchecked) = all.partition {
                    viewModel.selectedPackages.value.contains(it.appId)
                }

                val checkedAppIdsSet = checked.map { it.appId }.toSet()
                val uncheckedAppIdsSet = unchecked.map { it.appId }.toSet()

                when (saveType) {
                    AppListSaveType.Scrobbling -> {
                        GlobalScope.launch {
                            PlatformStuff.mainPrefs.updateData { pref ->
                                val firstArtistPackages =
                                    checkedAppIdsSet - pref.getRegexPresetApps(RegexPreset.parse_title)

                                pref.copy(
                                    allowedPackages = pref.allowedPackages +
                                            checkedAppIdsSet - uncheckedAppIdsSet,
                                    blockedPackages = pref.blockedPackages +
                                            uncheckedAppIdsSet - checkedAppIdsSet,
                                    extractFirstArtistPackages = firstArtistPackages.takeIf { !pref.appListWasRun }
                                        ?: pref.extractFirstArtistPackages,
                                    appListWasRun = true,
                                )
                            }
                        }
                    }

                    AppListSaveType.ExtractFirstArtist -> {
                        GlobalScope.launch {
                            PlatformStuff.mainPrefs.updateData { pref ->
                                pref.copy(
                                    extractFirstArtistPackages = pref.extractFirstArtistPackages +
                                            checkedAppIdsSet - uncheckedAppIdsSet,
                                )
                            }
                        }
                    }

                    AppListSaveType.Automation -> {
                        GlobalScope.launch {
                            PlatformStuff.mainPrefs.updateData { pref ->
                                pref.copy(
                                    allowedAutomationPackages = pref.allowedAutomationPackages +
                                            checkedAppIdsSet - uncheckedAppIdsSet,
                                )
                            }
                        }
                    }

                    is AppListSaveType.RegexPresetApps -> {
                        GlobalScope.launch {
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
                                    }.mapKeys { it.toString() }

                                pref.copy(regexPresetsApps = updatedRegexPresetsApps)
                            }
                        }
                    }

                    AppListSaveType.Callback -> {
                        onSetPackagesSelection(checked, unchecked)
                    }
                }
            }
        }
    }

    Column(modifier = modifier) {
        if ((appList.musicPlayers.size + appList.otherApps.size) >
            Stuff.MIN_ITEMS_TO_SHOW_SEARCH
        ) {
            SearchField(
                searchTerm = searchTerm,
                onSearchTermChange = {
                    searchTerm = it
                },
                modifier = Modifier
                    .padding(panoContentPadding(bottom = false))
            )
        }

        PanoLazyColumn(
            contentPadding = panoContentPadding(),
            modifier = Modifier
                .fillMaxSize()
        ) {
            fun addItems(
                items: List<AppItem>,
            ) {
                itemsIndexed(
                    items = items,
                    key = { idx, appItem -> appItem.appId }
                ) { idx, appItem ->

                    val showAppId =
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
                                viewModel.setSelectedPackages(setOf(appItem.appId))
                            } else {
                                viewModel.setMultiSelection(appItem.appId, selected)
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            fun addPlaceholderItems(
                count: Int,
            ) {
                items(
                    count,
                ) {
                    AppListItem(
                        appItem = null,
                        isSelected = false,
                        isSingleSelect = isSingleSelect,
                        showAppId = false,
                        onToggle = {},
                        modifier = Modifier.shimmerWindowBounds().animateItem(),
                        forShimmer = true,
                    )
                }
            }

            if (appListFiltered.musicPlayers.isNotEmpty() || !hasLoaded) {
                stickyHeader("header_music_players") {
                    SimpleHeaderItem(
                        text = stringResource(Res.string.music_players),
                        icon = Icons.Outlined.PlayCircle
                    )
                }
            }

            if (!hasLoaded) {
                addPlaceholderItems(10)
            } else {
                addItems(appListFiltered.musicPlayers)

                if (appListFiltered.otherApps.isNotEmpty()) {
                    stickyHeader("header_other_apps") {
                        SimpleHeaderItem(
                            text = stringResource(Res.string.other_apps),
                            icon = Icons.Outlined.Apps
                        )
                    }

                    addItems(appListFiltered.otherApps)
                }

                if (viewModel.pluginsNeeded.isNotEmpty()) {
                    stickyHeader("header_plugins_needed") {
                        ExpandableHeaderItem(
                            title = stringResource(Res.string.needs_plugin),
                            icon = Icons.Outlined.Info,
                            expanded = pluginsNeededExpanded,
                            onToggle = { pluginsNeededExpanded = it },
                        )
                    }

                    if (pluginsNeededExpanded) {
                        items(viewModel.pluginsNeeded) { (appName, pluginUrl) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable {
                                        PlatformStuff.openInBrowser(pluginUrl)
                                    }
                                    .padding(
                                        vertical = 8.dp,
                                        horizontal = horizontalOverscanPadding()
                                    ),
                            ) {
                                Text(
                                    text = appName,
                                    maxLines = 1,
                                )

                                Spacer(
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.Outlined.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier
                                )
                            }
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .toggleable(
                value = isSelected,
                onValueChange = onToggle
            )
            .then(
                if (isSelected)
                    Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                else
                    Modifier
            )
            .padding(vertical = 8.dp, horizontal = horizontalOverscanPadding()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppIcon(
            appItem = appItem,
            modifier = Modifier
                .size(32.dp)
                .backgroundForShimmer(forShimmer)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .backgroundForShimmer(forShimmer)
        ) {
            Text(
                text = appItem?.friendlyLabel ?: "",
                maxLines = 1,
            )

            if (showAppId) {
                Text(
                    text = appItem?.appId ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }

        if (isSingleSelect) {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        } else {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null
            )
        }
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