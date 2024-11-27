package com.arn.scrobble.recents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem

@Composable
fun TrackHistoryScreen(
    user: UserCached,
    track: Track,
    onNavigate: (PanoRoute) -> Unit,
    viewModel: ScrobblesVM = viewModel(),
    modifier: Modifier = Modifier,
) {
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val firstScrobbleTime by viewModel.firstScrobbleTime.collectAsStateWithLifecycle()
    val deletedTracksSet by viewModel.deletedTracksSet.collectAsStateWithLifecycle()
    val editedTracksMap by viewModel.editedTracksMap.collectAsStateWithLifecycle()
    val pkgMap by viewModel.pkgMap.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.setScrobblesInput(
            ScrobblesInput(
                user = user,
                track = track
            ),
            initial = true
        )
    }

    LazyColumn(
        contentPadding = panoContentPadding(),
        modifier = modifier
    ) {
        if (firstScrobbleTime != null) {
            item("first_scrobble_time") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .animateItem(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cake,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(
                            R.string.first_scrobbled_on,
                            Stuff.myRelativeTime(millis = firstScrobbleTime)
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        scrobblesListItems(
            tracks = tracks,
            user = user,
            deletedTracksSet = deletedTracksSet,
            editedTracksMap = editedTracksMap,
            pkgMap = pkgMap,
            fetchAlbumImageIfMissing = false,
            showFullMenu = false,
            showHate = false,
            onNavigate = onNavigate,
            viewModel = viewModel,
        )

        scrobblesPlaceholdersAndErrors(tracks = tracks)
    }
}