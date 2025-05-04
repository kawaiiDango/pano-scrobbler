package com.arn.scrobble.info

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.ui.EntriesGridOrList
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.not_found


@Composable
fun SimilarTracksScreen(
    musicEntry: Track,
    user: UserCached,
    pkgName: String?,
    onOpenDialog: (PanoDialog) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InfoMiscVM = viewModel { InfoMiscVM() },
) {

    val similarTracks = viewModel.similarTracks.collectAsLazyPagingItems()

    LaunchedEffect(musicEntry) {
        viewModel.setTrack(musicEntry)
    }

    EntriesGridOrList(
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
            onOpenDialog(
                PanoDialog.MusicEntryInfo(
                    track = it as Track,
                    pkgName = pkgName,
                    user = user
                )
            )
        },
        modifier = modifier
    )

}