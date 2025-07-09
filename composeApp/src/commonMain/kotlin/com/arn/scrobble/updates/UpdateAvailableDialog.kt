package com.arn.scrobble.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.info.InfoWikiText
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.download
import pano_scrobbler.composeapp.generated.resources.update_available

@Composable
fun UpdateAvailableDialog(
    updateAction: UpdateAction,
    modifier: Modifier = Modifier
) {
    var changelogExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(Res.string.update_available, updateAction.version),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        InfoWikiText(
            text = updateAction.changelog,
            maxLinesWhenCollapsed = 3,
            expanded = changelogExpanded,
            onExpandToggle = { changelogExpanded = !changelogExpanded },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = {
                runUpdateAction(updateAction)
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(Res.string.download),
            )
        }
    }
}