package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.icons.AlbumArtist
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.navigation.jsonSerializableSaver
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.ui.InfoText
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album
import pano_scrobbler.composeapp.generated.resources.album_artist
import pano_scrobbler.composeapp.generated.resources.artist
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.choose_an_app
import pano_scrobbler.composeapp.generated.resources.edit_regex_rules_matched
import pano_scrobbler.composeapp.generated.resources.num_matches
import pano_scrobbler.composeapp.generated.resources.required_fields_empty
import pano_scrobbler.composeapp.generated.resources.track

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RegexEditsTestScreen(
    mainViewModel: MainViewModel,
    onNavigateToAppList: () -> Unit,
    onNavigateToRegexEditsAdd: (RegexEdit) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegexEditsTestVM = viewModel { RegexEditsTestVM() },
) {
    val regexMatches by viewModel.regexResults.collectAsStateWithLifecycle()
    var appItem by rememberSaveable(saver = jsonSerializableSaver<AppItem?>()) { mutableStateOf(null) }
    var track by rememberSaveable { mutableStateOf("") }
    var album by rememberSaveable { mutableStateOf("") }
    var artist by rememberSaveable { mutableStateOf("") }
    var albumArtist by rememberSaveable { mutableStateOf("") }
    val gotMatches = regexMatches?.scrobbleData != null || regexMatches?.blockPlayerAction != null

    LaunchedEffect(Unit) {
        mainViewModel.selectedPackages.collectLatest { (checked, _) ->
            appItem = checked.firstOrNull()
        }
    }

    LaunchedEffect(track, album, artist, albumArtist, appItem) {
        val sd = ScrobbleData(
            track = track,
            album = album.ifEmpty { null },
            artist = artist,
            albumArtist = albumArtist.ifEmpty { null },
            timestamp = 0,
            duration = null,
            appId = appItem?.appId
        )
        viewModel.setScrobbleData(sd)
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
            isError = track.isEmpty(),
            label = { Text(stringResource(Res.string.track)) },
            leadingIcon = { Icon(Icons.Outlined.MusicNote, contentDescription = null) },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = artist,
            onValueChange = {
                artist = it
            },
            isError = artist.isEmpty(),
            label = { Text(stringResource(Res.string.artist)) },
            leadingIcon = { Icon(Icons.Outlined.Mic, contentDescription = null) },
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

        if (appItem != null) {
            AppItemChip(
                appListItem = appItem!!,
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

        AnimatedVisibility(regexMatches == null) {
            Text(
                text = stringResource(Res.string.required_fields_empty),
                color = MaterialTheme.colorScheme.error
            )
        }

        AnimatedVisibility(!gotMatches) {
            Text(
                text = pluralStringResource(Res.plurals.num_matches, 0, 0),
                color = MaterialTheme.colorScheme.error
            )
        }

        AnimatedVisibility(gotMatches) {
            Column {
                if (regexMatches?.blockPlayerAction != null) {
                    val blockPlayerAction = regexMatches!!.blockPlayerAction!!
                    InfoText(
                        icon = Icons.Outlined.Block,
                        text = stringResource(Res.string.block) +
                                " (${blockPlayerAction.name})",
                        style = MaterialTheme.typography.bodyLargeEmphasized
                    )
                } else if (regexMatches?.scrobbleData != null) {
                    val scrobbleData = regexMatches!!.scrobbleData!!

                    InfoText(
                        text = scrobbleData.artist,
                        icon = Icons.Outlined.Mic
                    )

                    InfoText(
                        text = scrobbleData.track,
                        icon = Icons.Outlined.MusicNote
                    )

                    InfoText(
                        text = scrobbleData.album ?: "",
                        icon = Icons.Outlined.Album
                    )

                    InfoText(
                        text = scrobbleData.albumArtist ?: "",
                        icon = PanoIcons.AlbumArtist
                    )
                }

                val matchedRegexEdits = regexMatches?.matches

                if (!matchedRegexEdits.isNullOrEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        Text(stringResource(Res.string.edit_regex_rules_matched))

                        matchedRegexEdits.forEach { regexEdit ->
                            AssistChip(
                                onClick = {
                                    onNavigateToRegexEditsAdd(regexEdit)
                                },
                                label = {
                                    Text(
                                        text = regexEdit.name,
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
}