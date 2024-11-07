package com.arn.scrobble.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Piano
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.spotify.TrackWithFeatures
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.BottomSheetDialogParent
import com.arn.scrobble.ui.EntriesHorizontal
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.MusicEntryLoaderInput
import com.arn.scrobble.ui.TextWithIcon
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.valentinilk.shimmer.shimmer
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
import java.text.DateFormat
import kotlin.math.roundToInt

@Composable
private fun InfoContent(
    musicEntry: MusicEntry,
    pkgName: String?,
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: InfoVM = viewModel(),
    modifier: Modifier = Modifier
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

    val trackWithFeatures by viewModel.spotifyTrackWithFeatures.collectAsStateWithLifecycle()
    val trackFeaturesLoaded by viewModel.trackFeaturesLoaded.collectAsStateWithLifecycle()
    val similarTracks by viewModel.similarTracksVM.entries.collectAsStateWithLifecycle()
    val similarTracksLoaded by viewModel.similarTracksVM.hasLoaded.collectAsStateWithLifecycle()

    val artistTopTracks by viewModel.artistTopTracksVM.entries.collectAsStateWithLifecycle()
    val artistTopTracksLoaded by viewModel.artistTopTracksVM.hasLoaded.collectAsStateWithLifecycle()
    val artistTopAlbums by viewModel.artistTopAlbumsVM.entries.collectAsStateWithLifecycle()
    val artistTopAlbumsLoaded by viewModel.artistTopAlbumsVM.hasLoaded.collectAsStateWithLifecycle()
    val similarArtists by viewModel.similarArtistsVM.entries.collectAsStateWithLifecycle()
    val similarArtistsLoaded by viewModel.similarArtistsVM.hasLoaded.collectAsStateWithLifecycle()

    val entriesForShimmer = remember {
        (0..4).map { Artist(" ", listeners = it) }
    }

    val context = LocalContext.current

    val onHorizontalEntryItemClick: (MusicEntry) -> Unit = {
        onNavigate(
            PanoRoute.MusicEntryInfo(
                musicEntry = it,
                pkgName = pkgName,
                user = user
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.setMusicEntryIfNeeded(musicEntry, user.name)
    }

    LaunchedEffect(infoMap) {
        if (infoMap != null) {
            isLoved = (infoMap?.get(Stuff.TYPE_TRACKS) as? Track)?.userloved
        }
    }

    LaunchedEffect(expandedHeaderType) {
        val expandedEntry = infoMap?.get(expandedHeaderType) ?: return@LaunchedEffect
        val input = MusicEntryLoaderInput(
            user = user,
            type = -1,
            entry = expandedEntry,
            timePeriod = null,
            page = 1
        )
        when (expandedHeaderType) {
            Stuff.TYPE_TRACKS -> {
                viewModel.loadTrackFeaturesIfNeeded()
                viewModel.similarTracksVM.setInput(input.copy(type = Stuff.TYPE_TRACKS))
            }

            Stuff.TYPE_ARTISTS -> {
                viewModel.artistTopTracksVM.setInput(input.copy(type = Stuff.TYPE_TRACKS))
                viewModel.artistTopAlbumsVM.setInput(input.copy(type = Stuff.TYPE_ALBUMS))
                viewModel.similarArtistsVM.setInput(input.copy(type = Stuff.TYPE_ARTISTS))
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .then(
                if (infoLoaded)
                    Modifier
                else
                    Modifier.shimmer()
            )
    ) {

        allTypes.forEach { type ->
            infoMap?.get(type)?.let { entry ->

                val imgRequest = remember {
                    ImageRequest.Builder(context)
                        .data(MusicEntryImageReq(entry, fetchAlbumInfoIfMissing = true))
                        .error(R.drawable.vd_no_image)
                        .build()
                }

                InfoSimpleHeader(
                    text = entry.name,
                    icon = getMusicEntryIcon(type),
                    onClick = { expandedHeaderType = if (expandedHeaderType == type) -1 else type },
                    leadingContent = {
                        AnimatedVisibility(expandedHeaderType != type) {
                            if (entry is Album || entry is Artist) {
                                AsyncImage(
                                    model = imgRequest,
                                    contentDescription = when (entry) {
                                        is Album -> stringResource(R.string.album_art)
                                        is Artist -> stringResource(R.string.artist_image)
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
                            contentDescription = stringResource(if (expandedHeaderType == type) R.string.collapse else R.string.expand),
                        )
                    },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                InfoActionsRow(
                    entry = entry,
                    originalEntry = viewModel.originalEntriesMap[type],
                    pkgName = pkgName,
                    user = user,
                    showUserTagsButton = userTags[type] == null,
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
                        onNavigate(
                            PanoRoute.TagInfo(it)
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
                    ) {
                        if (entry is Album || entry is Artist) {
                            InfoBigPicture(
                                entry = entry,
                                imgRequest = imgRequest,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )

                            if (entry is Album) {
                                if (entry.tracks?.track != null) {
                                    InfoTrackList(
                                        tracks = entry.tracks.track,
                                        onTrackClick = {
                                            onNavigate(
                                                PanoRoute.MusicEntryInfo(
                                                    musicEntry = it,
                                                    pkgName = pkgName,
                                                    user = user
                                                )
                                            )
                                        },
                                    )
                                }
                            } else {
                                EntriesHorizontal(
                                    title = stringResource(R.string.top_tracks),
                                    entries = artistTopTracks ?: entriesForShimmer,
                                    showArtists = false,
                                    shimmer = !artistTopTracksLoaded,
                                    headerIcon = Icons.Outlined.MusicNote,
                                    emptyStringRes = R.string.not_found,
                                    onHeaderClick = {
                                        // todo implement
//                                        val args = Bundle().putData(entry)
//                                        onNavigate(R.id.infoExtraFullFragment, args)
                                    },
                                    onItemClick = onHorizontalEntryItemClick,
                                )

                                EntriesHorizontal(
                                    title = stringResource(R.string.top_albums),
                                    entries = artistTopAlbums ?: entriesForShimmer,
                                    showArtists = false,
                                    shimmer = !artistTopAlbumsLoaded,
                                    headerIcon = Icons.Outlined.Album,
                                    emptyStringRes = R.string.not_found,
                                    onHeaderClick = {
                                        // todo implement
                                    },
                                    onItemClick = onHorizontalEntryItemClick,
                                )

                                EntriesHorizontal(
                                    title = stringResource(R.string.similar_artists),
                                    entries = similarArtists ?: entriesForShimmer,
                                    showArtists = true,
                                    shimmer = !similarArtistsLoaded,
                                    headerIcon = Icons.Outlined.Mic,
                                    emptyStringRes = R.string.not_found,
                                    onHeaderClick = {
                                        // todo implement
                                    },
                                    onItemClick = onHorizontalEntryItemClick,
                                )
                            }

                        } else if (entry is Track) {
                            if (entry.duration != null && entry.duration > 0) {
                                Text(
                                    text = Stuff.humanReadableDuration(entry.duration),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .padding(horizontal = 48.dp)
                                )
                            }

                            if (trackFeaturesLoaded && trackWithFeatures != null) {
                                TrackFeaturesPlot(
                                    trackWithFeatures = trackWithFeatures!!,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }

                            if (!trackFeaturesLoaded) {
                                Box(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .align(Alignment.CenterHorizontally)
                                        .clip(MaterialTheme.shapes.extraLarge)
                                        .shimmer()
                                        .backgroundForShimmer(true)
                                )
                            }

                            EntriesHorizontal(
                                title = stringResource(R.string.similar_tracks),
                                entries = similarTracks ?: entriesForShimmer,
                                shimmer = !similarTracksLoaded,
                                showArtists = true,
                                headerIcon = Icons.Outlined.MusicNote,
                                emptyStringRes = R.string.not_found,
                                onHeaderClick = {
                                    // todo implement
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
    imgRequest: ImageRequest,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = imgRequest,
        contentDescription = when (entry) {
            is Album -> stringResource(R.string.album_art)
            is Artist -> stringResource(R.string.artist_image)
            else -> null
        },
        modifier = modifier
            .height(300.dp)
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
    modifier: Modifier = Modifier
) {
    InfoCounts(
        countPairs = listOfNotNull(
            if (entry.playcount != null) {
                stringResource(
                    if (user.isSelf)
                        R.string.my_scrobbles
                    else
                        R.string.their_scrobbles
                ) to entry.userplaycount
            } else {
                null
            },
            stringResource(R.string.listeners) to entry.listeners,
            stringResource(R.string.scrobbles) to entry.playcount,
        ),
        avatarUrl = user.largeImage.takeIf { !user.isSelf && it.isNotEmpty() },
        onClickFirstItem = if (entry.userplaycount != null && entry.userplaycount!! > 0) {
            {
                when (entry) {
                    is Track -> {
                        onNavigate(
                            PanoRoute.TrackHistory(
                                track = entry,
                                user = user
                            )
                        )
                    }

                    else -> {
                        val _username = user.name
                        entry.url
                            ?.replace("/music/", "/user/$_username/library/music/")
                            ?.let {
                                Stuff.openInBrowser(it)
                            }
                    }
                }
            }
        } else null,
        modifier = modifier
    )
}

@Composable
private fun InfoActionsRow(
    entry: MusicEntry,
    originalEntry: MusicEntry?,
    pkgName: String?,
    user: UserCached,
    showUserTagsButton: Boolean,
    onUserTagsClick: () -> Unit,
    isLoved: Boolean?,
    onLoveClick: (() -> Unit)?,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        if (showUserTagsButton) {
            IconButtonWithTooltip(
                icon = ImageVector.vectorResource(R.drawable.vd_user_tag),
                onClick = onUserTagsClick,
                contentDescription = stringResource(R.string.my_tags),
            )
        }

        if (entry is Track && onLoveClick != null && isLoved != null) {
            IconButtonWithTooltip(
                enabled = user.isSelf,
                onClick = onLoveClick,
                icon = if (isLoved == true) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLoved == true)
                    stringResource(R.string.unlove)
                else if (user.isSelf)
                    stringResource(R.string.love)
                else
                    stringResource(R.string.user_loved, user.name),
                modifier = if (user.isSelf) Modifier else Modifier.alpha(0.5f)
            )
        }

        if (entry is Album || entry is Artist) {
            IconButtonWithTooltip(
                onClick = {
                    onNavigate(
                        PanoRoute.ImageSearch(
                            musicEntry = entry,
                            originalMusicEntry = originalEntry
                        )
                    )
                },
                icon = Icons.Outlined.AddPhotoAlternate,
                contentDescription = stringResource(R.string.add_photo),
            )
        }

        IconButtonWithTooltip(
            onClick = {
                Stuff.launchSearchIntent(entry, pkgName)
            },
            icon = ImageVector.vectorResource(R.drawable.vd_search_play),
            contentDescription = stringResource(R.string.search),
        )

        if (entry.url != null && !Stuff.isTv) {
            IconButtonWithTooltip(
                onClick = {
                    Stuff.openInBrowser(entry.url!!)
                },
                icon = Icons.Outlined.OpenInBrowser,
                contentDescription = stringResource(R.string.more_info),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.InfoTags(
    tags: List<Tag>,
    userTags: Iterable<String>?,
    userTagsHistory: List<String>,
    onTagClick: (Tag) -> Unit,
    onUserTagAdd: (String) -> Unit,
    onUserTagDelete: (String) -> Unit,
    modifier: Modifier = Modifier
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
                        contentDescription = stringResource(R.string.delete),
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
                label = { Text(stringResource(R.string.user_tags_hint)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownShown)
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onUserTagAdd(userTagInput)
                        userTagInput = ""
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

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalLayoutApi::class)
@Composable
@Suppress("MagicNumber")
private fun TrackFeaturesPlot(
    trackWithFeatures: TrackWithFeatures,
    modifier: Modifier = Modifier
) {
    val features = trackWithFeatures.features ?: return

    val categories = listOf(
        stringResource(R.string.acoustic),
        stringResource(R.string.danceable),
        stringResource(R.string.energetic),
        stringResource(R.string.instrumental),
        stringResource(R.string.valence),
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

    val data = remember {
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
                    text = stringResource(R.string.popularity, trackWithFeatures.track.popularity),
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
                text = String.format("%.2f dB", features.loudness),
            )

            features.getKeyString()?.let {
                TextWithIcon(
                    icon = Icons.Outlined.Piano,
                    text = it,
                )
            }

            TextWithIcon(
                icon = ImageVector.vectorResource(R.drawable.vd_metronome),
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
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) {
        return
    }

    val totalDuration = remember { tracks.sumOf { it.duration ?: 0 } }
    val durationsMissing = remember { tracks.any { it.duration == null } }
    val durationsString = remember {
        totalDuration.takeIf { it > 0 }
            ?.let { Stuff.humanReadableDuration(it) + if (durationsMissing) "+" else "" }
    }

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier
    ) {
        item("summary") {
            Text(
                text = pluralStringResource(
                    R.plurals.num_tracks,
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


@Composable
fun MusicEntryInfoScreen(
    musicEntry: MusicEntry,
    pkgName: String?,
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit
) {
    BottomSheetDialogParent(padding = false) {
        InfoContent(
            musicEntry = musicEntry,
            pkgName = pkgName,
            user = user,
            onNavigate = onNavigate,
            modifier = it,
        )
    }
}