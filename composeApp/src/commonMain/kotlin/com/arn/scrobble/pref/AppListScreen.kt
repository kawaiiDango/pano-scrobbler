package com.arn.scrobble.pref

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.autoshazam
import pano_scrobbler.composeapp.generated.resources.music_players
import pano_scrobbler.composeapp.generated.resources.other_apps
import pano_scrobbler.composeapp.generated.resources.pixel_np


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListScreen(
    isSingleSelect: Boolean,
    hasPreSelection: Boolean,
    preSelectedPackages: Set<String>,
    onSetSelectedPackages: (List<AppItem>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppListVM = viewModel { AppListVM() },
) {
    val appList by viewModel.appList.collectAsStateWithLifecycle()
    val selectedPackages by viewModel.selectedPackages.collectAsStateWithLifecycle()
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (hasPreSelection)
            viewModel.setSelectedPackages(preSelectedPackages)
        else
            viewModel.setSelectedPackages(PlatformStuff.mainPrefs.data.map { it.allowedPackages }
                .first())
    }

    DisposableEffect(Unit) {
        onDispose {
            if (hasLoaded) {
                if (hasPreSelection) {
                    val appListItems =
                        (viewModel.appList.value.musicPlayers + viewModel.appList.value.otherApps).filter {
                            viewModel.selectedPackages.value.contains(it.appId)
                        }

                    onSetSelectedPackages(appListItems)
                } else {
                    viewModel.saveToPrefs()
                }
            }
        }
    }

    PanoLazyColumn(modifier = modifier) {
        fun addItems(
            items: List<AppItem>,
        ) {
            items(
                items = items,
                key = { it.appId }
            ) {
                AppListItem(
                    appItem = it,
                    isSelected = selectedPackages.contains(it.appId),
                    onToggle = { selected ->
                        if (isSingleSelect) {
                            viewModel.setSelectedPackages(setOf(it.appId))
                        } else {
                            viewModel.setMultiSelection(it.appId, selected)
                        }
                    },
                    isSingleSelect = isSingleSelect,
                    modifier = Modifier.animateItem()
                )
            }
        }

        fun addPlaceholderItems(
            count: Int,
        ) {
            items(
                count,
                key = { it }
            ) {
                AppListItem(
                    appItem = null,
                    isSelected = false,
                    onToggle = {},
                    forShimmer = true,
                    isSingleSelect = isSingleSelect,
                    modifier = Modifier.shimmerWindowBounds().animateItem()
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

@Composable
private fun AppListItem(
    appItem: AppItem?,
    isSelected: Boolean,
    isSingleSelect: Boolean,
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
        Text(
            text = when (appItem?.appId) {
                Stuff.PACKAGE_PIXEL_NP, Stuff.PACKAGE_PIXEL_NP_R ->
                    stringResource(Res.string.pixel_np)

                Stuff.PACKAGE_SHAZAM ->
                    stringResource(Res.string.autoshazam)

                else ->
                    appItem?.label?.ifEmpty { appItem.appId } ?: ""
            },
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .backgroundForShimmer(forShimmer)
        )

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