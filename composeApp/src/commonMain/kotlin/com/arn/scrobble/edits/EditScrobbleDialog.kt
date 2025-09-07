package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.icons.AlbumArtist
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.VerifyButton
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.album_optional
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.edit_no_save
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.save
import pano_scrobbler.composeapp.generated.resources.swap
import pano_scrobbler.composeapp.generated.resources.track


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScrobbleDialog(
    onDone: (ScrobbleData) -> Unit,
    onReauthenticate: () -> Unit,
    scrobbleData: ScrobbleData,
    msid: String?,
    hash: Int?,
    modifier: Modifier = Modifier,
    viewModel: EditScrobbleViewModel = viewModel { EditScrobbleViewModel() },
) {
    var track by rememberSaveable { mutableStateOf(scrobbleData.track) }
    var album by rememberSaveable { mutableStateOf(scrobbleData.album ?: "") }
    var artist by rememberSaveable { mutableStateOf(scrobbleData.artist) }
    var albumArtist by rememberSaveable { mutableStateOf(scrobbleData.albumArtist ?: "") }
    var albumArtistVisible by rememberSaveable { mutableStateOf(!scrobbleData.albumArtist.isNullOrEmpty()) }
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    var reauthenticateButtonShown by remember { mutableStateOf(false) }
    var save by rememberSaveable { mutableStateOf(true) }

    val doEdit = {
        val newScrobbleData = scrobbleData.copy(
            track = track,
            album = album.takeIf { it.isNotBlank() },
            artist = artist,
            albumArtist = albumArtist.takeIf { it.isNotBlank() }
        )
        viewModel.doEdit(
            origScrobbleData = scrobbleData,
            newScrobbleData = newScrobbleData,
            msid = msid,
            isNowPlaying = hash != null,
            save = save,
        )
    }

    DisposableEffect(Unit) {
        if (hash != null) {
            notifyPlayingTrackEvent(
                PlayingTrackNotifyEvent.TrackScrobbleLocked(
                    hash = hash,
                    locked = true
                ),
            )
        }

        onDispose {
            if (hash != null) {
                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.TrackScrobbleLocked(
                        hash = hash,
                        locked = false
                    ),
                )
            }
        }
    }

    LaunchedEffect(result) {
        result?.onSuccess {
            onDone(it)
        }?.onFailure {
            if (it is LastfmUnscrobbler.CookiesInvalidatedException) {
                reauthenticateButtonShown = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updatedAlbum.collectLatest {
            album = it
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updatedAlbumArtist.collectLatest {
            albumArtist = it
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = track,
            onValueChange = { track = it },
            label = { Text(stringResource(Res.string.track)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = artist,
            onValueChange = { artist = it },
            label = { Text(stringResource(Res.string.artist)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = album,
            onValueChange = { album = it },
            label = { Text(stringResource(Res.string.album_optional)) },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction =
                    if (albumArtistVisible)
                        ImeAction.Next
                    else
                        ImeAction.Done
            ),
            keyboardActions = if (albumArtistVisible)
                KeyboardActions.Default
            else
                KeyboardActions(
                    onDone = {
                        doEdit()
                    }
                )
        )

        AnimatedVisibility(visible = albumArtistVisible) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = albumArtist,
                onValueChange = { albumArtist = it },
                label = { Text(stringResource(Res.string.album_artist)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        doEdit()
                    }
                )
            )
        }

        AnimatedVisibility(visible = !save) {
            Text(
                stringResource(Res.string.edit_no_save),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        VerifyButton(
            doStuff = doEdit,
            result = result,
            onDone = { },
            buttonText = stringResource(Res.string.edit),
        ) {
            if (reauthenticateButtonShown) {
                OutlinedButton(
                    colors = ButtonDefaults.outlinedButtonColors().copy(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    onClick = onReauthenticate,
                ) {
                    Text(stringResource(Res.string.pref_login))
                }
            } else {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above
                    ),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.save)) } },
                    state = rememberTooltipState(),
                ) {
                    IconToggleButton(
                        checked = save,
                        onCheckedChange = { save = it },
                        modifier = Modifier.alpha(
                            if (save) 1f else 0.5f
                        )
                    ) {
                        Icon(
                            imageVector = if (save)
                                Icons.Filled.Save
                            else
                                Icons.Outlined.Save,
                            contentDescription = stringResource(Res.string.save),
                        )
                    }
                }

                IconButtonWithTooltip(
                    onClick = {
                        // swap track and artist
                        val temp = track
                        track = artist
                        artist = temp
                    },
                    icon = Icons.Outlined.SwapVert,
                    contentDescription = stringResource(Res.string.swap),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            if (!albumArtistVisible) {
                IconButtonWithTooltip(
                    onClick = {
                        albumArtistVisible = true
                    },
                    icon = PanoIcons.AlbumArtist,
                    contentDescription = stringResource(Res.string.album_artist),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}