package com.arn.scrobble.discordrpc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.ResetSettings
import com.arn.scrobble.pref.DropdownPref
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.SliderPref
import com.arn.scrobble.pref.SwitchPref
import com.arn.scrobble.ui.HighlighterVisualTransformation
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.album_art_now_playing
import pano_scrobbler.composeapp.generated.resources.album_art_now_playing_desc
import pano_scrobbler.composeapp.generated.resources.available_placeholders
import pano_scrobbler.composeapp.generated.resources.discord_app_name
import pano_scrobbler.composeapp.generated.resources.discord_compact_view_line
import pano_scrobbler.composeapp.generated.resources.enable
import pano_scrobbler.composeapp.generated.resources.line_n
import pano_scrobbler.composeapp.generated.resources.reset
import pano_scrobbler.composeapp.generated.resources.show_paused_for
import pano_scrobbler.composeapp.generated.resources.show_track_url

private enum class Line {
    None,
    Line1,
    Line2,
}

@Composable
fun DiscordRpcScreen(
    modifier: Modifier = Modifier,
) {
    val settings by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.discordRpc }
    val defaultSettings = remember { MainPrefs.DiscordRpcSettings() }
    var line1Format by remember(settings.line1Format) { mutableStateOf(settings.line1Format) }
    var line2Format by remember(settings.line2Format) { mutableStateOf(settings.line2Format) }
    var line3Format by remember(settings.line3Format) { mutableStateOf(settings.line3Format) }
    var nameFormat by remember(settings.nameFormat) { mutableStateOf(settings.nameFormat) }
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val visualTransformation = remember {
        HighlighterVisualTransformation(
            stringsToHighlight = DiscordRpcPlaceholder.entries.map { "\$" + it.name },
            highlightColor = tertiaryColor
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            GlobalScope.launch {
                PlatformStuff.mainPrefs.updateData {
                    it.copy(
                        discordRpc = it.discordRpc.copy(
                            line1Format = line1Format.trim()
                                .ifEmpty { defaultSettings.line1Format },
                            line2Format = line2Format.trim()
                                .ifEmpty { defaultSettings.line2Format },
                            line3Format = line3Format.trim(), // line 3 can be empty
                            nameFormat = nameFormat.trim(), // name can be empty
                        )
                    )
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        SwitchPref(
            text = stringResource(Res.string.enable),
            value = settings.enabled,
            copyToSave = {
                copy(
                    discordRpc = settings.copy(enabled = it)
                )
            }
        )

        SwitchPref(
            text = stringResource(Res.string.album_art),
            value = settings.albumArt,
            enabled = settings.enabled,
            copyToSave = {
                copy(
                    discordRpc = settings.copy(albumArt = it)
                )
            }
        )

        SwitchPref(
            text = stringResource(Res.string.album_art_now_playing),
            summary = stringResource(Res.string.album_art_now_playing_desc),
            value = settings.albumArtFromNowPlaying,
            enabled = settings.enabled && settings.albumArt,
            copyToSave = {
                copy(
                    discordRpc = settings.copy(albumArtFromNowPlaying = it)
                )
            }
        )

        SwitchPref(
            text = stringResource(Res.string.show_track_url),
            value = settings.detailsUrl,
            enabled = settings.enabled,
            copyToSave = {
                copy(
                    discordRpc = settings.copy(detailsUrl = it)
                )
            }
        )

        Text(
            stringResource(
                Res.string.available_placeholders,
                DiscordRpcPlaceholder.entries.joinToString { "\$" + it.name }
            ),
            color = tertiaryColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalOverscanPadding())
        )

        PanoOutlinedTextField(
            label = { Text(stringResource(Res.string.line_n, 1)) },
            value = line1Format,
            onValueChange = {
                line1Format = it
            },
            visualTransformation = visualTransformation,
            isError = line1Format.trim().isEmpty(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        line1Format = defaultSettings.line1Format
                    }
                ) {
                    Icon(
                        imageVector = Icons.ResetSettings,
                        contentDescription = stringResource(Res.string.reset)
                    )
                }
            },
            enabled = settings.enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalOverscanPadding())
        )

        PanoOutlinedTextField(
            label = { Text(stringResource(Res.string.line_n, 2)) },
            value = line2Format,
            onValueChange = {
                line2Format = it
            },
            visualTransformation = visualTransformation,
            isError = line2Format.trim().isEmpty(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        line2Format = defaultSettings.line2Format
                    }
                ) {
                    Icon(
                        imageVector = Icons.ResetSettings,
                        contentDescription = stringResource(Res.string.reset)
                    )
                }
            },
            enabled = settings.enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalOverscanPadding())
        )

        PanoOutlinedTextField(
            label = { Text(stringResource(Res.string.line_n, 3)) },
            value = line3Format,
            onValueChange = {
                line3Format = it
            },
            visualTransformation = visualTransformation,
            trailingIcon = {
                IconButton(
                    onClick = {
                        line3Format = defaultSettings.line3Format
                    }
                ) {
                    Icon(
                        imageVector = Icons.ResetSettings,
                        contentDescription = stringResource(Res.string.reset)
                    )
                }
            },
            enabled = settings.enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalOverscanPadding())
        )

        PanoOutlinedTextField(
            label = { Text(stringResource(Res.string.discord_app_name)) },
            value = nameFormat,
            onValueChange = {
                nameFormat = it
            },
            visualTransformation = visualTransformation,
            trailingIcon = {
                IconButton(
                    onClick = {
                        nameFormat = defaultSettings.nameFormat
                    }
                ) {
                    Icon(
                        imageVector = Icons.ResetSettings,
                        contentDescription = stringResource(Res.string.reset)
                    )
                }
            },
            enabled = settings.enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalOverscanPadding())
        )

        DropdownPref(
            text = stringResource(Res.string.discord_compact_view_line),
            selectedValue = when (settings.statusLine) {
                1 -> Line.Line1
                2 -> Line.Line2
                else -> Line.None
            },
            values = Line.entries,
            toLabel = {
                when (it) {
                    Line.Line1 -> stringResource(Res.string.line_n, 1)
                    Line.Line2 -> stringResource(Res.string.line_n, 2)
                    Line.None -> stringResource(Res.string.discord_app_name)
                }
            },
            copyToSave = {
                copy(
                    discordRpc = settings.copy(
                        statusLine = when (it) {
                            Line.Line1 -> 1
                            Line.Line2 -> 2
                            Line.None -> 0
                        }
                    )
                )
            },
            enabled = settings.enabled,
        )

        SliderPref(
            text = stringResource(Res.string.show_paused_for),
            value = settings.showPausedForSecs.toFloat(),
            enabled = settings.enabled,
            min = 0,
            max = 600,
            increments = 10,
            stringRepresentation = { Stuff.humanReadableDuration(it * 1000L) },
            copyToSave = {
                copy(
                    discordRpc = settings.copy(showPausedForSecs = it)
                )
            },
        )
    }
}