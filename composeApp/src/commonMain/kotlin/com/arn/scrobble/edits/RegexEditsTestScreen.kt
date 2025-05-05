package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditFields
import com.arn.scrobble.icons.AlbumArtist
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.navigation.jsonSerializableSaver
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.InfoText
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.choose_an_app
import pano_scrobbler.composeapp.generated.resources.edit_preset_name
import pano_scrobbler.composeapp.generated.resources.num_matches
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.track

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegexEditsTestScreen(
    mainViewModel: MainViewModel,
    onNavigateToAppList: () -> Unit,
    onNavigateToRegexEditsAdd: (RegexEdit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegexEditsTestVM = viewModel { RegexEditsTestVM() },
) {
    val regexMatches by viewModel.regexResults.collectAsStateWithLifecycle()
    val hasPkgName by viewModel.hasPkgName.collectAsStateWithLifecycle()
    var appItem by rememberSaveable(saver = jsonSerializableSaver<AppItem?>()) { mutableStateOf(null) }
    var track by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var albumArtist by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        mainViewModel.selectedPackages.collectLatest { (checked, _) ->
            appItem = checked.firstOrNull()
        }
    }

    LaunchedEffect(track, album, artist, albumArtist) {
        val sd = ScrobbleData(
            track = track,
            album = album,
            artist = artist,
            albumArtist = albumArtist,
            timestamp = 0,
            duration = null,
            packageName = null
        )
        viewModel.setScrobbleData(sd)
    }

    LaunchedEffect(appItem) {
        viewModel.setApp(appItem)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            value = track,
            onValueChange = {
                track = it
            },
            label = { Text(stringResource(Res.string.track)) },
            leadingIcon = { Icon(Icons.Outlined.MusicNote, contentDescription = null) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = album,
            onValueChange = {
                album = it
            },
            label = { Text(stringResource(Res.string.album)) },
            leadingIcon = { Icon(Icons.Outlined.Album, contentDescription = null) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()

        )
        OutlinedTextField(
            value = artist,
            onValueChange = {
                artist = it
            },
            label = { Text(stringResource(Res.string.artist)) },
            leadingIcon = { Icon(Icons.Outlined.Mic, contentDescription = null) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()

        )
        OutlinedTextField(
            value = albumArtist,
            onValueChange = {
                albumArtist = it
            },
            label = { Text(stringResource(Res.string.album_artist)) },
            leadingIcon = {
                Icon(
                    imageVector = PanoIcons.AlbumArtist,
                    contentDescription = null
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        if (hasPkgName) {
            val _appItem = appItem
            if (_appItem != null) {
                AppItemChip(
                    appListItem = _appItem,
                    onClick = {
                        appItem = null
                    },
                )
            } else {
                InputChip(
                    onClick = {
                        onNavigateToAppList()
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.choose_an_app),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    avatar = {
                        Icon(
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = null
                        )
                    },
                    selected = false,
                )
            }
        }

        AnimatedVisibility(regexMatches == null) {
            Text(
                text = stringResource(Res.string.required_fields_empty),
                color = MaterialTheme.colorScheme.error
            )
        }

        AnimatedVisibility(regexMatches?.isEdit == false) {
            Text(
                text = pluralStringResource(Res.plurals.num_matches, 0, 0),
                color = MaterialTheme.colorScheme.error
            )
        }

        AnimatedVisibility(regexMatches?.isEdit == true) {

            Column {
                regexMatches?.fieldsMatched?.forEach { (field, regexEdits) ->
                    val count = regexEdits.size
                    val countString = if (count > 0) " (${count.format()})" else ""
                    when (field) {
                        RegexEditFields.ARTIST -> {
                            InfoText(
                                text = artist + countString,
                                icon = Icons.Outlined.Mic
                            )
                        }

                        RegexEditFields.ALBUM -> {
                            InfoText(
                                text = album + countString,
                                icon = Icons.Outlined.Album
                            )
                        }

                        RegexEditFields.ALBUM_ARTIST -> {
                            InfoText(
                                text = albumArtist + countString,
                                icon = PanoIcons.AlbumArtist
                            )
                        }

                        RegexEditFields.TRACK -> {
                            InfoText(
                                text = track + countString,
                                icon = Icons.Outlined.MusicNote
                            )
                        }

                        else -> {
                            throw IllegalArgumentException()
                        }
                    }
                }

                val matchedRegexEdits = regexMatches?.fieldsMatched?.values?.flatten()?.toSet()

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    matchedRegexEdits?.forEach { regexEdit ->
                        AssistChip(
                            onClick = {
                                onNavigateToRegexEditsAdd(regexEdit)
                            },
                            label = {
                                Text(
                                    text = regexEdit.preset?.let { preset ->
                                        stringResource(
                                            Res.string.edit_preset_name,
                                            RegexPresets.getString(preset)
                                        )
                                    } ?: (regexEdit.name ?: regexEdit.pattern.toString()),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}