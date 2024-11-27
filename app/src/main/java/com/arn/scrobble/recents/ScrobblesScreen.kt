package com.arn.scrobble.recents

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.DatePickerModal
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.charts.getPeriodTypeIcon
import com.arn.scrobble.charts.getPeriodTypePluralRes
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.main.PanoPullToRefresh
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.ExpandableHeaderMenu
import com.arn.scrobble.ui.expandableSublist
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.timeToLocal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private enum class ScrobblesType {
    RECENTS,
    LOVED,
    TIME_JUMP,
    RANDOM,
}

@Composable
fun ScrobblesScreen(
    user: UserCached,
    showChips: Boolean,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: ScrobblesVM = viewModel(),
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(ScrobblesType.RECENTS) }
    val timeJumpMillis by viewModel.timeJumpMillis.collectAsStateWithLifecycle(null)
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val pendingScrobbles by viewModel.pendingScrobbles.collectAsStateWithLifecycle()
    val pendingLoves by viewModel.pendingLoves.collectAsStateWithLifecycle()
    val deletedTracksSet by viewModel.deletedTracksSet.collectAsStateWithLifecycle()
    val editedTracksMap by viewModel.editedTracksMap.collectAsStateWithLifecycle()
    val pkgMap by viewModel.pkgMap.collectAsStateWithLifecycle()
    val scrobblerEnabled by viewModel.scrobblerEnabled.collectAsStateWithLifecycle()
    val scrobblerRunning by viewModel.scrobblerServiceRunning.collectAsStateWithLifecycle()
    val isListenBrainz by Scrobblables.current.map { it?.userAccount?.type == AccountType.LISTENBRAINZ }
        .collectAsStateWithLifecycle(false)
    var pendingScrobblesExpanded by remember { mutableStateOf(false) }
    var pendingLovesExpanded by remember { mutableStateOf(false) }
    var pendingScrobblesMenuShown by remember { mutableStateOf(false) }
    var pendingLovesMenuShown by remember { mutableStateOf(false) }
    val pendingScrobblesheader =
        pluralStringResource(R.plurals.num_pending, pendingScrobbles.size, pendingScrobbles.size)
    val pendingLovesHeader = stringResource(R.string.loved) + ": " + pendingLoves.size

    fun onTrackClick(track: Track, pkgName: String?) {
        onNavigate(PanoRoute.MusicEntryInfo(user = user, track = track, pkgName = pkgName))
    }

    LaunchedEffect(Unit) {
        viewModel.setScrobblesInput(
            ScrobblesInput(
                user = user,
            ),
            initial = true
        )
    }

    PanoPullToRefresh(
        isRefreshing = tracks.loadState.refresh is LoadState.Loading,
        onRefresh = { tracks.refresh() },
    ) {
        LazyColumn(
            state = listState,
            contentPadding = panoContentPadding(),
            modifier = modifier
        ) {
            if (showChips) {
                stickyHeader(key = "header_chips") {
                    ScrobblesTypeSelector(
                        selectedType = selectedType,
                        timeJumpMillis = timeJumpMillis,
                        registeredTime = user.registeredTime,
                        onTypeSelected = { type, timeJumpMillis ->
                            when (type) {
                                ScrobblesType.RANDOM -> {
                                    onNavigate(PanoRoute.Random(user))
                                }

                                ScrobblesType.LOVED -> {
                                    selectedType = type
                                    viewModel.setScrobblesInput(
                                        ScrobblesInput(
                                            user = user,
                                            loadLoved = true,
                                        )
                                    )
                                }

                                ScrobblesType.TIME_JUMP -> {
                                    selectedType = type
                                    viewModel.setScrobblesInput(
                                        ScrobblesInput(
                                            user = user,
                                            timeJumpMillis = timeJumpMillis
                                        )
                                    )
                                }

                                ScrobblesType.RECENTS -> {
                                    selectedType = type
                                    viewModel.setScrobblesInput(
                                        ScrobblesInput(
                                            user = user,
                                        )
                                    )
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
                                R.string.fix_it_title
                            else
                                R.string.enable
                        )

                    val text = stringResource(
                        if (scrobblerEnabled)
                            R.string.not_running
                        else
                            R.string.scrobbler_off
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
                                    Stuff.isNotificationListenerEnabled()

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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(
            label = {
                Text(text = stringResource(id = R.string.recents))
            },
            leadingIcon = {
                Icon(Icons.Rounded.History, contentDescription = null)
            },
            selected = selectedType == ScrobblesType.RECENTS,
            onClick = { onTypeSelected(ScrobblesType.RECENTS, null) },
        )

        FilterChip(
            label = {
                Text(text = stringResource(id = R.string.loved))
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
                            stringResource(id = R.string.time_jump)
                        else
                            Stuff.myRelativeTime(millis = timeJumpMillis)
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
                        PlatformStuff.application
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
                        Text(text = stringResource(id = R.string.charts_custom))
                    }
                )
            }
        }

        FilterChip(
            label = {
                Text(text = stringResource(id = R.string.random_text))
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
