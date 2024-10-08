package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.R
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.SearchBox
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import com.valentinilk.shimmer.shimmer

@Composable
fun SimpleEditsScreen(
    viewModel: SimpleEditsVM = viewModel(),
    onEdit: (SimpleEdit?) -> Unit,
    modifier: Modifier = Modifier
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
            text = pluralStringResource(R.plurals.num_simple_edits, 0, 0)
        )

        AnimatedVisibility(
            visible = simpleEdits == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val shimmerEdits = remember { List(10) { SimpleEdit(_id = it) } }
            LazyColumn(
                contentPadding = panoContentPadding(),
                modifier = Modifier
                    .fillMaxSize()
                    .shimmer()
            ) {
                items(shimmerEdits,
                    key = { it._id }
                ) { edit ->
                    SimpleEditItem(edit, forShimmer = true, onEdit = {}, onDelete = {})
                }
            }
        }

        AnimatedVisibility(
            visible = !simpleEdits.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LazyColumn(
                contentPadding = panoContentPadding(),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(simpleEdits!!,
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

@Composable
private fun SimpleEditItem(
    edit: SimpleEdit,
    forShimmer: Boolean = false,
    onEdit: (SimpleEdit?) -> Unit,
    onDelete: (SimpleEdit) -> Unit,
    modifier: Modifier = Modifier
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
                .clickable(enabled = !forShimmer) { onEdit(edit) }
                .padding(8.dp)
                .backgroundForShimmer(forShimmer)
        ) {
            Text(
                text = edit.track,
                style = MaterialTheme.typography.titleMedium,
            )
            if (edit.album.isNotBlank()) {
                Text(
                    text = edit.album,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(start = 16.dp)

                )
            }
            Text(
                text = edit.artist,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(start = 16.dp)

            )
        }

        EditsDeleteMenu(
            onDelete = { onDelete(edit) },
            enabled = !forShimmer
        )

    }
}