package com.arn.scrobble.pref

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.utils.Stuff
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


@Composable
fun AppListScreen(
    viewModel: AppListVM = viewModel(),
    isSingleSelect: Boolean,
    hasPreSelection: Boolean,
    preSelectedPackages: Set<String>,
    onSetSelectedPackages: (List<AppItem>) -> Unit,
    modifier: Modifier = Modifier
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

    AnimatedVisibility(
        visible = !hasLoaded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LazyColumn(modifier = modifier.shimmer()) {
            items(10,
                key = { it }
            ) {
                AppListItem(
                    packageName = null,
                    label = "",
                    isSelected = false,
                    onToggle = {},
                    forShimmer = true,
                )
            }
        }
    }

    AnimatedVisibility(
        visible = hasLoaded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        LazyColumn(modifier = modifier, contentPadding = panoContentPadding()) {

            fun addItems(
                items: List<AppItem>,
            ) {
                items(
                    items = items,
                    key = { it.appId }
                ) {
                    AppListItem(
                        packageName = it.appId,
                        label = it.label,
                        isSelected = selectedPackages.contains(it.appId),
                        onToggle = { selected ->
                            if (isSingleSelect) {
                                viewModel.setSelectedPackages(setOf(it.appId))
                            } else {
                                viewModel.setMultiSelection(it.appId, selected)
                            }
                        })
                }
            }


            stickyHeader("header_music_players") {
                SimpleHeaderItem(
                    text = stringResource(id = R.string.music_players),
                    icon = Icons.Outlined.PlayCircle
                )
            }

            addItems(appList.musicPlayers)

            stickyHeader("header_other_apps") {
                SimpleHeaderItem(
                    text = stringResource(id = R.string.other_apps),
                    icon = Icons.Outlined.Apps
                )
            }

            addItems(appList.otherApps)
        }
    }
}

@Composable
private fun AppListItem(
    packageName: String?,
    label: String,
    isSelected: Boolean,
    forShimmer: Boolean = false,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
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
        AsyncImage(
            model = packageName?.let { PackageName(it) },
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .backgroundForShimmer(forShimmer)
        )
        Text(
            text = when (packageName) {
                Stuff.PACKAGE_PIXEL_NP, Stuff.PACKAGE_PIXEL_NP_R ->
                    stringResource(R.string.pixel_np)

                Stuff.PACKAGE_SHAZAM ->
                    stringResource(R.string.autoshazam)

                else ->
                    label
            },
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .backgroundForShimmer(forShimmer)
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = null
        )
    }
}