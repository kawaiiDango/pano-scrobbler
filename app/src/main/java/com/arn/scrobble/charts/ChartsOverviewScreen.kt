package com.arn.scrobble.charts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.kumo.compat.MyKBitmap
import com.arn.scrobble.kumo.compat.MyKGraphicsFactory
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.EntriesHorizontal
import com.arn.scrobble.ui.ExpandableHeaderMenu
import com.arn.scrobble.ui.TextHeaderItem
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.sp
import com.kennycason.kumo.CollisionMode
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.WordFrequency
import com.kennycason.kumo.bg.CircleBackground
import com.kennycason.kumo.compat.KumoRect
import com.kennycason.kumo.font.scale.LinearFontScalar
import com.kennycason.kumo.image.AngleGenerator
import com.kennycason.kumo.palette.LinearGradientColorPalette
import com.valentinilk.shimmer.shimmer
import io.github.koalaplot.core.bar.DefaultVerticalBar
import io.github.koalaplot.core.bar.VerticalBarPlot
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Composable
fun ChartsOverviewScreen(
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    onTitleChange: (String?) -> Unit,
    viewModel: ChartsVM = viewModel(),
    chartsPeriodViewModel: ChartsPeriodVM = viewModel(),
    modifier: Modifier = Modifier,
) {
    val artists = viewModel.artists.collectAsLazyPagingItems()
    val albums = viewModel.albums.collectAsLazyPagingItems()
    val tracks = viewModel.tracks.collectAsLazyPagingItems()

    val artistsCount by viewModel.artistCount.collectAsStateWithLifecycle()
    val albumsCount by viewModel.albumCount.collectAsStateWithLifecycle()
    val tracksCount by viewModel.trackCount.collectAsStateWithLifecycle()

    val tagCloud by viewModel.tagCloud.collectAsStateWithLifecycle()
    val listeningActivity by viewModel.listeningActivity.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    val tagCloudVisible by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.any { it.key == "tagCloud" }
        }
    }

    val listeningActivityVisible by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.any { it.key == "listeningActivity" }
        }
    }

    val isTimePeriodContinuous by chartsPeriodViewModel.selectedPeriod.map { it?.period != null }
        .collectAsStateWithLifecycle(false)


    LaunchedEffect(Unit) {
        onTitleChange(PlatformStuff.application.getString(R.string.charts))
    }

    fun setInput(timePeriod: TimePeriod, prevTimePeriod: TimePeriod?) {
        viewModel.reset()
        viewModel.setChartsInput(
            ChartsLoaderInput(
                username = user.name,
                timePeriod = timePeriod,
                prevPeriod = prevTimePeriod,
                firstPageOnly = true
            )
        )
    }

    LaunchedEffect(Unit) {
        onTitleChange(PlatformStuff.application.getString(R.string.charts))
    }

    LaunchedEffect(artists.loadState.refresh, tagCloudVisible, listeningActivityVisible) {
        if (artists.loadState.refresh !is LoadState.Loading) {
            if (tagCloudVisible && tagCloud == null) {
                viewModel.loadTagCloud(artists.itemSnapshotList.items)
            }

            if (listeningActivityVisible && listeningActivity == null) {
                val timePeriod = chartsPeriodViewModel.selectedPeriod.value ?: return@LaunchedEffect
                viewModel.loadListeningActivity(user, artists.itemSnapshotList.items, timePeriod)
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = panoContentPadding(sides = false),
        modifier = modifier
    ) {
        stickyHeader("selector") {
            TimePeriodSelector(
                user = user,
                viewModel = chartsPeriodViewModel,
                onSelected = ::setInput,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item("artists") {
            EntriesHorizontal(
                title = remember(artistsCount) {
                    getMusicEntryQString(
                        R.string.artists,
                        R.plurals.num_artists,
                        artistsCount,
                        isTimePeriodContinuous
                    )
                },
                entries = artists,
                fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                showArtists = true,
                headerIcon = Icons.Outlined.Mic,
                emptyStringRes = R.string.charts_no_data,
                onHeaderClick = {
                    onNavigate(
                        PanoRoute.ChartsPager(
                            user = user,
                            type = Stuff.TYPE_ARTISTS
                        )
                    )
                },
                placeholderItem = remember {
                    getMusicEntryPlaceholderItem(Stuff.TYPE_ARTISTS)
                },
                onItemClick = {
                    onNavigate(
                        PanoRoute.MusicEntryInfo(
                            artist = it as Artist,
                            user = user,
                            pkgName = null
                        )
                    )
                },
            )
        }

        item("albums") {
            EntriesHorizontal(
                title = remember(albumsCount) {
                    getMusicEntryQString(
                        R.string.albums,
                        R.plurals.num_albums,
                        albumsCount,
                        isTimePeriodContinuous
                    )
                },
                entries = albums,
                fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                showArtists = true,
                headerIcon = Icons.Outlined.Album,
                emptyStringRes = R.string.charts_no_data,
                onHeaderClick = {
                    onNavigate(
                        PanoRoute.ChartsPager(
                            user = user,
                            type = Stuff.TYPE_ALBUMS
                        )
                    )
                },
                placeholderItem = remember {
                    getMusicEntryPlaceholderItem(Stuff.TYPE_ALBUMS)
                },
                onItemClick = {
                    onNavigate(
                        PanoRoute.MusicEntryInfo(
                            album = it as Album,
                            user = user,
                            pkgName = null
                        )
                    )
                },
            )
        }

        item("tracks") {
            EntriesHorizontal(
                title = remember(tracksCount) {
                    getMusicEntryQString(
                        R.string.tracks,
                        R.plurals.num_tracks,
                        tracksCount,
                        isTimePeriodContinuous
                    )
                },
                entries = tracks,
                fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                showArtists = true,
                headerIcon = Icons.Outlined.MusicNote,
                emptyStringRes = R.string.charts_no_data,
                onHeaderClick = {
                    onNavigate(
                        PanoRoute.ChartsPager(
                            user = user,
                            type = Stuff.TYPE_TRACKS
                        )
                    )
                },
                placeholderItem = remember {
                    getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS)
                },
                onItemClick = {
                    onNavigate(
                        PanoRoute.MusicEntryInfo(
                            track = it as Track,
                            user = user,
                            pkgName = null
                        )
                    )
                },
            )
        }

        if (!Stuff.isTv) {
            item("createCollage") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontalOverscanPadding())
                ) {
                    ButtonWithIcon(
                        onClick = {
                            onNavigate(
                                PanoRoute.CollageGenerator(
                                    collageType = Stuff.TYPE_ALL,
                                    user = user,
                                    timePeriod = chartsPeriodViewModel.selectedPeriod.value
                                        ?: return@ButtonWithIcon
                                )
                            )
                        },
                        icon = Icons.Outlined.GridView,
                        text = stringResource(R.string.create_collage),
                    )
                }
            }
        }

        item("listeningActivity") {
            ListeningActivity(
                listeningActivity = listeningActivity,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontalOverscanPadding())
            )
        }

        item("tagCloud") {
            TagCloud(
                tagCloud = tagCloud,
                onHeaderMenuClick = {
                    onNavigate(PanoRoute.HiddenTags)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontalOverscanPadding())
            )
        }

    }
}

