package com.arn.scrobble.charts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.MusicEntryGridItem
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.track


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomScreen(
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: RandomVM = viewModel { RandomVM() },
    chartsPeriodViewModel: ChartsPeriodVM = viewModel { ChartsPeriodVM() },
    modifier: Modifier = Modifier,
) {
    val musicEntry by viewModel.musicEntry.collectAsStateWithLifecycle()
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val type by PlatformStuff.mainPrefs.data.map { it.lastRandomType }
        .collectAsStateWithLifecycle(Stuff.TYPE_TRACKS)
    var timePeriod by remember { mutableStateOf<TimePeriod?>(null) }
    val isLandscape = LocalNavigationType.current != PanoNavigationType.BOTTOM_NAVIGATION

    val isTimePeriodContinuous by chartsPeriodViewModel.selectedPeriod.map { it?.lastfmPeriod != null }
        .collectAsStateWithLifecycle(false)

    fun load(type: Int, refresh: Boolean = false) {
        if (type != -1 && timePeriod != null) {
            viewModel.setRandomInput(
                RandomLoaderInput(
                    username = user.name,
                    timePeriod = timePeriod!!,
                    type = type
                ),
                refresh
            )
        }
    }

    fun onEntryClick(musicEntry: MusicEntry) {
        onNavigate(
            PanoRoute.MusicEntryInfo(
                user = user,
                artist = musicEntry as? Artist,
                album = musicEntry as? Album,
                track = musicEntry as? Track
            )
        )
    }


    // first load
    LaunchedEffect(Unit) {
        PlatformStuff.mainPrefs.data.map { it.lastRandomType }
            .combine(chartsPeriodViewModel.selectedPeriod) { type, selectedPeriod ->
                type to selectedPeriod
            }
            .take(1)
            .collect { (type, selectedPeriod) ->
                load(type)
            }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (isLandscape) {
                RandomTypeSelector(
                    type = type,
                    onSameClick = {
                        load(type, true)
                    },
                    onMenuItemClick = { newType ->
                        load(newType)
                    },
                )
            }

            AnimatedVisibility(
                visible = type != Stuff.TYPE_LOVES,
                modifier = Modifier.weight(1f)
            ) {
                TimePeriodSelector(
                    user = user,
                    viewModel = chartsPeriodViewModel,
                    onSelected = { curr, prev ->
                        timePeriod = curr
                        load(type)
                    },
                )
            }
        }

        Box(
            modifier =
                if (isLandscape) Modifier
                    .heightIn(max = 400.dp)
                    .weight(1f)
                else Modifier.widthIn(max = 400.dp)
        ) {
            val musicEntryOrPlaceholder =
                musicEntry.takeIf { hasLoaded } ?: getMusicEntryPlaceholderItem(
                    if (type == -1 || type == Stuff.TYPE_LOVES) Stuff.TYPE_TRACKS else type
                )

            if (hasLoaded) {
                ErrorText(
                    errorText = error?.localizedMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )
            }

            if (isLandscape) {
                MusicEntryListItem(
                    entry = musicEntryOrPlaceholder,
                    forShimmer = !hasLoaded,
                    fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                    onEntryClick = {
                        musicEntry?.let { onEntryClick(it) }
                    },
                    fixedImageHeight = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!hasLoaded) Modifier.shimmerWindowBounds()
                            else Modifier
                        )
                )
            } else {
                MusicEntryGridItem(
                    entry = musicEntryOrPlaceholder,
                    showArtist = true,
                    forShimmer = !hasLoaded,
                    fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                    index = null,
                    stonksDelta = null,
                    onClick = {
                        musicEntry?.let { onEntryClick(it) }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!hasLoaded) Modifier.shimmerWindowBounds()
                            else Modifier
                        )
                )
            }
        }


        if (!isLandscape) {
            RandomTypeSelector(
                type = type,
                onSameClick = {
                    load(type, true)
                },
                onMenuItemClick = { newType ->
                    load(newType)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RandomTypeSelector(
    type: Int,
    onSameClick: () -> Unit,
    onMenuItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var typeSelectorIsShown by remember { mutableStateOf(false) }

    fun getIconForType(type: Int) = when (type) {
        Stuff.TYPE_ARTISTS -> Icons.Outlined.Mic
        Stuff.TYPE_ALBUMS -> Icons.Outlined.Album
        Stuff.TYPE_TRACKS -> Icons.Outlined.MusicNote
        Stuff.TYPE_LOVES -> Icons.Outlined.FavoriteBorder
        else -> error("Unknown type $type")
    }

    @Composable
    fun getTextForType(type: Int) = when (type) {
        Stuff.TYPE_ARTISTS -> stringResource(Res.string.artist)
        Stuff.TYPE_ALBUMS -> stringResource(Res.string.album)
        Stuff.TYPE_TRACKS -> stringResource(Res.string.track)
        Stuff.TYPE_LOVES -> stringResource(Res.string.loved)
        else -> error("Unknown type $type")
    }

    Box(
        modifier = modifier
    ) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.OutlinedLeadingButton(
                    onClick = onSameClick
                ) {
                    Icon(
                        getIconForType(type),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    Text(text = getTextForType(type))
                }
            },
            trailingButton = {
                SplitButtonDefaults.OutlinedTrailingButton(
                    onCheckedChange = {
                        typeSelectorIsShown = it
                    },
                    checked = typeSelectorIsShown
                ) {
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = stringResource(Res.string.item_options)
                    )
                }
                Icon(
                    Icons.Outlined.ArrowDropDown,
                    contentDescription = stringResource(Res.string.item_options)
                )
            },
        )

        DropdownMenu(
            expanded = typeSelectorIsShown,
            onDismissRequest = { typeSelectorIsShown = false }
        ) {
            arrayOf(
                Stuff.TYPE_ARTISTS,
                Stuff.TYPE_ALBUMS,
                Stuff.TYPE_TRACKS,
                Stuff.TYPE_LOVES
            ).forEach { thisType ->
                DropdownMenuItem(
                    enabled = thisType != type,
                    onClick = {
                        typeSelectorIsShown = false
                        onMenuItemClick(thisType)
                    },
                    leadingIcon = {
                        Icon(
                            getIconForType(thisType),
                            contentDescription = getTextForType(thisType)
                        )
                    },
                    text = {
                        Text(getTextForType(thisType))
                    }
                )
            }
        }
    }
}