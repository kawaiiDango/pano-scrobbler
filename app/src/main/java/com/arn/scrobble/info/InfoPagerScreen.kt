package com.arn.scrobble.info

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.main.PanoPager
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTabType
import com.arn.scrobble.navigation.PanoTabs
import com.arn.scrobble.ui.EntriesGrid
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.utils.Stuff


@Composable
fun InfoPagerScreen(
    musicEntry: Artist,
    user: UserCached,
    pkgName: String?,
    tabsList: List<PanoTabs>,
    tabIdx: Int,
    initialTabIdx: Int,
    onSetTabIdx: (Int) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: InfoMiscVM = viewModel(),
    modifier: Modifier = Modifier,
) {

    val type by remember(tabIdx) {
        mutableIntStateOf(
            when (tabIdx) {
                0 -> Stuff.TYPE_TRACKS
                1 -> Stuff.TYPE_ALBUMS
                2 -> Stuff.TYPE_ARTISTS
                else -> throw IllegalArgumentException("Unknown page $tabIdx")
            }
        )
    }


    val artistTopTracks = viewModel.topTracks.collectAsLazyPagingItems()
    val artistTopAlbums = viewModel.topAlbums.collectAsLazyPagingItems()
    val similarArtists = viewModel.similarArtists.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.setArtist(musicEntry)
    }

    PanoPager(
        initialPage = initialTabIdx,
        selectedPage = tabIdx,
        onSelectPage = onSetTabIdx,
        totalPages = tabsList.count { it.type == PanoTabType.TAB },
        modifier = modifier,
    ) { page ->
        EntriesGrid(
            entries = when (type) {
                Stuff.TYPE_TRACKS -> artistTopTracks
                Stuff.TYPE_ALBUMS -> artistTopAlbums
                Stuff.TYPE_ARTISTS -> similarArtists
                else -> throw IllegalArgumentException("Unknown type $type")
            },
            fetchAlbumImageIfMissing = page == 0,
            showArtists = page == 2,
            emptyStringRes = R.string.not_found,
            placeholderItem = remember(type) {
                getMusicEntryPlaceholderItem(type, showScrobbleCount = type != Stuff.TYPE_ARTISTS)
            },
            onItemClick = {
                onNavigate(
                    PanoRoute.MusicEntryInfo(
                        track = it as? Track,
                        artist = it as? Artist,
                        album = it as? Album,
                        pkgName = pkgName,
                        user = user
                    )
                )
            },
        )
    }
}