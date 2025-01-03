package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.main.PanoPager
import com.arn.scrobble.navigation.PanoNavMetadata
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTabType
import com.arn.scrobble.navigation.PanoTabs
import com.arn.scrobble.ui.EntriesGrid
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.charts_no_data
import pano_scrobbler.composeapp.generated.resources.create_collage
import pano_scrobbler.composeapp.generated.resources.legend
import pano_scrobbler.composeapp.generated.resources.num_albums
import pano_scrobbler.composeapp.generated.resources.num_artists
import pano_scrobbler.composeapp.generated.resources.num_tracks
import pano_scrobbler.composeapp.generated.resources.tracks


@Composable
fun ChartsPagerScreen(
    user: UserCached,
    tabsList: List<PanoTabs>,
    tabIdx: Int,
    initialTabIdx: Int,
    onSetTabIdx: (Int) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onSetNavMetadataList: (List<PanoNavMetadata>) -> Unit,
    onTitleChange: (String?) -> Unit,
    viewModel: ChartsVM = viewModel { ChartsVM() },
    chartsPeriodViewModel: ChartsPeriodVM = viewModel { ChartsPeriodVM() },
    modifier: Modifier = Modifier,
) {
    val artists = viewModel.artists.collectAsLazyPagingItems()
    val albums = viewModel.albums.collectAsLazyPagingItems()
    val tracks = viewModel.tracks.collectAsLazyPagingItems()

    val artistsCount by viewModel.artistCount.collectAsStateWithLifecycle()
    val albumsCount by viewModel.albumCount.collectAsStateWithLifecycle()
    val tracksCount by viewModel.trackCount.collectAsStateWithLifecycle()

    val isTimePeriodContinuous by chartsPeriodViewModel.selectedPeriod.map { it?.lastfmPeriod != null }
        .collectAsStateWithLifecycle(false)

    val type by remember(tabIdx) {
        mutableIntStateOf(
            when (tabIdx) {
                0 -> Stuff.TYPE_ARTISTS
                1 -> Stuff.TYPE_ALBUMS
                2 -> Stuff.TYPE_TRACKS
                else -> throw IllegalArgumentException("Unknown page $tabIdx")
            }
        )
    }

    val title = when (type) {
        Stuff.TYPE_ARTISTS -> getMusicEntryQString(
            Res.string.artists,
            Res.plurals.num_artists,
            artistsCount,
            isTimePeriodContinuous
        )

        Stuff.TYPE_ALBUMS -> getMusicEntryQString(
            Res.string.albums,
            Res.plurals.num_albums,
            albumsCount,
            isTimePeriodContinuous
        )

        Stuff.TYPE_TRACKS -> getMusicEntryQString(
            Res.string.tracks,
            Res.plurals.num_tracks,
            tracksCount,
            isTimePeriodContinuous
        )

        else -> throw IllegalArgumentException("Unknown page $tabIdx")
    }

    LaunchedEffect(type) {
        chartsPeriodViewModel.selectedPeriod
            .filterNotNull()
            .collectLatest { timePeriod ->
                onSetNavMetadataList(
                    getChartsPagerNavMetadata(
                        user = user,
                        timePeriod = timePeriod,
                        type = type
                    )
                )
            }
    }

    LaunchedEffect(title) {
        onTitleChange(title)
    }

    fun setInput(timePeriod: TimePeriod, prevTimePeriod: TimePeriod?) {
        viewModel.reset()
        viewModel.setChartsInput(
            ChartsLoaderInput(
                username = user.name,
                timePeriod = timePeriod,
                prevPeriod = prevTimePeriod,
                firstPageOnly = false
            )
        )
    }

    Column(
        modifier = modifier,
    ) {
        TimePeriodSelector(
            user = user,
            viewModel = chartsPeriodViewModel,
            onSelected = ::setInput,
            modifier = Modifier.fillMaxWidth()
        )

        PanoPager(
            initialPage = initialTabIdx,
            selectedPage = tabIdx,
            onSelectPage = onSetTabIdx,
            totalPages = tabsList.count { it.type == PanoTabType.TAB },
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            EntriesGrid(
                entries = when (page) {
                    0 -> artists
                    1 -> albums
                    2 -> tracks
                    else -> throw IllegalArgumentException("Unknown page $page")
                },
                fetchAlbumImageIfMissing = !isTimePeriodContinuous,
                showArtists = true,
                emptyStringRes = Res.string.charts_no_data,
                placeholderItem = remember(type) {
                    getMusicEntryPlaceholderItem(type)
                },
                onItemClick = {
                    onNavigate(
                        PanoRoute.MusicEntryInfo(
                            track = it as? Track,
                            artist = it as? Artist,
                            album = it as? Album,
                            pkgName = null,
                            user = user
                        )
                    )
                },
            )
        }
    }
}


private fun getChartsPagerNavMetadata(
    user: UserCached,
    timePeriod: TimePeriod,
    type: Int,
) = listOf(
    PanoNavMetadata(
        titleRes = Res.string.create_collage,
        icon = Icons.Outlined.GridView,
        route = PanoRoute.CollageGenerator(
            user = user,
            timePeriod = timePeriod,
            collageType = type,
        ),
    ),
    PanoNavMetadata(
        titleRes = Res.string.legend,
        icon = Icons.Outlined.Info,
        route = PanoRoute.ChartsLegend,
    ),
)