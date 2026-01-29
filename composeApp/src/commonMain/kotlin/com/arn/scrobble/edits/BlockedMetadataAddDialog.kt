package com.arn.scrobble.edits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.db.BlockPlayerAction
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.BlockedMetadataDao.Companion.insertLowerCase
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.navigation.enumSaver
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.InlineCheckButton
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.any_value
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.artist_channel
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.do_nothing
import pano_scrobbler.composeapp.generated.resources.mute
import pano_scrobbler.composeapp.generated.resources.player_actions
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.skip
import pano_scrobbler.composeapp.generated.resources.track
import pano_scrobbler.composeapp.generated.resources.use_channel

@Composable
private fun BlockedMetadataAddContent(
    blockedMetadata: BlockedMetadata?,
    ignoredArtist: String?,
    onSave: (BlockedMetadata) -> Unit,
    onNavigateToBilling: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var artist by rememberSaveable(blockedMetadata) { mutableStateOf(blockedMetadata?.artist.orEmpty()) }
    var hasArtist by rememberSaveable(blockedMetadata) {
        mutableStateOf(blockedMetadata?.artist?.isNotEmpty() ?: true)
    }
    var albumArtist by rememberSaveable(blockedMetadata) { mutableStateOf(blockedMetadata?.albumArtist.orEmpty()) }
    var hasAlbumArtist by rememberSaveable(blockedMetadata) {
        mutableStateOf(blockedMetadata?.albumArtist?.isNotEmpty() ?: false)
    }
    var album by rememberSaveable(blockedMetadata) { mutableStateOf(blockedMetadata?.album.orEmpty()) }
    var hasAlbum by rememberSaveable(blockedMetadata) {
        mutableStateOf(blockedMetadata?.album?.isNotEmpty() ?: true)
    }
    var track by rememberSaveable(blockedMetadata) { mutableStateOf(blockedMetadata?.track.orEmpty()) }
    var hasTrack by rememberSaveable(blockedMetadata) {
        mutableStateOf(blockedMetadata?.track?.isNotEmpty() ?: true)
    }
    var blockPlayerAction by rememberSaveable(blockedMetadata, saver = enumSaver()) {
        mutableStateOf(
            blockedMetadata?.blockPlayerAction ?: BlockPlayerAction.ignore
        )
    }
    var useChannel by rememberSaveable(blockedMetadata) { mutableStateOf(false) }
    var errorText by rememberSaveable(blockedMetadata) { mutableStateOf<String?>(null) }
    val emptyText = stringResource(Res.string.required_fields_empty)
    val anythingText = "< " + stringResource(Res.string.any_value) + " >"

    val isLicenseValid = LocalLicenseValidState.current

    LaunchedEffect(useChannel) {
        if (ignoredArtist != null) {
            artist = if (useChannel) {
                ignoredArtist
            } else {
                blockedMetadata?.artist.orEmpty()
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        PanoOutlinedTextField(
            value = if (hasTrack) track else anythingText,
            onValueChange = { track = it },
            label = { Text(stringResource(Res.string.track)) },
            isError = hasTrack && track.isEmpty(),
            leadingIcon = {
                InlineCheckButton(
                    checked = hasTrack,
                    onCheckedChange = { hasTrack = it }
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            enabled = isLicenseValid && hasTrack,
            enabledOnTv = false,
            modifier = Modifier
                .fillMaxWidth()
        )

        PanoOutlinedTextField(
            value = if (hasArtist) artist else anythingText,
            onValueChange = { artist = it },
            label = {
                Text(
                    stringResource(
                        if (PlatformStuff.isDesktop)
                            Res.string.artist
                        else
                            Res.string.artist_channel
                    )
                )
            },
            isError = hasArtist && artist.isEmpty(),
            leadingIcon = {
                InlineCheckButton(
                    checked = hasArtist,
                    onCheckedChange = { hasArtist = it }
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            enabled = isLicenseValid && hasArtist,
            enabledOnTv = false,
            modifier = Modifier
                .fillMaxWidth()
        )

        PanoOutlinedTextField(
            value = if (hasAlbum) album else anythingText,
            onValueChange = { album = it },
            label = { Text(stringResource(Res.string.album)) },
            leadingIcon = {
                InlineCheckButton(
                    checked = hasAlbum,
                    onCheckedChange = { hasAlbum = it }
                )
            },
            isError = hasAlbum && album.isEmpty(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next
            ),
            enabled = isLicenseValid && hasAlbum,
            enabledOnTv = false,
            modifier = Modifier
                .fillMaxWidth()
        )

        PanoOutlinedTextField(
            value = if (hasAlbumArtist) albumArtist else anythingText,
            onValueChange = { albumArtist = it },
            label = { Text(stringResource(Res.string.album_artist)) },
            leadingIcon = {
                InlineCheckButton(
                    checked = hasAlbumArtist,
                    onCheckedChange = { hasAlbumArtist = it }
                )
            },
            isError = hasAlbumArtist && albumArtist.isEmpty(),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            enabled = isLicenseValid && hasAlbumArtist,
            enabledOnTv = false,
            modifier = Modifier
                .fillMaxWidth()
        )

        if (ignoredArtist != null && ignoredArtist != blockedMetadata?.artist) {
            LabeledCheckbox(
                text = stringResource(Res.string.use_channel),
                checked = useChannel,
                onCheckedChange = { useChannel = it },
                enabled = isLicenseValid,
            )
        }

        BlockPlayerActions(
            blockPlayerAction = blockPlayerAction,
            onChange = { blockPlayerAction = it },
            enabled = isLicenseValid,
        )

        ErrorText(errorText)

        OutlinedButton(
            onClick = {
                if (!isLicenseValid) {
                    onNavigateToBilling()
                } else {
                    val newBlockedMetadata = BlockedMetadata(
                        _id = blockedMetadata?._id ?: 0,
                        artist = if (hasArtist) artist else "",
                        albumArtist = if (hasAlbumArtist) albumArtist else "",
                        album = if (hasAlbum) album else "",
                        track = if (hasTrack) track else "",
                        blockPlayerAction = blockPlayerAction,
                    )
                    if (listOf(artist, albumArtist, album, track).all { it.isEmpty() } ||
                        hasArtist && artist.isEmpty() ||
                        hasAlbumArtist && albumArtist.isEmpty() ||
                        hasAlbum && album.isEmpty() ||
                        hasTrack && track.isEmpty()
                    ) {
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
fun BlockPlayerActions(
    blockPlayerAction: BlockPlayerAction,
    onChange: (BlockPlayerAction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.player_actions),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            FilterChip(
                selected = blockPlayerAction == BlockPlayerAction.skip,
                onClick = {
                    onChange(BlockPlayerAction.skip)
                },
                label = { Text(stringResource(Res.string.skip)) },
                enabled = enabled,
            )
            FilterChip(
                selected = blockPlayerAction == BlockPlayerAction.mute,
                onClick = {
                    onChange(BlockPlayerAction.mute)
                },
                label = { Text(stringResource(Res.string.mute)) },
                enabled = enabled,
            )
            FilterChip(
                selected = blockPlayerAction == BlockPlayerAction.ignore,
                onClick = {
                    onChange(BlockPlayerAction.ignore)
                },
                label = { Text(stringResource(Res.string.do_nothing)) },
                enabled = enabled,
            )
        }
    }
}

@Composable
fun BlockedMetadataAddDialog(
    blockedMetadata: BlockedMetadata?,
    ignoredArtist: String?,
    hash: Int?,
    onDismiss: () -> Unit,
    onNavigateToBilling: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    BlockedMetadataAddContent(
        blockedMetadata = blockedMetadata,
        ignoredArtist = ignoredArtist,
        onSave = { blockedMetadata ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    PanoDb.db.getBlockedMetadataDao()
                        .insertLowerCase(listOf(blockedMetadata), ignore = false)
                }

                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.TrackCancelled(
                        hash = hash,
                        showUnscrobbledNotification = false,
                        blockedMetadata = blockedMetadata,
                    ),
                )

                onDismiss()
            }
        },
        onNavigateToBilling = onNavigateToBilling,
        modifier = modifier
    )
}
