package com.arn.scrobble.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.SearchType
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ExpandableHeaderMenu
import com.arn.scrobble.ui.expandableSublist
import com.arn.scrobble.ui.MusicEntryListItem
import com.arn.scrobble.ui.SearchBox
import com.arn.scrobble.ui.getMusicEntryPlaceholderItem
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.mapLatest

@Composable
fun SearchScreen(
    viewModel: SearchVM = viewModel(),
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(null)
    val hasLoaded by viewModel.hasLoaded.collectAsStateWithLifecycle()

    var searchTerm by rememberSaveable { mutableStateOf("") }
    var searchType by rememberSaveable { mutableStateOf(SearchType.GLOBAL) }

    var artistsExpanded by rememberSaveable { mutableStateOf(false) }
    var albumsExpanded by rememberSaveable { mutableStateOf(false) }
    var tracksExpanded by rememberSaveable { mutableStateOf(false) }
    var lovedExpanded by rememberSaveable { mutableStateOf(false) }

    val artistsText = stringResource(R.string.artists)
    val albumsText = stringResource(R.string.albums)
    val tracksText = stringResource(R.string.tracks)
    val lovedText = stringResource(R.string.loved)

    val userSelf by Scrobblables.current.mapLatest { it?.userAccount?.user }
        .collectAsStateWithLifecycle(null)

    fun onItemClick(item: MusicEntry) {
        userSelf?.let { userSelf ->
            onNavigate(
                PanoRoute.MusicEntryInfo(
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

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        SearchBox(
            searchTerm = searchTerm,
            onSearchTermChange = { searchTerm = it },
            modifier = Modifier.padding(panoContentPadding(bottom = false))
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

        AnimatedVisibility(
            hasLoaded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LazyColumn(
                contentPadding = panoContentPadding(),
                modifier = Modifier.fillMaxSize()
            ) {
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
                    item(R.string.reindex) {
                        ExpandableHeaderMenu(
                            title = stringResource(
                                R.string.searched_n_items,
                                Stuff.MAX_INDEXED_ITEMS.format()
                            ),
                            icon = Icons.Outlined.Info,
                            menuItemText = stringResource(R.string.reindex),
                            onMenuItemClick = {
                                onNavigate(PanoRoute.Index)
                            }
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            searchResults != null && !hasLoaded,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LazyColumn(
                contentPadding = panoContentPadding(),
                modifier = Modifier
                    .fillMaxSize()
                    .shimmer()
            ) {
                items(
                    10,
                    key = { it }
                ) {
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