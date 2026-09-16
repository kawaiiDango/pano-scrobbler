package com.arn.scrobble.edits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.db.BlockPlayerAction
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.icons.SkipNext
import com.arn.scrobble.icons.VolumeOffAutoMirrored
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.panoicons.AlbumArtist
import com.arn.scrobble.panoicons.PanoIcons
import com.arn.scrobble.ui.EmptyTextWithImportButtonOnTv
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchEffect
import com.arn.scrobble.ui.TextWithIcon
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.myCheckableItemColors
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.shimmerWindowBounds
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.mute
import pano_scrobbler.composeapp.generated.resources.pref_blocked_metadata
import pano_scrobbler.composeapp.generated.resources.skip

@Composable
fun BlockedMetadatasScreen(
    searchFieldState: TextFieldState,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BlockedMetadataVM = viewModel { BlockedMetadataVM() },
) {
    val blockedMetadatas by viewModel.blockedMetadataFiltered.collectAsStateWithLifecycle()

    SearchEffect(searchFieldState) {
        viewModel.setFilter(it)
    }

    Column(modifier = modifier) {
        EmptyTextWithImportButtonOnTv(
            visible = blockedMetadatas?.isEmpty() == true,
            text = stringResource(Res.string.pref_blocked_metadata) + ": " + 0,
            onButtonClick = {
                onNavigate(PanoRoute.Import)
            }
        )

        PanoLazyColumn(
            contentPadding = panoContentPadding(mayHaveBottomFab = true),
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (blockedMetadatas == null) {
                val shimmerEdits = List(10) {
                    BlockedMetadata(
                        _id = it.toLong(),
                        track = " ",
                        artist = "",
                        album = "",
                        albumArtist = "",
                    )
                }
                items(
                    shimmerEdits,
                    key = { "shimmer_$it" }
                ) {
                    BlockedMetadataItem(
                        it,
                        forShimmer = true,
                        onEdit = {},
                        onDelete = {},
                        modifier = Modifier
                            .shimmerWindowBounds()
                            .animateItem()
                    )
                }
            } else {
                items(
                    blockedMetadatas!!,
                    key = { it._id }
                ) {
                    BlockedMetadataItem(
                        it,
                        onEdit = {
                            onNavigate(PanoRoute.Modal.BlockedMetadataAdd(it))
                        },
                        onDelete = {
                            viewModel.delete(it)
                        },
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    forShimmer: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {
        ListItem(
            enabled = !forShimmer,
            onClick = onEdit,
            verticalAlignment = Alignment.CenterVertically,
            colors = ListItemDefaults.myCheckableItemColors(),
            trailingContent = when (blockedMetadata.blockPlayerAction) {
                BlockPlayerAction.skip -> {
                    {
                        Icon(
                            imageVector = Icons.SkipNext,
                            contentDescription = stringResource(Res.string.skip),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                BlockPlayerAction.mute -> {
                    {
                        Icon(
                            imageVector = Icons.VolumeOffAutoMirrored,
                            contentDescription = stringResource(Res.string.mute),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                else -> null
            },
            modifier = Modifier
                .weight(1f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TextWithIcon(
                    text = blockedMetadata.track.ifEmpty { "*" },
                    icon = Icons.MusicNote,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier
                        .fillMaxWidth()
                        .backgroundForShimmer(forShimmer)
                )

                TextWithIcon(
                    text = blockedMetadata.artist.ifEmpty { "*" },
                    icon = Icons.Mic,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                TextWithIcon(
                    text = blockedMetadata.album.ifEmpty { "*" },
                    icon = Icons.Album,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                TextWithIcon(
                    text = blockedMetadata.albumArtist.ifEmpty { "*" },
                    icon = PanoIcons.AlbumArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }

        EditsDeleteMenu(
            onDelete = onDelete,
            enabled = !forShimmer
        )
    }
}