package com.arn.scrobble.recents

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.arn.scrobble.api.AccountType
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
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.edits.EditScrobbleDialog
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.ExpandableHeaderItem
import com.arn.scrobble.ui.ListLoadError
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.showTrackShareSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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

@Composable
fun TrackDropdownMenu(
    track: Track,
    pkgName: String?,
    onNavigate: (PanoRoute) -> Unit,
    user: UserCached,
    onLove: ((Boolean) -> Unit)?,
    onHate: ((Boolean) -> Unit)?,
    onEdit: ((ScrobbleData) -> Unit)?,
    onDelete: (() -> Unit)?,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    skipFirstLevel: Boolean,
    modifier: Modifier = Modifier,
) {

    var menuLevel by remember(expanded) {
        mutableStateOf(
            if (!skipFirstLevel)
                TrackMenuLevel.Root
            else
                TrackMenuLevel.More
        )
    }
    var editDialogShown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        if (user.isSelf) {
            when (menuLevel) {
                TrackMenuLevel.Root -> {

                    if (onLove != null) {
                        DropdownMenuItem(
                            onClick = {
                                GlobalScope.launch {
                                    withContext(Dispatchers.IO) {
                                        ScrobbleEverywhere.loveOrUnlove(
                                            track,
                                            track.userloved != true
                                        )
                                    }
                                }

                                onLove(track.userloved != true)
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
                                editDialogShown = true
                                onDismissRequest()
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
                                        ScrobbleEverywhere.delete(track)
                                            .forEach {
                                                it.onFailure {
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
                    DropdownMenuItem(
                        onClick = {},
                        enabled = false,
                        text = {
                            Text(stringResource(Res.string.more))
                        },
                    )

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
                        }
                    )

                    if (onHate != null) {

                        DropdownMenuItem(
                            onClick = {
                                val newHated = track.userHated != true
                                GlobalScope.launch(Dispatchers.IO) {
                                    if (newHated)
                                        (Scrobblables.current.value as? ListenBrainz)?.hate(track)
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

                    DropdownMenuItem(
                        onClick = {
                            scope.launch {
                                PlatformStuff.launchSearchIntent(track, pkgName)
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

                TrackMenuLevel.Block -> {
                    DropdownMenuItem(
                        onClick = {},
                        enabled = false,
                        text = {
                            Text(stringResource(Res.string.block))
                        },
                    )

                    DropdownMenuItem(
                        onClick = {
                            val b = BlockedMetadata(
                                artist = track.artist.name,
                                album = track.album?.name ?: "",
                                track = track.name,
                            )
                            onNavigate(PanoRoute.BlockedMetadataAdd(b))
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
                        }
                    )
                    DropdownMenuItem(
                        onClick = {
                            val b = BlockedMetadata(
                                artist = track.artist.name,
                                album = track.album?.name ?: "",
                            )
                            onNavigate(PanoRoute.BlockedMetadataAdd(b))
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
                            onNavigate(PanoRoute.BlockedMetadataAdd(b))
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
            DropdownMenuItem(
                onClick = {
                    scope.launch {
                        PlatformStuff.launchSearchIntent(track, pkgName)
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
    }

    if (editDialogShown) {
        val sd = ScrobbleData(
            track = track.name,
            artist = track.artist.name,
            album = track.album?.name,
            timestamp = track.date ?: 0,
            albumArtist = null,
            duration = null,
            packageName = null
        )

        EditScrobbleDialog(
            scrobbleData = sd,
            msid = track.msid,
            hash = null,
            onDone = onEdit!!,
            onDismiss = { editDialogShown = false },
            onNavigate = onNavigate
        )
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
                        .animateItem()
                )
            }
        }

        tracks.loadState.refresh is LoadState.Error ||
                tracks.loadState.append is LoadState.Error
            -> {
            val error = tracks.loadState.refresh as LoadState.Error
            item {
                ListLoadError(
                    modifier = Modifier.animateItem(),
                    throwable = error.error,
                    onRetry = { tracks.retry() })
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
        PendingScrobbleServicesDesc(pendingScrobble)

        if (pendingScrobble.event == ScrobbleEvent.scrobble) {
            DropdownMenuItem(
                onClick = {
                    GlobalScope.launch {
                        withContext(Dispatchers.IO) {
                            val track = Track(
                                pendingScrobble.track,
                                pendingScrobble.album.ifEmpty { null }?.let {
                                    Album(
                                        it,
                                        pendingScrobble.albumArtist.ifEmpty { null }
                                            ?.let { Artist(it) })
                                },
                                Artist(pendingScrobble.artist)
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
private fun ColumnScope.PendingScrobbleServicesDesc(
    pendingScrobble: PendingScrobble,
) {
    val currentScrobblableType by remember { mutableStateOf(Scrobblables.current.value?.userAccount?.type) }
    val accountTypesList = mutableListOf<AccountType>()
    AccountType.entries.forEach {
        if (pendingScrobble.state and (1 shl it.ordinal) != 0)
            accountTypesList += it
    }

    if (accountTypesList.size == 1 && accountTypesList.first() == currentScrobblableType) {

    } else {
        DropdownMenuItem(
            onClick = {},
            enabled = false,
            text = {
                Text(
                    stringResource(Res.string.scrobble_services) + ":\n" +
                            accountTypesList.map {
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
    }
}

fun LazyListScope.scrobblesListItems(
    tracks: LazyPagingItems<Track>,
    user: UserCached,
    deletedTracksSet: Set<Track>,
    editedTracksMap: Map<Track, Track>,
    pkgMap: Map<Long, String>,
    fetchAlbumImageIfMissing: Boolean,
    showFullMenu: Boolean,
    showLove: Boolean,
    showHate: Boolean,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: ScrobblesVM,
) {
    fun onTrackClick(track: Track, pkgName: String?) {
        onNavigate(PanoRoute.MusicEntryInfo(user = user, track = track, pkgName = pkgName))
    }

    for (i in 0 until tracks.itemCount) {
        val trackPeek = tracks.peek(i)
        if (trackPeek == null || trackPeek !in deletedTracksSet) {
            item(
                key = trackPeek?.toString() ?: i
            ) {
                val track = tracks[i]?.let {
                    editedTracksMap[it] ?: it
                } ?: getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS) as Track

                var menuVisible by remember { mutableStateOf(false) }

                val pkgName = track.date?.let { pkgMap[it] }

                MusicEntryListItem(
                    entry = track,
                    appId = pkgName,
                    showDateSeperator = track.date in viewModel.lastScrobbleOfTheDaySet,
                    onEntryClick = { onTrackClick(track, pkgName) },
                    forShimmer = trackPeek == null,
                    fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
                    menuContent = {
                        TrackDropdownMenu(
                            track = track,
                            pkgName = pkgName,
                            onNavigate = onNavigate,
                            user = user,
                            onLove = if (showLove) {
                                {
                                    viewModel.editTrack(
                                        trackPeek!!,
                                        track.copy(userloved = it)
                                    )
                                }
                            } else null,
                            onHate = if (showHate) {
                                {
                                    viewModel.editTrack(
                                        trackPeek!!,
                                        track.copy(userHated = it)
                                    )
                                }
                            } else null,
                            onDelete = { viewModel.removeTrack(trackPeek!!) },
                            onEdit = {
                                val _artist = Artist(it.artist)
                                val _album = it.album?.let { Album(it, _artist) }

                                val editedTrack = Track(
                                    it.track,
                                    _album,
                                    _artist,
                                    date = it.timestamp.takeIf { it > 0L }
                                )

                                viewModel.editTrack(trackPeek!!, editedTrack)
                            },
                            expanded = menuVisible,
                            skipFirstLevel = !showFullMenu,
                            onDismissRequest = { menuVisible = false }
                        )
                    },
                    onMenuClick = { menuVisible = true },
                    modifier = Modifier.animateItem()
                )

            }
        }
    }
}

fun LazyListScope.pendingScrobblesListItems(
    headerText: String,
    headerIcon: ImageVector,
    items: List<PendingScrobble>,
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
                name = item.track,
                artist = Artist(item.artist),
                album = item.album.ifEmpty { null }?.let { Album(it) },
                date = item.timestamp,
                duration = item.duration,
                userloved = item.event == ScrobbleEvent.love,
                userHated = item.event == ScrobbleEvent.unlove,
            )
        }
        var menuVisible by remember { mutableStateOf(false) }

        MusicEntryListItem(
            musicEntry,
            appId = item.packageName,
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
            modifier = modifier.animateItem()
        )
    }
}