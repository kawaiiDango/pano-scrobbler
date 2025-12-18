package com.arn.scrobble.edits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.automirrored.ArrowRight
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.EmptyTextWithImportButtonOnTv
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchField
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.Stuff
import org.jetbrains.compose.resources.pluralStringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.num_simple_edits

@Composable
fun SimpleEditsScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SimpleEditsVM = viewModel { SimpleEditsVM() },
) {
    val simpleEdits by viewModel.simpleEditsFiltered.collectAsStateWithLifecycle()
    val count by viewModel.count.collectAsStateWithLifecycle(0)
    var searchTerm by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchTerm) {
        viewModel.setFilter(searchTerm)
    }

    Column(modifier = modifier) {
        if (count > Stuff.MIN_ITEMS_TO_SHOW_SEARCH) {
            SearchField(
                searchTerm = searchTerm,
                onSearchTermChange = {
                    searchTerm = it
                },
                modifier = Modifier
                    .padding(panoContentPadding(bottom = false))
            )
        }

        EmptyTextWithImportButtonOnTv(
            visible = simpleEdits?.isEmpty() == true,
            text = pluralStringResource(Res.plurals.num_simple_edits, 0, 0),
            onButtonClick = {
                onNavigate(PanoRoute.Import)
            }
        )

        PanoLazyColumn(
            contentPadding = panoContentPadding(mayHaveBottomFab = true),
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (simpleEdits == null) {
                val shimmerEdits = List(10) {
                    SimpleEdit(
                        _id = it,
                        hasOrigAlbumArtist = true,
                        track = "",
                        artist = "",
                        album = "",
                        albumArtist = "",
                    )
                }
                items(
                    shimmerEdits,
                ) { edit ->
                    SimpleEditItem(
                        edit,
                        forShimmer = true,
                        onEdit = {},
                        onDelete = {},
                        modifier = Modifier.shimmerWindowBounds().animateItem()
                    )
                }
            } else {
                items(
                    simpleEdits!!,
                    key = { it._id }
                ) { edit ->
                    SimpleEditItem(
                        edit,
                        onEdit = {
                            onNavigate(PanoRoute.SimpleEditsAdd(it))
                        },
                        onDelete = { viewModel.delete(it) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SimpleEditItem(
    edit: SimpleEdit,
    onEdit: (SimpleEdit?) -> Unit,
    onDelete: (SimpleEdit) -> Unit,
    modifier: Modifier = Modifier,
    forShimmer: Boolean = false,
) {
    val wildcardStr = "*"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !forShimmer) { onEdit(edit) }
                .padding(8.dp)
                .backgroundForShimmer(forShimmer)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = edit.origTrack.takeIf { edit.hasOrigTrack } ?: wildcardStr,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Text(
                    text = edit.origArtist.takeIf { edit.hasOrigArtist } ?: wildcardStr,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                )
                Text(
                    text = edit.origAlbum.takeIf { edit.hasOrigAlbum } ?: wildcardStr,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                )
                Text(
                    text = edit.origAlbumArtist.takeIf { edit.hasOrigAlbumArtist } ?: wildcardStr,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.ArrowRight,
                contentDescription = null,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = edit.track ?: wildcardStr,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = edit.artist ?: wildcardStr,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = edit.album ?: wildcardStr,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = edit.albumArtist ?: wildcardStr,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        EditsDeleteMenu(
            onDelete = { onDelete(edit) },
            enabled = !forShimmer
        )

    }
}