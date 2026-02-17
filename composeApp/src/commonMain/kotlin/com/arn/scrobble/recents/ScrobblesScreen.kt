package com.arn.scrobble.recents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.charts.DatePickerModal
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator
import com.arn.scrobble.charts.getPeriodTypeIcon
import com.arn.scrobble.charts.getPeriodTypePluralRes
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.Casino
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.History
import com.arn.scrobble.icons.HourglassEmpty
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Info
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.icons.Refresh
import com.arn.scrobble.icons.Warning
import com.arn.scrobble.main.PanoPullToRefresh
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.AutoRefreshEffect
import com.arn.scrobble.ui.DismissableNotice
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.combineImageVectors
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.timeToLocal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.also_available_on
import pano_scrobbler.composeapp.generated.resources.android
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.desktop
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.no_scrobbles
import pano_scrobbler.composeapp.generated.resources.not_running
import pano_scrobbler.composeapp.generated.resources.num_pending
import pano_scrobbler.composeapp.generated.resources.random_text
import pano_scrobbler.composeapp.generated.resources.recents
import pano_scrobbler.composeapp.generated.resources.reload
import pano_scrobbler.composeapp.generated.resources.scrobbler_off
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.time_jump

private enum class ScrobblesType {
    RECENTS,
    LOVED,
    TIME_JUMP,
}

