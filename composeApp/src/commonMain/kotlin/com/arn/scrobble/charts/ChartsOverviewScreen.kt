package com.arn.scrobble.charts

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.graphics.KumoRect
import com.arn.scrobble.graphics.toImageBitmap
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.AutoAwesomeMosaic
import com.arn.scrobble.icons.BarChart4Bars
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Info
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MoreVert
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.icons.QuestionMark
import com.arn.scrobble.icons.Tag
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.EntriesRow
import com.arn.scrobble.ui.HeaderItemWithAction
import com.arn.scrobble.ui.OptionalHorizontalScrollbar
import com.arn.scrobble.ui.PanoDropdownMenu
import com.arn.scrobble.ui.SimpleHeaderItem
import com.arn.scrobble.ui.TextWithIcon
import com.arn.scrobble.ui.YesNoDropdown
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.WordFrequency
import com.kennycason.kumo.bg.CircleBackground
import com.kennycason.kumo.palette.LinearGradientColorPalette
import com.kennycason.kumo.scale.LinearFontScalar
import io.github.koalaplot.core.bar.DefaultBar
import io.github.koalaplot.core.bar.DefaultBarPosition
import io.github.koalaplot.core.bar.DefaultVerticalBarPlotEntry
import io.github.koalaplot.core.bar.VerticalBarPlot
import io.github.koalaplot.core.gestures.GestureConfig
import io.github.koalaplot.core.xygraph.AxisContent
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import io.github.koalaplot.core.xygraph.rememberIntLinearAxisModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.based_on
import pano_scrobbler.composeapp.generated.resources.charts
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.charts_no_data
import pano_scrobbler.composeapp.generated.resources.create_collage
import pano_scrobbler.composeapp.generated.resources.days
import pano_scrobbler.composeapp.generated.resources.external_metadata
import pano_scrobbler.composeapp.generated.resources.hidden_tags
import pano_scrobbler.composeapp.generated.resources.is_turned_off
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.listening_activity
import pano_scrobbler.composeapp.generated.resources.months
import pano_scrobbler.composeapp.generated.resources.not_enough_data
import pano_scrobbler.composeapp.generated.resources.num_albums
import pano_scrobbler.composeapp.generated.resources.num_artists
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_noti
import pano_scrobbler.composeapp.generated.resources.num_tracks
import pano_scrobbler.composeapp.generated.resources.spotify_consent
import pano_scrobbler.composeapp.generated.resources.tag_cloud
import pano_scrobbler.composeapp.generated.resources.tracks
import pano_scrobbler.composeapp.generated.resources.weeks
import pano_scrobbler.composeapp.generated.resources.years

