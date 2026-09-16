package com.arn.scrobble.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchEffect
import com.arn.scrobble.ui.emptyText
import com.arn.scrobble.ui.expandableSublist
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.external_metadata
import pano_scrobbler.composeapp.generated.resources.from
import pano_scrobbler.composeapp.generated.resources.is_turned_off
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.tracks

@Composable
fun SearchScreen(
    searchFieldState: TextFieldState,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchVM = viewModel { SearchVM() },
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(null)
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()

    var artistsExpanded by rememberSaveable { mutableStateOf(false) }
    var albumsExpanded by rememberSaveable { mutableStateOf(false) }
    var tracksExpanded by rememberSaveable { mutableStateOf(false) }
    var lovedExpanded by rememberSaveable { mutableStateOf(false) }

    val artistsText = stringResource(Res.string.artists)
    val albumsText = stringResource(Res.string.albums)
    val tracksText = stringResource(Res.string.tracks)
    val lovedText = stringResource(Res.string.loved)
    val currentAccount by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.currentAccount
    }
    val useLastfm by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.lastfmApiAlways || it.currentAccountType == AccountType.LASTFM
    }

    fun onItemClick(item: MusicEntry) {
        currentAccount?.user?.let { userSelf ->
            onNavigate(
                PanoRoute.Modal.MusicEntryInfo(
                    track = item as? Track,
                    album = item as? Album,
                    artist = item as? Artist,
                    user = userSelf,
                )
            )
        }
    }

    if (useLastfm) {
        SearchEffect(searchFieldState) {
            viewModel.search(it)
        }
    }

    PanoLazyColumn(
        modifier = modifier
    ) {
        if (hasLoaded) {
            item("results_header") {
                Text(
                    text = stringResource(Res.string.from, stringResource(Res.string.lastfm)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            expandableSublist(
                headerText = artistsText,
                headerIcon = Icons.Mic,
                items = searchResults?.artists ?: emptyList(),
                expanded = artistsExpanded,
                onToggle = { artistsExpanded = it },
                onItemClick = ::onItemClick,
            )

            expandableSublist(
                headerText = albumsText,
                headerIcon = Icons.Album,
                items = searchResults?.albums ?: emptyList(),
                expanded = albumsExpanded,
                onToggle = { albumsExpanded = it },
                onItemClick = ::onItemClick,
            )

            expandableSublist(
                headerText = tracksText,
                headerIcon = Icons.MusicNote,
                items = searchResults?.tracks ?: emptyList(),
                expanded = tracksExpanded,
                onToggle = { tracksExpanded = it },
                onItemClick = ::onItemClick,
                fetchAlbumImageIfMissing = true,
            )

            expandableSublist(
                headerText = lovedText,
                headerIcon = Icons.Favorite,
                items = searchResults?.lovedTracks ?: emptyList(),
                expanded = lovedExpanded,
                onToggle = { lovedExpanded = it },
                onItemClick = ::onItemClick,
                fetchAlbumImageIfMissing = true,
            )

            if (searchResults?.isEmpty == true)
                emptyText { stringResource(Res.string.not_found) }

        } else if (searchResults != null) {
            items(
                10,
                key = { "shimmer_$it" }
            ) {
                MusicEntryListItem(
                    getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS),
                    forShimmer = true,
                    onEntryClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (!useLastfm) {
            emptyText {
                stringResource(
                    Res.string.is_turned_off,
                    stringResource(Res.string.lastfm),
                    stringResource(Res.string.external_metadata),
                )
            }
        } else if (searchFieldState.text.isBlank()) {
            emptyText { stringResource(Res.string.search) }
        }
    }
}