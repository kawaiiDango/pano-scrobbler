package com.arn.scrobble.info

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Piano
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.spotify.TrackWithFeatures
import com.arn.scrobble.icons.Metronome
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.icons.UserTag
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.EntriesRow
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.PanoLazyRow
import com.arn.scrobble.ui.TextWithIcon
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.placeholderImageVectorPainter
import com.arn.scrobble.ui.placeholderPainter
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.LegendLocation
import io.github.koalaplot.core.polar.DefaultPolarPoint
import io.github.koalaplot.core.polar.PolarGraph
import io.github.koalaplot.core.polar.PolarGraphDefaults
import io.github.koalaplot.core.polar.PolarPlotSeries
import io.github.koalaplot.core.polar.RadialGridType
import io.github.koalaplot.core.polar.rememberCategoryAngularAxisModel
import io.github.koalaplot.core.polar.rememberFloatRadialAxisModel
import io.github.koalaplot.core.style.AreaStyle
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.acoustic
import pano_scrobbler.composeapp.generated.resources.add_photo
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.artist_image
import pano_scrobbler.composeapp.generated.resources.collapse
import pano_scrobbler.composeapp.generated.resources.copy
import pano_scrobbler.composeapp.generated.resources.danceable
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.energetic
import pano_scrobbler.composeapp.generated.resources.expand
import pano_scrobbler.composeapp.generated.resources.instrumental
import pano_scrobbler.composeapp.generated.resources.listeners
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.more_info
import pano_scrobbler.composeapp.generated.resources.my_tags
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.num_tracks
import pano_scrobbler.composeapp.generated.resources.popularity
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.similar_artists
import pano_scrobbler.composeapp.generated.resources.similar_tracks
import pano_scrobbler.composeapp.generated.resources.top_albums
import pano_scrobbler.composeapp.generated.resources.top_tracks
import pano_scrobbler.composeapp.generated.resources.unlove
import pano_scrobbler.composeapp.generated.resources.user_loved
import pano_scrobbler.composeapp.generated.resources.user_tags_hint
import pano_scrobbler.composeapp.generated.resources.valence
import java.text.DateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MusicEntryInfoDialog(
    musicEntry: MusicEntry,
    appId: String?,
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InfoVM = viewModel { InfoVM() },
    miscVM: InfoMiscVM = viewModel { InfoMiscVM() },
) {
    // the arguments can change for this screen

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
    var isLoved by rememberSaveable(musicEntry) { mutableStateOf<Boolean?>(null) }
    var expandedHeaderType by rememberSaveable(musicEntry) { mutableIntStateOf(-1) }
    var expandedWikiType by rememberSaveable(musicEntry) { mutableIntStateOf(-1) }
    val userTags by viewModel.userTags.collectAsStateWithLifecycle()
    val userTagsHistory by viewModel.userTagsHistory.collectAsStateWithLifecycle()
    val account by Scrobblables.currentAccount.collectAsStateWithLifecycle()

    val similarTracks = miscVM.similarTracks.collectAsLazyPagingItems()

    val artistTopTracks = miscVM.topTracks.collectAsLazyPagingItems()
    val artistTopAlbums = miscVM.topAlbums.collectAsLazyPagingItems()
    val similarArtists = miscVM.similarArtists.collectAsLazyPagingItems()
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
        onOpenDialog(
            PanoDialog.MusicEntryInfo(
                track = it as? Track,
                album = it as? Album,
                artist = it as? Artist,
                appId = appId,
                user = user
            )
        )
    }

    LaunchedEffect(musicEntry) {
        viewModel.setMusicEntryIfNeeded(musicEntry, user.name)
    }

    LaunchedEffect(infoMap) {
        if (infoMap != null) {
            isLoved = (infoMap?.get(Stuff.TYPE_TRACKS) as? Track)?.userloved
        }
    }

    LaunchedEffect(expandedHeaderType) {
        val expandedEntry = infoMap?.get(expandedHeaderType)

        when (expandedEntry) {
            is Track -> {
                miscVM.setTrack(expandedEntry)
            }

            is Artist -> {
                miscVM.setArtist(expandedEntry)
            }

            else -> Unit
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
                onClick = { expandedHeaderType = if (expandedHeaderType == type) -1 else type },
                leadingContent = {
                    AnimatedVisibility(expandedHeaderType != type) {
                        if (entry is Album || entry is Artist) {
                            AsyncImage(
                                model = remember(entry) {
                                    MusicEntryImageReq(entry)
                                },
                                placeholder = placeholderPainter(),
                                error = placeholderImageVectorPainter(
                                    entry,
                                    Icons.Outlined.ImageNotSupported
                                ),
                                contentDescription = when (entry) {
                                    is Album -> stringResource(Res.string.album_art)
                                    is Artist -> stringResource(Res.string.artist_image)
                                    else -> null
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
                        imageVector = if (expandedHeaderType == type) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
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
                showUserTagsButton = account?.type == AccountType.LASTFM && userTags[type] == null && !PlatformStuff.isTv,
                onUserTagsClick = { viewModel.loadTagsIfNeeded(type) },
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
                    onOpenDialog(
                        PanoDialog.TagInfo(it)
                    )
                },
                onUserTagAdd = {
                    viewModel.addTag(type, it)
                },
                onUserTagDelete = {
                    viewModel.deleteTag(type, it)
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
                                MusicEntryImageReq(entry, isHeroImage = true)
                            },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        if (entry is Album) {
                            if (entry.tracks?.track != null) {
                                InfoTrackList(
                                    tracks = entry.tracks.track,
                                    onTrackClick = {
                                        onOpenDialog(
                                            PanoDialog.MusicEntryInfo(
                                                track = it,
                                                appId = appId,
                                                user = user
                                            )
                                        )
                                    },
                                )
                            }
                        } else {
                            EntriesRow(
                                title = stringResource(Res.string.top_tracks),
                                entries = artistTopTracks,
                                fetchAlbumImageIfMissing = true,
                                showArtists = false,
                                headerIcon = Icons.Outlined.MusicNote,
                                emptyStringRes = Res.string.not_found,
                                placeholderItem = remember(type) {
                                    getMusicEntryPlaceholderItem(type)
                                },
                                onHeaderClick = {
                                    onNavigate(
                                        PanoRoute.MusicEntryInfoPager(
                                            artist = entry as Artist,
                                            appId = appId,
                                            user = user,
                                            type = Stuff.TYPE_TRACKS
                                        )
                                    )
                                },
                                onItemClick = onHorizontalEntryItemClick,
                            )

                            EntriesRow(
                                title = stringResource(Res.string.top_albums),
                                entries = artistTopAlbums,
                                fetchAlbumImageIfMissing = false,
                                showArtists = false,
                                headerIcon = Icons.Outlined.Album,
                                placeholderItem = remember(type) {
                                    getMusicEntryPlaceholderItem(type)
                                },
                                emptyStringRes = Res.string.not_found,
                                onHeaderClick = {
                                    onNavigate(
                                        PanoRoute.MusicEntryInfoPager(
                                            artist = entry as Artist,
                                            appId = appId,
                                            user = user,
                                            type = Stuff.TYPE_ALBUMS
                                        )
                                    )
                                },
                                onItemClick = onHorizontalEntryItemClick,
                            )

                            EntriesRow(
                                title = stringResource(Res.string.similar_artists),
                                entries = similarArtists,
                                fetchAlbumImageIfMissing = false,
                                showArtists = true,
                                headerIcon = Icons.Outlined.Mic,
                                emptyStringRes = Res.string.not_found,
                                placeholderItem = remember(type) {
                                    getMusicEntryPlaceholderItem(type, showScrobbleCount = false)
                                },
                                onHeaderClick = {
                                    onNavigate(
                                        PanoRoute.MusicEntryInfoPager(
                                            artist = entry as Artist,
                                            appId = appId,
                                            user = user,
                                            type = Stuff.TYPE_ARTISTS
                                        )
                                    )
                                },
                                onItemClick = onHorizontalEntryItemClick,
                            )
                        }

                    } else if (entry is Track) {
                        EntriesRow(
                            title = stringResource(Res.string.similar_tracks),
                            entries = similarTracks,
                            fetchAlbumImageIfMissing = true,
                            showArtists = true,
                            headerIcon = Icons.Outlined.MusicNote,
                            emptyStringRes = Res.string.not_found,
                            placeholderItem = remember(type) {
                                getMusicEntryPlaceholderItem(type)
                            },
                            onHeaderClick = {
                                onNavigate(
                                    PanoRoute.SimilarTracks(
                                        track = entry,
                                        appId = appId,
                                        user = user
                                    )
                                )
                            },
                            onItemClick = onHorizontalEntryItemClick,
                        )
                    }
                }
            }

            InfoCountsForMusicEntry(
                entry = entry,
                user = user,
                onNavigate = onNavigate,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            entry.wiki?.content?.let {
                InfoWikiText(
                    text = it,
                    maxLinesWhenCollapsed = 2,
                    expanded = expandedWikiType == type,
                    onExpandToggle = {
                        expandedWikiType = if (expandedWikiType == type) -1 else type
                    },
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
        error = placeholderImageVectorPainter(entry, Icons.Outlined.ImageNotSupported),
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
                        track = entry,
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
    showUserTagsButton: Boolean,
    onUserTagsClick: () -> Unit,
    isLoved: Boolean?,
    onLoveClick: (() -> Unit)?,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    Row(modifier = modifier.fillMaxWidth()) {
        if (showUserTagsButton) {
            IconButtonWithTooltip(
                icon = PanoIcons.UserTag,
                onClick = onUserTagsClick,
                contentDescription = stringResource(Res.string.my_tags),
            )
        }

        if (entry is Track && onLoveClick != null && isLoved != null) {
            IconButtonWithTooltip(
                enabled = user.isSelf,
                onClick = onLoveClick,
                icon = if (isLoved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLoved)
                    stringResource(Res.string.unlove)
                else if (user.isSelf)
                    stringResource(Res.string.love)
                else
                    stringResource(Res.string.user_loved, user.name),
                modifier = if (user.isSelf) Modifier else Modifier.alpha(0.5f)
            )
        }

        if (entry is Album || entry is Artist) {
            IconButtonWithTooltip(
                onClick = {
                    onNavigate(
                        PanoRoute.ImageSearch(
                            artist = entry as? Artist,
                            originalArtist = originalEntry as? Artist,
                            album = entry as? Album,
                            originalAlbum = originalEntry as? Album
                        )
                    )
                },
                icon = Icons.Outlined.AddPhotoAlternate,
                contentDescription = stringResource(Res.string.add_photo),
            )
        }

        IconButtonWithTooltip(
            onClick = {
                scope.launch {
                    PlatformStuff.launchSearchIntent(entry, appId)
                }
            },
            icon = Icons.Outlined.Search,
            contentDescription = stringResource(Res.string.search),
        )

        IconButtonWithTooltip(
            onClick = {
                val text = when (entry) {
                    is Track -> entry.artist.name + " " + entry.name
                    is Album -> entry.artist?.name.orEmpty() + " " + entry.name
                    is Artist -> entry.name
                }
                PlatformStuff.copyToClipboard(text)
            },
            icon = Icons.Outlined.ContentCopy,
            contentDescription = stringResource(Res.string.copy),
        )

        if (entry.url != null && !PlatformStuff.isTv) {
            IconButtonWithTooltip(
                onClick = {
                    PlatformStuff.openInBrowser(entry.url!!)
                },
                icon = Icons.Outlined.OpenInBrowser,
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
                        imageVector = Icons.Outlined.Close,
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
            OutlinedTextField(
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

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun TrackFeaturesPlot(
    trackWithFeatures: TrackWithFeatures,
    modifier: Modifier = Modifier,
) {
    val features = trackWithFeatures.features ?: return

    val categories = listOf(
        stringResource(Res.string.acoustic),
        stringResource(Res.string.danceable),
        stringResource(Res.string.energetic),
        stringResource(Res.string.instrumental),
        stringResource(Res.string.valence),
    )

    val values = remember {
        listOf(
            features.acousticness,
            features.danceability,
            features.energy,
            features.instrumentalness,
            features.valence,
        )
    }

    val data = remember(trackWithFeatures) {
        values.mapIndexed { index, value ->
            DefaultPolarPoint(value, categories[index])
        }
    }

    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        ChartLayout(
            legendLocation = LegendLocation.NONE,
            modifier = modifier
                .size(300.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            val angularAxisGridLineStyle =
                LineStyle(SolidColor(Color.LightGray), strokeWidth = 1.dp)

            PolarGraph(
                rememberFloatRadialAxisModel(listOf(0f, 0.5f, 1f)),
                rememberCategoryAngularAxisModel(categories),
                radialAxisLabels = { },
                angularAxisLabels = {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                polarGraphProperties = PolarGraphDefaults.PolarGraphPropertyDefaults()
                    .copy(
                        radialGridType = RadialGridType.LINES,
                        angularAxisGridLineStyle = angularAxisGridLineStyle,
                        radialAxisGridLineStyle = angularAxisGridLineStyle
                    )
            ) {
                PolarPlotSeries(
                    data = data,
                    lineStyle = LineStyle(
                        brush = SolidColor(MaterialTheme.colorScheme.secondary),
                        strokeWidth = 1.5.dp
                    ),
                    areaStyle = AreaStyle(
                        brush = SolidColor(MaterialTheme.colorScheme.secondary),
                        alpha = 0.3f
                    ),
                    symbols = {
                        Symbol(
                            shape = CircleShape,
                            fillBrush = SolidColor(MaterialTheme.colorScheme.tertiary)
                        )
                    }
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    progress = { trackWithFeatures.track.popularity.toFloat() / 100 },
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(
                        Res.string.popularity,
                        trackWithFeatures.track.popularity
                    ),
                )
            }
            trackWithFeatures.track.getReleaseDateDate()?.time?.let {
                TextWithIcon(
                    icon = Icons.Outlined.CalendarToday,
                    text = dateFormat.format(it),
                )
            }

            TextWithIcon(
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                text = String.format(Locale.getDefault(), "%.2f dB", features.loudness),
            )

            features.getKeyString()?.let {
                TextWithIcon(
                    icon = Icons.Outlined.Piano,
                    text = it,
                )
            }

            TextWithIcon(
                icon = PanoIcons.Metronome,
                text = "${features.tempo.roundToInt()} bpm • ${features.time_signature}/4",
            )

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