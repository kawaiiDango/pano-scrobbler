package com.arn.scrobble.recents

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.arn.scrobble.api.ScrobbleEvent
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.ContentCopy
import com.arn.scrobble.icons.Delete
import com.arn.scrobble.icons.Dns
import com.arn.scrobble.icons.Edit
import com.arn.scrobble.icons.Error
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.HeartBroken
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.icons.Schedule
import com.arn.scrobble.icons.Search
import com.arn.scrobble.icons.Share
import com.arn.scrobble.icons.automirrored.ArrowBack
import com.arn.scrobble.icons.automirrored.KeyboardArrowRight
import com.arn.scrobble.icons.filled.Favorite
import com.arn.scrobble.media.getNowPlayingFromMainProcess
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.ExpandableHeaderItem
import com.arn.scrobble.ui.ListLoadError
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.copy
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.hate
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.more
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.scrobble_services
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.share
import pano_scrobbler.composeapp.generated.resources.share_sig
import pano_scrobbler.composeapp.generated.resources.time_just_now
import pano_scrobbler.composeapp.generated.resources.track
import pano_scrobbler.composeapp.generated.resources.unlove

private enum class TrackMenuLevel {
    Root,
    More,
    Block
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrackDropdownMenu(
    track: Track,
    appId: String?,
    onNavigate: (PanoRoute) -> Unit,
    user: UserCached,
    editDialogArgs: (() -> PanoRoute.Modal.EditScrobble?)?,
    onLove: ((Boolean) -> Unit)?,
    onHate: ((Boolean) -> Unit)?,
    onDelete: (() -> Unit)?,
    onShare: ((Track, String?) -> Unit)?,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var menuLevel by remember(expanded) {
        mutableStateOf(
            if (editDialogArgs == null && onLove == null && onHate == null && onDelete == null)
                TrackMenuLevel.More
            else
                TrackMenuLevel.Root
        )
    }

    val moreFocusRequester = remember { FocusRequester() }
    val blockFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val isLicenseValid = LocalLicenseValidState.current

    LaunchedEffect(menuLevel) {
        when (menuLevel) {
            TrackMenuLevel.More -> {
                moreFocusRequester.requestFocus()
            }

            TrackMenuLevel.Block -> {
                blockFocusRequester.requestFocus()
            }

            TrackMenuLevel.Root -> {
                // nothing
            }
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        @Composable
        fun copyItem() {
            if (!PlatformStuff.isTv) {
                DropdownMenuItem(
                    onClick = {
                        PlatformStuff.copyToClipboard(track.artist.name + " - " + track.name)
                        onDismissRequest()
                    },
                    text = {
                        Text(stringResource(Res.string.copy))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.ContentCopy,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        @Composable
        fun searchItem() {
            val searchInSource by
            PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.searchInSource && isLicenseValid }

            DropdownMenuItem(
                onClick = {
                    scope.launch {
                        PlatformStuff.launchSearchIntent(track, appId.takeIf { searchInSource })
                    }
                    onDismissRequest()
                },
                text = {
                    Text(stringResource(Res.string.search))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Search,
                        contentDescription = null
                    )
                }
            )
        }

        if (user.isSelf) {
            when (menuLevel) {
                TrackMenuLevel.Root -> {

                    if (onLove != null) {
                        DropdownMenuItem(
                            onClick = {
                                val loved = track.userloved
                                onLove(loved != true)
                                onDismissRequest()
                            },
                            text = {
                                Text(
                                    stringResource(
                                        if (track.userloved != true)
                                            Res.string.love
                                        else
                                            Res.string.unlove
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (track.userloved != true)
                                        Icons.Favorite
                                    else
                                        Icons.Filled.Favorite,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    editDialogArgs?.invoke()?.let { dialogArgs ->
                        DropdownMenuItem(
                            onClick = {
                                onDismissRequest()
                                onNavigate(dialogArgs)
                            },
                            text = {
                                Text(stringResource(Res.string.edit))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Edit,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    if (onDelete != null && track.date != null && track.date > 0 && !track.isNowPlaying) {
                        DropdownMenuItem(
                            onClick = {
                                onDelete()
                                onDismissRequest()
                            },
                            text = {
                                Text(
                                    stringResource(Res.string.delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }

                    DropdownMenuItem(
                        onClick = {
                            menuLevel = TrackMenuLevel.More
                        },
                        text = {
                            Text(stringResource(Res.string.more))
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.KeyboardArrowRight,
                                contentDescription = null
                            )
                        }
                    )
                }

                TrackMenuLevel.More -> {
                    if (!PlatformStuff.isTv) {
                        DropdownMenuItem(
                            onClick = {
                                menuLevel = TrackMenuLevel.Root
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.ArrowBack,
                                    contentDescription = null
                                )
                            },
                            text = {
                                Text(
                                    stringResource(Res.string.more),
                                    style = MaterialTheme.typography.titleMediumEmphasized
                                )
                            },
                        )
                    }

                    DropdownMenuItem(
                        onClick = {
                            menuLevel = TrackMenuLevel.Block
                        },
                        text = {
                            Text(stringResource(Res.string.block))
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.KeyboardArrowRight,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.focusRequester(moreFocusRequester)
                    )

                    if (onHate != null) {

                        DropdownMenuItem(
                            onClick = {
                                val newHated = track.userHated != true
                                onHate(newHated)
                                onDismissRequest()
                            },
                            text = {
                                Text(stringResource(Res.string.hate))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.HeartBroken,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    searchItem()
                    copyItem()

                    if (onShare != null) {
                        val shareSig =
                            stringResource(Res.string.share_sig).takeIf { !isLicenseValid }

                        DropdownMenuItem(
                            onClick = {
                                onShare(track, shareSig)

                                onDismissRequest()
                            },
                            text = {
                                Text(stringResource(Res.string.share))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Share,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }

                TrackMenuLevel.Block -> {
                    if (!PlatformStuff.isTv) {
                        DropdownMenuItem(
                            onClick = {
                                menuLevel = TrackMenuLevel.More
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.ArrowBack,
                                    contentDescription = null
                                )
                            },
                            text = {
                                Text(
                                    stringResource(
                                        Res.string.block
                                    ),
                                    style = MaterialTheme.typography.titleMediumEmphasized
                                )
                            },
                        )
                    }

                    DropdownMenuItem(
                        onClick = {
                            val b = BlockedMetadata(
                                artist = track.artist.name,
                                album = track.album?.name ?: "",
                                track = track.name,
                            )
                            onNavigate(PanoRoute.Modal.BlockedMetadataAdd(b))
                            onDismissRequest()
                        },
                        text = {
                            Text(stringResource(Res.string.track))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.MusicNote,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.focusRequester(blockFocusRequester)
                    )

                    DropdownMenuItem(
                        onClick = {
                            val b = BlockedMetadata(
                                artist = track.artist.name,
                                album = track.album?.name ?: "",
                            )
                            onNavigate(PanoRoute.Modal.BlockedMetadataAdd(b))
                            onDismissRequest()
                        },
                        text = {
                            Text(stringResource(Res.string.album))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Album,
                                contentDescription = null
                            )
                        }
                    )

                    DropdownMenuItem(
                        onClick = {
                            val b = BlockedMetadata(
                                artist = track.artist.name,
                            )
                            onNavigate(PanoRoute.Modal.BlockedMetadataAdd(b))
                        },
                        text = {
                            Text(stringResource(Res.string.artist))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Mic,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        } else {
            searchItem()
            copyItem()
        }
    }
}

fun LazyListScope.scrobblesPlaceholdersAndErrors(
    tracks: LazyPagingItems<TrackWrapper>,
) {
    when {
        tracks.loadState.refresh is LoadState.Loading -> {
            items(10) {
                MusicEntryListItem(
                    getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS),
                    forShimmer = true,
                    onEntryClick = {},
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .shimmerWindowBounds()
                )
            }
        }

        tracks.loadState.hasError -> {
            val error = when {
                tracks.loadState.refresh is LoadState.Error -> tracks.loadState.refresh as LoadState.Error
                tracks.loadState.append is LoadState.Error -> tracks.loadState.append as LoadState.Error
                else -> null
            }

            if (error != null) {
                item {
                    ListLoadError(
                        modifier = Modifier.animateItem(),
                        throwable = error.error,
                        onRetry = { tracks.retry() })
                }
            }
        }
    }
}

@Composable
fun PendingDropdownMenu(
    pendingScrobble: PendingScrobble,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onLove: ((Boolean) -> Unit),
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        PendingScrobbleDesc(pendingScrobble)

        if (pendingScrobble.event == ScrobbleEvent.scrobble) {
            DropdownMenuItem(
                onClick = {
                    onLove(true)
                    onDismissRequest()
                },
                text = {
                    Text(
                        stringResource(Res.string.love)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Favorite,
                        contentDescription = null
                    )
                }
            )
        }

        DropdownMenuItem(
            onClick = {
                onDelete()
                onDismissRequest()
            },
            text = {
                Text(
                    stringResource(Res.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Composable
private fun PendingScrobbleDesc(
    pendingScrobble: PendingScrobble,
) {
    DropdownMenuItem(
        onClick = {},
        enabled = false,
        text = {
            Text(
                stringResource(Res.string.scrobble_services) + ":\n" +
                        pendingScrobble.services.map {
                            accountTypeLabel(it)
                        }.joinToString(", ")
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Dns,
                contentDescription = null
            )
        }
    )

    if (pendingScrobble.lastFailedTimestamp != null) {
        DropdownMenuItem(
            onClick = {},
            enabled = false,
            text = {
                Text(
                    (pendingScrobble.lastFailedReason ?: stringResource(Res.string.network_error))
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Error,
                    contentDescription = null
                )
            }
        )

        DropdownMenuItem(
            onClick = {},
            enabled = false,
            text = {
                Text(
                    PanoTimeFormatter.relative(
                        pendingScrobble.lastFailedTimestamp,
                        justNowString = stringResource(Res.string.time_just_now),
                        withPreposition = true,
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Schedule,
                    contentDescription = null
                )
            }
        )
    }
}

fun LazyListScope.scrobblesListItems(
    tracks: LazyPagingItems<TrackWrapper>,
    user: UserCached,
    pkgMap: Map<Long, String>,
    fetchAlbumImageIfMissing: Boolean,
    showScrobbleSources: Boolean,
    canLove: Boolean,
    canHate: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    expandedKey: () -> String?,
    onExpand: (String?) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    animateListItemContentSize: State<Boolean>,
    maxHeight: State<Dp>,
    viewModel: ScrobblesVM,
) {
    fun onTrackClick(track: Track, appId: String?) {
        onNavigate(PanoRoute.Modal.MusicEntryInfo(user = user, track = track, appId = appId))
    }

    items(
        tracks.itemCount,
        key = tracks.itemKey { it.key }
    ) { idx ->
        val item = tracks[idx]

        if (item is TrackWrapper.SeparatorItem) {
            HorizontalDivider(
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp)

            )
        } else if (item is TrackWrapper.TrackItem || item == null) {
            val track = item?.track
                ?: getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS) as Track
            val isPlaceholder = item == null
            val isExpanded = !isPlaceholder && expandedKey() == item.key

            var menuVisible by remember { mutableStateOf(false) }

            val appItem = remember(track) {
                if (showScrobbleSources) {
                    track.date
                        ?.let { track.appId ?: pkgMap[it] }
                        ?.let { AppItem(it, PlatformStuff.loadApplicationLabel(it)) }
                } else
                    null
            }

            MusicEntryListItem(
                entry = track,
                appItem = appItem,
                onEntryClick = { onTrackClick(track, appItem?.appId) },
                isColumn = isExpanded,
                fixedImageHeight = !isExpanded,
                onImageClick = if (!isPlaceholder) {
                    {
                        if (isExpanded)
                            onExpand(null)
                        else {
                            onExpand(item.key)
                        }
                    }
                } else null,
                forShimmer = item == null,
                fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
                menuContent = {
                    val key = item?.key ?: return@MusicEntryListItem

                    TrackDropdownMenu(
                        track = track,
                        appId = appItem?.appId,
                        onNavigate = onNavigate,
                        user = user,
                        editDialogArgs = if (canEdit) {
                            {
                                val sd: ScrobbleData
                                val hash: Int?

                                if (!track.isNowPlaying && track.date != null) {
                                    // no editing of now playing from here
                                    sd = ScrobbleData(
                                        track = track.name,
                                        artist = track.artist.name,
                                        album = track.album?.name,
                                        timestamp = track.date,
                                        albumArtist = null,
                                        duration = null,
                                        appId = null
                                    )
                                    hash = null
                                } else {
                                    val sdToHash = getNowPlayingFromMainProcess()
                                    if (sdToHash != null) {
                                        sd = sdToHash.first
                                        hash = sdToHash.second
                                    } else {

                                        return@TrackDropdownMenu null
                                    }
                                }

                                PanoRoute.Modal.EditScrobble(
                                    origScrobbleData = sd,
                                    msid = track.msid,
                                    key = key,
                                    hash = hash
                                )
                            }
                        } else null,
                        onLove = if (canLove) {
                            {
                                viewModel.loveOrUnlove(item, it)
                            }
                        } else null,
                        onHate = if (canHate) {
                            {
                                viewModel.hateOrUnhate(item, it)
                            }
                        } else null,
                        onDelete = if (canDelete) {
                            {
                                viewModel.removeTrack(item)
                            }
                        } else null,
                        onShare = if (!PlatformStuff.isDesktop && !PlatformStuff.isTv) {
                            { t, shareSig ->
                                viewModel.shareTrack(t, shareSig)
                            }
                        } else null,
                        expanded = menuVisible,
                        onDismissRequest = { menuVisible = false }
                    )
                },
                onMenuClick = { menuVisible = true },
                modifier = Modifier
                    .animateItem()
                    .then(
                        if (animateListItemContentSize.value)
                            Modifier.animateContentSize()
                        else
                            Modifier
                    )
                    .then(
                        if (isExpanded)
                            Modifier.heightIn(
                                max = maxHeight.value
                                    .coerceAtLeast(100.dp)
                            )
                        else
                            Modifier
                    )
                    .then(
                        if (item == null)
                            Modifier.shimmerWindowBounds()
                        else
                            Modifier
                    )
            )
        }
    }
}

@Composable
fun OnEditEffect(
    viewModel: ScrobblesVM,
    editDataFlow: Flow<Pair<String, Track>>
) {
    LaunchedEffect(Unit) {
        editDataFlow.collect { (key, editedTrack) ->
            viewModel.editTrack(key, editedTrack)
        }
    }
}

fun LazyListScope.pendingScrobblesListItems(
    headerText: String,
    headerIcon: ImageVector,
    items: List<PendingScrobble>,
    showScrobbleSources: Boolean,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onItemClick: (MusicEntry) -> Unit,
    viewModel: ScrobblesVM,
    fetchAlbumImageIfMissing: Boolean = false,
    minItems: Int = 3,
) {
    if (items.isEmpty()) return

    item(key = headerText) {
        ExpandableHeaderItem(
            title = headerText,
            icon = headerIcon,
            expanded = expanded || items.size <= minItems,
            enabled = items.size > minItems,
            onToggle = onToggle,
            modifier = Modifier.animateItem(),
        )
    }

    items(
        items.take(if (expanded) items.size else minItems),
        key = { it.hashCode() }
    ) { item ->
        val musicEntry = remember {
            Track(
                name = item.scrobbleData.track,
                artist = Artist(item.scrobbleData.artist),
                album = item.scrobbleData.album?.let { Album(it) },
                date = item.scrobbleData.timestamp,
                duration = item.scrobbleData.duration,
                userloved = item.event == ScrobbleEvent.love,
                userHated = item.event == ScrobbleEvent.unlove,
            )
        }
        var menuVisible by remember { mutableStateOf(false) }
        val appItem = remember(item) {
            if (showScrobbleSources)
                item.scrobbleData.appId?.let { AppItem(it, PlatformStuff.loadApplicationLabel(it)) }
            else null
        }

        MusicEntryListItem(
            musicEntry,
            appItem = appItem,
            onEntryClick = { onItemClick(musicEntry) },
            onMenuClick = { menuVisible = true },
            isPending = true,
            menuContent = {
                PendingDropdownMenu(
                    pendingScrobble = item,
                    expanded = menuVisible,
                    onDismissRequest = { menuVisible = false },
                    onLove = {
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            val track = Track(
                                item.scrobbleData.track,
                                item.scrobbleData.album?.let {
                                    Album(
                                        it,
                                        item.scrobbleData.albumArtist
                                            ?.let { Artist(it) })
                                },
                                Artist(item.scrobbleData.artist)
                            )

                            ScrobbleEverywhere.loveOrUnlove(track, it)
                        }
                    },
                    onDelete = {
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            PanoDb.db.getPendingScrobblesDao().delete(item)
                        }
                    }
                )
            },
            fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
            modifier = Modifier
                .animateItem()
        )
    }
}