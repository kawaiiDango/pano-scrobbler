package com.arn.scrobble.billing

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.billing_troubleshoot
import pano_scrobbler.composeapp.generated.resources.billing_troubleshoot_github
import pano_scrobbler.composeapp.generated.resources.close

@Composable
fun BillingTroubleshootScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        val text = if (PlatformStuff.isNonPlayBuild) {
            stringResource(Res.string.billing_troubleshoot_github, 5, "July 2025")
        } else {
            stringResource(Res.string.billing_troubleshoot)
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(Res.string.close))
        }
    }
}
