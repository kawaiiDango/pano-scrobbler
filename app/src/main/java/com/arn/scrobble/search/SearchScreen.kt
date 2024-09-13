package com.arn.scrobble.search

import android.os.Bundle
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.SearchType
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ExpandableHeaderMenu
import com.arn.scrobble.ui.ExpandableSublist
import com.arn.scrobble.ui.ExtraBottomSpace
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.ui.SearchBox
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.arn.scrobble.utils.Stuff.putData
import com.valentinilk.shimmer.shimmer

@Composable
private fun SearchContent(
    viewModel: SearchVM = viewModel(),
    onItemClick: (MusicEntry) -> Unit,
    onReindexClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(null)
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()

    var searchTerm by rememberSaveable { mutableStateOf("") }
    var searchType by rememberSaveable { mutableStateOf(SearchType.GLOBAL) }

    var artistsExpanded by rememberSaveable { mutableStateOf(false) }
    var albumsExpanded by rememberSaveable { mutableStateOf(false) }
    var tracksExpanded by rememberSaveable { mutableStateOf(false) }
    var lovedExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(searchTerm, searchType) {
        viewModel.search(searchTerm, searchType)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        SearchBox(
            searchTerm = searchTerm,
            onSearchTermChange = { searchTerm = it },
        )

        if (BuildConfig.DEBUG) {

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = searchType == SearchType.GLOBAL,
                    onClick = { searchType = SearchType.GLOBAL },
                    label = { Text(text = stringResource(R.string.global)) }
                )
                FilterChip(
                    selected = searchType == SearchType.LOCAL,
                    onClick = { searchType = SearchType.LOCAL },
                    label = { Text(text = stringResource(R.string.library)) }
                )
            }
        }

        EmptyText(
            text = stringResource(R.string.not_found),
            visible = hasLoaded && searchResults?.isEmpty == true,
        )

        AnimatedVisibility(hasLoaded) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                ExpandableSublist(
                    headerRes = R.string.artists,
                    headerIcon = Icons.Outlined.Mic,
                    items = searchResults?.artists ?: emptyList(),
                    expanded = artistsExpanded,
                    onToggle = { artistsExpanded = it },
                    onItemClick = onItemClick,
                )

                ExpandableSublist(
                    headerRes = R.string.albums,
                    headerIcon = Icons.Outlined.Album,
                    items = searchResults?.albums ?: emptyList(),
                    expanded = albumsExpanded,
                    onToggle = { albumsExpanded = it },
                    onItemClick = onItemClick,
                )

                ExpandableSublist(
                    headerRes = R.string.tracks,
                    headerIcon = Icons.Outlined.MusicNote,
                    items = searchResults?.tracks ?: emptyList(),
                    expanded = tracksExpanded,
                    onToggle = { tracksExpanded = it },
                    onItemClick = onItemClick,
                    fetchAlbumImageIfMissing = true,
                )

                ExpandableSublist(
                    headerRes = R.string.loved,
                    headerIcon = Icons.Outlined.FavoriteBorder,
                    items = searchResults?.lovedTracks ?: emptyList(),
                    expanded = lovedExpanded,
                    onToggle = { lovedExpanded = it },
                    onItemClick = onItemClick,
                    fetchAlbumImageIfMissing = true,
                )

                if (searchType == SearchType.LOCAL) {
                    item(R.string.reindex) {
                        ExpandableHeaderMenu(
                            title = stringResource(
                                R.string.searched_n_items,
                                Stuff.MAX_INDEXED_ITEMS.format()
                            ),
                            icon = Icons.Outlined.Info,
                            menuItemText = stringResource(R.string.reindex),
                            onMenuClick = onReindexClick
                        )
                    }
                }

                item("extraBottomSpace") {
                    ExtraBottomSpace()
                }
            }
        }
        AnimatedVisibility(searchResults != null && !hasLoaded) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmer()
            ) {
                items(
                    10,
                    key = { it }
                ) {
                    MusicEntryListItem(
                        Track(
                            name = "",
                            artist = Artist(""),
                            album = Album(""),
                        ),
                        forShimmer = true,
                        onTrackClick = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Keep
@Composable
fun SearchScreen() {
    val fragment = LocalFragment.current

    ScreenParent {
        SearchContent(
            onItemClick = {
                val args = Bundle().apply {
                    putData(it)
                }

                fragment.findNavController().navigate(R.id.infoFragment, args)
            },

            onReindexClick = {
                fragment.findNavController().navigate(R.id.indexingDialogFragment)
            },
            modifier = it
        )
    }
}