package com.arn.scrobble.recents

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.DatePickerModal
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.charts.getPeriodTypeIcon
import com.arn.scrobble.charts.getPeriodTypePluralRes
import com.arn.scrobble.main.PanoPullToRefresh
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.AutoRefreshEffect
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ExpandableHeaderMenu
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.combineImageVectors
import com.arn.scrobble.ui.generateKey
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.timeToLocal
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.getString
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
import pano_scrobbler.composeapp.generated.resources.retry
import pano_scrobbler.composeapp.generated.resources.scrobbler_off
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.time_jump

private enum class ScrobblesType {
    RECENTS,
    LOVED,
    TIME_JUMP,
    RANDOM,
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScrobblesScreen(
    user: UserCached,
    showChips: Boolean,
    pullToRefreshState: PullToRefreshState,
    onSetRefreshing: (PanoPullToRefreshStateForTab) -> Unit,
    pullToRefreshTriggered: Flow<Unit>,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    onTitleChange: (String?) -> Unit,
    editDataFlow: Flow<Pair<Track, ScrobbleData>>,
    modifier: Modifier = Modifier,
    viewModel: ScrobblesVM = viewModel { ScrobblesVM() },
) {
    val listState = rememberLazyListState()
    var selectedType by rememberSaveable { mutableStateOf(ScrobblesType.RECENTS) }
    var timeJumpMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val lastRecentsRefreshTime by viewModel.lastRecentsRefreshTime.collectAsStateWithLifecycle()
    val pendingScrobbles by viewModel.pendingScrobbles.collectAsStateWithLifecycle()
    val deletedTracksSet by viewModel.deletedTracksSet.collectAsStateWithLifecycle()
    val editedTracksMap by viewModel.editedTracksMap.collectAsStateWithLifecycle()
    val pkgMap by viewModel.pkgMap.collectAsStateWithLifecycle()
    val scrobblerEnabled by viewModel.scrobblerEnabled.collectAsStateWithLifecycle()
    val scrobblerRunning by viewModel.scrobblerServiceRunning.collectAsStateWithLifecycle()
    val currentAccoutType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    var pendingScrobblesExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedIdx by rememberSaveable { mutableIntStateOf(-1) }
    var canExpandNowPlaying by rememberSaveable { mutableStateOf(true) }
    var stickyHeaderHeight by remember { mutableIntStateOf(0) }
    val pendingScrobblesheader =
        pluralStringResource(Res.plurals.num_pending, pendingScrobbles.size, pendingScrobbles.size)
    val canShowTrackFullMenu by remember(selectedType, currentAccoutType) {
        mutableStateOf(
            selectedType != ScrobblesType.LOVED && currentAccoutType !in arrayOf(
                AccountType.FILE,
                AccountType.MALOJA,
                AccountType.PLEROMA,
            )
        )
    }

    fun onTrackClick(track: Track, pkgName: String?) {
        onOpenDialog(PanoDialog.MusicEntryInfo(user = user, track = track, pkgName = pkgName))
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

                onTitleChange(getString(Res.string.loved))
                expandedIdx = -1
            }

            ScrobblesType.TIME_JUMP -> {
                if (timeJumpMillis != null)
                    viewModel.setScrobblesInput(
                        ScrobblesInput(
                            user = user,
                            timeJumpMillis = timeJumpMillis
                        )
                    )

                onTitleChange(getString(Res.string.time_jump))
                expandedIdx = -1
            }

            ScrobblesType.RECENTS -> {
                viewModel.setScrobblesInput(
                    ScrobblesInput(
                        user = user,
                    )
                )

                onTitleChange(getString(Res.string.scrobbles))

                canExpandNowPlaying = true
            }
        }
    }

    LaunchedEffect(expandedIdx) {
        if (expandedIdx != -1) {
            val key = if (expandedIdx < tracks.itemCount)
                tracks.peek(expandedIdx)?.generateKey() ?: return@LaunchedEffect
            else
                return@LaunchedEffect

            listState.layoutInfo.visibleItemsInfo.find {
                it.key == key
            }?.let {
                listState.scrollToItem(it.index, -stickyHeaderHeight)
            }
        }
    }

    LifecycleResumeEffect(tracks.loadState) {
        onSetRefreshing(
            if (tracks.loadState.refresh is LoadState.Loading) {
                PanoPullToRefreshStateForTab.Refreshing
            } else {
                PanoPullToRefreshStateForTab.NotRefreshing
            }
        )

        // expand now playing
        if (tracks.loadState.refresh is LoadState.NotLoading) {
            if (canExpandNowPlaying && tracks.itemCount > 0 && tracks.peek(0)?.isNowPlaying == true && (expandedIdx == -1 || expandedIdx == 0)) {
                expandedIdx = 0
            }
        }

        onPauseOrDispose {
            onSetRefreshing(PanoPullToRefreshStateForTab.Disabled)
        }
    }

    LaunchedEffect(Unit) {
        pullToRefreshTriggered.collect {
            if (tracks.loadState.refresh is LoadState.NotLoading) {
                tracks.refresh()
            }
        }
    }

    AutoRefreshEffect(
        lastRefreshTime = lastRecentsRefreshTime,
        interval = Stuff.RECENTS_REFRESH_INTERVAL,
        shouldRefresh = {
            selectedType == ScrobblesType.RECENTS && listState.firstVisibleItemIndex < 4
        },
        lazyPagingItems = tracks,
    )

    OnEditEffect(
        viewModel,
        editDataFlow
    )

    PanoPullToRefresh(
        isRefreshing = tracks.loadState.refresh is LoadState.Loading,
        state = pullToRefreshState,
    ) {
        EmptyText(
            visible = tracks.loadState.refresh is LoadState.NotLoading &&
                    tracks.itemCount == 0 &&
                    pendingScrobbles.isEmpty(),
            text = stringResource(Res.string.no_scrobbles)
        )

        PanoLazyColumn(
            state = listState,
            modifier = modifier
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
                        onRefresh = {
                            if (tracks.loadState.refresh is LoadState.NotLoading) {
                                tracks.refresh()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                            .onSizeChanged {
                                stickyHeaderHeight = it.height
                            },
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
                                onOpenDialog(PanoDialog.FixIt)
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

            if (selectedType == ScrobblesType.RECENTS) {
                pendingScrobblesListItems(
                    headerText = pendingScrobblesheader,
                    headerIcon = Icons.Outlined.HourglassEmpty,
                    items = pendingScrobbles,
                    expanded = pendingScrobblesExpanded,
                    onToggle = {
                        pendingScrobblesExpanded = it
                    },
                    onItemClick = {
                        onTrackClick(it as Track, null)
                    },
                )

                if (pendingScrobbles.isNotEmpty()) {
                    item("pending_divider") {
                        HorizontalDivider()
                    }
                }
            }

            scrobblesListItems(
                tracks = tracks,
                user = user,
                deletedTracksSet = deletedTracksSet,
                editedTracksMap = editedTracksMap,
                pkgMap = pkgMap,
                fetchAlbumImageIfMissing = selectedType == ScrobblesType.LOVED,
                showFullMenu = canShowTrackFullMenu,
                showLove = true,
                showHate = currentAccoutType == AccountType.LISTENBRAINZ,
                expandedIdx = { expandedIdx },
                onExpand = {
                    if (expandedIdx == 0 && it == -1)
                        canExpandNowPlaying = false
                    else if (it == 0)
                        canExpandNowPlaying = true

                    expandedIdx = it
                },
                onOpenDialog = onOpenDialog,
                viewModel = viewModel,
            )

            scrobblesPlaceholdersAndErrors(tracks = tracks)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScrobblesTypeSelector(
    selectedType: ScrobblesType,
    timeJumpMillis: Long?,
    registeredTime: Long,
    onTypeSelected: (ScrobblesType, Long?) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeJumpMenuShown by remember { mutableStateOf(false) }
    var datePickerShown by rememberSaveable { mutableStateOf(false) }

    ButtonGroup(
        horizontalArrangement = Arrangement.spacedBy(
            ButtonGroupDefaults.HorizontalArrangement.spacing,
            alignment = Alignment.CenterHorizontally
        ),
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.shapes.large
        )
    ) {
        if (PlatformStuff.isDesktop || PlatformStuff.isTv) {
            OutlinedToggleButton(
                checked = false,
                onCheckedChange = {
                    if (it)
                        onRefresh()
                },
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(Res.string.retry))
            }
        }

        OutlinedToggleButton(
            checked = selectedType == ScrobblesType.RECENTS,
            onCheckedChange = {
                if (it)
                    onTypeSelected(ScrobblesType.RECENTS, null)
            },
        ) {
            Icon(Icons.Rounded.History, contentDescription = stringResource(Res.string.recents))

            if (selectedType == ScrobblesType.RECENTS) {
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(text = stringResource(Res.string.recents), maxLines = 1)
            }
        }

        OutlinedToggleButton(
            checked = selectedType == ScrobblesType.LOVED,
            onCheckedChange = {
                if (it)
                    onTypeSelected(ScrobblesType.LOVED, null)
            },
        ) {
            Icon(
                Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(Res.string.loved)
            )

            if (selectedType == ScrobblesType.LOVED) {
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(text = stringResource(Res.string.loved), maxLines = 1)
            }
        }

        OutlinedToggleButton(
            checked = selectedType == ScrobblesType.TIME_JUMP,
            onCheckedChange = {
                if ((it && selectedType != ScrobblesType.TIME_JUMP) ||
                    (!it && selectedType == ScrobblesType.TIME_JUMP)
                )
                    timeJumpMenuShown = true
            },
        ) {
            Icon(
                combineImageVectors(
                    getPeriodTypeIcon(TimePeriodType.CUSTOM),
                    Icons.Outlined.ArrowDropDown
                ), contentDescription = stringResource(Res.string.time_jump)
            )

            if (selectedType == ScrobblesType.TIME_JUMP) {
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(
                    if (timeJumpMillis == null)
                        stringResource(Res.string.time_jump)
                    else
                        PanoTimeFormatter.relative(timeJumpMillis),
                    maxLines = 1
                )
            }

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

        OutlinedToggleButton(
            checked = selectedType == ScrobblesType.RANDOM,
            onCheckedChange = {
                if (it)
                    onTypeSelected(ScrobblesType.RANDOM, null)
            },
        ) {
            Icon(Icons.Outlined.Casino, contentDescription = stringResource(Res.string.random_text))

            if (selectedType == ScrobblesType.RANDOM) {
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(text = stringResource(Res.string.random_text), maxLines = 1)
            }
        }
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
