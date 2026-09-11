package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.myTransparentCheckableItemColors
import com.arn.scrobble.utils.PlatformStuff
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.MissingResourceException
import pano_scrobbler.composeapp.generated.resources.Res

@Composable
fun OssCreditsScreen(
    modifier: Modifier = Modifier,
) {
    val libraries by rememberLibraries {
        try {
            Res.readBytes("files/aboutlibraries.json").decodeToString()
        } catch (e: MissingResourceException) {
            "{}" // Fallback to empty JSON if the resource is missing
        }
    }

    PanoLazyColumn(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        items(libraries?.libraries ?: emptyList()) { library ->
            LibraryItem(
                library,
                modifier = Modifier
                    .fillMaxWidth()
            )
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
private fun LibraryItem(
    library: Library,
    modifier: Modifier = Modifier,
) {
    val url = remember(library) {
        library.website ?: library.scm?.url ?: library.licenses.firstOrNull()?.url
    }

    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.myTransparentCheckableItemColors(),
        supportingContent = {
            Column {
                Text(
                    text = library.uniqueId,
                )
                Text(
                    text = library.licenses.joinToString { it.name },
                )
            }
        },
        onClick = { url?.let { PlatformStuff.openInBrowser(it) } }
    ) {
        Text(
            text = if (library.name == "\${project.artifactId}")
                library.uniqueId.split(':').last()
            else
                library.name + " " + library.artifactVersion,
        )
    }
}