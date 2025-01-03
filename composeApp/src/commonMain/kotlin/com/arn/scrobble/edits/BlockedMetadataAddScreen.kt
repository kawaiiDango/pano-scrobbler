package com.arn.scrobble.edits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.BlockedMetadataDao.Companion.insertLowerCase
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.ui.DialogParent
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.InfoText
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.artist_channel
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.blocked_metadata_info
import pano_scrobbler.composeapp.generated.resources.ignore
import pano_scrobbler.composeapp.generated.resources.mute
import pano_scrobbler.composeapp.generated.resources.player_actions
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.skip
import pano_scrobbler.composeapp.generated.resources.track
import pano_scrobbler.composeapp.generated.resources.use_channel

@Composable
private fun BlockedMetadataAddContent(
    blockedMetadata: BlockedMetadata,
    ignoredArtist: String?,
    onSave: (BlockedMetadata) -> Unit,
    onNavigateToBilling: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var artist by remember { mutableStateOf(blockedMetadata.artist) }
    var albumArtist by remember { mutableStateOf(blockedMetadata.albumArtist) }
    var album by remember { mutableStateOf(blockedMetadata.album) }
    var track by remember { mutableStateOf(blockedMetadata.track) }
    var skip by remember { mutableStateOf(blockedMetadata.skip) }
    var mute by remember { mutableStateOf(blockedMetadata.mute) }
    var useChannel by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val emptyText = stringResource(Res.string.required_fields_empty)

    LaunchedEffect(useChannel) {
        if (ignoredArtist != null) {
            artist = if (useChannel) {
                ignoredArtist
            } else {
                blockedMetadata.artist
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            value = track,
            onValueChange = { track = it },
            label = { Text(stringResource(Res.string.track)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text(stringResource(Res.string.album)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ), modifier = Modifier
                .fillMaxWidth()
        )

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text(stringResource(Res.string.artist_channel)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ), modifier = Modifier
                .fillMaxWidth()
        )

        OutlinedTextField(
            value = albumArtist,
            onValueChange = { albumArtist = it },
            label = { Text(stringResource(Res.string.album_artist)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        if (ignoredArtist != null && ignoredArtist != blockedMetadata.artist) {
            LabeledCheckbox(
                text = stringResource(Res.string.use_channel),
                checked = useChannel,
                onCheckedChange = { useChannel = it }
            )
        }

        Text(
            text = stringResource(Res.string.player_actions),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = skip,
                onClick = {
                    skip = true
                    mute = false
                },
                label = { Text(stringResource(Res.string.skip)) }
            )
            FilterChip(
                selected = mute,
                onClick = {
                    mute = true
                    skip = false
                },
                label = { Text(stringResource(Res.string.mute)) }
            )
            FilterChip(
                selected = !skip && !mute,
                onClick = {
                    skip = false
                    mute = false
                },
                label = { Text(stringResource(Res.string.ignore)) }
            )
        }

        InfoText(
            text = stringResource(Res.string.blocked_metadata_info)
                .replace("ℹ️", "")
                .trimStart(),
            style = MaterialTheme.typography.bodyMedium,
        )

        ErrorText(errorText)

        TextButton(
            onClick = {
                if (!PlatformStuff.billingRepository.isLicenseValid) {
                    onNavigateToBilling()
                } else {
                    val newBlockedMetadata = blockedMetadata.copy(
                        artist = artist,
                        albumArtist = albumArtist,
                        album = album,
                        track = track,
                        skip = skip,
                        mute = mute
                    )
                    if (listOf(artist, albumArtist, album, track).all { it.isEmpty() }) {
                        errorText = emptyText
                    } else {
                        onSave(newBlockedMetadata)
                    }
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(Res.string.block))
        }
    }
}

@Composable
fun BlockedMetadataAddScreen(
    blockedMetadata: BlockedMetadata,
    ignoredArtist: String?,
    hash: Int?,
    onDismiss: () -> Unit,
    onNavigateToBilling: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    DialogParent(
        onDismiss = onDismiss
    ) {
        BlockedMetadataAddContent(
            blockedMetadata = blockedMetadata,
            ignoredArtist = ignoredArtist,
            onSave = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        PanoDb.db.getBlockedMetadataDao()
                            .insertLowerCase(listOf(it), ignore = false)
                    }

                    if (hash != 0) {
                        // todo reimplement
//                        val i = Intent(NLService.iBLOCK_ACTION_S).apply {
//                            `package` = AndroidStuff.application.packageName
//                            putSingle(it)
//                            putExtra(NLService.B_HASH, hash)
//                        }
//                        AndroidStuff.application.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                    }

                    onDismiss()
                }
            },
            onNavigateToBilling = onNavigateToBilling,
            modifier = it
        )
    }
}
