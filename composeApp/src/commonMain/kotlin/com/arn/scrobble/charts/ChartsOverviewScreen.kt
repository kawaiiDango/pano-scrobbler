package com.arn.scrobble.charts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.graphics.KumoRect
import com.arn.scrobble.graphics.toImageBitmap
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.EntriesRow
import com.arn.scrobble.ui.ExpandableHeaderMenu
import com.arn.scrobble.ui.OptionalHorizontalScrollbar
import com.arn.scrobble.ui.TextHeaderItem
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.WordFrequency
import com.kennycason.kumo.bg.CircleBackground
import com.kennycason.kumo.palette.LinearGradientColorPalette
import com.kennycason.kumo.scale.LinearFontScalar
import io.github.koalaplot.core.bar.DefaultVerticalBar
import io.github.koalaplot.core.bar.VerticalBarPlot
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.charts
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.charts_no_data
import pano_scrobbler.composeapp.generated.resources.create_collage
import pano_scrobbler.composeapp.generated.resources.days
import pano_scrobbler.composeapp.generated.resources.hidden_tags
import pano_scrobbler.composeapp.generated.resources.listening_activity
import pano_scrobbler.composeapp.generated.resources.months
import pano_scrobbler.composeapp.generated.resources.not_enough_data
import pano_scrobbler.composeapp.generated.resources.num_albums
import pano_scrobbler.composeapp.generated.resources.num_artists
import pano_scrobbler.composeapp.generated.resources.num_tracks
import pano_scrobbler.composeapp.generated.resources.scrobbles
import pano_scrobbler.composeapp.generated.resources.tag_cloud
import pano_scrobbler.composeapp.generated.resources.tracks
import pano_scrobbler.composeapp.generated.resources.weeks
import pano_scrobbler.composeapp.generated.resources.years

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChartsOverviewScreen(
    user: UserCached,
    onNavigate: (PanoRoute) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    onTitleChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChartsVM = viewModel { ChartsVM() },
    chartsPeriodViewModel: ChartsPeriodVM = viewModel { ChartsPeriodVM() },
) {
    val artists = viewModel.artists.collectAsLazyPagingItems()
    val albums = viewModel.albums.collectAsLazyPagingItems()
    val tracks = viewModel.tracks.collectAsLazyPagingItems()

    val artistsCount by viewModel.artistCount.collectAsStateWithLifecycle()
    val albumsCount by viewModel.albumCount.collectAsStateWithLifecycle()
    val tracksCount by viewModel.trackCount.collectAsStateWithLifecycle()

    val tagCloud by viewModel.tagCloud.collectAsStateWithLifecycle()
    val listeningActivity by viewModel.listeningActivity.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    var tagCloudOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var tagCloudVisible by rememberSaveable { mutableStateOf(false) }
    var listeningActivityOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var listeningActivityVisible by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    val isTimePeriodContinuous by chartsPeriodViewModel.selectedPeriod.map { it?.lastfmPeriod != null }
        .collectAsStateWithLifecycle(false)


    LaunchedEffect(Unit) {
        onTitleChange(getString(Res.string.charts))
    }

    LaunchedEffect(scrollState.value) {
        val scrollBottomOffset = scrollState.value + scrollState.viewportSize
        val minAdditionalOffset = 96.dp.value * density.density

        tagCloudVisible = scrollBottomOffset >= tagCloudOffsetY + minAdditionalOffset
        listeningActivityVisible =
            scrollBottomOffset >= listeningActivityOffsetY + minAdditionalOffset
    }

    fun setInput(timePeriod: TimePeriod, prevTimePeriod: TimePeriod?) {
        viewModel.setChartsInput(
            ChartsLoaderInput(
                username = user.name,
                timePeriod = timePeriod,
                prevPeriod = prevTimePeriod,
                firstPageOnly = true
            )
        )
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

    Column(
        modifier = Modifier
            .padding(panoContentPadding(sides = false))
            .then(modifier)
    ) {
        TimePeriodSelector(
            user = user,
            viewModel = chartsPeriodViewModel,
            onSelected = ::setInput,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            EntriesRow(
                title = getMusicEntryQString(
                    Res.string.artists,
                    Res.plurals.num_artists,
                    artistsCount,
                    isTimePeriodContinuous
                ),
                entries = artists,
                fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                showArtists = true,
                headerIcon = Icons.Outlined.Mic,
                emptyStringRes = Res.string.charts_no_data,
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
                    onOpenDialog(
                        PanoDialog.MusicEntryInfo(
                            artist = it as Artist,
                            user = user,
                            appId = null
                        )
                    )
                },
            )

            EntriesRow(
                title = getMusicEntryQString(
                    Res.string.albums,
                    Res.plurals.num_albums,
                    albumsCount,
                    isTimePeriodContinuous
                ),
                entries = albums,
                fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                showArtists = true,
                headerIcon = Icons.Outlined.Album,
                emptyStringRes = Res.string.charts_no_data,
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
                    onOpenDialog(
                        PanoDialog.MusicEntryInfo(
                            album = it as Album,
                            user = user,
                            appId = null
                        )
                    )
                },
            )

            EntriesRow(
                title = getMusicEntryQString(
                    Res.string.tracks,
                    Res.plurals.num_tracks,
                    tracksCount,
                    isTimePeriodContinuous
                ),
                entries = tracks,
                fetchAlbumImageIfMissing = true,
                showArtists = true,
                headerIcon = Icons.Outlined.MusicNote,
                emptyStringRes = Res.string.charts_no_data,
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
                    onOpenDialog(
                        PanoDialog.MusicEntryInfo(
                            track = it as Track,
                            user = user,
                            appId = null
                        )
                    )
                },
            )


            if (!PlatformStuff.isTv) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontalOverscanPadding())
                ) {
                    ButtonWithIcon(
                        onClick = {
                            onOpenDialog(
                                PanoDialog.CollageGenerator(
                                    collageType = Stuff.TYPE_ALL,
                                    user = user,
                                    timePeriod = chartsPeriodViewModel.selectedPeriod.value
                                        ?: return@ButtonWithIcon
                                )
                            )
                        },
                        icon = Icons.Outlined.AutoAwesomeMosaic,
                        text = stringResource(Res.string.create_collage),
                    )
                }
            }

            ListeningActivityContent(
                listeningActivity = listeningActivity,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontalOverscanPadding())
                    .onGloballyPositioned { coordinates ->
                        listeningActivityOffsetY = coordinates.positionInParent().y
                    }
            )

            TagCloudContent(
                tagCloud = tagCloud,
                onHeaderMenuClick = {
                    onOpenDialog(PanoDialog.HiddenTags)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontalOverscanPadding())
                    .onGloballyPositioned { coordinates ->
                        tagCloudOffsetY = coordinates.positionInParent().y
                    }
            )

        }
    }
}

