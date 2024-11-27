package com.arn.scrobble.info

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.PanoTabType
import com.arn.scrobble.navigation.PanoTabs
import com.arn.scrobble.ui.EntriesGrid


@Composable
fun SimilarTracksScreen(
    musicEntry: Track,
    user: UserCached,
    pkgName: String?,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: InfoMiscVM = viewModel(),
    modifier: Modifier = Modifier,
) {

    val similarTracks = viewModel.similarTracks.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        viewModel.setTrack(musicEntry)
    }

    EntriesGrid(
        entries = similarTracks,
        fetchAlbumImageIfMissing = true,
        showArtists = true,
        emptyStringRes = R.string.not_found,
        placeholderItem = remember {
            Track(
                name = "Track",
                artist = Artist(
                    name = "Artist",
                ),
                playcount = 10,
                album = null,
            )
        },
        onItemClick = {
            onNavigate(
                PanoRoute.MusicEntryInfo(
                    track = it as Track,
                    pkgName = pkgName,
                    user = user
                )
            )
        },
        modifier = modifier
    )

}