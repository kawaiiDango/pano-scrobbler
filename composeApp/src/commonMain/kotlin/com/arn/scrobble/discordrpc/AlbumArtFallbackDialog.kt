package com.arn.scrobble.discordrpc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.KeyboardArrowDown
import com.arn.scrobble.icons.KeyboardArrowUp
import com.arn.scrobble.pref.MainPrefs.DiscordRpcPrefs.ArtFallback
import com.arn.scrobble.pref.MainPrefs.DiscordRpcPrefs.ArtFallbackPref
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_art_fallback_sources
import pano_scrobbler.composeapp.generated.resources.album_art_now_playing_desc
import pano_scrobbler.composeapp.generated.resources.deezer
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.lastfm

// every source in priority order; sources missing from prefs (added in a later version) go last, disabled
fun List<ArtFallbackPref>.withAllArtFallbacks(): List<ArtFallbackPref> =
    this + ArtFallback.entries
        .filter { source -> none { it.source == source } }
        .map { ArtFallbackPref(it, enabled = false) }

fun ArtFallback.isAvailable(lastfmSignedIn: Boolean) = when (this) {
    ArtFallback.LASTFM_NOW_PLAYING -> lastfmSignedIn
    ArtFallback.DEEZER -> true
}

@Composable
fun artFallbackLabel(source: ArtFallback): String = when (source) {
    ArtFallback.LASTFM_NOW_PLAYING -> stringResource(Res.string.lastfm)
    ArtFallback.DEEZER -> stringResource(Res.string.deezer)
}

private fun List<ArtFallbackPref>.swap(from: Int, to: Int) =
    toMutableList().apply { add(to, removeAt(from)) }

@Composable
fun AlbumArtFallbackDialog(
    fallbacks: List<ArtFallbackPref>,
    lastfmSignedIn: Boolean,
    onChange: (List<ArtFallbackPref>) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.album_art_fallback_sources)) },
        text = {
            Column {
                fallbacks.forEachIndexed { index, item ->
                    val available = item.source.isAvailable(lastfmSignedIn)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = item.enabled && available,
                            enabled = available,
                            onCheckedChange = { checked ->
                                onChange(fallbacks.map {
                                    if (it == item) it.copy(enabled = checked) else it
                                })
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(artFallbackLabel(item.source))
                            if (!available)
                                Text(
                                    text = stringResource(Res.string.album_art_now_playing_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                        }
                        IconButton(
                            enabled = index > 0,
                            onClick = { onChange(fallbacks.swap(index, index - 1)) },
                        ) {
                            Icon(Icons.KeyboardArrowUp, contentDescription = null)
                        }
                        IconButton(
                            enabled = index < fallbacks.lastIndex,
                            onClick = { onChange(fallbacks.swap(index, index + 1)) },
                        ) {
                            Icon(Icons.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.done))
            }
        },
    )
}
