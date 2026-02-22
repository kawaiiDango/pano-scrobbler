package com.arn.scrobble.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.icons.AddPhotoAlternate
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.BrokenImage
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.ContentCopy
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.KeyboardArrowDown
import com.arn.scrobble.icons.KeyboardArrowUp
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.icons.Search
import com.arn.scrobble.icons.filled.Favorite
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.panoicons.PanoIcons
import com.arn.scrobble.panoicons.UserTag
import com.arn.scrobble.ui.EntriesRow
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.PanoLazyRow
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.placeholderImageVectorPainter
import com.arn.scrobble.ui.placeholderPainter
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add_photo
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.artist_image
import pano_scrobbler.composeapp.generated.resources.collapse
import pano_scrobbler.composeapp.generated.resources.copy
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.expand
import pano_scrobbler.composeapp.generated.resources.listeners
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.more_info
import pano_scrobbler.composeapp.generated.resources.my_tags
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.num_tracks
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.similar_artists
import pano_scrobbler.composeapp.generated.resources.similar_tracks
import pano_scrobbler.composeapp.generated.resources.top_albums
import pano_scrobbler.composeapp.generated.resources.top_tracks
import pano_scrobbler.composeapp.generated.resources.unlove
import pano_scrobbler.composeapp.generated.resources.user_loved
import pano_scrobbler.composeapp.generated.resources.user_tags_hint

