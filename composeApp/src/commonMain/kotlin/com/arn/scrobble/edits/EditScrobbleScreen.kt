package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.icons.AlbumArtist
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.ui.DialogParent
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.VerifyButton
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.album_optional
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.no
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.regex_edits_suggestion
import pano_scrobbler.composeapp.generated.resources.swap
import pano_scrobbler.composeapp.generated.resources.track
import pano_scrobbler.composeapp.generated.resources.yes


@Composable
private fun EditScrobbleContent(
    onDone: (ScrobbleData) -> Unit,
    onReauthenticate: () -> Unit,
    scrobbleData: ScrobbleData,
    msid: String?,
    hash: Int,
    onNavigateToRegexEdits: () -> Unit,
    viewModel: EditScrobbleViewModel = viewModel { EditScrobbleViewModel() },
    modifier: Modifier = Modifier,
) {
    var track by rememberSaveable { mutableStateOf(scrobbleData.track) }
    var album by rememberSaveable { mutableStateOf(scrobbleData.album ?: "") }
    var artist by rememberSaveable { mutableStateOf(scrobbleData.artist) }
    var albumArtist by rememberSaveable { mutableStateOf("") }
    var albumArtistVisible by rememberSaveable { mutableStateOf(false) }
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val regexRecommendation by viewModel.regexRecommendation.collectAsStateWithLifecycle()
    var regexRecommendationShown by remember { mutableStateOf(false) }
    var reauthenticateButtonShown by remember { mutableStateOf(false) }


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
            hash = hash
        )
    }

    LaunchedEffect(Unit) {
        notifyPlayingTrackEvent(
            PlayingTrackNotifyEvent.TrackScrobbleLocked(
                hash = hash,
                locked = true
            )
        )
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

    LaunchedEffect(regexRecommendation) {
        regexRecommendationShown = regexRecommendation != null
    }


    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            value = track,
            onValueChange = { track = it },
            label = { Text(stringResource(Res.string.track)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            value = album,
            onValueChange = { album = it },
            label = { Text(stringResource(Res.string.album_optional)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            value = artist,
            onValueChange = { artist = it },
            label = { Text(stringResource(Res.string.artist)) },
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
                singleLine = true,
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

    if (regexRecommendationShown && regexRecommendation != null) {
        ShowRegexRecommendation(
            regexRecommendation = regexRecommendation!!,
            onDismissRequest = { regexRecommendationShown = false },
            onNavigateToRegexEdits = onNavigateToRegexEdits
        )
    }
}

@Composable
private fun ShowRegexRecommendation(
    regexRecommendation: RegexEdit,
    onDismissRequest: () -> Unit,
    onNavigateToRegexEdits: () -> Unit,
) {
    val presetName = RegexPresets.getString(regexRecommendation.preset!!)
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Text(
                stringResource(
                    Res.string.regex_edits_suggestion,
                    presetName
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onNavigateToRegexEdits()
                }
            ) {
                Text(stringResource(Res.string.yes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    scope.launch {
                        PlatformStuff.mainPrefs.updateData { it.copy(regexEditsLearnt = true) }
                    }
                }
            ) {
                Text(stringResource(Res.string.no))
            }
        }
    )
}

@Composable
fun EditScrobbleDialog(
    scrobbleData: ScrobbleData,
    msid: String?,
    hash: Int,
    onDone: (ScrobbleData) -> Unit,
    onDismiss: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
) {

    DialogParent(
        onDismiss = onDismiss
    ) {
        EditScrobbleContent(
            onDone = {
                onDone(it)
                onDismiss()
            },
            onReauthenticate = {
                onDismiss()
                val route = LoginDestinations.route(AccountType.LASTFM)
                onNavigate(route)
            },
            scrobbleData = scrobbleData,
            msid = msid,
            hash = hash,
            onNavigateToRegexEdits = {
                onDismiss()
                onNavigate(PanoRoute.RegexEdits)
            },
            modifier = it
        )
    }
}