@Composable
fun ScrobblesScreen(
    user: UserCached,
    showChips: Boolean,
    pullToRefreshState: PullToRefreshState,
    onSetRefreshing: (PanoPullToRefreshStateForTab) -> Unit,
    pullToRefreshTriggered: Flow<Unit>,
    onNavigate: (PanoRoute) -> Unit,
    onTitleChange: (String) -> Unit,
    editDataFlow: Flow<Pair<String, Track>>,
    modifier: Modifier = Modifier,
    viewModel: ScrobblesVM = viewModel(key = user.key<ScrobblesVM>()) { ScrobblesVM(user, null) },
) {
    val listState = rememberLazyListState()
    var selectedType by rememberSaveable { mutableStateOf(ScrobblesType.RECENTS) }
    var timeJumpMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val firstPageLoadedTime by viewModel.firstPageLoadedTime.collectAsStateWithLifecycle()
    val pendingScrobbles by
    if (user.isSelf)
        viewModel.pendingScrobbles.collectAsStateWithLifecycle()
    else
        remember { mutableStateOf(emptyList()) }
    val total by viewModel.total.collectAsStateWithLifecycle()
    val pkgMap by viewModel.pkgMap.collectAsStateWithLifecycle()
    val nlsEnabled by viewModel.nlsEnabled.collectAsStateWithLifecycle()
    val scrobblerEnabled by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.scrobblerEnabled }
    val scrobblerRunning by viewModel.scrobblerServiceRunning.collectAsStateWithLifecycle()
    val showScrobbleSources by if (LocalLicenseValidState.current)
        PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.showScrobbleSources }
    else
        remember { mutableStateOf(false) }
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val otherPlatformsLearnt by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.otherPlatformsLearnt }
    var pendingScrobblesExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var canExpandNowPlaying by rememberSaveable { mutableStateOf(true) }
    val pendingScrobblesheader =
        pluralStringResource(Res.plurals.num_pending, pendingScrobbles.size, pendingScrobbles.size)
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
                    selectedType != ScrobblesType.LOVED &&
                    accountType !in arrayOf(AccountType.FILE, AccountType.PLEROMA)
        )
    }


    fun onTrackClick(track: Track, appId: String?) {
        onNavigate(PanoRoute.Modal.MusicEntryInfo(user = user, track = track, appId = appId))
    }

    LaunchedEffect(user, selectedType, timeJumpMillis, total) {
        when (selectedType) {
            ScrobblesType.LOVED -> {
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
            }

            ScrobblesType.TIME_JUMP -> {
                if (timeJumpMillis != null)
                    viewModel.setScrobblesInput(
                        ScrobblesInput(
                            showScrobbleSources = showScrobbleSources,
                            timeJumpMillis = timeJumpMillis
                        )
                    )

                onTitleChange(getString(Res.string.time_jump))
                expandedKey = null
            }

            ScrobblesType.RECENTS -> {
                viewModel.setScrobblesInput(
                    ScrobblesInput(showScrobbleSources = showScrobbleSources)
                )

                onTitleChange(
                    getString(Res.string.scrobbles) +
                            if (total != null)
                                ": " + total!!.format()
                            else
                                ""
                )
                canExpandNowPlaying = true
            }
        }
    }

    LaunchedEffect(expandedKey) {
        if (expandedKey != null) {
            val expandedItem = listState.layoutInfo.visibleItemsInfo.find {
                it.key == expandedKey
            }

            listState.animateScrollToItem(expandedItem?.index ?: 0)
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
        pullToRefreshTriggered.collect {
            if (tracks.loadState.refresh is LoadState.NotLoading) {
                tracks.refresh()
            }
        }
    }

    AutoRefreshEffect(
        firstPageLoadedTime = firstPageLoadedTime,
        interval = Stuff.RECENTS_REFRESH_INTERVAL,
        doRefresh = {
            if (selectedType == ScrobblesType.RECENTS && listState.firstVisibleItemIndex < 4) {
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

        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)
        ) {
            if (showChips) {
                ScrobblesTypeSelector(
                    selectedType = selectedType,
                    timeJumpMillis = timeJumpMillis,
                    registeredTime = user.registeredTime,
                    onTypeSelected = { type, timeJumpMillisp ->
                        when (type) {
                            ScrobblesType.RECENTS,
                            ScrobblesType.LOVED,
                                -> {
                                selectedType = type
                                timeJumpMillis = null
                            }

                            ScrobblesType.TIME_JUMP -> {
                                timeJumpMillis = timeJumpMillisp
                                selectedType = type
                            }
                        }

                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    onRefresh = {
                        if (tracks.loadState.refresh is LoadState.NotLoading) {
                            tracks.refresh()
                        }
                    },
                    onNavigateToRandom = {
                        onNavigate(PanoRoute.Random(user))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PanoLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {

                // todo remove canEditOrDelete
                if (user.isSelf && (!nlsEnabled || !scrobblerEnabled || scrobblerRunning == false ||
                            (!otherPlatformsLearnt && canEditOrDelete && !PlatformStuff.isTv))
                ) {
                    item("notice") {
                        val text: String
                        val icon: ImageVector
                        val onClick: () -> Unit
                        var onDismiss: (() -> Unit)? = null

                        when {
                            !nlsEnabled -> {
                                text = stringResource(Res.string.scrobbler_off)
                                icon = Icons.Info

                                onClick = {
                                    viewModel.updateScrobblerServiceStatus()
                                    onNavigate(PanoRoute.Onboarding)
                                }
                            }

                            !scrobblerEnabled -> {
                                text = stringResource(Res.string.scrobbler_off)
                                icon = Icons.Info

                                onClick = { onNavigate(PanoRoute.Prefs) }
                            }

                            scrobblerRunning == false -> {
                                text = stringResource(Res.string.not_running)
                                icon = Icons.Warning
                                onClick = { onNavigate(PanoRoute.Modal.FixIt) }
                            }

                            else -> {
                                text = stringResource(
                                    Res.string.also_available_on,
                                    if (PlatformStuff.isDesktop)
                                        stringResource(Res.string.android)
                                    else
                                        stringResource(Res.string.desktop)
                                )
                                icon = Icons.OpenInBrowser
                                onClick = {
                                    onNavigate(PanoRoute.Modal.ShowLink(Stuff.HOMEPAGE_URL))
                                }
                                onDismiss = {
                                    scope.launch {
                                        PlatformStuff.mainPrefs.updateData {
                                            it.copy(otherPlatformsLearnt = true)
                                        }
                                    }
                                }
                            }
                        }

                        DismissableNotice(
                            title = text,
                            icon = icon,
                            onDismiss = onDismiss,
                            onClick = onClick,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                if (selectedType == ScrobblesType.RECENTS && user.isSelf) {
                    pendingScrobblesListItems(
                        headerText = pendingScrobblesheader,
                        headerIcon = Icons.HourglassEmpty,
                        items = pendingScrobbles,
                        expanded = pendingScrobblesExpanded,
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
                    fetchAlbumImageIfMissing = selectedType == ScrobblesType.LOVED,
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScrobblesTypeSelectorButton(
    type: ScrobblesType?,
    checked: Boolean,
    text: String,
    imageVector: ImageVector,
    onTypeSelected: (ScrobblesType?) -> Unit,
    isFirst: Boolean = false,
    isLast: Boolean = false,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        enableUserInput = !checked,
    ) {
        OutlinedToggleButton(
            checked = checked,
            onCheckedChange = {
                if (it)
                    onTypeSelected(type)
            },
            shapes = when {
                isFirst -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                isLast -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            },
        ) {
            if (checked) {
                Icon(imageVector, contentDescription = text)
                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                Text(text = text, maxLines = 1)
            } else {
                Icon(
                    imageVector,
                    contentDescription = text
                )
            }
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
    onNavigateToRandom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeJumpMenuShown by remember { mutableStateOf(false) }
    var datePickerShown by rememberSaveable { mutableStateOf(false) }
    val firstDayOfWeek by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.firstDayOfWeek }

    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(
            ButtonGroupDefaults.ConnectedSpaceBetween,
            Alignment.CenterHorizontally
        ),
    ) {
        if (PlatformStuff.isDesktop || PlatformStuff.isTv) {
            ScrobblesTypeSelectorButton(
                type = null,
                checked = false,
                text = stringResource(Res.string.reload),
                imageVector = Icons.Refresh,
                onTypeSelected = {
                    onRefresh()
                },
                isFirst = true
            )
        }

        ScrobblesTypeSelectorButton(
            type = ScrobblesType.RECENTS,
            checked = selectedType == ScrobblesType.RECENTS,
            text = stringResource(Res.string.recents),
            imageVector = Icons.History,
            onTypeSelected = {
                if (it != null)
                    onTypeSelected(it, null)
            },
            isFirst = !(PlatformStuff.isDesktop || PlatformStuff.isTv)
        )
        ScrobblesTypeSelectorButton(
            type = ScrobblesType.LOVED,
            checked = selectedType == ScrobblesType.LOVED,
            text = stringResource(Res.string.loved),
            imageVector = Icons.Favorite,
            onTypeSelected = {
                if (it != null)
                    onTypeSelected(it, null)
            }
        )
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(stringResource(Res.string.time_jump)) } },
            state = rememberTooltipState(),
            enableUserInput = selectedType != ScrobblesType.TIME_JUMP,
        ) {
            OutlinedToggleButton(
                checked = selectedType == ScrobblesType.TIME_JUMP || timeJumpMenuShown,
                onCheckedChange = {
                    if ((it && selectedType != ScrobblesType.TIME_JUMP) ||
                        (!it && selectedType == ScrobblesType.TIME_JUMP)
                    )
                        timeJumpMenuShown = true
                },
                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
            ) {
                val painter = combineImageVectors(
                    getPeriodTypeIcon(TimePeriodType.CUSTOM),
                    Icons.ArrowDropDown
                )

                if (selectedType == ScrobblesType.TIME_JUMP) {
                    Icon(painter, contentDescription = stringResource(Res.string.time_jump))

                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                    Text(
                        if (timeJumpMillis == null)
                            stringResource(Res.string.time_jump)
                        else
                            PanoTimeFormatter.relative(timeJumpMillis, null),
                        maxLines = 1
                    )
                } else {
                    Icon(painter, contentDescription = stringResource(Res.string.time_jump))
                }
            }

            DropdownMenu(
                expanded = timeJumpMenuShown,
                onDismissRequest = { timeJumpMenuShown = false },
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

        ScrobblesTypeSelectorButton(
            type = null,
            checked = false,
            text = stringResource(Res.string.random_text),
            imageVector = Icons.Casino,
            onTypeSelected = {
                onNavigateToRandom()
            },
            isLast = true
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
