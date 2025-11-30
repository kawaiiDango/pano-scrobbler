package com.arn.scrobble.edits

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.icons.ContentSaveOffOutline
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.InlineCheckButton
import com.arn.scrobble.ui.OutlinedTextFieldTvSafe
import com.arn.scrobble.utils.redactedMessage
import com.valentinilk.shimmer.LocalShimmerTheme
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer
import com.valentinilk.shimmer.shimmerSpec
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.any_value
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.collapse
import pano_scrobbler.composeapp.generated.resources.corrected
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.edit_example
import pano_scrobbler.composeapp.generated.resources.edit_no_save
import pano_scrobbler.composeapp.generated.resources.existing_value
import pano_scrobbler.composeapp.generated.resources.expand
import pano_scrobbler.composeapp.generated.resources.original
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.rank_change_no_change
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.save
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
    notifyEdit: (String, Track) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditScrobbleViewModel = viewModel { EditScrobbleViewModel() },
) {
    var hasOrigTrack by rememberSaveable { mutableStateOf(simpleEdit?.hasOrigTrack ?: true) }
    var origTrack by rememberSaveable { mutableStateOf(simpleEdit?.origTrack ?: "") }
    var hasTrack by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.track != null) }
    var track by rememberSaveable { mutableStateOf(simpleEdit?.track ?: "") }

    var hasOrigAlbum by rememberSaveable { mutableStateOf(simpleEdit?.hasOrigAlbum ?: true) }
    var origAlbum by rememberSaveable { mutableStateOf(simpleEdit?.origAlbum ?: "") }
    var hasAlbum by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.album != null) }
    var album by rememberSaveable { mutableStateOf(simpleEdit?.album ?: "") }

    var hasOrigArtist by rememberSaveable { mutableStateOf(simpleEdit?.hasOrigArtist ?: true) }
    var origArtist by rememberSaveable { mutableStateOf(simpleEdit?.origArtist ?: "") }
    var hasArtist by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.artist != null) }
    var artist by rememberSaveable { mutableStateOf(simpleEdit?.artist ?: "") }

    var reauthenticateButtonShown by remember { mutableStateOf(false) }
    var save by rememberSaveable { mutableStateOf(true) }
    val networkEditMode = simpleEdit != null && origScrobbleData != null
    var isExpanded by rememberSaveable { mutableStateOf(!networkEditMode) }

    var hasOrigAlbumArtist by rememberSaveable {
        mutableStateOf(simpleEdit?.hasOrigAlbumArtist ?: false)
    }
    var origAlbumArtist by rememberSaveable { mutableStateOf(simpleEdit?.origAlbumArtist ?: "") }
    var hasAlbumArtist by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.albumArtist != null) }
    var albumArtist by rememberSaveable { mutableStateOf(simpleEdit?.albumArtist ?: "") }

    val anythingText = "< " + stringResource(Res.string.any_value) + " >"
    val existingText = "< " + stringResource(Res.string.existing_value) + " >"
    val missingFieldsText = stringResource(Res.string.required_fields_empty)
    val editNoSaveText = stringResource(Res.string.edit_no_save)
    val noChangeText = stringResource(Res.string.rank_change_no_change)
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var verifying by rememberSaveable { mutableStateOf(false) }
    val shimmer = rememberShimmer(
        shimmerBounds = ShimmerBounds.View,
        theme = LocalShimmerTheme.current.copy(
            animationSpec = infiniteRepeatable(
                animation = shimmerSpec(
                    durationMillis = 800,
                    easing = LinearEasing,
                    delayMillis = 200,
                ),
                repeatMode = RepeatMode.Restart,
            ),
        )
    )

    fun doEdit() {
        if (
        // check if everything is disabled
            !hasOrigTrack && !hasOrigArtist && !hasOrigAlbum && !hasOrigAlbumArtist ||
            !hasTrack && !hasArtist && !hasAlbum && !hasAlbumArtist ||

            // artist and track cannot be empty if enabled
            hasOrigTrack && origTrack.isEmpty() ||
            hasTrack && track.isEmpty() ||
            hasOrigArtist && origArtist.isEmpty() ||
            hasArtist && artist.isEmpty()
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
                origTrack = origTrack,
                track = track.takeIf { hasTrack },

                hasOrigArtist = hasOrigArtist,
                origArtist = origArtist,
                artist = artist.takeIf { hasArtist },

                hasOrigAlbum = hasOrigAlbum,
                origAlbum = origAlbum,
                album = album.takeIf { hasAlbum },

                hasOrigAlbumArtist = hasOrigAlbumArtist,
                origAlbumArtist = origAlbumArtist,
                albumArtist = albumArtist.takeIf { hasAlbumArtist },
            )

            if (origScrobbleData != null)
                verifying = true
            errorText = null

            viewModel.doEdit(
                simpleEdit = newEdit,
                origScrobbleData = origScrobbleData,
                msid = msid,
                hash = hash,
                key = key,
                notifyEdit = notifyEdit,
                save = save,
            )
        }
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

    LaunchedEffect(Unit) {
        viewModel.result.collectLatest { (origSd, result) ->
            if (origSd == origScrobbleData) {
                result.onSuccess {
                    verifying = false
                    errorText = null

                    onDone()
                }.onFailure {
                    verifying = false
                    errorText = it.redactedMessage

                    if (it is LastfmUnscrobbler.CookiesInvalidatedException) {
                        reauthenticateButtonShown = true
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updatedAlbum.collectLatest { (origSd, it) ->
            if (origSd == origScrobbleData)
                album = it
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updatedAlbumArtist.collectLatest { (origSd, it) ->
            if (origSd == origScrobbleData)
                albumArtist = it
        }
    }

    Column(
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = if (networkEditMode)
//                Modifier
//            else
//                Modifier
//                    .weight(1f)
//                    .verticalScroll(rememberScrollState())
        ) {

            if (networkEditMode) {
                ButtonWithIcon(
                    onClick = { isExpanded = !isExpanded },
                    icon = if (!isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    text = if (!isExpanded) stringResource(Res.string.expand)
                    else stringResource(Res.string.collapse),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            if (isExpanded) {
                Text(
                    text = stringResource(Res.string.original),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )

                OutlinedTextFieldTvSafe(
                    enabled = hasOrigTrack,
                    value = if (hasOrigTrack) origTrack else anythingText,
                    onValueChange = { origTrack = it },
                    leadingIcon = {
                        InlineCheckButton(
                            checked = hasOrigTrack,
                            onCheckedChange = { hasOrigTrack = it }
                        )
                    },
                    label = { Text(stringResource(Res.string.track)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextFieldTvSafe(
                    enabled = hasOrigArtist,
                    value = if (hasOrigArtist) origArtist else anythingText,
                    onValueChange = { origArtist = it },
                    leadingIcon = {
                        InlineCheckButton(
                            checked = hasOrigArtist,
                            onCheckedChange = { hasOrigArtist = it }
                        )
                    },
                    label = { Text(stringResource(Res.string.artist)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextFieldTvSafe(
                    enabled = hasOrigAlbum,
                    value = if (hasOrigAlbum) origAlbum else anythingText,
                    onValueChange = { origAlbum = it },
                    leadingIcon = {
                        InlineCheckButton(
                            checked = hasOrigAlbum,
                            onCheckedChange = { hasOrigAlbum = it }
                        )
                    },
                    label = { Text(stringResource(Res.string.album)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextFieldTvSafe(
                    enabled = hasOrigAlbumArtist,
                    value = if (hasOrigAlbumArtist) origAlbumArtist else anythingText,
                    onValueChange = { origAlbumArtist = it },
                    leadingIcon = {
                        InlineCheckButton(
                            checked = hasOrigAlbumArtist,
                            onCheckedChange = { hasOrigAlbumArtist = it }
                        )
                    },
                    label = { Text(stringResource(Res.string.album_artist)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
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

            OutlinedTextFieldTvSafe(
                enabled = hasTrack,
                value = if (hasTrack) track else existingText,
                onValueChange = { track = it },
                leadingIcon = if (isExpanded) {
                    {
                        InlineCheckButton(
                            checked = hasTrack,
                            onCheckedChange = { hasTrack = it }
                        )
                    }
                } else null,
                label = { Text(stringResource(Res.string.track)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextFieldTvSafe(
                enabled = hasArtist,
                value = if (hasArtist) artist else existingText,
                onValueChange = { artist = it },
                leadingIcon =
                    if (isExpanded) {
                        {
                            InlineCheckButton(
                                checked = hasArtist,
                                onCheckedChange = { hasArtist = it }
                            )
                        }
                    } else null,
                label = { Text(stringResource(Res.string.artist)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextFieldTvSafe(
                enabled = hasAlbum,
                value = if (hasAlbum) album else existingText,
                onValueChange = { album = it },
                leadingIcon =
                    if (isExpanded) {
                        {
                            InlineCheckButton(
                                checked = hasAlbum,
                                onCheckedChange = { hasAlbum = it }
                            )
                        }
                    } else null,
                label = { Text(stringResource(Res.string.album)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            if (isExpanded || !origScrobbleData?.albumArtist.isNullOrEmpty()) {
                OutlinedTextFieldTvSafe(
                    enabled = hasAlbumArtist,
                    value = if (hasAlbumArtist) albumArtist else existingText,
                    onValueChange = { albumArtist = it },
                    leadingIcon = if (isExpanded) {
                        {
                            InlineCheckButton(
                                checked = hasAlbumArtist,
                                onCheckedChange = { hasAlbumArtist = it }
                            )
                        }
                    } else null,
                    label = { Text(stringResource(Res.string.album_artist)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { doEdit() }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ErrorText(errorText)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                modifier = Modifier.weight(1f)
            )

            if (simpleEdit != null && !networkEditMode) {
                IconButton(
                    onClick = {
                        viewModel.deleteSimpleEdit(simpleEdit)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(Res.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (networkEditMode) {
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
                            onCheckedChange = {
                                save = it

                                if (!save) {
                                    errorText = editNoSaveText
                                } else {
                                    errorText = null
                                }
                            },
                            modifier = Modifier.alpha(
                                if (save) 1f else 0.5f
                            )
                        ) {
                            Icon(
                                imageVector = if (save)
                                    Icons.Outlined.Save
                                else
                                    PanoIcons.ContentSaveOffOutline,
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
            }

            ButtonWithIcon(
                onClick = ::doEdit,
                icon = Icons.Outlined.Check,
                enabled = !verifying,
                text = stringResource(
                    if (networkEditMode)
                        Res.string.edit
                    else
                        Res.string.save
                ),
                modifier = if (verifying)
                    Modifier.shimmer(customShimmer = shimmer)
                else
                    Modifier
            )
        }
    }
}