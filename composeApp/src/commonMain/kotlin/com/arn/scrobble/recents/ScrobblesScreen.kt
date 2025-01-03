package com.arn.scrobble.recents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.DatePickerModal
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.charts.getPeriodTypeIcon
import com.arn.scrobble.charts.getPeriodTypePluralRes
import com.arn.scrobble.main.PanoPullToRefresh
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ExpandableHeaderMenu
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.expandableSublist
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.timeToLocal
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.enable
import pano_scrobbler.composeapp.generated.resources.fix_it_title
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.no_scrobbles
import pano_scrobbler.composeapp.generated.resources.not_running
import pano_scrobbler.composeapp.generated.resources.num_pending
import pano_scrobbler.composeapp.generated.resources.random_text
import pano_scrobbler.composeapp.generated.resources.recents
import pano_scrobbler.composeapp.generated.resources.scrobbler_off
import pano_scrobbler.composeapp.generated.resources.time_jump

private enum class ScrobblesType {
    RECENTS,
    LOVED,
    TIME_JUMP,
    RANDOM,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrobblesScreen(
    user: UserCached,
    showChips: Boolean,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: ScrobblesVM = viewModel { ScrobblesVM() },
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var selectedType by rememberSaveable { mutableStateOf(ScrobblesType.RECENTS) }
    var timeJumpMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val pendingScrobbles by viewModel.pendingScrobbles.collectAsStateWithLifecycle()
    val pendingLoves by viewModel.pendingLoves.collectAsStateWithLifecycle()
    val deletedTracksSet by viewModel.deletedTracksSet.collectAsStateWithLifecycle()
    val editedTracksMap by viewModel.editedTracksMap.collectAsStateWithLifecycle()
    val pkgMap by viewModel.pkgMap.collectAsStateWithLifecycle()
    val scrobblerEnabled by viewModel.scrobblerEnabled.collectAsStateWithLifecycle()
    val scrobblerRunning by viewModel.scrobblerServiceRunning.collectAsStateWithLifecycle()
    val isListenBrainz by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType == AccountType.LISTENBRAINZ }
    var pendingScrobblesExpanded by rememberSaveable { mutableStateOf(false) }
    var pendingLovesExpanded by rememberSaveable { mutableStateOf(false) }
    var pendingScrobblesMenuShown by remember { mutableStateOf(false) }
    var pendingLovesMenuShown by remember { mutableStateOf(false) }
    val pendingScrobblesheader =
        pluralStringResource(Res.plurals.num_pending, pendingScrobbles.size, pendingScrobbles.size)
    val pendingLovesHeader = stringResource(Res.string.loved) + ": " + pendingLoves.size

    fun onTrackClick(track: Track, pkgName: String?) {
        onNavigate(PanoRoute.MusicEntryInfo(user = user, track = track, pkgName = pkgName))
    }

    LaunchedEffect(user, selectedType, timeJumpMillis) {
        when (selectedType) {
            ScrobblesType.RANDOM -> {
            }

            ScrobblesType.LOVED -> {
                viewModel.setScrobblesInput(
                    ScrobblesInput(
                        user = user,
                        loadLoved = true,
                    )
                )
            }

            ScrobblesType.TIME_JUMP -> {
                if (timeJumpMillis != null)
                    viewModel.setScrobblesInput(
                        ScrobblesInput(
                            user = user,
                            timeJumpMillis = timeJumpMillis
                        )
                    )
            }

            ScrobblesType.RECENTS -> {
                viewModel.setScrobblesInput(
                    ScrobblesInput(
                        user = user,
                    )
                )
            }
        }
    }

