package com.arn.scrobble.pref

import androidx.annotation.Keep
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.ui.AppIcon
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.music_players
import pano_scrobbler.composeapp.generated.resources.needs_plugin
import pano_scrobbler.composeapp.generated.resources.other_apps


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    isSingleSelect: Boolean,
    saveType: AppListSaveType,
    preSelectedPackages: Set<String>,
    onSetPackagesSelection: (List<AppItem>, List<AppItem>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListVM = viewModel { AppListVM() },
) {
    val appList by viewModel.appList.collectAsStateWithLifecycle()
    val selectedPackages by viewModel.selectedPackages.collectAsStateWithLifecycle()
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()

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

                when (saveType) {
                    AppListSaveType.Scrobbling -> {
                        GlobalScope.launch {
                            PlatformStuff.mainPrefs.updateData {
                                it.copy(
                                    allowedPackages = checked.map { it.appId }.toSet(),
                                    blockedPackages = unchecked.map { it.appId }.toSet(),
                                )
                            }
                        }
                    }

                    AppListSaveType.Automation -> {
                        GlobalScope.launch {
                            PlatformStuff.mainPrefs.updateData {
                                it.copy(
                                    allowedAutomationPackages = checked.map { it.appId }.toSet(),
                                )
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

    PanoLazyColumn(modifier = modifier) {
        fun addItems(
            items: List<AppItem>,
        ) {
            itemsIndexed(
                items = items,
                key = { idx, appItem -> appItem.appId }
            ) { idx, appItem ->

                val showAppId =
                    items.getOrNull(idx - 1)?.friendlyLabel?.lowercase() == appItem.friendlyLabel.lowercase() ||
                            items.getOrNull(idx + 1)?.friendlyLabel?.lowercase() == appItem.friendlyLabel.lowercase()

                AppListItem(
                    appItem = appItem,
                    isSelected = selectedPackages.contains(appItem.appId),
                    isSingleSelect = isSingleSelect,
                    showAppId = showAppId,
                    pluginUrl = if (saveType == AppListSaveType.Scrobbling)
                        viewModel.pluginUrl(appItem)
                    else
                        null,
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
                    pluginUrl = null,
                    onToggle = {},
                    modifier = Modifier.shimmerWindowBounds().animateItem(),
                    forShimmer = true,
                )
            }
        }

        stickyHeader("header_music_players") {
            SimpleHeaderItem(
                text = stringResource(Res.string.music_players),
                icon = Icons.Outlined.PlayCircle
            )
        }

        if (!hasLoaded) {
            addPlaceholderItems(10)
        } else {
            addItems(appList.musicPlayers)

            if (appList.otherApps.isNotEmpty()) {
                stickyHeader("header_other_apps") {
                    SimpleHeaderItem(
                        text = stringResource(Res.string.other_apps),
                        icon = Icons.Outlined.Apps
                    )
                }

                addItems(appList.otherApps)
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
    pluginUrl: String?,
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

            if (pluginUrl != null) {
                OutlinedButton(
                    onClick = {
                        PlatformStuff.openInBrowser(pluginUrl)
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.needs_plugin),
                    )
                }
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

@Keep
enum class AppListSaveType {
    Scrobbling, Automation, Callback
}