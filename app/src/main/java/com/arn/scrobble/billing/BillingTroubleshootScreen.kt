package com.arn.scrobble.billing

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.R

@Composable
fun BillingTroubleshootScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        val text = if (ExtrasConsts.isNonPlayBuild) {
            stringResource(id = R.string.billing_troubleshoot_github, 6, "November 2024")
        } else {
            stringResource(id = R.string.billing_troubleshoot)
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(id = R.string.close))
        }
    }
}
