package com.arn.scrobble.info

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.EntriesGrid
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.not_found


@Composable
fun SimilarTracksScreen(
    musicEntry: Track,
    user: UserCached,
    pkgName: String?,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: InfoMiscVM = viewModel { InfoMiscVM() },
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
        emptyStringRes = Res.string.not_found,
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