package com.arn.scrobble.edits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.result.ResultEffect
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Delete
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.KeyboardArrowUp
import com.arn.scrobble.icons.SwapVert
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.navigation.FabClickedResult
import com.arn.scrobble.panoicons.ContentSaveOffOutline
import com.arn.scrobble.panoicons.PanoIcons
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.InlineCheckButton
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.redactedMessage
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.any_value
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.corrected
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.disable
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.edit_continue_simple
import pano_scrobbler.composeapp.generated.resources.edit_example
import pano_scrobbler.composeapp.generated.resources.edit_no_save
import pano_scrobbler.composeapp.generated.resources.existing_value
import pano_scrobbler.composeapp.generated.resources.original
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.rank_change_no_change
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.save
import pano_scrobbler.composeapp.generated.resources.show_all
import pano_scrobbler.composeapp.generated.resources.swap
import pano_scrobbler.composeapp.generated.resources.track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleEditsAddScreen(
    simpleEdit: SimpleEdit?,
    onDone: () -> Unit,
    onReauthenticate: () -> Unit,
    origScrobbleData: ScrobbleData?,
    msid: String?,
    hash: Int?,
    key: String?,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
) {
    var hasOrigTrack by rememberSaveable { mutableStateOf(simpleEdit?.hasOrigTrack ?: true) }
    val origTrack = rememberTextFieldState(simpleEdit?.origTrack ?: "")
    var hasTrack by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.track != null) }
    val track = rememberTextFieldState(simpleEdit?.track ?: "")

    var hasOrigAlbum by rememberSaveable { mutableStateOf(simpleEdit?.hasOrigAlbum ?: true) }
    val origAlbum = rememberTextFieldState(simpleEdit?.origAlbum ?: "")
    var hasAlbum by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.album != null) }
    val album = rememberTextFieldState(simpleEdit?.album ?: "")

    var hasOrigArtist by rememberSaveable { mutableStateOf(simpleEdit?.hasOrigArtist ?: true) }
    val origArtist = rememberTextFieldState(simpleEdit?.origArtist ?: "")
    var hasArtist by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.artist != null) }
    val artist = rememberTextFieldState(simpleEdit?.artist ?: "")

    var continueMatching by rememberSaveable {
        mutableStateOf(simpleEdit?.continueMatching ?: true)
    }

    var reauthenticateButtonShown by remember { mutableStateOf(false) }
    var save by rememberSaveable { mutableStateOf(true) }
    val networkEditMode = simpleEdit != null && origScrobbleData != null

    var hasOrigAlbumArtist by rememberSaveable {
        mutableStateOf(simpleEdit?.hasOrigAlbumArtist ?: false)
    }
    val origAlbumArtist = rememberTextFieldState(simpleEdit?.origAlbumArtist ?: "")
    var hasAlbumArtist by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.albumArtist != null) }
    val albumArtist = rememberTextFieldState(simpleEdit?.albumArtist ?: "")

    val anythingText = "< " + stringResource(Res.string.any_value) + " >"
    val existingText = "< " + stringResource(Res.string.existing_value) + " >"
    val missingFieldsText = stringResource(Res.string.required_fields_empty)
    val editNoSaveText = stringResource(Res.string.edit_no_save)
    val noChangeText = stringResource(Res.string.rank_change_no_change)
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var verifying by rememberSaveable { mutableStateOf(false) }
    var forceRecomposed by remember { mutableStateOf(false) }

    fun doEdit() {
        if (
        // check if everything is disabled
            !hasOrigTrack && !hasOrigArtist && !hasOrigAlbum && !hasOrigAlbumArtist ||
            !hasTrack && !hasArtist && !hasAlbum && !hasAlbumArtist ||

            // artist and track cannot be empty if enabled
            hasOrigTrack && origTrack.text.isEmpty() ||
            hasTrack && track.text.isEmpty() ||
            hasOrigArtist && origArtist.text.isEmpty() ||
            hasArtist && artist.text.isEmpty()
        ) {
            errorText = missingFieldsText
        } else if (
        // check if the edit rule actually changes data
            origTrack.takeIf { hasOrigTrack } == track.takeIf { hasTrack } &&
            origArtist.takeIf { hasOrigArtist } == artist.takeIf { hasArtist } &&
            origAlbum.takeIf { hasOrigAlbum } == album.takeIf { hasAlbum } &&
            origAlbumArtist.takeIf { hasOrigAlbumArtist } == albumArtist.takeIf { hasAlbumArtist }
        ) {
            errorText = noChangeText
        } else {
            val newEdit = SimpleEdit(
                _id = simpleEdit?._id ?: 0,

                hasOrigTrack = hasOrigTrack,
                origTrack = origTrack.text.toString(),
                track = track.text.toString().takeIf { hasTrack },

                hasOrigArtist = hasOrigArtist,
                origArtist = origArtist.text.toString(),
                artist = artist.text.toString().takeIf { hasArtist },

                hasOrigAlbum = hasOrigAlbum,
                origAlbum = origAlbum.text.toString(),
                album = album.text.toString().takeIf { hasAlbum },

                hasOrigAlbumArtist = hasOrigAlbumArtist,
                origAlbumArtist = origAlbumArtist.text.toString(),
                albumArtist = albumArtist.text.toString().takeIf { hasAlbumArtist },

                continueMatching = continueMatching,
            )

            if (origScrobbleData != null)
                verifying = true
            errorText = null

            viewModel.editScrobbleUtils.doEdit(
                simpleEdit = newEdit,
                origScrobbleData = origScrobbleData,
                msid = msid,
                hash = hash,
                key = key,
                save = save,
            )
        }
    }

    @Composable
    fun TextFieldWrapper(
        enabled: Boolean,
        state: TextFieldState,
        disabledText: String,
        onCheckedChange: (Boolean) -> Unit,
        labelStr: String,
        isLast: Boolean = false,
    ) {
        key(forceRecomposed) {
            PanoOutlinedTextField(
                state,
                enabled = enabled,
                leadingIcon = {
                    InlineCheckButton(
                        checked = enabled,
                        onCheckedChange = onCheckedChange
                    )
                },
                label = { Text(labelStr) },
                outputTransformation = if (!enabled) {
                    {
                        delete(0, length)
                        append(disabledText)
                    }
                } else null,
                enabledOnTv = false,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = if (isLast) ImeAction.Done else ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    DisposableEffect(Unit) {
        if (hash != null) {
            notifyPlayingTrackEvent(
                PlayingTrackNotifyEvent.TrackScrobbleLocked(
                    hash = hash,
                    state = PlayingTrackNotifyEvent.TrackScrobbleLocked.LockState.LOCKED
                ),
            )
        }

        onDispose {
            if (hash != null) {
                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.TrackScrobbleLocked(
                        hash = hash,
                        state = PlayingTrackNotifyEvent.TrackScrobbleLocked.LockState.UNLOCKED
                    ),
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.editScrobbleUtils.result.collect { (origSd, result) ->
            if (origSd == origScrobbleData) {
                result.onSuccess {
                    verifying = false
                    errorText = null

                    onDone()
                }.onFailure {
                    verifying = false
                    errorText = it.redactedMessage

                    if (it is LastFm.CookiesInvalidatedException) {
                        reauthenticateButtonShown = true
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.editScrobbleUtils.updatedAlbum.collect { (origSd, it) ->
            if (origSd == origScrobbleData)
                album.setTextAndPlaceCursorAtEnd(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.editScrobbleUtils.updatedAlbumArtist.collect { (origSd, it) ->
            if (origSd == origScrobbleData)
                albumArtist.setTextAndPlaceCursorAtEnd(it)
        }
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            // workaround for focus getting stuck https://issuetracker.google.com/issues/290343159
            // works without needing a delay
            forceRecomposed = true
        }
    }

    ResultEffect<FabClickedResult> {
        doEdit()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
//        if (networkEditMode) {
//            ButtonWithIcon(
//                onClick = { isExpanded = !isExpanded },
//                icon = if (!isExpanded) Icons.KeyboardArrowUp else Icons.KeyboardArrowDown,
//                text = if (!isExpanded) stringResource(Res.string.expand)
//                else stringResource(Res.string.collapse),
//                modifier = Modifier.align(Alignment.CenterHorizontally)
//            )
//        }

        if (isExpanded) {
            Text(
                text = stringResource(Res.string.original),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )

            TextFieldWrapper(
                enabled = hasOrigTrack,
                state = origTrack,
                onCheckedChange = { hasOrigTrack = it },
                disabledText = anythingText,
                labelStr = stringResource(Res.string.track),
            )

            TextFieldWrapper(
                enabled = hasOrigArtist,
                state = origArtist,
                onCheckedChange = { hasOrigArtist = it },
                disabledText = anythingText,
                labelStr = stringResource(Res.string.artist),
            )

            TextFieldWrapper(
                enabled = hasOrigAlbum,
                state = origAlbum,
                onCheckedChange = { hasOrigAlbum = it },
                disabledText = anythingText,
                labelStr = stringResource(Res.string.album),
            )

            TextFieldWrapper(
                enabled = hasOrigAlbumArtist,
                state = origAlbumArtist,
                onCheckedChange = { hasOrigAlbumArtist = it },
                disabledText = anythingText,
                labelStr = stringResource(Res.string.album_artist),
            )

            Text(
                stringResource(Res.string.edit_example),
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = stringResource(Res.string.corrected),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }

        TextFieldWrapper(
            enabled = hasTrack,
            state = track,
            disabledText = existingText,
            onCheckedChange = { hasTrack = it },
            labelStr = stringResource(Res.string.track),
        )

        TextFieldWrapper(
            enabled = hasArtist,
            state = artist,
            disabledText = existingText,
            onCheckedChange = { hasArtist = it },
            labelStr = stringResource(Res.string.artist),
        )

        TextFieldWrapper(
            enabled = hasAlbum,
            state = album,
            disabledText = existingText,
            onCheckedChange = { hasAlbum = it },
            labelStr = stringResource(Res.string.album),
            isLast = (isExpanded || !origScrobbleData?.albumArtist.isNullOrEmpty())
        )


        if (isExpanded || !origScrobbleData?.albumArtist.isNullOrEmpty()) {
            TextFieldWrapper(
                enabled = hasAlbumArtist,
                state = albumArtist,
                disabledText = existingText,
                onCheckedChange = { hasAlbumArtist = it },
                labelStr = stringResource(Res.string.album_artist),
                isLast = true
            )
        }

        ErrorText(errorText)

        if (isExpanded) {
            LabeledCheckbox(
                checked = continueMatching,
                onCheckedChange = { continueMatching = it },
                text = stringResource(Res.string.edit_continue_simple),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (simpleEdit != null && !networkEditMode) {
            ButtonWithIcon(
                onClick = {
                    viewModel.editScrobbleUtils.deleteSimpleEdit(simpleEdit)
                },
                icon = Icons.Delete,
                text = stringResource(Res.string.delete),
                contentColorOverride = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        if (networkEditMode) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (reauthenticateButtonShown) {
                    OutlinedButton(
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.outlinedButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        onClick = onReauthenticate,
                    ) {
                        Text(stringResource(Res.string.pref_login))
                    }
                } else {
                    if (!isExpanded) {
                        ButtonWithIcon(
                            onClick = onExpand,
                            icon = Icons.KeyboardArrowUp,
                            text = stringResource(Res.string.show_all),
                        )

                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                        )
                    }

                    val noSaveText = stringResource(Res.string.save) + ": " +
                            stringResource(Res.string.disable)
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Above
                        ),
                        tooltip = { PlainTooltip { Text(noSaveText) } },
                        state = rememberTooltipState(),
                    ) {
                        FilledIconToggleButton(
                            checked = !save,
                            shapes = IconButtonDefaults.toggleableShapes(),
                            onCheckedChange = {
                                save = !it

                                errorText = if (!save) {
                                    editNoSaveText
                                } else {
                                    null
                                }
                            }
                        ) {
                            Icon(
                                imageVector = PanoIcons.ContentSaveOffOutline,
                                contentDescription = noSaveText,
                            )
                        }
                    }

                    IconButtonWithTooltip(
                        onClick = {
                            // swap track and artist
                            val temp = track.text.toString()
                            track.setTextAndPlaceCursorAtEnd(artist.text.toString())
                            artist.setTextAndPlaceCursorAtEnd(temp)
                        },
                        icon = Icons.SwapVert,
                        contentDescription = stringResource(Res.string.swap),
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }

                if (!isExpanded)
                    FilledTonalButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = ::doEdit,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        elevation = ButtonDefaults.elevatedButtonElevation(),
                        modifier = if (verifying)
                            Modifier.shimmerWindowBounds()
                        else
                            Modifier
                    ) {
                        Icon(
                            imageVector = Icons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(Res.string.edit),
                        )
                    }
            }
        }
    }
}