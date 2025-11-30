package com.arn.scrobble.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.SearchType
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.navigation.PanoDialog
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ExpandableHeaderMenu
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchField
import com.arn.scrobble.ui.expandableSublist
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.external_metadata
import pano_scrobbler.composeapp.generated.resources.global
import pano_scrobbler.composeapp.generated.resources.is_turned_off
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.library
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.reindex
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.searched_n_items
import pano_scrobbler.composeapp.generated.resources.tracks

@Composable
fun SearchScreen(
    onOpenDialog: (PanoDialog) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchVM = viewModel { SearchVM() },
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(null)
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()

    var searchTerm by rememberSaveable { mutableStateOf("") }
    var searchType by rememberSaveable { mutableStateOf(SearchType.GLOBAL) }

    var artistsExpanded by rememberSaveable { mutableStateOf(false) }
    var albumsExpanded by rememberSaveable { mutableStateOf(false) }
    var tracksExpanded by rememberSaveable { mutableStateOf(false) }
    var lovedExpanded by rememberSaveable { mutableStateOf(false) }

    val artistsText = stringResource(Res.string.artists)
    val albumsText = stringResource(Res.string.albums)
    val tracksText = stringResource(Res.string.tracks)
    val lovedText = stringResource(Res.string.loved)
    val useLastfm by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.lastfmApiAlways || Scrobblables.currentAccount.value?.type == AccountType.LASTFM
    }

    val focusRequester = remember { FocusRequester() }

    fun onItemClick(item: MusicEntry) {
        val userSelf = Scrobblables.currentAccount.value?.user

        userSelf?.let { userSelf ->
            onOpenDialog(
                PanoDialog.MusicEntryInfo(
                    track = item as? Track,
                    album = item as? Album,
                    artist = item as? Artist,
                    user = userSelf,
                )
            )
        }
    }

    LaunchedEffect(searchTerm, searchType) {
        viewModel.search(searchTerm, searchType)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        SearchField(
            searchTerm = if (useLastfm) searchTerm else "",
            label = if (useLastfm)
                stringResource(Res.string.search)
            else
                stringResource(
                    Res.string.is_turned_off,
                    stringResource(Res.string.lastfm),
                    stringResource(Res.string.external_metadata),
                ),
            enabled = useLastfm,
            onSearchTermChange = { searchTerm = it },
            modifier = Modifier
                .padding(panoContentPadding(bottom = false))
                .focusRequester(focusRequester)

        )

        if (BuildKonfig.DEBUG) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = searchType == SearchType.GLOBAL,
                    onClick = { searchType = SearchType.GLOBAL },
                    label = { Text(text = stringResource(Res.string.global)) }
                )
                FilterChip(
                    selected = searchType == SearchType.LOCAL,
                    onClick = { searchType = SearchType.LOCAL },
                    label = { Text(text = stringResource(Res.string.library)) }
                )
            }
        }

        EmptyText(
            text = stringResource(Res.string.not_found),
            visible = hasLoaded && searchResults?.isEmpty == true,
        )

        PanoLazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            if (hasLoaded) {
                expandableSublist(
                    headerText = artistsText,
                    headerIcon = Icons.Outlined.Mic,
                    items = searchResults?.artists ?: emptyList(),
                    expanded = artistsExpanded,
                    onToggle = { artistsExpanded = it },
                    onItemClick = ::onItemClick,
                )

                expandableSublist(
                    headerText = albumsText,
                    headerIcon = Icons.Outlined.Album,
                    items = searchResults?.albums ?: emptyList(),
                    expanded = albumsExpanded,
                    onToggle = { albumsExpanded = it },
                    onItemClick = ::onItemClick,
                )

                expandableSublist(
                    headerText = tracksText,
                    headerIcon = Icons.Outlined.MusicNote,
                    items = searchResults?.tracks ?: emptyList(),
                    expanded = tracksExpanded,
                    onToggle = { tracksExpanded = it },
                    onItemClick = ::onItemClick,
                    fetchAlbumImageIfMissing = true,
                )

                expandableSublist(
                    headerText = lovedText,
                    headerIcon = Icons.Outlined.FavoriteBorder,
                    items = searchResults?.lovedTracks ?: emptyList(),
                    expanded = lovedExpanded,
                    onToggle = { lovedExpanded = it },
                    onItemClick = ::onItemClick,
                    fetchAlbumImageIfMissing = true,
                )

                if (searchType == SearchType.LOCAL) {
                    item(Res.string.reindex) {
                        ExpandableHeaderMenu(
                            title = stringResource(
                                Res.string.searched_n_items,
                                Stuff.MAX_INDEXED_ITEMS.format()
                            ),
                            icon = Icons.Outlined.Info,
                            menuItemText = stringResource(Res.string.reindex),
                            onMenuItemClick = {
                                onOpenDialog(PanoDialog.Index)
                            }
                        )
                    }
                }
            } else if (searchResults != null) {
                items(10) {
                    MusicEntryListItem(
                        getMusicEntryPlaceholderItem(Stuff.TYPE_TRACKS),
                        forShimmer = true,
                        onEntryClick = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}