@Composable
fun ChartsOverviewScreen(
    user: UserCached,
    digestTimePeriod: LastfmPeriod?,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChartsVM = viewModel(key = user.key<ChartsVM>()) { ChartsVM(user, true) },
    chartsPeriodViewModel: ChartsPeriodVM = viewModel { ChartsPeriodVM() },
) {
    val artists = viewModel.artists.collectAsLazyPagingItems()
    val albums = viewModel.albums.collectAsLazyPagingItems()
    val tracks = viewModel.tracks.collectAsLazyPagingItems()

    val artistsCount by viewModel.artistCount.collectAsStateWithLifecycle()
    val albumsCount by viewModel.albumCount.collectAsStateWithLifecycle()
    val tracksCount by viewModel.trackCount.collectAsStateWithLifecycle()
    val scrobblesCount by viewModel.scrobblesCount.collectAsStateWithLifecycle()

    val tagCloud by viewModel.tagCloud.collectAsStateWithLifecycle()
    val listeningActivity by viewModel.listeningActivity.collectAsStateWithLifecycle()
    val useLastfm by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.lastfmApiAlways || it.currentAccountType == AccountType.LASTFM
    }
    val spotifyConsentLearnt by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.spotifyConsentLearnt
    }
    var spotifyConsentLearntDropdownShown by remember { mutableStateOf(false) }
    var isTimePeriodContinuous by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val minAdditionalScrollOffset = with(density) { 96.dp.toPx() }

    var tagCloudOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    val scrolledToTagCloud by remember {
        derivedStateOf {
            scrollState.value + scrollState.viewportSize >=
                    tagCloudOffsetY + minAdditionalScrollOffset
        }
    }
    var listeningActivityOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    val scrolledToListeningActivity by remember {
        derivedStateOf {
            scrollState.value + scrollState.viewportSize >=
                    listeningActivityOffsetY + minAdditionalScrollOffset
        }
    }

    fun setInput(timePeriod: TimePeriod, prevTimePeriod: TimePeriod?, refreshCount: Int) {
        isTimePeriodContinuous = timePeriod.lastfmPeriod != null

        viewModel.setChartsInput(
            ChartsLoaderInput(
                timePeriod = timePeriod,
                prevPeriod = prevTimePeriod,
                refreshCount = refreshCount
            )
        )
    }

    LaunchedEffect(scrolledToTagCloud) {
        viewModel.setTagCloudVisible(scrolledToTagCloud)
    }

    LaunchedEffect(scrolledToListeningActivity) {
        viewModel.setListeningActivityVisible(scrolledToListeningActivity)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.setTagCloudVisible(scrolledToTagCloud)
        viewModel.setListeningActivityVisible(scrolledToListeningActivity)

        onPauseOrDispose {
            viewModel.setTagCloudVisible(false)
            viewModel.setListeningActivityVisible(false)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(panoContentPadding(sides = false)),
    ) {
        TimePeriodSelector(
            registeredTime = user.registeredTime,
            viewModel = chartsPeriodViewModel,
            onNavigate = onNavigate,
            onSelected = ::setInput,
            showRefreshButton = true,
            digestTimePeriod = digestTimePeriod,
        )


        ChartsCount(
            text = if (scrobblesCount > 0) {
                pluralStringResource(
                    Res.plurals.num_scrobbles_noti,
                    scrobblesCount,
                    scrobblesCount.format()
                )
            } else {
                stringResource(Res.string.charts)
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)

        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!spotifyConsentLearnt) {
            HeaderItemWithAction(
                title = stringResource(Res.string.spotify_consent),
                icon = Icons.QuestionMark,
                trailingIconContentDescription = stringResource(Res.string.item_options),
                onClick = {
                    spotifyConsentLearntDropdownShown = true
                },
            )

            Box(
                modifier = Modifier.align(Alignment.End)
            ) {
                YesNoDropdown(
                    expanded = spotifyConsentLearntDropdownShown,
                    onDismissRequest = {
                        spotifyConsentLearntDropdownShown = false
                    },
                    onYes = {
                        scope.launch {
                            PlatformStuff.mainPrefs.updateData {
                                it.copy(spotifyConsentLearnt = true, spotifyApi = true)
                            }
                        }
                    },
                    onNo = {
                        scope.launch {
                            PlatformStuff.mainPrefs.updateData {
                                it.copy(spotifyConsentLearnt = true, spotifyApi = false)
                            }
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

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
            headerIcon = Icons.Mic,
            emptyStringRes = Res.string.charts_no_data,
            onHeaderClick = {
                onNavigate(
                    PanoRoute.ChartsPager(
                        user = user,
                        chartsType = Stuff.TYPE_ARTISTS
                    )
                )
            },
            placeholderItem = remember {
                getMusicEntryPlaceholderItem(Stuff.TYPE_ARTISTS)
            },
            onItemClick = {
                onNavigate(
                    PanoRoute.Modal.MusicEntryInfo(
                        artist = it as Artist,
                        user = user,
                        appId = null
                    )
                )
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            headerIcon = Icons.Album,
            emptyStringRes = Res.string.charts_no_data,
            onHeaderClick = {
                onNavigate(
                    PanoRoute.ChartsPager(
                        user = user,
                        chartsType = Stuff.TYPE_ALBUMS
                    )
                )
            },
            placeholderItem = remember {
                getMusicEntryPlaceholderItem(Stuff.TYPE_ALBUMS)
            },
            onItemClick = {
                onNavigate(
                    PanoRoute.Modal.MusicEntryInfo(
                        album = it as Album,
                        user = user,
                        appId = null
                    )
                )
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

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
            headerIcon = Icons.MusicNote,
            emptyStringRes = Res.string.charts_no_data,
            onHeaderClick = {
                onNavigate(
                    PanoRoute.ChartsPager(
                        user = user,
                        chartsType = Stuff.TYPE_TRACKS
                    )
                )
            },
            placeholderItem = remember {
                getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS)
            },
            onItemClick = {
                onNavigate(
                    PanoRoute.Modal.MusicEntryInfo(
                        track = it as Track,
                        user = user,
                        appId = null
                    )
                )
            },
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!PlatformStuff.isTv) {
            FilledTonalButton(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    onNavigate(
                        PanoRoute.Modal.CollageGenerator(
                            collageType = Stuff.TYPE_ALL,
                            user = user,
                            timePeriod = chartsPeriodViewModel.selectedPeriod.value
                                ?: return@FilledTonalButton
                        )
                    )
                }, contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    Icons.AutoAwesomeMosaic,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    stringResource(Res.string.create_collage),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        SimpleHeaderItem(
            text = stringResource(Res.string.listening_activity),
            icon = Icons.BarChart4Bars,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        ListeningActivityContent(
            listeningActivity = listeningActivity,
            modifier = Modifier
                .fillMaxWidth()
                .padding(panoContentPadding(bottom = false))
                .onGloballyPositioned { coordinates ->
                    listeningActivityOffsetY = coordinates.positionInParent().y
                }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TagCloudContent(
            tagCloud = tagCloud,
            onHeaderMenuClick = {
                onNavigate(PanoRoute.Modal.HiddenTags)
            },
            enabled = useLastfm,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    tagCloudOffsetY = coordinates.positionInParent().y
                }
        )
    }
}

@Composable
private fun TagCloudContent(
    tagCloud: Map<String, Float>?,
    onHeaderMenuClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val hiddenTags by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.hiddenTags }

    var kumoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val color1 = MaterialTheme.colorScheme.inverseSurface
    val color2 = MaterialTheme.colorScheme.secondary
    var tagCloudSizePx by remember { mutableIntStateOf(0) }
    val isLoading by remember(tagCloud, kumoBitmap) {
        mutableStateOf(tagCloud == null || kumoBitmap == null && tagCloud.isNotEmpty())
    }
    var menuShown by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    LaunchedEffect(tagCloud, hiddenTags, tagCloudSizePx) {
        withContext(Dispatchers.Default) {
            tagCloud?.let {
                if (tagCloudSizePx > 0 && tagCloud.isNotEmpty()) {
                    kumoBitmap = generateTagCloud(
                        tagCloud.filterKeys { it !in hiddenTags },
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
        HeaderItemWithAction(
            title = stringResource(Res.string.tag_cloud),
            icon = Icons.Tag,
            trailingIcon = Icons.MoreVert,
            trailingIconContentDescription = stringResource(Res.string.hidden_tags),
            onClick = { menuShown = true },
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 8.dp)
        ) {
            PanoDropdownMenu(
                expanded = menuShown,
                onDismissRequest = { menuShown = false },
            ) {
                item(
                    text = { Text(stringResource(Res.string.hidden_tags)) },
                    onClick = {
                        onHeaderMenuClick()
                        menuShown = false
                    }
                )
            }
        }

        if (!enabled) {
            TextWithIcon(
                stringResource(
                    Res.string.is_turned_off,
                    stringResource(Res.string.lastfm),
                    stringResource(Res.string.external_metadata),
                ),
                icon = Icons.Info,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

        } else {
            val boxInteractionSource = remember { MutableInteractionSource() }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = 370.dp)
                    .aspectRatio(1f)
                    .then(
                        if (isLoading)
                            Modifier
                                .shimmerWindowBounds()
                                .backgroundForShimmer(true, shape = CircleShape)
                        else
                            Modifier
                    )
                    .indication(boxInteractionSource, LocalIndication.current)
                    .focusable(interactionSource = boxInteractionSource)
                    .onGloballyPositioned { coordinates ->
                        tagCloudSizePx = coordinates.size.width
                    }
            ) {
                if (tagCloud?.isEmpty() == true) {
                    Text(
                        text = stringResource(Res.string.not_enough_data),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

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

            val textInteractionSource = remember { MutableInteractionSource() }
            Text(
                stringResource(
                    Res.string.based_on,
                    stringResource(Res.string.artists) + " (" +
                            stringResource(Res.string.lastfm) + ")"
                ),
                modifier = Modifier
                    .align(Alignment.End)
                    .indication(textInteractionSource, LocalIndication.current)
                    .focusable(interactionSource = textInteractionSource)
                    .padding(panoContentPadding(bottom = false))
            )
        }
    }
}

@Composable
private fun ListeningActivityContent(
    listeningActivity: ListeningActivity?,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
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
            listeningActivity?.timePeriodsToCounts?.values?.toList()
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

    val yValuesMax by remember(yData) { mutableIntStateOf(yData.maxOrNull() ?: 0) }

    val tintColor = MaterialTheme.colorScheme.secondary

    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
    ) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .shimmerWindowBounds(isLoading)
                .backgroundForShimmer(isLoading, shape = MaterialTheme.shapes.extraLarge)
                .horizontalScroll(scrollState)
                .clip(MaterialTheme.shapes.medium)
                .indication(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                )
                .focusable(interactionSource = interactionSource)
        ) {
            if (listeningActivity?.timePeriodsToCounts?.isEmpty() == true) {
                Text(
                    text = stringResource(Res.string.charts_no_data),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            listeningActivity?.let { listeningActivity ->
                if (listeningActivity.timePeriodsToCounts.isNotEmpty()) {
                    XYGraph(
                        xAxisModel = remember(xData) { CategoryAxisModel(xData) },
                        yAxisModel = rememberIntLinearAxisModel(
                            range = 0..(1.2 * yValuesMax).toInt(),
                            minViewExtent = 1,
                            maxViewExtent = (1.2 * yValuesMax).toInt(),
                            minorTickCount = 0,
                        ),
                        xAxisContent = AxisContent(
                            labels = { AxisLabel(it.name) },
                            title = {
                                Text(
                                    stringResource(typeStringRes),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            style = rememberAxisStyle()
                        ),
                        yAxisContent = AxisContent(
                            labels = { AxisLabel(it.toString()) },
                            title = {},
                            style = rememberAxisStyle(labelRotation = 90)
                        ),
                        gestureConfig = remember { GestureConfig() },
                        modifier = Modifier
                            .width(max(minWidth, (xData.size * 24).dp)),
                    ) {
                        val data = remember(xData, yData) {
                            xData
                                .zip(yData)
                                .map { (xd, yd) ->
                                    DefaultVerticalBarPlotEntry(
                                        xd,
                                        DefaultBarPosition(0, yd)
                                    )
                                }
                        }

                        VerticalBarPlot(
                            data = data,
                            bar = { series, index, value ->
                                val currentYValue = value.y.end
                                val fontSizeDp = with(density) {
                                    MaterialTheme.typography.labelSmall.fontSize.toDp()
                                }

                                DefaultBar(
                                    color = tintColor.copy(
                                        alpha = 0.3f + 0.7f * currentYValue / yValuesMax
                                    ),
                                    shape = MaterialTheme.shapes.small.copy(
                                        bottomEnd = CornerSize(0.dp),
                                        bottomStart = CornerSize(0.dp)
                                    ),
                                )

                                Text(
                                    currentYValue.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (yValuesMax == currentYValue)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.secondary,
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
        OptionalHorizontalScrollbar(scrollState)
    }
}

@Composable
private fun AxisLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        label,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
        maxLines = 2,
    )
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
