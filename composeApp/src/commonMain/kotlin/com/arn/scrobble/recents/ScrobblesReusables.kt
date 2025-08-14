package com.arn.scrobble.recents

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEvent
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.ExpandableHeaderItem
import com.arn.scrobble.ui.ListLoadError
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.generateKey
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.showTrackShareSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.copy
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.hate
import pano_scrobbler.composeapp.generated.resources.lastfm_reauth
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.more
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.scrobble_services
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.share
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
    onOpenDialog: (PanoDialog) -> Unit,
    user: UserCached,
    onEdit: (() -> Unit)?,
    onLove: ((Boolean) -> Unit)?,
    onHate: ((Boolean) -> Unit)?,
    onDelete: (() -> Unit)?,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {

    var menuLevel by remember(expanded) {
        mutableStateOf(
            if (onEdit == null && onLove == null && onHate == null && onDelete == null)
                TrackMenuLevel.More
            else
                TrackMenuLevel.Root
        )
    }

    val moreFocusRequester = remember { FocusRequester() }
    val blockFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

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
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null
                        )
                    }
                )
            }
        }

        @Composable
        fun searchItem() {
            DropdownMenuItem(
                onClick = {
                    scope.launch {
                        PlatformStuff.launchSearchIntent(track, appId)
                    }
                    onDismissRequest()
                },
                text = {
                    Text(stringResource(Res.string.search))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
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

                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        ScrobbleEverywhere.loveOrUnlove(
                                            track,
                                            loved != true
                                        )
                                    }
                                }

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
                                        Icons.Outlined.FavoriteBorder
                                    else
                                        Icons.Filled.Favorite,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    if (onEdit != null) {
                        DropdownMenuItem(
                            onClick = {
                                onDismissRequest()
                                onEdit.invoke()
                            },
                            text = {
                                Text(stringResource(Res.string.edit))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    if (onDelete != null && track.date != null && track.date > 0) {
                        DropdownMenuItem(
                            onClick = {
                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        Scrobblables.current?.delete(track)
                                            ?.onFailure {
                                                it.printStackTrace()
                                                if (it is LastfmUnscrobbler.CookiesInvalidatedException) {
                                                    Stuff.globalSnackbarFlow.emit(
                                                        PanoSnackbarVisuals(
                                                            getString(Res.string.lastfm_reauth),
                                                            isError = true
                                                        )
                                                    )
                                                } else
                                                    Stuff.globalExceptionFlow.emit(it)
                                            }
                                            ?.onSuccess {
                                                CachedTracksDao.deltaUpdateAll(
                                                    track,
                                                    -1,
                                                    DirtyUpdate.BOTH
                                                )
                                            }
                                    }
                                }
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
                                    imageVector = Icons.Outlined.Delete,
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
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
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
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.focusRequester(moreFocusRequester)
                    )

                    if (onHate != null) {

                        DropdownMenuItem(
                            onClick = {
                                val newHated = track.userHated != true
                                GlobalScope.launch(Dispatchers.IO) {
                                    if (newHated)
                                        (Scrobblables.current as? ListenBrainz)?.hate(track)
                                    else
                                        ScrobbleEverywhere.loveOrUnlove(track, false)
                                }
                                onHate(newHated)
                                onDismissRequest()
                            },
                            text = {
                                Text(stringResource(Res.string.hate))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.HeartBroken,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    searchItem()
                    copyItem()

                    if (!PlatformStuff.isDesktop && !PlatformStuff.isTv) {
                        DropdownMenuItem(
                            onClick = {
                                scope.launch {
                                    showTrackShareSheet(track, user)
                                }

                                onDismissRequest()
                            },
                            text = {
                                Text(stringResource(Res.string.share))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
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
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
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
                            onOpenDialog(PanoDialog.BlockedMetadataAdd(b))
                            onDismissRequest()
                        },
                        text = {
                            Text(stringResource(Res.string.track))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
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
                            onOpenDialog(PanoDialog.BlockedMetadataAdd(b))
                            onDismissRequest()
                        },
                        text = {
                            Text(stringResource(Res.string.album))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Album,
                                contentDescription = null
                            )
                        }
                    )

                    DropdownMenuItem(
                        onClick = {
                            val b = BlockedMetadata(
                                artist = track.artist.name,
                            )
                            onOpenDialog(PanoDialog.BlockedMetadataAdd(b))
                        },
                        text = {
                            Text(stringResource(Res.string.artist))
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
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
    tracks: LazyPagingItems<Track>,
) {
    when {
        tracks.loadState.refresh is LoadState.Loading -> {
            items(10) {
                MusicEntryListItem(
                    getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS),
                    forShimmer = true,
                    onEntryClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .shimmerWindowBounds()
                        .animateItem()
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
                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            val track = Track(
                                pendingScrobble.scrobbleData.track,
                                pendingScrobble.scrobbleData.album?.let {
                                    Album(
                                        it,
                                        pendingScrobble.scrobbleData.albumArtist
                                            ?.let { Artist(it) })
                                },
                                Artist(pendingScrobble.scrobbleData.artist)
                            )

                            ScrobbleEverywhere.loveOrUnlove(track, true)
                        }
                    }
                    onDismissRequest()
                },
                text = {
                    Text(
                        stringResource(Res.string.love)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                }
            )
        }

        DropdownMenuItem(
            onClick = {
                GlobalScope.launch {
                    withContext(Dispatchers.IO) {
                        PanoDb.db.getPendingScrobblesDao().delete(pendingScrobble)
                    }
                }
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
                    imageVector = Icons.Outlined.Delete,
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
                imageVector = Icons.Outlined.Dns,
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
                    imageVector = Icons.Outlined.ErrorOutline,
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
                        withPreposition = true
                    )
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null
                )
            }
        )
    }
}

fun LazyListScope.scrobblesListItems(
    tracks: LazyPagingItems<Track>,
    user: UserCached,
    deletedTracksSet: Set<Track>,
    editedTracksMap: Map<String, Track>,
    pkgMap: Map<Long, String>,
    seenApps: Map<String, String>,
    fetchAlbumImageIfMissing: Boolean,
    showScrobbleSources: Boolean,
    canLove: Boolean,
    canHate: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    expandedKey: () -> String?,
    onExpand: (String?) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    viewModel: ScrobblesVM,
) {
    fun onTrackClick(track: Track, appId: String?) {
        onOpenDialog(PanoDialog.MusicEntryInfo(user = user, track = track, appId = appId))
    }

    for (i in 0 until tracks.itemCount) {
        val trackPeek = tracks.peek(i)?.let {
            editedTracksMap[it.generateKey()] ?: it
        }

        if (trackPeek == null || trackPeek !in deletedTracksSet) {

            if (trackPeek?.date in viewModel.lastScrobbleOfTheDaySet) {
                item(
                    key = "date_separator\n${trackPeek?.generateKey()}"
                ) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }
            }

            val key = trackPeek?.generateKey() ?: "placeholder_$i"
            item(
                key = key
            ) {
                val track = tracks[i]?.let {
                    editedTracksMap[it.generateKey()] ?: it
                } ?: getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS) as Track

                var menuVisible by remember { mutableStateOf(false) }

                val appItem = remember(track) {
                    if (showScrobbleSources) {
                        track.date
                            ?.let { track.appId ?: pkgMap[it] }
                            ?.let { AppItem(it, seenApps[it] ?: "") }
                    } else
                        null
                }

                MusicEntryListItem(
                    entry = track,
                    appItem = appItem,
                    onEntryClick = { onTrackClick(track, appItem?.appId) },
                    isColumn = expandedKey() == key,
                    fixedImageHeight = expandedKey() != key,
                    onImageClick = {
                        if (expandedKey() == key)
                            onExpand(null)
                        else {
                            onExpand(key)
                        }
                    },
                    forShimmer = trackPeek == null,
                    fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
                    menuContent = {
                        TrackDropdownMenu(
                            track = track,
                            appId = appItem?.appId,
                            onOpenDialog = onOpenDialog,
                            user = user,
                            onEdit = if (canEdit) {
                                {
                                    val sd = ScrobbleData(
                                        track = track.name,
                                        artist = track.artist.name,
                                        album = track.album?.name,
                                        timestamp = track.date ?: 0,
                                        albumArtist = null,
                                        duration = null,
                                        appId = null
                                    )

                                    val dialogArgs = PanoDialog.EditScrobble(
                                        origTrack = trackPeek,
                                        scrobbleData = sd,
                                        msid = track.msid,
                                        hash = null
                                    )

                                    onOpenDialog(dialogArgs)
                                }
                            } else null,
                            onLove = if (canLove) {
                                {
                                    viewModel.editTrack(
                                        trackPeek!!,
                                        track.copy(userloved = it)
                                    )
                                }
                            } else null,
                            onHate = if (canHate) {
                                {
                                    viewModel.editTrack(
                                        trackPeek!!,
                                        track.copy(userHated = it)
                                    )
                                }
                            } else null,
                            onDelete = if (canDelete) {
                                {
                                    viewModel.removeTrack(trackPeek!!)
                                }
                            } else null,
                            expanded = menuVisible,
                            onDismissRequest = { menuVisible = false }
                        )
                    },
                    onMenuClick = { menuVisible = true },
                    modifier = Modifier
                        .animateContentSize()
                        .then(
                            if (expandedKey() == key)
                                Modifier.fillParentMaxHeight()
                            else
                                Modifier
                        )
                        .then(
                            if (trackPeek == null)
                                Modifier.shimmerWindowBounds()
                            else
                                Modifier
                        )
                        .animateItem()
                )

            }
        }
    }
}

@Composable
fun OnEditEffect(
    viewModel: ScrobblesVM,
    editDataFlow: Flow<Pair<Track, ScrobbleData>>
) {
    LaunchedEffect(Unit) {
        editDataFlow.collect { (origTrack, newScrobbleData) ->
            val _artist = Artist(newScrobbleData.artist)
            val _album = newScrobbleData.album?.let { Album(it, _artist) }

            val editedTrack = Track(
                newScrobbleData.track,
                _album,
                _artist,
                date = newScrobbleData.timestamp.takeIf { it > 0L }
            )

            viewModel.editTrack(origTrack, editedTrack)
        }
    }
}

fun LazyListScope.pendingScrobblesListItems(
    headerText: String,
    headerIcon: ImageVector,
    items: List<PendingScrobble>,
    showScrobbleSources: Boolean,
    seenApps: Map<String, String>,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onItemClick: (MusicEntry) -> Unit,
    modifier: Modifier = Modifier,
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
            modifier = modifier.animateItem(),
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
                item.scrobbleData.appId?.let { AppItem(it, seenApps[it] ?: "") }
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
                    onDismissRequest = { menuVisible = false }
                )
            },
            fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
            modifier = modifier
                .animateContentSize()
                .animateItem()
        )
    }
}