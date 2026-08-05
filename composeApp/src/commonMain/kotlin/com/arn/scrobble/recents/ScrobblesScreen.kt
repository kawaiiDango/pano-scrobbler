package com.arn.scrobble.recents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.result.ResultEffect
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.charts.getPeriodTypeIcon
import com.arn.scrobble.charts.getPeriodTypePluralRes
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.icons.HourglassEmpty
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.main.PanoPullToRefresh
import com.arn.scrobble.main.ScrobblerState
import com.arn.scrobble.navigation.DatePickerResult
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTab
import com.arn.scrobble.navigation.SubTabClickedResult
import com.arn.scrobble.ui.AutoRefreshEffect
import com.arn.scrobble.ui.DismissableNotice
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.PanoDropdownMenu
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.timeToLocal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.also_available_on
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.desktop
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.no_scrobbles
import pano_scrobbler.composeapp.generated.resources.not_running
import pano_scrobbler.composeapp.generated.resources.pending_scrobbles
import pano_scrobbler.composeapp.generated.resources.recents
import pano_scrobbler.composeapp.generated.resources.scrobbler_off
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.time_jump
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


@Composable
fun ScrobblesScreen(
    user: UserCached,
    pullToRefreshState: PullToRefreshState,
    onSetRefreshing: (PanoPullToRefreshStateForTab) -> Unit,
    pullToRefreshTriggered: () -> Flow<Unit>,
    onNavigate: (PanoRoute) -> Unit,
    onTitleChange: (String) -> Unit,
    editDataFlow: Flow<Pair<String, Track>>,
    scrobblerStateFlow: StateFlow<ScrobblerState>,
    updateScrobblerState: () -> Unit,
    selectSubTabId: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScrobblesVM = viewModel(key = user.key<ScrobblesVM>()) { ScrobblesVM(user, null) },
) {
    val listState = rememberLazyListState()
    var selectedType by rememberSaveable { mutableStateOf(PanoTab.Scrobbles.ScrobblesType.RECENTS) }
    var timeJumpMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val firstPageLoadedTime by viewModel.firstPageLoadedTime.collectAsStateWithLifecycle()
    val pendingScrobblesWithCount by
    if (user.isSelf)
        viewModel.pendingScrobblesWithCount.collectAsStateWithLifecycle()
    else
        remember { mutableStateOf(emptyList<PendingScrobble>() to 0) }
    val (pendingScrobbles, pendingScrobblesCount) = pendingScrobblesWithCount
    val pendingScrobbleLastErrored by if (user.isSelf)
        viewModel.pendingScrobbleLastErrored.collectAsStateWithLifecycle()
    else
        remember { mutableStateOf(null) }
    val total by viewModel.total.collectAsStateWithLifecycle()
    val pkgMap by viewModel.pkgMap.collectAsStateWithLifecycle()
    val scrobblerState by scrobblerStateFlow.collectAsStateWithLifecycle()
    val showScrobbleSources by if (LocalLicenseValidState.current)
        PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.showScrobbleSources }
    else
        remember { mutableStateOf(false) }
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val otherPlatformsLearnt by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.desktopAppLearnt }
    var pendingScrobblesExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var canExpandNowPlaying by rememberSaveable { mutableStateOf(true) }
    var timeJumpMenuShown by rememberSaveable { mutableStateOf(false) }
    val pendingScrobblesHeader =
        stringResource(Res.string.pending_scrobbles) + ": " + pendingScrobblesCount
    val canLove = accountType != AccountType.PLEROMA
    val density = LocalDensity.current
    val listViewportHeight = remember {
        derivedStateOf {
            with(density) {
                listState.layoutInfo.viewportSize.height.toDp()
            }
        }
    }
    val animateListItemContentSize = remember {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount > listState.layoutInfo.visibleItemsInfo.size
        }
    }
    val scope = rememberCoroutineScope()

    val canEditOrDelete by remember(selectedType, accountType) {
        mutableStateOf(
            !PlatformStuff.isTv &&
                    selectedType != PanoTab.Scrobbles.ScrobblesType.LOVED &&
                    accountType !in arrayOf(AccountType.FILE, AccountType.PLEROMA)
        )
    }


    fun onTrackClick(track: Track, appId: String?) {
        onNavigate(PanoRoute.Modal.MusicEntryInfo(user = user, track = track, appId = appId))
    }

    LaunchedEffect(user, selectedType, timeJumpMillis, total, timeJumpMenuShown) {
        if (timeJumpMenuShown) {
            onTitleChange(getString(Res.string.time_jump))
            selectSubTabId(PanoTab.Scrobbles.ScrobblesType.TIME_JUMP.ordinal)
            return@LaunchedEffect
        }

        when (selectedType) {
            PanoTab.Scrobbles.ScrobblesType.LOVED -> {
                viewModel.setScrobblesInput(
                    ScrobblesInput(
                        showScrobbleSources = showScrobbleSources,
                        loadLoved = true,
                    )
                )

                onTitleChange(
                    getString(Res.string.loved) +
                            if (total != null)
                                ": " + total!!.format()
                            else
                                ""
                )
                expandedKey = null
                selectSubTabId(selectedType.ordinal)
            }

            PanoTab.Scrobbles.ScrobblesType.TIME_JUMP -> {
                val timeJumpMillis = timeJumpMillis

                if (timeJumpMillis != null) {
                    viewModel.setScrobblesInput(
                        ScrobblesInput(
                            showScrobbleSources = showScrobbleSources,
                            timeJumpMillis = timeJumpMillis
                        )
                    )

                    onTitleChange(PanoTimeFormatter.day(timeJumpMillis))
                } else
                    onTitleChange(getString(Res.string.time_jump))
                expandedKey = null
                selectSubTabId(selectedType.ordinal)
            }

            PanoTab.Scrobbles.ScrobblesType.RECENTS -> {
                viewModel.setScrobblesInput(
                    ScrobblesInput(showScrobbleSources = showScrobbleSources)
                )

                if (total != null)
                    onTitleChange(getString(Res.string.scrobbles) + ": " + total!!.format())
                else
                    onTitleChange(getString(Res.string.recents))
                canExpandNowPlaying = true
                selectSubTabId(selectedType.ordinal)
            }

            else -> {}
        }
    }

    LaunchedEffect(expandedKey) {
        if (expandedKey != null) {
            val expandedItem = listState.layoutInfo.visibleItemsInfo.find {
                it.key == expandedKey
            }

            listState.requestScrollToItem(expandedItem?.index ?: 0)
//            listState.animateScrollToItem(expandedItem?.index ?: 0)
        }
    }

    if (user.isSelf) {
        LifecycleStartEffect(Unit) {
            viewModel.setForeground(true)
            onStopOrDispose {
                viewModel.setForeground(false)
            }
        }

        LaunchedEffect(pendingScrobblesExpanded) {
            viewModel.setPendingScrobblesExpanded(pendingScrobblesExpanded)
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
            if (canExpandNowPlaying && tracks.itemCount > 0 &&
                (tracks.peek(0) as? TrackWrapper.TrackItem)?.track?.isNowPlaying == true
            ) {
                val newKey = tracks.peek(0)?.key

                val newExpandedItemIsVisible = listState.layoutInfo.visibleItemsInfo.find {
                    it.key == newKey
                } != null

                val isAlmostAtTop = listState.firstVisibleItemIndex < 5

                if (isAlmostAtTop || newExpandedItemIsVisible)
                    expandedKey = newKey
            }
        }

        onPauseOrDispose {
            onSetRefreshing(PanoPullToRefreshStateForTab.Disabled)
        }
    }

    LaunchedEffect(Unit) {
        pullToRefreshTriggered().collect {
            if (tracks.loadState.refresh is LoadState.NotLoading) {
                tracks.refresh()
            }
        }
    }

    AutoRefreshEffect(
        firstPageLoadedTime = firstPageLoadedTime,
        interval = Stuff.RECENTS_REFRESH_INTERVAL_S.seconds,
        doRefresh = {
            if (selectedType == PanoTab.Scrobbles.ScrobblesType.RECENTS && listState.firstVisibleItemIndex < 4) {
                tracks.refresh()
                true
            } else {
                false
            }
        },
        lazyPagingItems = tracks,
    )

    OnEditEffect(
        viewModel,
        editDataFlow
    )

    ResultEffect<SubTabClickedResult> { res ->
        when (res.id) {
            PanoTab.Scrobbles.ScrobblesType.REFRESH.ordinal -> {
                if (tracks.loadState.refresh is LoadState.NotLoading) {
                    tracks.refresh()
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            }

            PanoTab.Scrobbles.ScrobblesType.RECENTS.ordinal -> {
                selectedType = PanoTab.Scrobbles.ScrobblesType.RECENTS
                timeJumpMillis = null
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }

            PanoTab.Scrobbles.ScrobblesType.LOVED.ordinal -> {
                selectedType = PanoTab.Scrobbles.ScrobblesType.LOVED
                timeJumpMillis = null
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }

            PanoTab.Scrobbles.ScrobblesType.TIME_JUMP.ordinal -> {
                timeJumpMenuShown = true
            }

            PanoTab.Scrobbles.ScrobblesType.RANDOM.ordinal -> {
                onNavigate(PanoRoute.Random(user))
            }
        }
    }

    ResultEffect<DatePickerResult> { res ->
        selectedType = PanoTab.Scrobbles.ScrobblesType.TIME_JUMP
        timeJumpMillis = res.dateMillis.timeToLocal().plus((24 * 60 * 60 - 1) * 1000)
    }

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

        if (timeJumpMenuShown) {
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                TimeJumpMenu(
                    timeJumpMillis = timeJumpMillis,
                    registeredTime = user.registeredTime,
                    onNavigate = onNavigate,
                    onDismiss = { timeJumpMenuShown = false },
                    onTimeJumpSelected = {
                        selectedType = PanoTab.Scrobbles.ScrobblesType.TIME_JUMP
                        timeJumpMillis = it
                    },
                )
            }
        }

        PanoLazyColumn(
            state = listState,
            modifier = modifier
        ) {

            if (user.isSelf) {
                when (val scrobblerState = scrobblerState) {
                    ScrobblerState.Disabled, ScrobblerState.NLSDisabled -> {
                        item("notice") {
                            val innerScope = rememberCoroutineScope()
                            DismissableNotice(
                                title = stringResource(Res.string.scrobbler_off),
                                onClick = {
                                    updateScrobblerState()

                                    innerScope.launch {
                                        delay(500.milliseconds)
                                        if (scrobblerState == ScrobblerState.Disabled)
                                            onNavigate(PanoRoute.Prefs)
                                        else
                                            onNavigate(PanoRoute.Onboarding)

                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    is ScrobblerState.Killed -> {
                        item("notice") {
                            DismissableNotice(
                                title = stringResource(Res.string.not_running) + ": " +
                                        scrobblerState.reason?.shortText().orEmpty(),
                                onClick = { onNavigate(PanoRoute.Modal.FixIt(scrobblerState.reason)) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    ScrobblerState.Unknown,
                    ScrobblerState.Running -> {
                        // todo remove canEditOrDelete
                        if (!otherPlatformsLearnt && canEditOrDelete && !PlatformStuff.isTv && !PlatformStuff.isDesktop) {
                            item("notice") {
                                DismissableNotice(
                                    title = stringResource(
                                        Res.string.also_available_on,
                                        stringResource(Res.string.desktop)
                                    ),
                                    onClick = {
                                        onNavigate(PanoRoute.Modal.ShowLink(Stuff.HOMEPAGE_URL))
                                    },
                                    onDismiss = {
                                        scope.launch {
                                            PlatformStuff.mainPrefs.updateData {
                                                it.copy(desktopAppLearnt = true)
                                            }
                                        }
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }

            if (selectedType == PanoTab.Scrobbles.ScrobblesType.RECENTS && user.isSelf) {
                pendingScrobblesListItems(
                    headerText = pendingScrobblesHeader,
                    headerIcon = Icons.HourglassEmpty,
                    items = pendingScrobbles,
                    lastErrored = pendingScrobbleLastErrored,
                    expanded = if (pendingScrobblesCount <= viewModel.pendingScrobblesPreviewCount)
                        null
                    else
                        pendingScrobblesExpanded,
                    onToggle = {
                        pendingScrobblesExpanded = it
                    },
                    showScrobbleSources = showScrobbleSources,
                    onItemClick = {
                        onTrackClick(it as Track, null)
                    },
                    viewModel = viewModel,
                )

                if (pendingScrobbles.isNotEmpty()) {
                    item("pending_divider") {
                        HorizontalDivider(
                            modifier = Modifier.animateItem().padding(vertical = 8.dp)
                        )
                    }
                }
            }

            scrobblesListItems(
                tracks = tracks,
                user = user,
                pkgMap = pkgMap,
                fetchAlbumImageIfMissing = selectedType == PanoTab.Scrobbles.ScrobblesType.LOVED,
                showScrobbleSources = showScrobbleSources,
                canLove = canLove,
                canEdit = canEditOrDelete,
                canDelete = canEditOrDelete,
                canHate = accountType == AccountType.LISTENBRAINZ,
                expandedKey = { expandedKey },
                onExpand = {
                    canExpandNowPlaying = !(expandedKey != null && it == null)

                    expandedKey = it
                },
                onNavigate = onNavigate,
                animateListItemContentSize = animateListItemContentSize,
                maxHeight = listViewportHeight,
                viewModel = viewModel,
            )

            scrobblesPlaceholdersAndErrors(tracks = tracks)
        }
    }
}

@Composable
private fun TimeJumpMenu(
    timeJumpMillis: Long?,
    registeredTime: Long,
    onDismiss: () -> Unit,
    onTimeJumpSelected: (Long) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
) {
    val firstDayOfWeek by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.firstDayOfWeek }

    PanoDropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
    ) {
        val timeJumpEntries = remember(registeredTime, timeJumpMillis) {
            TimePeriodsGenerator(
                registeredTime,
                timeJumpMillis ?: System.currentTimeMillis(),
                firstDayOfWeek
            ).recentsTimeJumps
        }

        timeJumpEntries.forEach {
            DropdownMenuItem(
                onClick = {
                    onTimeJumpSelected(it.timeMillis)
                    onDismiss()
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
                val route = PanoRoute.Modal.DatePicker(
                    selectedDate = timeJumpMillis,
                    allowedRange = registeredTime to System.currentTimeMillis(),
                    weeksOnly = false,
                )
                onNavigate(route)
                onDismiss()
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
