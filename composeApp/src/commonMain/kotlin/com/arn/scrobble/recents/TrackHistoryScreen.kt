package com.arn.scrobble.recents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.first_scrobbled_on

@Composable
fun TrackHistoryScreen(
    user: UserCached,
    track: Track,
    onOpenDialog: (PanoDialog) -> Unit,
    editDataFlow: Flow<Pair<Track, ScrobbleData>>,
    modifier: Modifier = Modifier,
    viewModel: ScrobblesVM = viewModel { ScrobblesVM() },
) {
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val firstScrobbleTime by viewModel.firstScrobbleTime.collectAsStateWithLifecycle()
    val deletedTracksSet by viewModel.deletedTracksSet.collectAsStateWithLifecycle()
    val editedTracksMap by viewModel.editedTracksMap.collectAsStateWithLifecycle()
    val pkgMap by viewModel.pkgMap.collectAsStateWithLifecycle()
    val seenApps by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.seenApps }
    var expandedIdx by rememberSaveable { mutableIntStateOf(-1) }

    LaunchedEffect(user, track) {
        viewModel.setScrobblesInput(
            ScrobblesInput(
                user = user,
                track = track
            ),
        )
    }

    OnEditEffect(
        viewModel,
        editDataFlow
    )

    PanoLazyColumn(
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
                            Res.string.first_scrobbled_on,
                            PanoTimeFormatter.relative(firstScrobbleTime!!)
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
            seenApps = seenApps,
            fetchAlbumImageIfMissing = false,
            canEdit = true,
            canDelete = true,
            canLove = false,
            canHate = false,
            expandedIdx = { expandedIdx },
            onExpand = { expandedIdx = it },
            onOpenDialog = onOpenDialog,
            viewModel = viewModel,
        )

        scrobblesPlaceholdersAndErrors(tracks = tracks)
    }
}