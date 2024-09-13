package com.arn.scrobble.pref

import android.os.Bundle
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import coil3.compose.AsyncImage
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.ui.ExtraBottomSpace
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.utils.Stuff
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListContent(
    viewModel: AppListVM = viewModel(),
    singleSelection: Boolean,
    preSelectedPackageNames: Set<String>? = null,
    modifier: Modifier = Modifier
) {
    val appList by viewModel.appList.collectAsStateWithLifecycle()
    val selectedPackages by viewModel.selectedPackages.collectAsStateWithLifecycle()
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (preSelectedPackageNames == null)
            viewModel.setSelectedPackages(PlatformStuff.mainPrefs.data.map { it.allowedPackages }
                .first())
        else
            viewModel.setSelectedPackages(preSelectedPackageNames)
    }

    AnimatedVisibility(visible = !hasLoaded) {
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

    AnimatedVisibility(visible = hasLoaded) {
        LazyColumn(modifier = modifier) {

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
                            if (singleSelection) {
                                viewModel.setSelectedPackages(setOf(it.appId))
                            } else {
                                viewModel.setMultiSelection(it.appId, selected)
                            }
                        })
                }
            }


            stickyHeader("header_music_players") {
                Header(
                    title = stringResource(id = R.string.music_players),
                    icon = Icons.Outlined.PlayCircle
                )
            }

            addItems(appList.musicPlayers)

            stickyHeader("header_other_apps") {
                Header(title = stringResource(id = R.string.other_apps), icon = Icons.Outlined.Apps)
            }

            addItems(appList.otherApps)


            item("extra_space") {
                ExtraBottomSpace()
            }
        }
    }
}

@Composable
private fun Header(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppListItem(
    packageName: String?,
    label: String,
    isSelected: Boolean,
    forShimmer: Boolean = false,
    onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier
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
            .padding(8.dp),
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

@Keep
@Composable
fun AppListScreen() {
    val fragment = LocalFragment.current

    val allowedPackagesArg =
        fragment.arguments?.getStringArray(Stuff.ARG_ALLOWED_PACKAGES)?.toSet()

    val singleChoiceArg = fragment.arguments?.getBoolean(Stuff.ARG_SINGLE_CHOICE, false) ?: false
    val viewModel: AppListVM = viewModel()

    LaunchedEffect(Unit) {
        // todo do nothing if the app list has not loaded yet
        val onDone = {
            fragment.lifecycleScope.launch {
                if (allowedPackagesArg == null)
                    viewModel.saveToPrefs()
                else {
                    val appListItems =
                        (viewModel.appList.value.musicPlayers + viewModel.appList.value.otherApps).filter {
                            viewModel.selectedPackages.value.contains(it.appId)
                        }.toTypedArray()

                    val arg = Bundle()
                    arg.putParcelableArray(
                        Stuff.ARG_ALLOWED_PACKAGES,
                        appListItems
                    )

                    fragment.setFragmentResult(
                        Stuff.ARG_ALLOWED_PACKAGES,
                        arg
                    )
                }
                fragment.findNavController().navigateUp()
            }
        }
        val fabData = FabData(
            fragment.viewLifecycleOwner,
            R.string.done,
            R.drawable.vd_check_simple,
            {
                onDone()
            }
        )
        val mainNotifierViewModel by fragment.activityViewModels<MainNotifierViewModel>()
        mainNotifierViewModel.setFabData(fabData)
    }

    ScreenParent {
        AppListContent(
            preSelectedPackageNames = allowedPackagesArg,
            singleSelection = singleChoiceArg,
            viewModel = viewModel,
            modifier = it
        )
    }
}