package com.arn.scrobble.pref

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.HighlighterVisualTransformation
import com.arn.scrobble.ui.OutlinedTextFieldTvSafe
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.apple_music
import pano_scrobbler.composeapp.generated.resources.bandcamp
import pano_scrobbler.composeapp.generated.resources.deezer
import pano_scrobbler.composeapp.generated.resources.genius
import pano_scrobbler.composeapp.generated.resources.pref_search_url_template
import pano_scrobbler.composeapp.generated.resources.pref_search_url_template_desc
import pano_scrobbler.composeapp.generated.resources.search_in_media_player
import pano_scrobbler.composeapp.generated.resources.spotify
import pano_scrobbler.composeapp.generated.resources.tidal
import pano_scrobbler.composeapp.generated.resources.yt_music

@Composable
fun MediaSearchPrefDialog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 16.dp)
    ) {
        val usePlayFromSearch by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.usePlayFromSearchP }
        val searchUrlTemplate by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.searchUrlTemplate }
        var searchUrlTemplateText by remember {
            mutableStateOf(
                if (usePlayFromSearch)
                    null
                else
                    searchUrlTemplate
            )
        }
        val queryText = "\$query"
        val tertiaryColor = MaterialTheme.colorScheme.tertiary

        val visualTransformation = remember {
            HighlighterVisualTransformation(
                stringsToHighlight = listOf(queryText),
                highlightColor = tertiaryColor
            )
        }

        fun isError(): Boolean {
            return searchUrlTemplateText?.contains(queryText) == false
        }

        DisposableEffect(Unit) {
            onDispose {
                GlobalScope.launch {
                    PlatformStuff.mainPrefs.updateData {
                        it.copy(
                            usePlayFromSearch = isError() || searchUrlTemplateText.isNullOrBlank(),
                            searchUrlTemplate = if (isError() || searchUrlTemplateText.isNullOrBlank())
                                it.searchUrlTemplate
                            else
                                searchUrlTemplateText!!,
                        )
                    }
                }
            }
        }

        if (!PlatformStuff.isDesktop)
            RadioButtonEntry(
                text = stringResource(Res.string.search_in_media_player),
                selected = searchUrlTemplateText == null,
                onSelect = {
                    searchUrlTemplateText = null
                }
            )

        RadioButtonEntry(
            text = stringResource(Res.string.spotify),
            selected = searchUrlTemplateText == Stuff.SPOTIFY_SEARCH_URL,
            onSelect = {
                searchUrlTemplateText = Stuff.SPOTIFY_SEARCH_URL
            }
        )

        RadioButtonEntry(
            text = stringResource(Res.string.apple_music),
            selected = searchUrlTemplateText == Stuff.APPLE_MUSIC_SEARCH_URL,
            onSelect = {
                searchUrlTemplateText = Stuff.APPLE_MUSIC_SEARCH_URL
            }
        )

        RadioButtonEntry(
            text = stringResource(Res.string.deezer),
            selected = searchUrlTemplateText == Stuff.DEEZER_SEARCH_URL,
            onSelect = {
                searchUrlTemplateText = Stuff.DEEZER_SEARCH_URL
            }
        )

        RadioButtonEntry(
            text = stringResource(Res.string.tidal),
            selected = searchUrlTemplateText == Stuff.TIDAL_SEARCH_URL,
            onSelect = {
                searchUrlTemplateText = Stuff.TIDAL_SEARCH_URL
            }
        )

        RadioButtonEntry(
            text = stringResource(Res.string.yt_music),
            selected = searchUrlTemplateText == Stuff.YT_MUSIC_SEARCH_URL,
            onSelect = {
                searchUrlTemplateText = Stuff.YT_MUSIC_SEARCH_URL
            }
        )

        RadioButtonEntry(
            text = stringResource(Res.string.bandcamp),
            selected = searchUrlTemplateText == Stuff.BANDCAMP_SEARCH_URL,
            onSelect = {
                searchUrlTemplateText = Stuff.BANDCAMP_SEARCH_URL
            }
        )

        RadioButtonEntry(
            text = stringResource(Res.string.genius),
            selected = searchUrlTemplateText == Stuff.GENIUS_SEARCH_URL,
            onSelect = {
                searchUrlTemplateText = Stuff.GENIUS_SEARCH_URL
            }
        )

        OutlinedTextFieldTvSafe(
            value = searchUrlTemplateText ?: "",
            onValueChange = { searchUrlTemplateText = it },
            label = {
                Text(stringResource(Res.string.pref_search_url_template))
            },
            supportingText = {
                Text(stringResource(Res.string.pref_search_url_template_desc))
            },
            visualTransformation = visualTransformation,
            isError = isError(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RadioButtonEntry(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(text)
    }
}