@Composable
private fun TagCloudContent(
    tagCloud: Map<String, Float>?,
    onHeaderMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var kumoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val color1 = MaterialTheme.colorScheme.inverseSurface
    val color2 = MaterialTheme.colorScheme.secondary
    var tagCloudSizePx by remember { mutableIntStateOf(0) }
    val isLoading by remember(tagCloud, kumoBitmap) {
        mutableStateOf(tagCloud == null || kumoBitmap == null && tagCloud.isNotEmpty())
    }

    val density = LocalDensity.current

    LaunchedEffect(tagCloud) {
        withContext(Dispatchers.Default) {
            tagCloud?.let {
                if (tagCloudSizePx > 0 && tagCloud.isNotEmpty()) {
                    kumoBitmap = generateTagCloud(
                        tagCloud,
                        tagCloudSizePx,
                        density.density * density.fontScale,
                        color1 = color1,
                        color2 = color2,
                        color3 = null
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
    ) {
        ExpandableHeaderMenu(
            title = stringResource(Res.string.tag_cloud),
            icon = Icons.Outlined.Tag,
            menuItemText = stringResource(Res.string.hidden_tags),
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
                        .shimmerWindowBounds() else Modifier
                )
        ) {
            EmptyText(
                visible = tagCloud?.isEmpty() == true,
                text = stringResource(Res.string.not_enough_data),
            )

            if (tagCloud?.isNotEmpty() == true) {
                kumoBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(Res.string.tag_cloud),
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListeningActivityContent(
    listeningActivity: ListeningActivity?,
    modifier: Modifier = Modifier,
) {
    val isLoading by remember(listeningActivity) { mutableStateOf(listeningActivity == null) }
    val xData by remember(listeningActivity) {
        mutableStateOf(
            listeningActivity?.timePeriodsToCounts?.keys?.toList()
                ?: emptyList()
        )
    }
    val yData by remember(listeningActivity) {
        mutableStateOf(
            listeningActivity?.timePeriodsToCounts?.values?.map { it.toFloat() }?.toList()
                ?: emptyList()
        )
    }

    val typeStringRes = when (listeningActivity?.type) {
        TimePeriodType.YEAR -> Res.string.years
        TimePeriodType.MONTH -> Res.string.months
        TimePeriodType.WEEK -> Res.string.weeks
        TimePeriodType.DAY -> Res.string.days
        else -> Res.string.charts_custom
    }

    val yValuesMax by remember(yData) { mutableFloatStateOf(yData.maxOrNull() ?: 0f) }

    val tintColor = MaterialTheme.colorScheme.secondary

    val scrollstate = rememberScrollState()
    var boxWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Column(
        modifier = modifier
    ) {
        TextHeaderItem(
            title = stringResource(Res.string.listening_activity),
            icon = Icons.Outlined.BarChart,
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .then(
                    if (isLoading) Modifier
                        .shimmerWindowBounds()
                        .backgroundForShimmer(true, shape = MaterialTheme.shapes.extraLarge)
                    else Modifier
                )
                .onSizeChanged {
                    boxWidth = with(density) {
                        it.width.toDp()
                    }
                }
                .horizontalScroll(scrollstate)
        ) {
            EmptyText(
                visible = listeningActivity?.timePeriodsToCounts?.isEmpty() == true,
                text = stringResource(Res.string.charts_no_data),
            )

            listeningActivity?.let { listeningActivity ->
                if (listeningActivity.timePeriodsToCounts.isNotEmpty() && boxWidth > 0.dp) {
                    XYGraph(
                        xAxisModel = remember(xData) { CategoryAxisModel(xData) },
                        yAxisModel = rememberFloatLinearAxisModel(
                            range = 0f..1.2f * yValuesMax,
                            minViewExtent = 1f,
                            maxViewExtent = 1.2f * yValuesMax,
                            minorTickCount = 0,
                        ),
                        yAxisTitle = stringResource(Res.string.scrobbles),
                        yAxisLabels = { it.toInt().toString() },
                        xAxisTitle = stringResource(typeStringRes),
                        xAxisLabels = { it.name },
                        modifier = Modifier
                            .width(max(boxWidth, (xData.size * 24).dp))
                    ) {
                        VerticalBarPlot(
                            xData = xData,
                            yData = yData,
                            bar = {
                                val currentYValue = yData[it]
                                val fontSizeDp = with(density) {
                                    MaterialTheme.typography.labelSmall.fontSize.toDp()
                                }

                                DefaultVerticalBar(
                                    color = tintColor.copy(
                                        alpha = 0.5f + 0.5f * (currentYValue / yValuesMax)
                                    ),
                                    shape = MaterialTheme.shapes.small.copy(
                                        bottomEnd = CornerSize(0.dp),
                                        bottomStart = CornerSize(0.dp)
                                    ),
                                )

                                Text(
                                    currentYValue.toInt().toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    modifier = Modifier.align(Alignment.TopCenter)
                                        .offset(y = -fontSizeDp * 2)
                                        .background(
                                            color = MaterialTheme.colorScheme.background,
                                            shape = MaterialTheme.shapes.small
                                        )
                                )

                            }
                        )
                    }
                }
            }
        }
        OptionalHorizontalScrollbar(scrollstate)
    }
}

private fun generateTagCloud(
    weights: Map<String, Float>,
    dimensionPx: Int,
    fontScale: Float,
    color1: Color,
    color2: Color,
    color3: Color?,
): ImageBitmap {
    fun TextUnit.toPx() = (this.value * fontScale).toInt()

    val wordFrequenciesFetched = weights.map { (tag, size) ->
        WordFrequency(tag, size.toInt())
    }

    val dimension = KumoRect(dimensionPx, dimensionPx)

    val palette = LinearGradientColorPalette(
        color1.toArgb(),
        color2.toArgb(),
        color3?.toArgb(),
        gradientSteps = wordFrequenciesFetched.size - 2
    )

    return WordCloud(
        dimension = dimension,
        background = CircleBackground(dimensionPx / 2),
        padding = 4.sp.toPx(),
        fontScalar = LinearFontScalar(12.sp.toPx(), 48.sp.toPx()),
        colorPalette = palette,
    ).build(wordFrequenciesFetched)
        .bitmap
        .toImageBitmap()

}
