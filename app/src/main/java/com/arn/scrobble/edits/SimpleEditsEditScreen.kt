package com.arn.scrobble.edits

import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.R
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.SimpleEditsDao.Companion.insertReplaceLowerCase
import com.arn.scrobble.themes.AppPreviewTheme
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SimpleEditsEditContent(
    simpleEdit: SimpleEdit? = null,
    onSave: (SimpleEdit) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var origTrack by remember { mutableStateOf(simpleEdit?.origTrack ?: "") }
    var track by remember { mutableStateOf(simpleEdit?.track ?: "") }
    var origAlbum by remember { mutableStateOf(simpleEdit?.origAlbum ?: "") }
    var album by remember { mutableStateOf(simpleEdit?.album ?: "") }
    var origArtist by remember { mutableStateOf(simpleEdit?.origArtist ?: "") }
    var artist by remember { mutableStateOf(simpleEdit?.artist ?: "") }
    var albumArtist by remember { mutableStateOf(simpleEdit?.albumArtist ?: "") }
    var showError by remember { mutableStateOf(false) }

    fun onDone() {
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
            onSave(newEdit)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(id = R.string.original),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )
        OutlinedTextField(
            value = origTrack,
            onValueChange = { origTrack = it },
            label = { Text(stringResource(R.string.track)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()

        )
        OutlinedTextField(
            value = origAlbum,
            onValueChange = { origAlbum = it },
            label = { Text(stringResource(R.string.album)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()

        )
        OutlinedTextField(
            value = origArtist,
            onValueChange = { origArtist = it },
            label = { Text(stringResource(R.string.artist)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()

        )

        Text(
            text = stringResource(id = R.string.corrected),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )

        OutlinedTextField(
            value = track,
            onValueChange = { track = it },
            label = { Text(stringResource(R.string.track)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text(stringResource(R.string.album)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text(stringResource(R.string.artist)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = albumArtist,
            onValueChange = { albumArtist = it },
            label = { Text(stringResource(R.string.album_artist)) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { onDone() }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (showError) {
            ErrorText(stringResource(id = R.string.required_fields_empty))
        }

        Row(
            modifier = Modifier.align(Alignment.End)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(stringResource(id = android.R.string.cancel))
            }
            TextButton(onClick = ::onDone) {
                Text(stringResource(id = android.R.string.ok))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleEditsEditContentPreview() {
    AppPreviewTheme {
        SimpleEditsEditContent(simpleEdit = SimpleEdit(), onSave = {}, onCancel = {})
    }
}

@Keep
@Composable
fun SimpleEditsEditScreen(
) {
    val fragment = LocalFragment.current
    val simpleEdit = fragment.arguments?.getParcelable<SimpleEdit>(Stuff.ARG_EDIT)
    val scope = rememberCoroutineScope()

    ScreenParent {
        SimpleEditsEditContent(simpleEdit = simpleEdit, onSave = {
            scope.launch(Dispatchers.IO) {
                PanoDb.db.getSimpleEditsDao().insertReplaceLowerCase(it)
            }
            fragment.findNavController().popBackStack()
        }, onCancel = {
            fragment.findNavController().popBackStack()
        }, modifier = it)
    }
}