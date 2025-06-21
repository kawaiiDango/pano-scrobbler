package com.arn.scrobble.edits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchBox
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.Stuff
import org.jetbrains.compose.resources.pluralStringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.num_simple_edits

@Composable
fun SimpleEditsScreen(
    onEdit: (SimpleEdit?) -> Unit,
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
            SearchBox(
                searchTerm = searchTerm,
                onSearchTermChange = {
                    searchTerm = it
                },
                modifier = Modifier
                    .padding(panoContentPadding(bottom = false))
            )
        }

        EmptyText(
            visible = simpleEdits?.isEmpty() == true,
            text = pluralStringResource(Res.plurals.num_simple_edits, 0, 0)
        )

        PanoLazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (simpleEdits == null) {
                val shimmerEdits = List(10) { SimpleEdit(_id = it) }
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
                        onEdit = onEdit,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .padding(horizontal = 8.dp)
    ) {
        if (edit.legacyHash == null) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = null,
                modifier = Modifier
                    .alpha(0.5f)
                    .size(56.dp)
                    .padding(12.dp)
            )
        } else {
            Spacer(modifier = Modifier.size(60.dp))
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !forShimmer && edit.legacyHash == null) { onEdit(edit) }
                .padding(8.dp)
                .backgroundForShimmer(forShimmer)
        ) {
            Text(
                text = edit.track,
                style = MaterialTheme.typography.titleMediumEmphasized,
            )

            Text(
                text = edit.artist,
                style = MaterialTheme.typography.bodyLargeEmphasized,
            )

            if (edit.album.isNotBlank()) {
                Text(
                    text = edit.album,
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