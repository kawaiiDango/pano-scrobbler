package com.arn.scrobble.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTab
import com.arn.scrobble.ui.EntriesGridOrList
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.map
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.charts_no_data
import pano_scrobbler.composeapp.generated.resources.num_albums
import pano_scrobbler.composeapp.generated.resources.num_artists
import pano_scrobbler.composeapp.generated.resources.num_tracks
import pano_scrobbler.composeapp.generated.resources.tracks


@Composable
fun ChartsPagerScreen(
    user: UserCached,
    tabIdx: Int,
    onSetTabData: (String, List<PanoTab>?) -> Unit,
    onSetTabIdx: (Int) -> Unit,
    onOpenDialog: (PanoDialog) -> Unit,
    onSetTitle: (String?) -> Unit,
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

    val selectedPeriod by chartsPeriodViewModel.selectedPeriod.collectAsStateWithLifecycle()
    val isTimePeriodContinuous by chartsPeriodViewModel.selectedPeriod.map { it?.lastfmPeriod != null }
        .collectAsStateWithLifecycle(false)

    val tabsList = remember { getTabData() }

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


    DisposableEffect(title) {
        onSetTitle(title)

        onDispose {
            onSetTitle(null)
        }
    }

    DisposableEffect(user) {
        val id = PanoRoute.ChartsPager::class.simpleName + " " + user.name

        onSetTabData(id, tabsList)

        onDispose {
            onSetTabData(id, null)
        }
    }

    fun setInput(timePeriod: TimePeriod, prevTimePeriod: TimePeriod?) {
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
            selectedPage = tabIdx,
            onSelectPage = onSetTabIdx,
            totalPages = tabsList.count(),
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            EntriesGridOrList(
                entries = when (page) {
                    0 -> artists
                    1 -> albums
                    2 -> tracks
                    else -> throw IllegalArgumentException("Unknown page $page")
                },
                fetchAlbumImageIfMissing = !isTimePeriodContinuous || type == Stuff.TYPE_TRACKS,
                showArtists = true,
                emptyStringRes = Res.string.charts_no_data,
                placeholderItem = remember(type) {
                    getMusicEntryPlaceholderItem(type)
                },
                onCollageClick = {
                    onOpenDialog(
                        PanoDialog.CollageGenerator(
                            user = user,
                            timePeriod = selectedPeriod ?: return@EntriesGridOrList,
                            collageType = type,
                        )
                    )
                },
                onLegendClick = {
                    onOpenDialog(PanoDialog.ChartsLegend)
                },
                onItemClick = {
                    onOpenDialog(
                        PanoDialog.MusicEntryInfo(
                            track = it as? Track,
                            artist = it as? Artist,
                            album = it as? Album,
                            appId = null,
                            user = user
                        )
                    )
                },
            )
        }
    }
}

private fun getTabData() =
    listOf(
        PanoTab.TopArtists,
        PanoTab.TopAlbums,
        PanoTab.TopTracks,
    )