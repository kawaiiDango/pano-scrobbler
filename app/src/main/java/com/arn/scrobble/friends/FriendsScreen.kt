package com.arn.scrobble.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import co.touchlab.kermit.Logger
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.main.PanoPullToRefresh
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ListLoadError
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.toast
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    onTitleChange: (String?) -> Unit,
    viewModel: FriendsVM = viewModel(),
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val friends = viewModel.friends.collectAsLazyPagingItems()
    val friendsExtraDataMap by viewModel.friendsExtraDataMap.collectAsStateWithLifecycle()
    val friendsExtraDataMapState = remember { mutableStateMapOf<String, Result<FriendExtraData>>() }
    val pinnedFriends by viewModel.pinnedFriends.collectAsStateWithLifecycle()
    val pinnedUsernamesSet by viewModel.pinnedUsernamesSet.collectAsStateWithLifecycle()
    var expandedFriend by remember { mutableStateOf<UserCached?>(null) }
    val sortedFriends by viewModel.sortedFriends.collectAsStateWithLifecycle()
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
    val followingText = stringResource(R.string.following)

    LaunchedEffect(Unit) {
        viewModel.setUser(user)
    }

    LaunchedEffect(friendsExtraDataMap) {
        friendsExtraDataMapState.putAll(friendsExtraDataMap)
    }

    LaunchedEffect(Unit) {
        viewModel.totalFriends.collectLatest {
            if (it > 0)
                onTitleChange("$followingText: $it")
            else
                onTitleChange(followingText)
        }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.key } }
            .collectLatest { visibleKeys ->
                visibleKeys
                    .filterIsInstance<String>()
                    .forEach { key -> // key is the username
                        val cachedData = friendsExtraDataMapState[key]
                        if (cachedData == null ||
                            (System.currentTimeMillis() - (cachedData.getOrNull()?.lastUpdated
                                ?: 0) > Stuff.RECENTS_REFRESH_INTERVAL)
                        ) {
                            viewModel.loadFriendsRecents(key)
                        }
                    }
            }
    }


    PanoPullToRefresh(
        isRefreshing = friends.loadState.refresh is LoadState.Loading,
        onRefresh = {
            viewModel.markExtraDataAsStale()
            viewModel.clearSortedFriends()
            friends.refresh()
        },
        modifier = modifier,
    ) {

        EmptyText(
            visible = friends.loadState.refresh is LoadState.NotLoading &&
                    friends.itemCount == 0,
            text = stringResource(R.string.no_friends)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = Stuff.GRID_MIN_SIZE.dp),
            contentPadding = panoContentPadding(),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (sortedFriends != null) {
                items(
                    sortedFriends!!,
                    key = { it.name }
                ) { friend ->
                    FriendItem(
                        friend,
                        onNavigateToScrobbles = { },
                        onNavigateToTrackInfo = { _, _ -> },
                        extraDataResult = friendsExtraDataMapState[friend.name],
                        isPinned = false,
                        showPinConfig = false,
                        onExpand = { expandedFriend = friend },
                        onPinUnpin = {},
                        onMove = { },
                        modifier = Modifier.animateItem()
                    )
                }
                return@LazyVerticalGrid // return early
            }

            if (user.isSelf) {
                items(
                    pinnedFriends,
                    key = { it.name }
                ) { friend ->
                    FriendItem(
                        friend,
                        onNavigateToScrobbles = { },
                        onNavigateToTrackInfo = { _, _ -> },
                        extraDataResult = friendsExtraDataMapState[friend.name],
                        isPinned = true,
                        showPinConfig = false,
                        onExpand = { expandedFriend = friend },
                        onPinUnpin = {},
                        onMove = { },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            for (i in 0 until friends.itemCount) {
                val friendPeek = friends.peek(i)
                if (friendPeek == null || friendPeek.name !in pinnedUsernamesSet) {
                    item(
                        key = friendPeek?.name ?: i
                    ) {
                        val friend = friends[i]

                        if (friend == null) {
                            FriendItemShimmer(
                                modifier = Modifier.animateItem()
                            )
                        } else {
                            FriendItem(
                                friend,
                                onNavigateToScrobbles = { },
                                onNavigateToTrackInfo = { _, _ -> },
                                extraDataResult = friendsExtraDataMapState[friend.name],
                                isPinned = false,
                                showPinConfig = false,
                                onExpand = { expandedFriend = friend },
                                onPinUnpin = {},
                                onMove = { },
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
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    loadState.refresh is LoadState.Error ||
                            loadState.append is LoadState.Error
                        -> {
                        val error = loadState.refresh as LoadState.Error
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ListLoadError(
                                modifier = Modifier.animateItem(),
                                throwable = error.error,
                                onRetry = { retry() })
                        }
                    }
                }
            }

            if (sortable) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box {
                        OutlinedButton(
                            onClick = {
                                viewModel.sortByTime(friends.itemSnapshotList.items)
                                scope.launch { gridState.animateScrollToItem(0) }
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        ) {
                            Text(text = stringResource(R.string.sort))
                        }
                    }
                }
            }
        }
    }

    expandedFriend?.let { friend ->
        ModalBottomSheet(
            onDismissRequest = { expandedFriend = null },
            dragHandle = if (!Stuff.isTv) {
                { BottomSheetDefaults.DragHandle() }
            } else null,
            sheetGesturesEnabled = !Stuff.isTv,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            FriendItem(
                friend = friend,
                onNavigateToScrobbles = {
                    expandedFriend = null
                    onNavigate(PanoRoute.HomePager(it))
                },
                onNavigateToTrackInfo = { track, user ->
                    expandedFriend = null
                    onNavigate(
                        PanoRoute.MusicEntryInfo(
                            track = track,
                            user = user,
                            pkgName = null
                        )
                    )
                },
                extraDataResult = friendsExtraDataMapState[friend.name],
                isPinned = friend.name in pinnedUsernamesSet,
                showPinConfig = user.isSelf,
                expanded = true,
                onExpand = { },
                onPinUnpin = { pin ->

                    if (Stuff.billingRepository.isLicenseValid) {
                        if (pin)
                            viewModel.addPinAndSave(friend)
                        else
                            viewModel.removePinAndSave(friend)
                    } else {
                        onNavigate(PanoRoute.Billing)
                    }
                },
                onMove = {
                    expandedFriend?.let { friend ->
                        val moved = viewModel.movePinAndSave(friend.name, it)

                        if (!moved) {
                            PlatformStuff.application.toast(R.string.cannot_move)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun FriendItemShimmer(modifier: Modifier = Modifier) {
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
        onNavigateToScrobbles = {},
        onNavigateToTrackInfo = { _, _ -> },
        forShimmer = true,
        extraDataResult = null,
        onExpand = {},
        onPinUnpin = {},
        onMove = { },
        isPinned = false,
        showPinConfig = false,
        modifier = modifier
    )
}

@Composable
private fun FriendItem(
    friend: UserCached,
    extraDataResult: Result<FriendExtraData>?,
    isPinned: Boolean,
    showPinConfig: Boolean,
    forShimmer: Boolean = false,
    expanded: Boolean = false,
    onExpand: () -> Unit,
    onPinUnpin: (Boolean) -> Unit,
    onMove: (right: Boolean) -> Unit,
    onNavigateToScrobbles: (UserCached) -> Unit,
    onNavigateToTrackInfo: (Track, UserCached) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playCount = remember(extraDataResult) { extraDataResult?.getOrNull()?.playCount }

    val track = remember(extraDataResult) { extraDataResult?.getOrNull()?.track }
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .padding(4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !expanded && !forShimmer, onClick = onExpand)
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            AvatarOrInitials(
                avatarUrl = friend.largeImage,
                avatarInitialLetter = friend.name.first(),
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
                    .then(if (forShimmer) Modifier.shimmer() else Modifier)
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
                        R.string.from,
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
                            R.string.num_scrobbles_since,
                            playCount,
                            Stuff.myRelativeTime(millis = friend.registeredTime)
                        )
                    else
                        pluralStringResource(R.plurals.num_scrobbles_noti, playCount),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {

                Row {
                    if (showPinConfig) {

                        if (isPinned) {
                            IconButton(
                                onClick = {
                                    onMove(false)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                                    contentDescription = stringResource(R.string.move_left),
                                    tint = LocalContentColor.current
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onPinUnpin(!isPinned)
                            },
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = stringResource(
                                    if (isPinned) R.string.unpin else R.string.pin
                                ),
                                tint = if (isPinned)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    LocalContentColor.current
                            )
                        }

                        if (isPinned) {
                            IconButton(
                                onClick = {
                                    onMove(true)
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.move_right),
                                    tint = LocalContentColor.current
                                )
                            }
                        }
                    }
                }

                OutlinedButton(onClick = { onNavigateToScrobbles(friend) }) {
                    Text(text = stringResource(R.string.scrobbles))
                }

                if (!Stuff.isTv) {
                    IconButton(onClick = {
                        Stuff.openInBrowser(friend.url)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.profile)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .then(
                    if (forShimmer || extraDataResult == null)
                        Modifier.shimmer()
                    else
                        Modifier
                )
                .background(
                    if (track?.isNowPlaying == true)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh
                )
                .then(
                    if (expanded && track != null)
                        Modifier.clickable {
//                            Stuff.launchSearchIntent(track, null)
                            onNavigateToTrackInfo(track, friend)
                        }
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
                    else
                        42.dp
                )
                .clip(MaterialTheme.shapes.small)

            if (extraDataResult?.isFailure == true) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = stringResource(R.string.network_error),
                    modifier = imageModifier
                )
            } else {
                AsyncImage(
                    model = remember(track) {
                        ImageRequest.Builder(context)
                            .data(
                                track?.let {
                                    MusicEntryImageReq(
                                        musicEntry = it as MusicEntry,
                                        fetchAlbumInfoIfMissing = false,
                                    )
                                }
                            )
                            .placeholder(R.drawable.avd_loading)
                            .error(R.drawable.vd_wave_simple_filled)
                            .build()
                    },
                    contentDescription = stringResource(R.string.album_art),
                    modifier = imageModifier
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                val textColor = if (track?.isNowPlaying == true)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    Color.Unspecified

                Text(
                    text = track?.name ?: extraDataResult?.exceptionOrNull()?.localizedMessage
                    ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.artist?.name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = track?.date?.let { Stuff.myRelativeTime(millis = it) } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )

                    if (track?.isNowPlaying == true) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.time_just_now),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }
    }
}