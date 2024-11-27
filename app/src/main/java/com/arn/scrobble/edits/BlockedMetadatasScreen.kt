package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.SkipNext
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.R
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.SearchBox
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import com.valentinilk.shimmer.shimmer

@Composable
fun BlockedMetadatasScreen(
    viewModel: BlockedMetadataVM = viewModel(),
    onEdit: (BlockedMetadata) -> Unit,
    modifier: Modifier = Modifier,
) {
    val blockedMetadatas by viewModel.blockedMetadataFiltered.collectAsStateWithLifecycle()
    val count by viewModel.count.collectAsStateWithLifecycle(0)
    var searchTerm by rememberSaveable { mutableStateOf("") }

    fun doDelete(edit: BlockedMetadata) {
        viewModel.delete(edit)
    }

    LaunchedEffect(searchTerm) {
        viewModel.setFilter(searchTerm)
    }

    Column(modifier = modifier) {
        if (count > Stuff.MIN_ITEMS_TO_SHOW_SEARCH) {
            SearchBox(
                searchTerm = searchTerm,
                onSearchTermChange = {
                    searchTerm = it
                    viewModel.setFilter(it)
                },
                modifier = Modifier.padding(panoContentPadding(bottom = false))
            )
        }

        EmptyText(
            visible = blockedMetadatas?.isEmpty() == true,
            text = pluralStringResource(R.plurals.num_blocked_metadata, 0, 0)
        )

        AnimatedVisibility(
            visible = blockedMetadatas == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val shimmerEdits = remember { List(10) { BlockedMetadata(track = " ", _id = it) } }
            LazyColumn(
                contentPadding = panoContentPadding(),
                modifier = Modifier
                    .fillMaxSize()
                    .shimmer()
            ) {
                items(
                    shimmerEdits,
                    key = { it._id }
                ) {
                    BlockedMetadataItem(it, forShimmer = true, onEdit = {}, onDelete = {})
                }
            }
        }

        AnimatedVisibility(
            visible = !blockedMetadatas.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LazyColumn(
                contentPadding = panoContentPadding(),
                modifier = Modifier
                    .fillMaxSize()
            ) {
                items(
                    blockedMetadatas!!,
                    key = { it._id }
                ) { edit ->
                    BlockedMetadataItem(
                        edit,
                        onEdit = onEdit,
                        onDelete = ::doDelete,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedMetadataItem(
    blockedMetadata: BlockedMetadata,
    onEdit: (BlockedMetadata) -> Unit,
    onDelete: (BlockedMetadata) -> Unit,
    forShimmer: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 56.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !forShimmer) { onEdit(blockedMetadata) }
                .padding(8.dp)
        ) {
            if (blockedMetadata.track.isNotEmpty()) {
                Text(
                    text = blockedMetadata.track,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .backgroundForShimmer(forShimmer)
                )
            }
            if (blockedMetadata.artist.isNotEmpty()) {
                Text(
                    text = blockedMetadata.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                )
            }
            if (blockedMetadata.album.isNotEmpty()) {
                Text(
                    text = blockedMetadata.album,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                )
            }
            if (blockedMetadata.albumArtist.isNotEmpty()) {
                Text(
                    text = blockedMetadata.albumArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                )
            }
        }

        if (blockedMetadata.skip) {
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = stringResource(R.string.skip),
                tint = MaterialTheme.colorScheme.secondary,
            )
        } else if (blockedMetadata.mute) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.VolumeOff,
                contentDescription = stringResource(R.string.mute),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }

        EditsDeleteMenu(
            onDelete = { onDelete(blockedMetadata) },
            enabled = !forShimmer
        )
    }
}