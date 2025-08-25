package com.arn.scrobble.edits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.SimpleEditsDao.Companion.insertReplaceLowerCase
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.OutlinedTextFieldTvSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.any_value
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.corrected
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.edit_example
import pano_scrobbler.composeapp.generated.resources.existing_value
import pano_scrobbler.composeapp.generated.resources.original
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.save
import pano_scrobbler.composeapp.generated.resources.track

@Composable
fun SimpleEditsAddScreen(
    simpleEdit: SimpleEdit?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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

    var hasOrigAlbumArtist by rememberSaveable {
        mutableStateOf(
            simpleEdit?.hasOrigAlbumArtist ?: false
        )
    }
    var origAlbumArtist by rememberSaveable { mutableStateOf(simpleEdit?.origAlbumArtist ?: "") }
    var hasAlbumArtist by rememberSaveable { mutableStateOf(simpleEdit == null || simpleEdit.albumArtist != null) }
    var albumArtist by rememberSaveable { mutableStateOf(simpleEdit?.albumArtist ?: "") }

    val anythingText = "< " + stringResource(Res.string.any_value) + " >"
    val existingText = "< " + stringResource(Res.string.existing_value) + " >"
    var showError by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun onDone() {
        scope.launch {
            if (
            // check if everything is disabled
                !hasOrigTrack && !hasOrigArtist && !hasOrigAlbum && !hasOrigAlbumArtist ||
                !hasTrack && !hasArtist && !hasAlbum && !hasAlbumArtist ||

                // artist and track cannot be empty if enabled
                hasOrigTrack && origTrack.isEmpty() ||
                hasTrack && track.isEmpty() ||
                hasOrigArtist && origArtist.isEmpty() ||
                hasArtist && artist.isEmpty() ||

                // check if the edit rule actually changes data
                origTrack.takeIf { hasOrigTrack } == track.takeIf { hasTrack } &&
                origArtist.takeIf { hasOrigArtist } == artist.takeIf { hasArtist } &&
                origAlbum.takeIf { hasOrigAlbum } == album.takeIf { hasAlbum } &&
                origAlbumArtist.takeIf { hasOrigAlbumArtist } == albumArtist.takeIf { hasAlbumArtist }

            ) {
                showError = true
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
                withContext(Dispatchers.IO) {
                    PanoDb.db.getSimpleEditsDao().insertReplaceLowerCase(newEdit)
                }
                onBack()
            }
        }
    }

    fun onDelete() {
        scope.launch {
            withContext(Dispatchers.IO) {
                simpleEdit?.let { PanoDb.db.getSimpleEditsDao().delete(it) }
            }
            onBack()
        }
    }

    Column(
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(Res.string.original),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasOrigTrack,
                    onCheckedChange = { hasOrigTrack = it },
                )

                OutlinedTextFieldTvSafe(
                    enabled = hasOrigTrack,
                    value = if (hasOrigTrack) origTrack else anythingText,
                    onValueChange = { origTrack = it },
                    label = { Text(stringResource(Res.string.track)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasOrigArtist,
                    onCheckedChange = { hasOrigArtist = it },
                )

                OutlinedTextFieldTvSafe(
                    enabled = hasOrigArtist,
                    value = if (hasOrigArtist) origArtist else anythingText,
                    onValueChange = { origArtist = it },
                    label = { Text(stringResource(Res.string.artist)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasOrigAlbum,
                    onCheckedChange = { hasOrigAlbum = it },
                )

                OutlinedTextFieldTvSafe(
                    enabled = hasOrigAlbum,
                    value = if (hasOrigAlbum) origAlbum else anythingText,
                    onValueChange = { origAlbum = it },
                    label = { Text(stringResource(Res.string.album)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasOrigAlbumArtist,
                    onCheckedChange = { hasOrigAlbumArtist = it },
                )
                OutlinedTextFieldTvSafe(
                    enabled = hasOrigAlbumArtist,
                    value = if (hasOrigAlbumArtist) origAlbumArtist else anythingText,
                    onValueChange = { origAlbumArtist = it },
                    label = { Text(stringResource(Res.string.album_artist)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                stringResource(Res.string.edit_example),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp)
            )

            Text(
                text = stringResource(Res.string.corrected),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasTrack,
                    onCheckedChange = { hasTrack = it },
                )
                OutlinedTextFieldTvSafe(
                    enabled = hasTrack,
                    value = if (hasTrack) track else existingText,
                    onValueChange = { track = it },
                    label = { Text(stringResource(Res.string.track)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasArtist,
                    onCheckedChange = { hasArtist = it },
                )
                OutlinedTextFieldTvSafe(
                    enabled = hasArtist,
                    value = if (hasArtist) artist else existingText,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(Res.string.artist)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasAlbum,
                    onCheckedChange = { hasAlbum = it },
                )

                OutlinedTextFieldTvSafe(
                    enabled = hasAlbum,
                    value = if (hasAlbum) album else existingText,
                    onValueChange = { album = it },
                    label = { Text(stringResource(Res.string.album)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = hasAlbumArtist,
                    onCheckedChange = { hasAlbumArtist = it },
                )
                OutlinedTextFieldTvSafe(
                    enabled = hasAlbumArtist,
                    value = if (hasAlbumArtist) albumArtist else existingText,
                    onValueChange = { albumArtist = it },
                    label = { Text(stringResource(Res.string.album_artist)) },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { onDone() }
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (showError) {
                ErrorText(stringResource(Res.string.required_fields_empty))
            }
        }

        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier = Modifier.weight(1f)
                )

                if (simpleEdit != null) {
                    IconButton(
                        onClick = ::onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(Res.string.delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                ButtonWithIcon(
                    onClick = ::onDone,
                    icon = Icons.Outlined.Check,
                    text = stringResource(Res.string.save),
                )
            }
        }
    }
}