@Composable
fun MusicEntryInfoDialog(
    musicEntry: MusicEntry,
    appId: String?,
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    viewModel: InfoVM = viewModel { InfoVM(musicEntry, user.name) },
) {
    val infoMap by viewModel.infoMap.collectAsStateWithLifecycle()
    val infoLoaded by viewModel.infoLoaded.collectAsStateWithLifecycle()
    val allTypes = remember {
        arrayOf(
            Stuff.TYPE_TRACKS,
            Stuff.TYPE_ARTISTS,
            Stuff.TYPE_ALBUMS,
            Stuff.TYPE_ALBUM_ARTISTS,
        )
    }
    var isLoved by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var expandedHeaderType by rememberSaveable { mutableIntStateOf(-1) }
    var expandedWikiType by rememberSaveable { mutableIntStateOf(-1) }
    val userTags by viewModel.userTags.collectAsStateWithLifecycle()
    val userTagsHistory by viewModel.userTagsHistory.collectAsStateWithLifecycle()
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.currentAccountType
    }
    val useLastfm by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.lastfmApiAlways || it.currentAccountType == AccountType.LASTFM
    }
    val entries = remember(infoMap) {
        allTypes.mapNotNull { type ->
            val entry = infoMap?.get(type)
            if (entry != null) {
                type to entry
            } else {
                null
            }
        }
    }

    val onHorizontalEntryItemClick: (MusicEntry) -> Unit = {
        onNavigate(
            PanoRoute.Modal.MusicEntryInfo(
                track = it as? Track,
                album = it as? Album,
                artist = it as? Artist,
                appId = appId,
                user = user
            )
        )
    }

    LaunchedEffect(infoMap) {
        if (infoMap != null) {
            isLoved = (infoMap?.get(Stuff.TYPE_TRACKS) as? Track)?.userloved
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .then(
                if (infoLoaded)
                    Modifier
                else
                    Modifier.shimmerWindowBounds()
            )
    ) {
        entries.forEachIndexed { index, (type, entry) ->
            InfoSimpleHeader(
                text = entry.name,
                icon = getMusicEntryIcon(type),
                onClick = if (!useLastfm && entry is Track) null
                else {
                    { expandedHeaderType = if (expandedHeaderType == type) -1 else type }
                },
                leadingContent = {
                    AnimatedVisibility(expandedHeaderType != type) {
                        if (entry is Album || entry is Artist) {
                            AsyncImage(
                                model = remember(entry) {
                                    MusicEntryImageReq(entry, accountType)
                                },
                                placeholder = placeholderPainter(),
                                error = placeholderImageVectorPainter(
                                    entry,
                                    Icons.BrokenImage
                                ),
                                contentDescription = when (entry) {
                                    is Album -> stringResource(Res.string.album_art)
                                    is Artist -> stringResource(Res.string.artist_image)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                        }
                    }
                },
                trailingContent = {
                    Icon(
                        imageVector = if (expandedHeaderType == type) Icons.KeyboardArrowUp else Icons.KeyboardArrowDown,
                        contentDescription = stringResource(if (expandedHeaderType == type) Res.string.collapse else Res.string.expand),
                    )
                },
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            InfoActionsRow(
                entry = entry,
                originalEntry = viewModel.originalEntriesMap[type],
                appId = appId,
                user = user,
                userTagsButtonSelected = userTags[type] != null,
                onUserTagsClick = if (accountType == AccountType.LASTFM && !PlatformStuff.isTv) {
                    {
                        if (userTags[type] == null)
                            viewModel.loadUserTagsIfNeeded(type)
                        else
                            viewModel.clearUserTags(type)
                    }
                } else null,
                isLoved = isLoved,
                onLoveClick = if ((entry as? Track)?.userloved != null) {
                    {
                        viewModel.setLoved(entry, isLoved != true)
                        isLoved = isLoved != true
                    }
                } else null,
                onNavigate = onNavigate,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            InfoTags(
                tags = entry.tags?.tag ?: emptyList(),
                userTags = userTags[type],
                userTagsHistory = userTagsHistory,
                onTagClick = {
                    onNavigate(
                        PanoRoute.Modal.TagInfo(it)
                    )
                },
                onUserTagAdd = {
                    viewModel.addUserTag(type, it)
                },
                onUserTagDelete = {
                    viewModel.deleteUserTag(type, it)
                },
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            AnimatedVisibility(expandedHeaderType == type) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (entry is Album || entry is Artist) {
                        InfoBigPicture(
                            entry = entry,
                            imgRequest = remember(entry) {
                                MusicEntryImageReq(entry, accountType, isHeroImage = true)
                            },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        if (entry is Album) {
                            if (entry.tracks?.track != null) {
                                InfoTrackList(
                                    tracks = entry.tracks.track,
                                    onTrackClick = {
                                        onNavigate(
                                            PanoRoute.Modal.MusicEntryInfo(
                                                track = it,
                                                appId = appId,
                                                user = user
                                            )
                                        )
                                    },
                                )
                            }
                        } else if (entry is Artist && useLastfm) {
                            ArtistMisc(
                                entry,
                                isAlbumArtist = type == Stuff.TYPE_ALBUM_ARTISTS,
                                onHeaderClick = { t ->
                                    onNavigate(
                                        PanoRoute.MusicEntryInfoPager(
                                            artist = entry.copy(wiki = null),
                                            appId = appId,
                                            user = user,
                                            entryType = t
                                        )
                                    )
                                },
                                onItemClick = onHorizontalEntryItemClick
                            )
                        }

                    } else if (entry is Track && useLastfm) {
                        SimilarTracks(
                            entry,
                            onHeaderClick = {
                                onNavigate(
                                    PanoRoute.SimilarTracks(
                                        track = entry.copy(wiki = null),
                                        appId = appId,
                                        user = user
                                    )
                                )
                            },
                            onItemClick = onHorizontalEntryItemClick
                        )
                    }
                }
            }

            if (entry.playcount != null || entry.listeners != null || !infoLoaded) {
                InfoCountsForMusicEntry(
                    entry = entry,
                    user = user,
                    onNavigate = onNavigate,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            entry.wiki?.content?.let {
                InfoWikiText(
                    text = it,
                    maxLinesWhenCollapsed = 2,
                    expanded = expandedWikiType == type,
                    onExpandToggle = {
                        expandedWikiType = if (expandedWikiType == type) -1 else type
                    },
                    scrollState = scrollState,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            if (index < entries.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.InfoBigPicture(
    entry: MusicEntry,
    imgRequest: MusicEntryImageReq,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = imgRequest,
        error = placeholderImageVectorPainter(entry, Icons.BrokenImage),
        placeholder = placeholderPainter(),
        contentDescription = when (entry) {
            is Album -> stringResource(Res.string.album_art)
            is Artist -> stringResource(Res.string.artist_image)
            else -> null
        },
        modifier = modifier
            .height(320.dp)
            .aspectRatio(1f)
            .align(Alignment.CenterHorizontally)
            .clip(MaterialTheme.shapes.large)
    )
}

@Composable
private fun InfoCountsForMusicEntry(
    entry: MusicEntry,
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoCounts(
        countPairs = listOfNotNull(
            if (entry.userplaycount != null) {
                "" to entry.userplaycount
            } else {
                null
            },
            stringResource(Res.string.listeners) to entry.listeners,
            stringResource(Res.string.scrobbles) to entry.playcount,
        ),
        avatarUrl = user.largeImage.takeIf { it.isNotEmpty() },
        avatarName = user.name,
        firstItemIsUsers = entry.userplaycount != null,
        onClickFirstItem = if ((entry.userplaycount ?: 0) > 0 && entry is Track) {
            {
                onNavigate(
                    PanoRoute.TrackHistory(
                        track = entry.copy(wiki = null),
                        user = user
                    )
                )
            }
        } else if ((entry.userplaycount ?: 0) > 0 && !PlatformStuff.isTv) {
            {
                entry.url
                    ?.replace("/music/", "/user/${user.name}/library/music/")
                    ?.let {
                        PlatformStuff.openInBrowser(it)
                    }
            }
        } else
            null,
        modifier = modifier
    )
}

@Composable
private fun InfoActionsRow(
    entry: MusicEntry,
    originalEntry: MusicEntry?,
    appId: String?,
    user: UserCached,
    userTagsButtonSelected: Boolean,
    onUserTagsClick: (() -> Unit)?,
    isLoved: Boolean?,
    onLoveClick: (() -> Unit)?,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val isLicenseValid = LocalLicenseValidState.current
    val searchInSource by
    PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.searchInSource && isLicenseValid }

    Row(modifier = modifier.fillMaxWidth()) {
        if (onUserTagsClick != null) {
            IconButtonWithTooltip(
                icon = PanoIcons.UserTag,
                onClick = onUserTagsClick,
                filledStyle = userTagsButtonSelected,
                contentDescription = stringResource(Res.string.my_tags),
            )
        }

        if (entry is Track && onLoveClick != null && isLoved != null) {
            IconButtonWithTooltip(
                enabled = user.isSelf,
                onClick = onLoveClick,
                icon = if (isLoved) Icons.Filled.Favorite else Icons.Favorite,
                contentDescription = if (isLoved && user.isSelf)
                    stringResource(Res.string.unlove)
                else if (isLoved)
                    stringResource(Res.string.user_loved, user.name)
                else
                    stringResource(Res.string.love),
                modifier = if (user.isSelf) Modifier else Modifier.alpha(0.5f)
            )
        }

        if (entry is Album || entry is Artist) {
            IconButtonWithTooltip(
                onClick = {
                    onNavigate(
                        PanoRoute.ImageSearch(
                            artist = (entry as? Artist)?.copy(wiki = null), // wiki can be too long to serialize and parcel
                            originalArtist = (originalEntry as? Artist)?.copy(wiki = null),
                            album = (entry as? Album)?.copy(
                                wiki = null,
                                tracks = null
                            ),
                            originalAlbum = (originalEntry as? Album)?.copy(
                                wiki = null,
                                tracks = null
                            )
                        )
                    )
                },
                icon = Icons.AddPhotoAlternate,
                contentDescription = stringResource(Res.string.add_photo),
            )
        }

        IconButtonWithTooltip(
            onClick = {
                scope.launch {
                    PlatformStuff.launchSearchIntent(entry, appId.takeIf { searchInSource })
                }
            },
            icon = Icons.Search,
            contentDescription = stringResource(Res.string.search),
        )

        if (!PlatformStuff.isTv) {
            IconButtonWithTooltip(
                onClick = {
                    val text = when (entry) {
                        is Track -> entry.artist.name + " " + entry.name
                        is Album -> entry.artist?.name.orEmpty() + " " + entry.name
                        is Artist -> entry.name
                    }
                    PlatformStuff.copyToClipboard(text)
                },
                icon = Icons.ContentCopy,
                contentDescription = stringResource(Res.string.copy),
            )
        }

        if (entry.url != null && !PlatformStuff.isTv) {
            IconButtonWithTooltip(
                onClick = {
                    PlatformStuff.openInBrowser(entry.url!!)
                },
                icon = Icons.OpenInBrowser,
                contentDescription = stringResource(Res.string.more_info),
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        if (entry is Track && entry.duration != null && entry.duration > 0) {
            Text(
                text = Stuff.humanReadableDuration(entry.duration),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.InfoTags(
    tags: List<Tag>,
    userTags: Iterable<String>?,
    userTagsHistory: List<String>,
    onTagClick: (Tag) -> Unit,
    onUserTagAdd: (String) -> Unit,
    onUserTagDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty() && userTags == null) {
        return
    }

    var userTagInput by rememberSaveable { mutableStateOf("") }
    var dropdownShown by rememberSaveable { mutableStateOf(false) }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        tags.forEach { tag ->
            AssistChip(
                label = {
                    Text(
                        text = tag.name,
                    )
                },
                onClick = { onTagClick(tag) }
            )
        }

        userTags?.forEach { tagName ->
            InputChip(
                selected = true,
                label = {
                    Text(
                        text = tagName,
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Close,
                        contentDescription = stringResource(Res.string.delete),
                    )
                },
                onClick = { onUserTagDelete(tagName) },
            )
        }
    }

    if (userTags != null) {
        ExposedDropdownMenuBox(
            expanded = dropdownShown,
            onExpandedChange = { dropdownShown = it },
            modifier = Modifier
                .padding(end = 24.dp)
                .align(Alignment.End)
                .width(200.dp)
        ) {
            PanoOutlinedTextField(
                value = userTagInput,
                onValueChange = { userTagInput = it },
                label = { Text(stringResource(Res.string.user_tags_hint)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownShown)
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (userTagInput.isNotBlank() &&
                            userTagInput.split(",").all { it.isNotBlank() }
                        ) {
                            onUserTagAdd(userTagInput)
                            userTagInput = ""
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
            )
            ExposedDropdownMenu(
                expanded = dropdownShown,
                onDismissRequest = { dropdownShown = false }) {
                userTagsHistory.forEach {
                    DropdownMenuItem(
                        onClick = {
                            userTagInput = it
                            dropdownShown = false
                        },
                        text = { Text(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackListTrack(idx: Int, track: Track, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .padding(8.dp)
    ) {
        Text(
            text = "${idx + 1}.",
        )

        Text(
            text = track.name,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            modifier = Modifier.align(Alignment.End),
            text = track.duration?.let { Stuff.humanReadableDuration(it) } ?: "",
        )
    }
}

@Composable
private fun ColumnScope.SimilarTracks(
    entry: Track,
    onHeaderClick: () -> Unit,
    onItemClick: (MusicEntry) -> Unit,
    viewModel: SimilarTracksVM = viewModel { SimilarTracksVM(entry) },
) {
    val similarTracks = viewModel.similarTracks.collectAsLazyPagingItems()
    EntriesRow(
        title = stringResource(Res.string.similar_tracks),
        entries = similarTracks,
        fetchAlbumImageIfMissing = true,
        showArtists = true,
        headerIcon = Icons.MusicNote,
        emptyStringRes = Res.string.not_found,
        placeholderItem = remember {
            getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS)
        },
        onHeaderClick = onHeaderClick,
        onItemClick = onItemClick,
    )
}

@Composable
private fun ColumnScope.ArtistMisc(
    entry: Artist,
    isAlbumArtist: Boolean,
    onHeaderClick: (type: Int) -> Unit,
    onItemClick: (MusicEntry) -> Unit,
    viewModel: ArtistMiscVM = viewModel(key = "ArtistMiscVM|$isAlbumArtist") { ArtistMiscVM(entry) },
) {
    val artistTopTracks = viewModel.topTracks.collectAsLazyPagingItems()
    val artistTopAlbums = viewModel.topAlbums.collectAsLazyPagingItems()
    val similarArtists = viewModel.similarArtists.collectAsLazyPagingItems()

    EntriesRow(
        title = stringResource(Res.string.top_tracks),
        entries = artistTopTracks,
        fetchAlbumImageIfMissing = true,
        showArtists = false,
        headerIcon = Icons.MusicNote,
        emptyStringRes = Res.string.not_found,
        placeholderItem = remember {
            getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS)
        },
        onHeaderClick = {
            onHeaderClick(Stuff.TYPE_TRACKS)
        },
        onItemClick = onItemClick,
    )

    EntriesRow(
        title = stringResource(Res.string.top_albums),
        entries = artistTopAlbums,
        fetchAlbumImageIfMissing = false,
        showArtists = false,
        headerIcon = Icons.Album,
        placeholderItem = remember {
            getMusicEntryPlaceholderItem(Stuff.TYPE_ALBUMS)
        },
        emptyStringRes = Res.string.not_found,
        onHeaderClick = {
            onHeaderClick(Stuff.TYPE_ALBUMS)
        },
        onItemClick = onItemClick,
    )

    EntriesRow(
        title = stringResource(Res.string.similar_artists),
        entries = similarArtists,
        fetchAlbumImageIfMissing = false,
        showArtists = true,
        headerIcon = Icons.Mic,
        emptyStringRes = Res.string.not_found,
        placeholderItem = remember {
            getMusicEntryPlaceholderItem(Stuff.TYPE_ARTISTS, showScrobbleCount = false)
        },
        onHeaderClick = {
            onHeaderClick(Stuff.TYPE_ARTISTS)
        },
        onItemClick = onItemClick,
    )
}

@Composable
private fun InfoTrackList(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tracks.isEmpty()) {
        return
    }

    val totalDuration = remember(tracks) { tracks.sumOf { it.duration ?: 0 } }
    val durationsMissing = remember(tracks) { tracks.any { it.duration == null } }
    val durationsString = remember(tracks) {
        totalDuration.takeIf { it > 0 }
            ?.let { Stuff.humanReadableDuration(it) + if (durationsMissing) "+" else "" }
    }

    PanoLazyRow(
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier
    ) {
        item("summary") {
            Text(
                text = pluralStringResource(
                    Res.plurals.num_tracks,
                    tracks.size,
                    tracks.size.format()
                ) + (durationsString?.let { "\n\n$it" } ?: ""),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(8.dp)
            )
        }

        itemsIndexed(tracks, key = { i, it ->
            i
        }) { i, track ->
            TrackListTrack(
                i, track,
                Modifier
                    .size(150.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        onTrackClick(track)
                    }
                    .padding(8.dp),
            )
        }
    }
}