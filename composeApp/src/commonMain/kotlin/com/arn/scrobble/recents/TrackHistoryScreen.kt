package com.arn.scrobble.recents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.icons.Cake
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.first_scrobbled_on
import pano_scrobbler.composeapp.generated.resources.my_scrobbles
import pano_scrobbler.composeapp.generated.resources.time_just_now

@Composable
fun TrackHistoryScreen(
    user: UserCached,
    track: Track,
    onSetTitle: (String) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    editDataFlow: Flow<Pair<String, Track>>,
    modifier: Modifier = Modifier,
    viewModel: ScrobblesVM = viewModel { ScrobblesVM(user, track) },
) {
    val listState = rememberLazyListState()
    val tracks = viewModel.tracks.collectAsLazyPagingItems()
    val firstScrobbleTime by viewModel.firstScrobbleTime.collectAsStateWithLifecycle()
    val total by viewModel.total.collectAsStateWithLifecycle()
    val deletedTracksCount by viewModel.deletedTracksCount.collectAsStateWithLifecycle()
    val pkgMap by viewModel.pkgMap.collectAsStateWithLifecycle()
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val showScrobbleSources by if (LocalLicenseValidState.current)
        PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.showScrobbleSources }
    else
        remember { mutableStateOf(false) }
    val myScrobblesStr = stringResource(Res.string.my_scrobbles)
    val density = LocalDensity.current
    val listViewportHeight = remember {
        derivedStateOf {
            with(density) {
                (listState.layoutInfo.viewportSize.height - listState.layoutInfo.afterContentPadding - listState.layoutInfo.beforeContentPadding).toDp()
            }
        }
    }
    val animateListItemContentSize = remember {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount > listState.layoutInfo.visibleItemsInfo.size
        }
    }

    LaunchedEffect(total, deletedTracksCount) {
        val formattedCount = ((total ?: 0) - deletedTracksCount)
            .coerceAtLeast(0)
            .format()
        val title = if (user.isSelf) {
            "$myScrobblesStr: $formattedCount"
        } else {
            "${user.name}: $formattedCount"
        }
        onSetTitle(title)
    }

    LaunchedEffect(Unit) {
        viewModel.setScrobblesInput(
            ScrobblesInput(showScrobbleSources = showScrobbleSources)
        )
    }

    OnEditEffect(
        viewModel,
        editDataFlow
    )

    PanoLazyColumn(
        state = listState,
        modifier = modifier
    ) {
        if (firstScrobbleTime != null) {
            item("first_scrobble_time") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Cake,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(
                            Res.string.first_scrobbled_on,
                            PanoTimeFormatter.relative(
                                firstScrobbleTime!!,
                                stringResource(Res.string.time_just_now)
                            )
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        scrobblesListItems(
            tracks = tracks,
            user = user,
            pkgMap = pkgMap,
            fetchAlbumImageIfMissing = false,
            showScrobbleSources = showScrobbleSources,
            canEdit = true,
            canDelete = true,
            canLove = false,
            canHate = false,
            expandedKey = { expandedKey },
            onExpand = { expandedKey = it },
            onNavigate = onNavigate,
            animateListItemContentSize = animateListItemContentSize,
            maxHeight = listViewportHeight,
            viewModel = viewModel,
        )

        scrobblesPlaceholdersAndErrors(tracks = tracks)
    }
}