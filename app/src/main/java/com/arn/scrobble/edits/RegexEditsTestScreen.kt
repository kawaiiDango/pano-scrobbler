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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.InfoText
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegexEditsTestScreen(
    viewModel: RegexEditsTestVM = viewModel(),
    mainViewModel: MainViewModel,
    onNavigateToAppList: () -> Unit,
    onNavigateToRegexEditsAdd: (RegexEdit) -> Unit,
    modifier: Modifier = Modifier
) {
    val regexMatches by viewModel.regexMatches.collectAsStateWithLifecycle()
    val hasPkgName by viewModel.hasPkgName.collectAsStateWithLifecycle()
    var appItem by rememberSaveable { mutableStateOf<AppItem?>(null) }
    var track by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var albumArtist by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        mainViewModel.selectedPackages.collectLatest {
            appItem = it.firstOrNull()
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
            label = { Text(stringResource(R.string.track)) },
            leadingIcon = { Icon(Icons.Outlined.MusicNote, contentDescription = null) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = album,
            onValueChange = {
                album = it
            },
            label = { Text(stringResource(R.string.album)) },
            leadingIcon = { Icon(Icons.Outlined.Album, contentDescription = null) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()

        )
        OutlinedTextField(
            value = artist,
            onValueChange = {
                artist = it
            },
            label = { Text(stringResource(R.string.artist)) },
            leadingIcon = { Icon(Icons.Outlined.Mic, contentDescription = null) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()

        )
        OutlinedTextField(
            value = albumArtist,
            onValueChange = {
                albumArtist = it
            },
            label = { Text(stringResource(R.string.album_artist)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.vd_album_artist),
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
                            text = stringResource(R.string.choose_an_app),
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
                text = stringResource(R.string.required_fields_empty),
                color = MaterialTheme.colorScheme.error
            )
        }

        AnimatedVisibility(regexMatches?.values?.all { it.isEmpty() } == true) {
            Text(
                text = pluralStringResource(R.plurals.num_matches, 0, 0),
                color = MaterialTheme.colorScheme.error
            )
        }

        AnimatedVisibility(regexMatches?.values?.all { it.isEmpty() } == false) {

            Column {
                regexMatches?.forEach { (field, regexEdits) ->
                    val count = regexEdits.size
                    val countString = if (count > 0) " (${count.format()})" else ""
                    when (field) {
                        NLService.B_ARTIST -> {
                            InfoText(
                                text = artist + countString,
                                icon = Icons.Outlined.Mic
                            )
                        }

                        NLService.B_ALBUM -> {
                            InfoText(
                                text = album + countString,
                                icon = Icons.Outlined.Album
                            )
                        }

                        NLService.B_ALBUM_ARTIST -> {
                            InfoText(
                                text = albumArtist + countString,
                                icon = ImageVector.vectorResource(id = R.drawable.vd_album_artist)
                            )
                        }

                        NLService.B_TRACK -> {
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

                val matchedRegexEdits = regexMatches?.values?.flatten()?.toSet()

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
                                            R.string.edit_preset_name,
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