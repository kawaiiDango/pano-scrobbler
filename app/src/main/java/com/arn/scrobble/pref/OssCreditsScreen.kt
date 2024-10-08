package com.arn.scrobble.pref

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.Stuff
import com.mikepenz.aboutlibraries.entity.Library

@Composable
fun OssCreditsScreen(
    viewModel: OssCreditsVM = viewModel(),
    modifier: Modifier = Modifier
) {

    val libraries = remember { viewModel.libraries }

    LazyColumn(
        contentPadding = panoContentPadding(),
        modifier = modifier
    ) {
        items(libraries) { library ->
            LibraryItem(library)
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
                if (Stuff.isTv)
                    Modifier
                        .indication(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current
                        )
                        .focusable(interactionSource = interactionSource)
                else
                    Modifier.clickable { url?.let { Stuff.openInBrowser(it) } }
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