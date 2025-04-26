package com.arn.scrobble.edits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.corrected
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
    var origTrack by remember { mutableStateOf(simpleEdit?.origTrack ?: "") }
    var track by remember { mutableStateOf(simpleEdit?.track ?: "") }
    var origAlbum by remember { mutableStateOf(simpleEdit?.origAlbum ?: "") }
    var album by remember { mutableStateOf(simpleEdit?.album ?: "") }
    var origArtist by remember { mutableStateOf(simpleEdit?.origArtist ?: "") }
    var artist by remember { mutableStateOf(simpleEdit?.artist ?: "") }
    var albumArtist by remember { mutableStateOf(simpleEdit?.albumArtist ?: "") }
    var showError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun onDone() {
        scope.launch {
            if (origTrack.isEmpty() || artist.isEmpty() || track.isEmpty()) {
                showError = true
            } else {
                val newEdit = SimpleEdit(
                    _id = simpleEdit?._id ?: 0,
                    origTrack = origTrack,
                    track = track,
                    origAlbum = origAlbum,
                    album = album,
                    origArtist = origArtist,
                    artist = artist,
                    albumArtist = albumArtist
                )
                withContext(Dispatchers.IO) {
                    PanoDb.db.getSimpleEditsDao().insertReplaceLowerCase(newEdit)
                }
                onBack()
            }
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
            OutlinedTextField(
                value = origTrack,
                onValueChange = { origTrack = it },
                label = { Text(stringResource(Res.string.track)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()

            )
            OutlinedTextField(
                value = origAlbum,
                onValueChange = { origAlbum = it },
                label = { Text(stringResource(Res.string.album)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()

            )
            OutlinedTextField(
                value = origArtist,
                onValueChange = { origArtist = it },
                label = { Text(stringResource(Res.string.artist)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()

            )

            Text(
                text = stringResource(Res.string.corrected),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )

            OutlinedTextField(
                value = track,
                onValueChange = { track = it },
                label = { Text(stringResource(Res.string.track)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = album,
                onValueChange = { album = it },
                label = { Text(stringResource(Res.string.album)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text(stringResource(Res.string.artist)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = albumArtist,
                onValueChange = { albumArtist = it },
                label = { Text(stringResource(Res.string.album_artist)) },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onDone() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (showError) {
                ErrorText(stringResource(Res.string.required_fields_empty))
            }
        }

        ButtonWithIcon(
            onClick = ::onDone,
            text = stringResource(Res.string.save),
            icon = Icons.Outlined.Check,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(8.dp)
        )
    }
}