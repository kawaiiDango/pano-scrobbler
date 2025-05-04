package com.arn.scrobble.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.api.github.GithubReleases
import com.arn.scrobble.info.InfoWikiText
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.download
import pano_scrobbler.composeapp.generated.resources.update_available

@Composable
fun UpdateAvailableDialog(
    githubReleases: GithubReleases,
    modifier: Modifier = Modifier
) {
    var dropdownShown by remember { mutableStateOf(false) }
    val assets = remember { githubReleases.getDownloadUrl(PlatformStuff.platformSubstring) }
    var changelogExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(Res.string.update_available, githubReleases.tag_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        InfoWikiText(
            text = githubReleases.body,
            maxLinesWhenCollapsed = 3,
            expanded = changelogExpanded,
            onExpandToggle = { changelogExpanded = true },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = {
                if (assets.size == 1) {
                    PlatformStuff.openInBrowser(assets[0].browser_download_url)
                } else if (assets.size > 1) {
                    dropdownShown = true
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(Res.string.download),
            )

            DropdownMenu(
                expanded = dropdownShown,
                onDismissRequest = { dropdownShown = false },
            ) {
                assets.forEach { asset ->
                    DropdownMenuItem(
                        text = { Text(asset.name) },
                        onClick = {
                            PlatformStuff.openInBrowser(asset.browser_download_url)
                            dropdownShown = false
                        }
                    )
                }
            }
        }
    }
}