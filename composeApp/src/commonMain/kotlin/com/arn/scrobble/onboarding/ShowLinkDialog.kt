package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.fix_it_action

@Composable
fun ShowLinkDialog(url: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = url.removePrefix("https://"),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        if (!PlatformStuff.isTv) {
            FilledTonalIconButton(
                onClick = {
                    PlatformStuff.openInBrowser(url)
                },
            ) {
                Icon(
                    imageVector = Icons.OpenInBrowser,
                    contentDescription = stringResource(Res.string.fix_it_action),
                )
            }
        }
    }
}