package com.arn.scrobble.pref

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.utils.PlatformStuff
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pano_scrobbler.composeapp.generated.resources.Res

@Composable
fun OssCreditsScreen(
    modifier: Modifier = Modifier,
) {
    val libraries by rememberLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    PanoLazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(libraries?.libraries ?: emptyList()) { library ->
            LibraryItem(library)
        }
    }
}

@Composable
private fun rememberLibraries(
    block: suspend () -> String,
): State<Libs?> {
    return produceState(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            Libs.Builder()
                .withJson(block())
                .build()
        }
    }
}

@Composable
private fun LibraryItem(library: Library) {
    val url = remember {
        library.website ?: library.scm?.url ?: library.licenses.firstOrNull()?.url
    }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (PlatformStuff.isTv)
                    Modifier
                        .indication(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current
                        )
                        .focusable(interactionSource = interactionSource)
                else
                    Modifier.clickable { url?.let { PlatformStuff.openInBrowser(it) } }
            )
            .padding(horizontal = horizontalOverscanPadding())
    ) {
        Text(
            text = if (library.name == "\${project.artifactId}") library.uniqueId.split(':')
                .last() else library.name + " " + library.artifactVersion,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = library.uniqueId,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
        Text(
            text = library.licenses.joinToString { it.name },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}