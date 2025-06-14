package com.arn.scrobble.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.main.PanoPullToRefresh
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.AutoRefreshEffect
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.GridOrListSelector
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.ListLoadError
import com.arn.scrobble.ui.NowPlayingSurface
import com.arn.scrobble.ui.PanoLazyVerticalGrid
import com.arn.scrobble.ui.PanoPullToRefreshStateForTab
import com.arn.scrobble.ui.minGridSize
import com.arn.scrobble.ui.placeholderImageVectorPainter
import com.arn.scrobble.ui.placeholderPainter
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.following
import pano_scrobbler.composeapp.generated.resources.from
import pano_scrobbler.composeapp.generated.resources.move_down
import pano_scrobbler.composeapp.generated.resources.move_left
import pano_scrobbler.composeapp.generated.resources.move_right
import pano_scrobbler.composeapp.generated.resources.move_up
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.no_friends
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_noti
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_since
import pano_scrobbler.composeapp.generated.resources.pin
import pano_scrobbler.composeapp.generated.resources.profile
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.sort
import pano_scrobbler.composeapp.generated.resources.time_just_now
import pano_scrobbler.composeapp.generated.resources.unpin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    user: UserCached,
    pullToRefreshState: PullToRefreshState,
    onSetRefreshing: (PanoPullToRefreshStateForTab) -> Unit,
    pullToRefreshTriggered: Flow<Unit>,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    onTitleChange: (String?) -> Unit,
    dialogFriendExtraDataFlow: MutableSharedFlow<FriendExtraData>,
    modifier: Modifier = Modifier,
    viewModel: FriendsVM = viewModel { FriendsVM() },
) {
    val scope = rememberCoroutineScope()
    val friends = viewModel.friends.collectAsLazyPagingItems()
    val totalFriends by viewModel.totalFriends.collectAsStateWithLifecycle()
    val friendsExtraDataMap by viewModel.friendsExtraDataMap.collectAsStateWithLifecycle()
    val friendsExtraDataMapState = remember { mutableStateMapOf<String, FriendExtraData>() }
    val pinnedFriends = if (PlatformStuff.billingRepository.isLicenseValid) {
        viewModel.pinnedFriends.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val pinnedUsernamesSet = if (PlatformStuff.billingRepository.isLicenseValid) {
        viewModel.pinnedUsernamesSet.collectAsStateWithLifecycle()
    } else {
        remember { mutableStateOf(emptySet()) }
    }
    var isInEditMode by rememberSaveable { mutableStateOf(false) }
    val sortedFriends by viewModel.sortedFriends.collectAsStateWithLifecycle()
    val lastFriendsRefreshTime by viewModel.lastFriendsRefreshTime.collectAsStateWithLifecycle()
    var lastExpandedFriendUsername by remember { mutableStateOf<String?>(null) }
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

    val gridState = rememberLazyGridState()
    val followingText = stringResource(Res.string.following)
    val isColumn by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.gridSingleColumn }

    LaunchedEffect(user) {
        viewModel.setUser(user)
    }

    LaunchedEffect(friendsExtraDataMap) {
        friendsExtraDataMapState.putAll(friendsExtraDataMap)
        if (lastExpandedFriendUsername != null) {
            val newExtraData = friendsExtraDataMapState[lastExpandedFriendUsername]
            if (newExtraData != null) {
                dialogFriendExtraDataFlow.emit(newExtraData)
            }
        }
    }

    LaunchedEffect(totalFriends) {
        if (totalFriends > 0)
            onTitleChange("$followingText: $totalFriends")
        else
            onTitleChange(followingText)
    }

    LaunchedEffect(gridState, lastFriendsRefreshTime) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.key } }
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

        lastExpandedFriendUsername = null

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
        lastRefreshTime = lastFriendsRefreshTime,
        interval = Stuff.FRIENDS_REFRESH_INTERVAL,
        shouldRefresh = {
            sortedFriends == null &&
                    gridState.firstVisibleItemIndex < 4
        },
        lazyPagingItems = friends,
    )

    fun expandFriend(friend: UserCached) {
        val dialogArgs = PanoDialog.Friend(
            friend = friend,
            isPinned = friend.name in pinnedUsernamesSet.value,
            extraData = friendsExtraDataMapState[friend.name],
        )
        lastExpandedFriendUsername = friend.name

        onOpenDialog(dialogArgs)
    }

    PanoPullToRefresh(
        state = pullToRefreshState,
        isRefreshing = friends.loadState.refresh is LoadState.Loading,
        modifier = modifier,
    ) {

        EmptyText(
            visible = friends.loadState.refresh is LoadState.NotLoading &&
                    friends.itemCount == 0,
            text = stringResource(Res.string.no_friends)
        )

        PanoLazyVerticalGrid(
            columns = if (isColumn)
                GridCells.Fixed(1)
            else
                GridCells.Adaptive(minSize = minGridSize()),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
        ) {

            item(
                key = "header row",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                ButtonsBarForFriends(
                    isColumn = isColumn,
                    isInEditMode = isInEditMode,
                    onEditClick = if (user.isSelf && sortedFriends == null) {
                        { isInEditMode = it }
                    } else
                        null,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            if (sortedFriends != null) {
                items(
                    sortedFriends!!,
                    key = { it.name }
                ) { friend ->
                    FriendItem(
                        friend,
                        extraData = friendsExtraDataMapState[friend.name],
                        isPinned = false,
                        showPinConfig = false,
                        onExpand = { expandFriend(friend) },
                        isColumn = isColumn,
                        modifier = Modifier.animateItem()
                    )
                }
                return@PanoLazyVerticalGrid // return early
            }

            if (user.isSelf && PlatformStuff.billingRepository.isLicenseValid) {
                itemsIndexed(
                    pinnedFriends.value,
                    key = { idx, friend -> friend.name }
                ) { idx, friend ->
                    FriendItem(
                        friend,
                        extraData = friendsExtraDataMapState[friend.name],
                        isPinned = true,
                        onExpand = { expandFriend(friend) },
                        isColumn = isColumn,
                        showPinConfig = user.isSelf && isInEditMode,
                        onPinUnpin = { pin ->
                            if (PlatformStuff.billingRepository.isLicenseValid) {
                                if (pin)
                                    viewModel.addPinAndSave(friend)
                                else
                                    viewModel.removePinAndSave(friend)
                            } else {
                                onNavigate(PanoRoute.Billing)
                            }
                        },
                        onMoveLeft = if (idx == 0) null else {
                            {
                                viewModel.movePinAndSave(friend.name, false)
                            }
                        },
                        onMoveRight = if (idx == pinnedFriends.value.lastIndex) null else {
                            {
                                viewModel.movePinAndSave(friend.name, true)
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            for (i in 0 until friends.itemCount) {
                val friendPeek = friends.peek(i)
                if (friendPeek == null || friendPeek.name !in pinnedUsernamesSet.value) {
                    item(
                        key = friendPeek?.name ?: i
                    ) {
                        val friend = friends[i]

                        if (friend == null) {
                            FriendItemShimmer(
                                isColumn = isColumn,
                                modifier = Modifier.animateItem()
                            )
                        } else {
                            FriendItem(
                                friend,
                                extraData = friendsExtraDataMapState[friend.name],
                                isPinned = false,
                                showPinConfig = user.isSelf && isInEditMode,
                                onPinUnpin = { pin ->
                                    if (PlatformStuff.billingRepository.isLicenseValid) {
                                        if (pin)
                                            viewModel.addPinAndSave(friend)
                                        else
                                            viewModel.removePinAndSave(friend)
                                    } else {
                                        onNavigate(PanoRoute.Billing)
                                    }
                                },
                                onExpand = { expandFriend(friend) },
                                isColumn = isColumn,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }

            friends.apply {
                when {
                    loadState.refresh is LoadState.Loading -> {
                        items(8) { // don't put key here, top items are not scrolled to initially, otherwise
                            FriendItemShimmer(
                                isColumn = isColumn,
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
                            item(span = { GridItemSpan(maxLineSpan) }) {
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box {
                        OutlinedButton(
                            onClick = {
                                isInEditMode = false
                                viewModel.sortByTime(friends.itemSnapshotList.items)
                                scope.launch { gridState.animateScrollToItem(0) }
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
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
fun FriendDialog(
    friend: UserCached,
    isPinned: Boolean,
    extraData: FriendExtraData?,
    extraDataFlow: Flow<FriendExtraData>,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extraData by extraDataFlow.collectAsStateWithLifecycle(initialValue = extraData)

    FriendItem(
        friend = friend,
        onNavigateToScrobbles = {
            onNavigate(PanoRoute.OthersHomePager(it))
        },
        onNavigateToTrackInfo = { track, user ->
            onOpenDialog(
                PanoDialog.MusicEntryInfo(
                    track = track,
                    user = user,
                    pkgName = null
                )
            )
        },
        extraData = extraData,
        isPinned = isPinned,
        showPinConfig = false,
        expanded = true,
        isColumn = false,
        onExpand = { },
        modifier = modifier
    )
}

@Composable
private fun FriendItemShimmer(
    isColumn: Boolean,
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
        isPinned = false,
        showPinConfig = false,
        isColumn = isColumn,
        modifier = modifier
    )
}

@Composable
private fun FriendItemRowOrColumn(
    isColumn: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    if (!isColumn) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier
        ) {
            content()
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = modifier
                .padding(vertical = 16.dp)
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FriendItem(
    friend: UserCached,
    extraData: FriendExtraData?,
    isPinned: Boolean,
    showPinConfig: Boolean,
    isColumn: Boolean,
    forShimmer: Boolean = false,
    expanded: Boolean = false,
    onExpand: () -> Unit = {},
    onPinUnpin: (Boolean) -> Unit = {},
    onMoveLeft: (() -> Boolean)? = null,
    onMoveRight: (() -> Boolean)? = null,
    onNavigateToScrobbles: (UserCached) -> Unit = {},
    onNavigateToTrackInfo: (Track, UserCached) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val playCount = remember(extraData) { extraData?.playCount }
    val track = remember(extraData) { extraData?.track }
    val scope = rememberCoroutineScope()

    FriendItemRowOrColumn(
        isColumn = isColumn,
        modifier = modifier
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !expanded && !showPinConfig && !forShimmer, onClick = onExpand)
    ) {
        Column(
            modifier = if (isColumn)
                Modifier
                    .width(110.dp)
            else
                Modifier
        ) {
            Box(
                modifier = Modifier
                    .padding(6.dp).align(Alignment.CenterHorizontally)
            ) {
                AvatarOrInitials(
                    avatarUrl = friend.largeImage,
                    avatarName = friend.name,
                    textStyle = if (expanded) MaterialTheme.typography.displayLarge else MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .size(
                            if (expanded) 200.dp else 64.dp
                        )
                        .clip(
                            if (expanded)
                                MaterialTheme.shapes.large
                            else
                                CircleShape
                        )
                        .then(if (forShimmer) Modifier.shimmerWindowBounds() else Modifier)
                        .then(
                            if (isPinned)
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = if (expanded)
                                        MaterialTheme.shapes.large
                                    else
                                        CircleShape
                                )
                            else
                                Modifier
                        )
                )

                if (isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Text(
                text = if (Stuff.isInDemoMode) "user" else friend.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (expanded) {
                if (friend.realname.isNotEmpty())
                    Text(
                        text = friend.realname,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                if (friend.country.isNotEmpty() && friend.country != "None")
                    Text(
                        text = stringResource(
                            Res.string.from,
                            friend.country + " " + Stuff.getCountryFlag(friend.country)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                if (playCount != null) {
                    Text(
                        text = if (friend.registeredTime > Stuff.TIME_2002)
                            stringResource(
                                Res.string.num_scrobbles_since,
                                playCount,
                                PanoTimeFormatter.relative(friend.registeredTime)
                            )
                        else
                            pluralStringResource(
                                Res.plurals.num_scrobbles_noti,
                                playCount,
                                playCount.format()
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    OutlinedButton(onClick = { onNavigateToScrobbles(friend) }) {
                        Text(text = stringResource(Res.string.scrobbles))
                    }

                    if (!PlatformStuff.isTv) {
                        IconButtonWithTooltip(
                            onClick = {
                                PlatformStuff.openInBrowser(friend.url)
                            },
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(Res.string.profile)
                        )
                    }
                }
            }
        }

        if (!showPinConfig) {
            NowPlayingSurface(
                nowPlaying = track?.isNowPlaying == true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .then(
                            if (forShimmer || extraData == null)
                                Modifier.shimmerWindowBounds()
                            else
                                Modifier
                        )
                        .then(
                            if (track?.isNowPlaying != true)
                                Modifier.background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.shapes.large
                                )
                            else
                                Modifier
                        )
                        .padding(vertical = 8.dp)
                ) {
                    val imageModifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(
                            if (expanded)
                                64.dp
                            else if (isColumn)
                                64.dp
                            else
                                42.dp
                        )
                        .clip(MaterialTheme.shapes.small)

                    if (extraData?.errorMessage != null) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = stringResource(Res.string.network_error),
                            modifier = imageModifier
                        )
                    } else {
                        AsyncImage(
                            model = remember(track) {
                                track?.let {
                                    MusicEntryImageReq(
                                        musicEntry = it as MusicEntry,
                                        fetchAlbumInfoIfMissing = false,
                                    )
                                }
                            },
                            fallback = placeholderImageVectorPainter(null),
                            error = placeholderImageVectorPainter(track),
                            placeholder = placeholderPainter(),
                            contentDescription = stringResource(Res.string.album_art),
                            modifier = imageModifier
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .then(
                                if (expanded && track != null)
                                    Modifier.clickable {
                                        onNavigateToTrackInfo(track, friend)
                                    }
                                else
                                    Modifier
                            )
                    ) {
                        Text(
                            text = track?.name
                                ?: extraData?.errorMessage
                                ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track?.artist?.name ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Box(
                            modifier = if (!isColumn)
                                Modifier
                                    .align(Alignment.End)
                                    .padding(end = 4.dp)
                            else
                                Modifier
                                    .align(Alignment.Start)
                                    .padding(start = 4.dp)
                        ) {
                            Text(
                                text = track?.date?.let { PanoTimeFormatter.relative(it) } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )

                            if (track?.isNowPlaying == true) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(Res.string.time_just_now),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }

                    if (expanded && track != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    PlatformStuff.launchSearchIntent(track, null)
                                }
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(Res.string.search),
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                ButtonGroup(
                    overflowIndicator = {}
                ) {
                    if (isPinned) {
                        customItem(
                            buttonGroupContent = {
                                OutlinedButton(
                                    enabled = onMoveLeft != null,
                                    onClick = {
                                        onMoveLeft?.invoke()
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (!isColumn)
                                            Icons.AutoMirrored.Outlined.KeyboardArrowLeft
                                        else
                                            Icons.Outlined.KeyboardArrowUp,
                                        contentDescription = stringResource(
                                            if (!isColumn)
                                                Res.string.move_left
                                            else
                                                Res.string.move_up
                                        ),
                                    )
                                }
                            }, menuContent = {})
                    }

                    customItem(
                        buttonGroupContent = {
                            ToggleButton(
                                checked = isPinned,
                                onCheckedChange = {
                                    onPinUnpin(!isPinned)
                                },
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = stringResource(
                                        if (isPinned) Res.string.unpin else Res.string.pin
                                    )
                                )
                            }
                        }, menuContent = {})

                    if (isPinned) {
                        customItem(
                            buttonGroupContent = {
                                OutlinedButton(
                                    enabled = onMoveRight != null,
                                    onClick = {
                                        onMoveRight?.invoke()
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (!isColumn)
                                            Icons.AutoMirrored.Outlined.KeyboardArrowRight
                                        else
                                            Icons.Outlined.KeyboardArrowDown,
                                        contentDescription = stringResource(
                                            if (!isColumn)
                                                Res.string.move_right
                                            else
                                                Res.string.move_down
                                        ),
                                    )
                                }
                            }, menuContent = {})
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ButtonsBarForFriends(
    isColumn: Boolean,
    isInEditMode: Boolean,
    onEditClick: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {

        Spacer(
            modifier = Modifier.size(IconButtonDefaults.smallContainerSize())
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        GridOrListSelector(
            isColumn = isColumn,
            onIsColumnChange = { isColumn ->
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(gridSingleColumn = isColumn) }
                }
            },
        )

        Spacer(
            modifier = Modifier.weight(1f)
        )

        if (onEditClick != null)
            IconButtonWithTooltip(
                onClick = {
                    onEditClick(!isInEditMode)
                },
                icon = if (isInEditMode) Icons.Outlined.Done else Icons.Outlined.PushPin,
                contentDescription = if (isInEditMode)
                    stringResource(Res.string.done)
                else
                    stringResource(Res.string.pin),
            )
    }
}