    PanoPullToRefresh(
        isRefreshing = tracks.loadState.refresh is LoadState.Loading,
        onRefresh = { tracks.refresh() },
    ) {
        EmptyText(
            visible = tracks.loadState.refresh is LoadState.NotLoading &&
                    tracks.itemCount == 0 &&
                    pendingScrobbles.isEmpty() &&
                    pendingLoves.isEmpty(),
            text = stringResource(Res.string.no_scrobbles)
        )

        PanoLazyColumn(
            state = listState,
            modifier = modifier
                .then(
                    if (tracks.loadState.refresh is LoadState.Loading)
                        Modifier.shimmerWindowBounds()
                    else Modifier
                )

        ) {
            if (showChips) {
                stickyHeader(key = "header_chips") {
                    ScrobblesTypeSelector(
                        selectedType = selectedType,
                        timeJumpMillis = timeJumpMillis,
                        registeredTime = user.registeredTime,
                        onTypeSelected = { type, _timeJumpMillis ->
                            when (type) {
                                ScrobblesType.RANDOM -> {
                                    onNavigate(PanoRoute.Random(user))
                                }

                                ScrobblesType.RECENTS,
                                ScrobblesType.LOVED,
                                    -> {
                                    selectedType = type
                                }

                                ScrobblesType.TIME_JUMP -> {
                                    timeJumpMillis = _timeJumpMillis
                                    selectedType = type
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (!scrobblerEnabled || scrobblerRunning == false) {
                item("notice") {
                    val menuItemText =
                        stringResource(
                            if (scrobblerEnabled)
                                Res.string.fix_it_title
                            else
                                Res.string.enable
                        )

                    val text = stringResource(
                        if (scrobblerEnabled)
                            Res.string.not_running
                        else
                            Res.string.scrobbler_off
                    )

                    ExpandableHeaderMenu(
                        title = text,
                        icon = Icons.Outlined.Info,
                        menuItemText = menuItemText,
                        onMenuItemClick = {
                            if (scrobblerEnabled) {
                                onNavigate(PanoRoute.FixIt)
                            } else {
                                val hasNotificationListenerPerms =
                                    PlatformStuff.isNotificationListenerEnabled()

                                if (!hasNotificationListenerPerms) {
                                    onNavigate(PanoRoute.Onboarding)
                                } else {
                                    onNavigate(PanoRoute.Prefs)
                                }
                            }

                            viewModel.updateScrobblerServiceStatus()
                        },
                    )
                }
            }

            expandableSublist(
                headerText = pendingScrobblesheader,
                headerIcon = Icons.Outlined.HourglassEmpty,
                items = pendingScrobbles,
                transformToMusicEntry = {
                    Track(
                        name = it.track,
                        artist = Artist(it.artist),
                        album = it.album.ifEmpty { null }?.let { Album(it) },
                        date = it.timestamp,
                        duration = it.duration,
                    )
                },
                expanded = pendingScrobblesExpanded,
                onToggle = {
                    pendingScrobblesExpanded = it
                },
                onItemClick = {
                    onTrackClick(it as Track, null)
                },
                onMenuClick = {
                    pendingScrobblesMenuShown = true
                },
                menuContent = {
                    PendingDropdownMenu(
                        pending = it,
                        expanded = pendingScrobblesMenuShown,
                        onDismissRequest = { pendingScrobblesMenuShown = false }
                    )
                },
            )

            expandableSublist(
                headerText = pendingLovesHeader,
                headerIcon = Icons.Outlined.HourglassEmpty,
                items = pendingLoves,
                transformToMusicEntry = {
                    Track(
                        name = it.track,
                        artist = Artist(it.artist),
                        album = null,
                        userloved = it.shouldLove,
                        userHated = !it.shouldLove,
                    )
                },
                expanded = pendingLovesExpanded,
                onToggle = {
                    pendingLovesExpanded = it
                },
                onItemClick = {
                    onTrackClick(it as Track, null)
                },
                onMenuClick = {
                    pendingLovesMenuShown = true
                },
                menuContent = {
                    PendingDropdownMenu(
                        pending = it,
                        expanded = pendingLovesMenuShown,
                        onDismissRequest = { pendingLovesMenuShown = false }
                    )
                },
            )

            scrobblesListItems(
                tracks = tracks,
                user = user,
                deletedTracksSet = deletedTracksSet,
                editedTracksMap = editedTracksMap,
                pkgMap = pkgMap,
                fetchAlbumImageIfMissing = selectedType == ScrobblesType.LOVED,
                showFullMenu = selectedType != ScrobblesType.LOVED,
                showHate = isListenBrainz,
                onNavigate = onNavigate,
                viewModel = viewModel,
            )

            scrobblesPlaceholdersAndErrors(tracks = tracks)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScrobblesTypeSelector(
    selectedType: ScrobblesType,
    timeJumpMillis: Long?,
    registeredTime: Long,
    onTypeSelected: (ScrobblesType, Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeJumpMenuShown by remember { mutableStateOf(false) }
    var datePickerShown by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            label = {
                Text(text = stringResource(Res.string.recents))
            },
            leadingIcon = {
                Icon(Icons.Rounded.History, contentDescription = null)
            },
            selected = selectedType == ScrobblesType.RECENTS,
            onClick = { onTypeSelected(ScrobblesType.RECENTS, null) },
        )

        FilterChip(
            label = {
                Text(text = stringResource(Res.string.loved))
            },
            leadingIcon = {
                Icon(Icons.Rounded.FavoriteBorder, contentDescription = null)
            },
            selected = selectedType == ScrobblesType.LOVED,
            onClick = { onTypeSelected(ScrobblesType.LOVED, null) },
        )

        Box {
            FilterChip(
                label = {
                    Text(
                        if (timeJumpMillis == null)
                            stringResource(Res.string.time_jump)
                        else
                            PanoTimeFormatter.relative(timeJumpMillis)
                    )
                },
                trailingIcon = {
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                },
                selected = selectedType == ScrobblesType.TIME_JUMP,
                onClick = { timeJumpMenuShown = true },
            )

            DropdownMenu(
                expanded = timeJumpMenuShown,
                onDismissRequest = { timeJumpMenuShown = false },
            ) {
                val timeJumpEntries = remember(registeredTime, timeJumpMillis) {
                    TimePeriodsGenerator(
                        registeredTime,
                        timeJumpMillis ?: System.currentTimeMillis(),
                    ).recentsTimeJumps
                }

                timeJumpEntries.forEach {
                    DropdownMenuItem(
                        onClick = {
                            onTypeSelected(ScrobblesType.TIME_JUMP, it.timeMillis)
                            timeJumpMenuShown = false
                        },
                        leadingIcon = {
                            Icon(getPeriodTypeIcon(it.type), contentDescription = null)
                        },
                        text = {
                            val name = pluralStringResource(
                                getPeriodTypePluralRes(it.type),
                                1,
                                (if (it.addsTime) "+1" else "-1")
                            )
                            Text(text = name)
                        }
                    )
                }
                DropdownMenuItem(
                    onClick = {
                        datePickerShown = true
                        timeJumpMenuShown = false
                    },
                    leadingIcon = {
                        Icon(getPeriodTypeIcon(TimePeriodType.CUSTOM), contentDescription = null)
                    },
                    text = {
                        Text(text = stringResource(Res.string.charts_custom))
                    }
                )
            }
        }

        FilterChip(
            label = {
                Text(text = stringResource(Res.string.random_text))
            },
            selected = selectedType == ScrobblesType.RANDOM,
            onClick = { onTypeSelected(ScrobblesType.RANDOM, null) },
        )
    }

    if (datePickerShown) {
        DatePickerModal(
            selectedDate = timeJumpMillis,
            allowedRange = Pair(registeredTime, System.currentTimeMillis()),
            onDateSelected = {
                onTypeSelected(
                    ScrobblesType.TIME_JUMP,
                    it?.timeToLocal()?.plus((24 * 60 * 60 - 1) * 1000)
                )
            },
            onDismiss = { datePickerShown = false },
        )
    }
}
