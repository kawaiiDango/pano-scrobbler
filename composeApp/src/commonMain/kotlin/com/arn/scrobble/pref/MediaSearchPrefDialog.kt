package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.HighlighterVisualTransformation
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.ui.myCheckableItemColors
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
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
        horizontalAlignment = Alignment.CenterHorizontally,
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
                Stuff.appScope.launch {
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

        val textToUrls = listOfNotNull(
            if (!PlatformStuff.isDesktop)
                stringResource(Res.string.search_in_media_player) to null
            else
                null,
            stringResource(Res.string.spotify) to Stuff.SPOTIFY_SEARCH_URL,
            stringResource(Res.string.apple_music) to Stuff.APPLE_MUSIC_SEARCH_URL,
            stringResource(Res.string.deezer) to Stuff.DEEZER_SEARCH_URL,
            stringResource(Res.string.tidal) to Stuff.TIDAL_SEARCH_URL,
            stringResource(Res.string.yt_music) to Stuff.YT_MUSIC_SEARCH_URL,
            stringResource(Res.string.bandcamp) to Stuff.BANDCAMP_SEARCH_URL,
            stringResource(Res.string.genius) to Stuff.GENIUS_SEARCH_URL
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .width(IntrinsicSize.Max)
                .verticalScroll(rememberScrollState())
        ) {
            textToUrls.forEach { (text, url) ->
                ListItem(
                    selected = searchUrlTemplateText == url,
                    onClick = { searchUrlTemplateText = url },
                    colors = ListItemDefaults.myCheckableItemColors(),
                    leadingContent = {
                        RadioButton(
                            selected = searchUrlTemplateText == url,
                            onClick = null,
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text)
                }
            }
        }

        PanoOutlinedTextField(
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
            modifier = Modifier.fillMaxWidth()
        )
    }
}