@Composable
private fun TagCloud(
    tagCloud: Map<String, Float>?,
    onHeaderMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var kumoBitmap by remember { mutableStateOf<MyKBitmap?>(null) }
    val tintColor = MaterialTheme.colorScheme.secondary
    val foregroundColor = MaterialTheme.colorScheme.onSurface
    var tagCloudSizePx by remember { mutableIntStateOf(0) }
    val isLoading by remember(tagCloud, kumoBitmap) {
        mutableStateOf(tagCloud == null || kumoBitmap == null && tagCloud.isNotEmpty())
    }

    LaunchedEffect(tagCloud) {
        withContext(Dispatchers.Default) {
            tagCloud?.let {
                if (tagCloudSizePx > 0 && tagCloud.isNotEmpty()) {
                    kumoBitmap = generateTagCloud(
                        tagCloud,
                        tagCloudSizePx,
                        tintColor,
                        foregroundColor
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
    ) {
        ExpandableHeaderMenu(
            title = stringResource(R.string.tag_cloud),
            icon = Icons.Outlined.Tag,
            menuItemText = stringResource(R.string.hidden_tags),
            onMenuItemClick = onHeaderMenuClick,
            modifier = Modifier
                .fillMaxWidth()
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 370.dp)
                .aspectRatio(1f)
                .onGloballyPositioned { coordinates ->
                    tagCloudSizePx = coordinates.size.width
                }
                .then(
                    if (isLoading) Modifier
                        .backgroundForShimmer(true, shape = CircleShape)
                        .shimmer() else Modifier
                )
        ) {
            EmptyText(
                visible = tagCloud?.isEmpty() == true,
                text = stringResource(R.string.not_enough_data),
            )
            kumoBitmap?.let {
                Image(
                    bitmap = it.convertTo() as ImageBitmap,
                    contentDescription = stringResource(R.string.tag_cloud),
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
private fun ListeningActivity(
    listeningActivity: Map<TimePeriod, Int>?,
    modifier: Modifier = Modifier,
) {
    val isLoading by remember(listeningActivity) { mutableStateOf(listeningActivity == null) }
    val xLabels by remember(listeningActivity) {
        mutableStateOf(listeningActivity?.keys?.map { it.name } ?: emptyList<String>())
    }
    val yValues by remember(listeningActivity) {
        mutableStateOf(listeningActivity?.values?.map { it.toFloat() } ?: emptyList<Float>())
    }

    val yValuesMax by remember(yValues) { mutableFloatStateOf(yValues.maxOrNull() ?: 0f) }

    val tintColor = MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
    ) {
        TextHeaderItem(
            title = stringResource(R.string.listening_activity),
            icon = Icons.Outlined.BarChart,
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .then(
                    if (isLoading) Modifier
                        .shimmer()
                        .backgroundForShimmer(true, shape = MaterialTheme.shapes.extraLarge)
                    else Modifier
                )
        ) {
            EmptyText(
                visible = listeningActivity?.isEmpty() == true,
                text = stringResource(R.string.charts_no_data),
            )

            listeningActivity?.let { listeningActivity ->
                if (listeningActivity.isNotEmpty()) {
                    XYGraph(
                        xAxisModel = remember { CategoryAxisModel(xLabels) },
                        yAxisModel = rememberFloatLinearAxisModel(
                            range = 0f..yValuesMax,
                            minViewExtent = 1f,
                            maxViewExtent = yValuesMax,
                            minorTickCount = 0,
                        ),
                        yAxisTitle = stringResource(R.string.scrobbles),
                        yAxisLabels = { it.toInt().toString() },
                        xAxisLabels = { it.toString().take(2) },
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        VerticalBarPlot(
                            xData = xLabels,
                            yData = yValues,
                            bar = {
                                DefaultVerticalBar(
                                    SolidColor(tintColor),
                                    shape = MaterialTheme.shapes.small.copy(
                                        bottomEnd = CornerSize(0.dp),
                                        bottomStart = CornerSize(0.dp)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun generateTagCloud(
    weights: Map<String, Float>,
    dimensionPx: Int,
    tintColor: Color,
    foregroundPureColor: Color,
): MyKBitmap {
    val wordFrequenciesFetched = weights.map { (tag, size) ->
        WordFrequency(tag, size.toInt())
    }

    val dimension = KumoRect(0, 0, dimensionPx, dimensionPx)

    val palette = LinearGradientColorPalette(
        tintColor.toArgb(),
        foregroundPureColor.toArgb(),
        wordFrequenciesFetched.size - 2
    )

    return WordCloud(
        dimension,
        CollisionMode.PIXEL_PERFECT,
        MyKGraphicsFactory,
    ).apply {
        setBackground(CircleBackground(dimensionPx / 2))
        setBackgroundColor(Color.Transparent.toArgb())
        setColorPalette(palette)
        setAngleGenerator(AngleGenerator(0))
        setPadding(4.sp)
        setFontScalar(LinearFontScalar(12.sp, 48.sp))
        build(wordFrequenciesFetched)
    }.bufferedImage as MyKBitmap
}