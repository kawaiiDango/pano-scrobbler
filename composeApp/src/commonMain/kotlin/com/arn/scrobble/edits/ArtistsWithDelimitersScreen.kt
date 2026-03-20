package com.arn.scrobble.edits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.db.ArtistWithDelimiters
import com.arn.scrobble.icons.Edit
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchField
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add_exception
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.rank_change_no_change

@Composable
fun ArtistsWithDelimitersScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistsWithDelimitersVM = viewModel { ArtistsWithDelimitersVM() },
) {
    val artists by viewModel.artistsFiltered.collectAsStateWithLifecycle()
    val searchTermToFirstArtist by viewModel.searchTermToFirstArtist.collectAsStateWithLifecycle()
    var searchTerm by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchTerm) {
        viewModel.setFilter(searchTerm)
    }

    Column(modifier = modifier) {
        SearchField(
            searchTerm = searchTerm,
            onSearchTermChange = {
                searchTerm = it
            },
            label = stringResource(Res.string.artist),
            icon = Icons.Mic,
            modifier = Modifier
                .padding(panoContentPadding(bottom = false))
        )

        PanoLazyColumn(
            contentPadding = panoContentPadding(mayHaveBottomFab = true),
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (artists == null) {
                items(
                    10,
                    key = { "shimmer_$it" }
                ) { idx ->
                    ArtistItem(
                        artist = ArtistWithDelimiters(
                            _id = idx.toLong(),
                            artist = "",
                        ),
                        forShimmer = true,
                        onDelete = {},
                        modifier = Modifier.shimmerWindowBounds().animateItem()
                    )
                }
            } else {
                if (searchTermToFirstArtist != null && searchTerm.isNotBlank()) {
                    val (searchTermTrimmed, firstArtist) = searchTermToFirstArtist!!

                    item(key = "first_artist") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 2.dp
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Edit,
                                        contentDescription = stringResource(Res.string.edit),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                    )
                                    Text(
                                        firstArtist,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }

                                if (!firstArtist.equals(searchTermTrimmed, true)) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.insert(searchTermTrimmed)
                                        },
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                    ) {
                                        Text(
                                            stringResource(Res.string.add_exception)
                                        )
                                    }
                                } else {
                                    Text(
                                        stringResource(Res.string.rank_change_no_change),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    artists!!,
                    key = { it._id }
                ) { artist ->
                    ArtistItem(
                        artist,
                        onDelete = { viewModel.delete(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistItem(
    artist: ArtistWithDelimiters,
    onDelete: (ArtistWithDelimiters) -> Unit,
    modifier: Modifier = Modifier,
    forShimmer: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
    ) {
        Text(
            text = artist.artist,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium,
        )

        EditsDeleteMenu(
            onDelete = { onDelete(artist) },
            enabled = !forShimmer
        )
    }
}