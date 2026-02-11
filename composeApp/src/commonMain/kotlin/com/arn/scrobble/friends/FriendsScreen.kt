package com.arn.scrobble.friends

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.icons.Error
import com.arn.scrobble.icons.History
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Keep
import com.arn.scrobble.icons.KeepOff
import com.arn.scrobble.icons.KeyboardArrowDown
import com.arn.scrobble.icons.KeyboardArrowUp
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.icons.Search
import com.arn.scrobble.main.PanoPullToRefresh
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.AutoRefreshEffect
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.DraggableItem
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ListLoadError
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.dragContainer
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.rememberDragDropState
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.following
import pano_scrobbler.composeapp.generated.resources.from
import pano_scrobbler.composeapp.generated.resources.move_down
import pano_scrobbler.composeapp.generated.resources.move_up
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.no_friends
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_noti
import pano_scrobbler.composeapp.generated.resources.pin
import pano_scrobbler.composeapp.generated.resources.profile
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.since_time
import pano_scrobbler.composeapp.generated.resources.sort
import pano_scrobbler.composeapp.generated.resources.track
import pano_scrobbler.composeapp.generated.resources.unpin

@Composable
fun FriendsScreen(
    user: UserCached,
    pullToRefreshState: PullToRefreshState,
    onSetRefreshing: (PanoPullToRefreshStateForTab) -> Unit,
    pullToRefreshTriggered: Flow<Unit>,
    onNavigate: (PanoRoute) -> Unit,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    showPinned: Boolean = LocalLicenseValidState.current && user.isSelf,
    viewModel: FriendsVM = viewModel(key = user.key<FriendsVM>()) { FriendsVM(user, showPinned) },
) {
    val scope = rememberCoroutineScope()
    val friends = viewModel.friends.collectAsLazyPagingItems()
    val totalFriends by viewModel.totalFriends.collectAsStateWithLifecycle()
    val friendsExtraDataMap by viewModel.friendsExtraDataMap.collectAsStateWithLifecycle()
    val friendsExtraDataMapState = remember { mutableStateMapOf<String, FriendExtraData>() }
    val pinnedFriends by viewModel.pinnedFriends.collectAsStateWithLifecycle()
    var pinnedFriendsReordered by remember { mutableStateOf(pinnedFriends) }

    val pinnedUsernamesSet by remember(pinnedFriendsReordered) {
        mutableStateOf(pinnedFriends.map { it.name }.toSet())
    }
    val sortedFriends by viewModel.sortedFriends.collectAsStateWithLifecycle()
    val lastFriendsRefreshTime by viewModel.lastFriendsRefreshTime.collectAsStateWithLifecycle()
    var lastFriendsRecentsRefreshTime by rememberSaveable(lastFriendsRefreshTime) {
        mutableStateOf(lastFriendsRefreshTime)
    }
    val sortable by remember(
        friends.loadState.append,
        friends.itemCount,
        sortedFriends,
        friendsExtraDataMapState.size
    ) {
        derivedStateOf {
            friends.loadState.append is LoadState.NotLoading &&
                    friends.loadState.append.endOfPaginationReached &&
                    friends.itemCount > 1 &&
                    sortedFriends == null &&
                    friendsExtraDataMapState.size >= friends.itemCount
        }
    }

    val listState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(
            listState,
            onDragEnd = {
                pinnedFriendsReordered.let { viewModel.savePins(it) }
            }
        ) { fromIndex, toIndex ->
            pinnedFriendsReordered =
                viewModel.movePin(pinnedFriendsReordered, fromIndex, toIndex)
        }

    val followingText = stringResource(Res.string.following)

    LaunchedEffect(pinnedFriends) {
        if (showPinned) {
            pinnedFriendsReordered = pinnedFriends
        }
    }

    LaunchedEffect(friendsExtraDataMap) {
        friendsExtraDataMapState.putAll(friendsExtraDataMap)
    }

    LaunchedEffect(totalFriends) {
        if (totalFriends > 0)
            onTitleChange("$followingText: " + totalFriends.format())
        else
            onTitleChange(followingText)
    }

    LaunchedEffect(listState, lastFriendsRecentsRefreshTime) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.key } }
            .collectLatest { visibleKeys ->
                visibleKeys
                    .filterIsInstance<String>()
                    .forEach { key -> // key is the username
                        val cachedData = friendsExtraDataMapState[key]
                        if (cachedData == null ||
                            (System.currentTimeMillis() - cachedData.lastUpdated > Stuff.FRIENDS_REFRESH_INTERVAL)
                        ) {
                            Logger.d { "Loading extra data for friend: $key" }
                            viewModel.loadFriendsRecents(key)
                        }
                    }
            }
    }

    LifecycleResumeEffect(friends.loadState.refresh) {
        onSetRefreshing(
            if (friends.loadState.refresh is LoadState.Loading) {
                PanoPullToRefreshStateForTab.Refreshing
            } else {
                PanoPullToRefreshStateForTab.NotRefreshing
            }
        )

        onPauseOrDispose {
            onSetRefreshing(PanoPullToRefreshStateForTab.Disabled)
        }
    }


    LaunchedEffect(Unit) {
        pullToRefreshTriggered.collect {
            if (friends.loadState.refresh is LoadState.NotLoading) {
                viewModel.markExtraDataAsStale()
                viewModel.clearSortedFriends()
                friends.refresh()
            }
        }
    }


    AutoRefreshEffect(
        firstPageLoadedTime = lastFriendsRecentsRefreshTime,
        interval = Stuff.FRIENDS_REFRESH_INTERVAL,
        doRefresh = {
            if (sortedFriends == null && listState.firstVisibleItemIndex < 4) {
                viewModel.markExtraDataAsStale()
                lastFriendsRecentsRefreshTime = System.currentTimeMillis()
                true
            } else {
                false
            }
        },
        lazyPagingItems = friends,
    )

    PanoPullToRefresh(
        state = pullToRefreshState,
        isRefreshing = friends.loadState.refresh is LoadState.Loading,
        modifier = modifier,
    ) {

        EmptyText(
            visible = friends.loadState.refresh is LoadState.NotLoading &&
                    friends.itemCount == 0 && pinnedFriendsReordered.isEmpty(),
            text = stringResource(Res.string.no_friends)
        )

        PanoLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .dragContainer(dragDropState)
        ) {
            if (sortedFriends != null) {
                items(
                    sortedFriends!!,
                    key = { it.name }
                ) { friend ->
                    FriendItem(
                        friend,
                        extraData = friendsExtraDataMapState[friend.name],
                        canPinUnpin = false,
                        onNavigateToScrobbles = {
                            onNavigate(PanoRoute.OthersHomePager(it))
                        },
                        onNavigateToTrackInfo = { track, user ->
                            onNavigate(
                                PanoRoute.Modal.MusicEntryInfo(
                                    track = track,
                                    user = user,
                                )
                            )
                        },
                        modifier = Modifier.animateItem()
                    )
                }
                return@PanoLazyColumn // return early
            }

            itemsIndexed(
                pinnedFriendsReordered,
                key = { idx, friend -> friend.name }
            ) { idx, friend ->

                DraggableItem(dragDropState, idx) { isDragging ->
                    FriendItem(
                        friend,
                        extraData = friendsExtraDataMapState[friend.name],
                        pinIndex = idx,
                        canPinUnpin = user.isSelf,
                        onPinUnpin = { pin ->
                            if (showPinned) {
                                if (pin && friend.name !in pinnedUsernamesSet)
                                    viewModel.addPinAndSave(friend)
                                else if (!pin && friend.name in pinnedUsernamesSet)
                                    viewModel.removePinAndSave(friend)
                            } else {
                                onNavigate(PanoRoute.Billing)
                            }
                        },
                        isLastPin = idx == pinnedFriendsReordered.size - 1,
                        onMove = { f, t ->
                            pinnedFriendsReordered =
                                viewModel.movePin(pinnedFriendsReordered, f, t)
                        },
                        onNavigateToScrobbles = {
                            onNavigate(PanoRoute.OthersHomePager(it))
                        },
                        onNavigateToTrackInfo = { track, user ->
                            onNavigate(
                                PanoRoute.Modal.MusicEntryInfo(
                                    track = track,
                                    user = user,
                                )
                            )
                        },
                        modifier = Modifier
                            .alpha(if (isDragging) 0.5f else 1f)
                            .animateItem()
                    )
                }
            }

            items(
                friends.itemCount,
                key = friends.itemKey { it.name }
            ) { idx ->
                val friend = friends[idx]

                if (friend == null) {
                    FriendItemShimmer(
                        modifier = Modifier.animateItem()
                    )
                } else {
                    FriendItem(
                        friend,
                        extraData = friendsExtraDataMapState[friend.name],
                        canPinUnpin = user.isSelf,
                        onPinUnpin = { pin ->
                            if (showPinned) {
                                if (pin && friend.name !in pinnedUsernamesSet)
                                    viewModel.addPinAndSave(friend)
                                else if (!pin && friend.name in pinnedUsernamesSet)
                                    viewModel.removePinAndSave(friend)
                            } else {
                                onNavigate(PanoRoute.Billing)
                            }
                        },
                        onNavigateToScrobbles = {
                            onNavigate(PanoRoute.OthersHomePager(it))
                        },
                        onNavigateToTrackInfo = { track, user ->
                            onNavigate(
                                PanoRoute.Modal.MusicEntryInfo(
                                    track = track,
                                    user = user,
                                )
                            )
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            friends.apply {
                when {
                    loadState.refresh is LoadState.Loading -> {
                        items(8) { // don't put key here, top items are not scrolled to initially, otherwise
                            FriendItemShimmer(
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    loadState.hasError -> {
                        val error = when {
                            loadState.refresh is LoadState.Error -> loadState.refresh as LoadState.Error
                            loadState.append is LoadState.Error -> loadState.append as LoadState.Error
                            else -> null
                        }

                        if (error != null) {
                            item {
                                ListLoadError(
                                    modifier = Modifier.animateItem(),
                                    throwable = error.error,
                                    onRetry = { retry() })
                            }
                        }
                    }
                }
            }

            if (sortable) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.sortByTime(friends.itemSnapshotList.items)
                                scope.launch { listState.animateScrollToItem(0) }
                            },
                            modifier = Modifier
                                .padding(16.dp)
                        ) {
                            Text(text = stringResource(Res.string.sort))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendItemShimmer(
    modifier: Modifier = Modifier
) {
    val friend = remember {
        UserCached(
            name = " ",
            url = "",
            realname = "",
            country = "",
            registeredTime = 0L,
            largeImage = ""
        )
    }
    FriendItem(
        friend,
        forShimmer = true,
        extraData = null,
        canPinUnpin = false,
        onNavigateToScrobbles = {},
        onNavigateToTrackInfo = { _, _ -> },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FriendItem(
    friend: UserCached,
    extraData: FriendExtraData?,
    canPinUnpin: Boolean,
    onNavigateToScrobbles: (UserCached) -> Unit,
    onNavigateToTrackInfo: (Track, UserCached) -> Unit,
    modifier: Modifier = Modifier,
    forShimmer: Boolean = false,
    pinIndex: Int? = null,
    onPinUnpin: (Boolean) -> Unit = {},
    onMove: (Int, Int) -> Unit = { _, _ -> },
    isLastPin: Boolean = false,
) {
    val playCount = remember(extraData) { extraData?.playCount }
    val track = remember(extraData) { extraData?.track }
    val scope = rememberCoroutineScope()
    var detailsShown by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .width(72.dp)
                .padding(vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(
                    enabled = !forShimmer,
                    onClick = { detailsShown = true })
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
            ) {
                AvatarOrInitials(
                    avatarUrl = friend.largeImage,
                    avatarName = friend.name,
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .matchParentSize()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .then(if (forShimmer) Modifier.shimmerWindowBounds() else Modifier)
                        .then(
                            if (pinIndex != null)
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = CircleShape
                                )
                            else
                                Modifier
                        )
                )

                if (pinIndex != null) {
                    Icon(
                        imageVector = Icons.KeepOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Text(
                text = if (Stuff.isInDemoMode) "user" else friend.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (pinIndex != null)
                    FontWeight.Bold
                else
                    null,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (extraData?.errorMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Error,
                    contentDescription = stringResource(Res.string.network_error),
                )

                Text(
                    text = extraData.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            MusicEntryListItem(
                entry = track ?: getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS),
                onEntryClick = {
                    if (track != null)
                        onNavigateToTrackInfo(track, friend)
                },
                forShimmer = track == null,
                modifier = if (track == null)
                    Modifier
                        .shimmerWindowBounds()
                else
                    Modifier
            )
        }

        if (detailsShown) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = { detailsShown = false },
            ) {
                val textModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)

                Text(
                    text = friend.realname.ifEmpty { friend.name },
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = textModifier,
                )

                if (friend.country.isNotEmpty() && friend.country != "None")
                    Text(
                        text = stringResource(
                            Res.string.from,
                            friend.country + " " + Stuff.getCountryFlag(friend.country)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = textModifier,
                    )

                if (playCount != null) {
                    Text(
                        text = pluralStringResource(
                            Res.plurals.num_scrobbles_noti,
                            playCount,
                            playCount.format()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = textModifier,
                    )
                }

                if (friend.registeredTime > Stuff.TIME_2002)
                    Text(
                        stringResource(
                            Res.string.since_time,
                            PanoTimeFormatter.relative(friend.registeredTime, null)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = textModifier,
                    )

                if (pinIndex != null) {
                    PinControls(
                        isPinned = true,
                        onPinUnpin = onPinUnpin,
                        onMoveUp = if (pinIndex > 0) {
                            {
                                onMove(pinIndex, pinIndex - 1)
                            }
                        } else
                            null,
                        onMoveDown = if (!isLastPin) {
                            {
                                onMove(pinIndex, pinIndex + 1)
                            }
                        } else
                            null,
                    )
                }

                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.History,
                            contentDescription = stringResource(Res.string.profile),
                        )
                    },
                    text = {
                        Text(stringResource(Res.string.scrobbles))
                    },
                    onClick = {
                        detailsShown = false
                        onNavigateToScrobbles(friend)
                    },
                )

                if (pinIndex == null && canPinUnpin) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Keep,
                                contentDescription = stringResource(Res.string.pin),
                            )
                        },
                        text = {
                            Text(stringResource(Res.string.pin))
                        },
                        onClick = {
                            onPinUnpin(true)
                        },
                    )
                }

                if (track is Track) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Search,
                                contentDescription = stringResource(Res.string.search),
                            )
                        },
                        text = {
                            Text(
                                stringResource(Res.string.search) + ": " +
                                        stringResource(Res.string.track)
                            )
                        },
                        onClick = {
                            detailsShown = false

                            scope.launch {
                                PlatformStuff.launchSearchIntent(track, null)
                            }
                        },
                    )
                }

                if (friend.url.isNotEmpty() && !PlatformStuff.isTv) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.OpenInBrowser,
                                contentDescription = stringResource(Res.string.profile),
                            )
                        },
                        text = {
                            Text(
                                stringResource(Res.string.profile)
                            )
                        },
                        onClick = {
                            detailsShown = false

                            PlatformStuff.openInBrowser(friend.url)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PinControls(
    isPinned: Boolean,
    onPinUnpin: (Boolean) -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(
            ButtonGroupDefaults.ConnectedSpaceBetween,
            Alignment.CenterHorizontally
        ),
    ) {
        if (isPinned) {
            OutlinedToggleButton(
                enabled = onMoveUp != null,
                checked = false,
                onCheckedChange = {
                    onMoveUp?.invoke()
                },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
            ) {
                Icon(
                    imageVector =
                        Icons.KeyboardArrowUp,
                    contentDescription = stringResource(Res.string.move_up),
                )
            }
        }

        ToggleButton(
            checked = isPinned,
            onCheckedChange = {
                onPinUnpin(!isPinned)
            },
            shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
        ) {
            Icon(
                imageVector = if (isPinned) Icons.KeepOff else Icons.Keep,
                contentDescription = stringResource(
                    if (isPinned) Res.string.unpin else Res.string.pin
                )
            )
        }

        if (isPinned) {
            OutlinedToggleButton(
                enabled = onMoveDown != null,
                checked = false,
                onCheckedChange = {
                    onMoveDown?.invoke()
                },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
            ) {
                Icon(
                    imageVector = Icons.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.move_down),
                )
            }
        }
    }
}