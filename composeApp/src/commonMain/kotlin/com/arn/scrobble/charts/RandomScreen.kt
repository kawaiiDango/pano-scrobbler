package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.jsonSerializableSaver
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.PanoDropdownMenu
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.random_text
import pano_scrobbler.composeapp.generated.resources.track


@Composable
fun RandomScreen(
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RandomVM = viewModel { RandomVM(user.name) },
    chartsPeriodViewModel: ChartsPeriodVM = viewModel { ChartsPeriodVM() },
) {
    val musicEntry by viewModel.musicEntry.collectAsStateWithLifecycle()
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val type by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.randomType }
    var timePeriod by rememberSaveable(saver = jsonSerializableSaver<TimePeriod?>()) {
        mutableStateOf(null)
    }

    val isTimePeriodContinuous = timePeriod?.lastfmPeriod != null

    fun load(type: Int, refresh: Boolean = false) {
        if (type != -1 && timePeriod != null) {
            viewModel.setRandomInput(
                RandomLoaderInput(
                    timePeriod = timePeriod!!,
                    type = type
                ),
                refresh
            )
        }
    }

    fun onEntryClick(musicEntry: MusicEntry) {
        onNavigate(
            PanoRoute.Modal.MusicEntryInfo(
                user = user,
                artist = musicEntry as? Artist,
                album = musicEntry as? Album,
                track = musicEntry as? Track
            )
        )
    }


    // first load
    LaunchedEffect(user) {
        PlatformStuff.mainPrefs.data.map { it.randomType }
            .distinctUntilChanged()
            .combine(chartsPeriodViewModel.selectedPeriod) { type, selectedPeriod ->
                type to selectedPeriod
            }
            .take(1)
            .collect { (type, selectedPeriod) ->
                load(type)
            }
    }

    Column(
        modifier = modifier
    ) {
        TimePeriodSelector(
            registeredTime = user.registeredTime,
            viewModel = chartsPeriodViewModel,
            onNavigate = onNavigate,
            onSelected = { curr, prev, _ ->
                timePeriod = curr
                load(type)
            },
            showRefreshButton = false,
            enabled = type != Stuff.TYPE_LOVES
        )

        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterHorizontally)
        ) {
            val isLandscape = (maxWidth * 0.7f) > maxHeight

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    if (isLandscape) Modifier
                        .heightIn(max = 400.dp)
                    else Modifier.widthIn(max = 400.dp)
            ) {
                val musicEntryOrPlaceholder =
                    musicEntry.takeIf { hasLoaded } ?: getMusicEntryPlaceholderItem(
                        if (type == -1 || type == Stuff.TYPE_LOVES) Stuff.TYPE_TRACKS else type
                    )

                if (hasLoaded) {
                    ErrorText(
                        errorText = error?.redactedMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                if (error == null) {
                    MusicEntryListItem(
                        entry = musicEntryOrPlaceholder,
                        forShimmer = !hasLoaded,
                        fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                        onEntryClick = {
                            musicEntry?.let { onEntryClick(it) }
                        },
                        fixedImageHeight = false,
                        isColumn = !isLandscape,
                        modifier = Modifier
                            .then(
                                if (!hasLoaded) Modifier.shimmerWindowBounds()
                                else Modifier
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        RandomTypeSelector(
            type = type,
            onSameClick = {
                load(type, true)
            },
            onMenuItemClick = { newType ->
                load(newType)
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )

    }
}

@Composable
private fun RandomTypeSelector(
    type: Int,
    onSameClick: () -> Unit,
    onMenuItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var typeSelectorIsShown by remember { mutableStateOf(false) }

    fun getIconForType(type: Int) = when (type) {
        Stuff.TYPE_ARTISTS -> Icons.Mic
        Stuff.TYPE_ALBUMS -> Icons.Album
        Stuff.TYPE_TRACKS -> Icons.MusicNote
        Stuff.TYPE_LOVES -> Icons.Favorite
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
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.random_text),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(text = getTextForType(type))
                    }
                }
            },
            trailingButton = {
                SplitButtonDefaults.OutlinedTrailingButton(
                    onCheckedChange = {
                        typeSelectorIsShown = it
                    },
                    checked = typeSelectorIsShown,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Icon(
                        Icons.ArrowDropDown,
                        contentDescription = stringResource(Res.string.item_options)
                    )
                }

                PanoDropdownMenu(
                    expanded = typeSelectorIsShown,
                    onDismissRequest = { typeSelectorIsShown = false }
                ) {
                    arrayOf(
                        Stuff.TYPE_ARTISTS,
                        Stuff.TYPE_ALBUMS,
                        Stuff.TYPE_TRACKS,
                        Stuff.TYPE_LOVES
                    ).forEach { thisType ->
                        item(
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
            },
        )
    }
}