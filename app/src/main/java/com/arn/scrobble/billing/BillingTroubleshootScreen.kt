package com.arn.scrobble.billing

import androidx.annotation.Keep
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.R
import com.arn.scrobble.ui.ScreenParent

@Composable
private fun BillingTroubleshootContent(modifier: Modifier = Modifier) {
    val fragment = LocalFragment.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        val text = if (ExtrasConsts.isFossBuild) {
            stringResource(id = R.string.billing_troubleshoot_github, 6, "November 2024")
        } else {
            stringResource(id = R.string.billing_troubleshoot)
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = { fragment.findNavController().popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(id = R.string.close))
        }
    }
}

@Keep
@Composable
fun BillingTroubleshootScreen() {
    ScreenParent {
        BillingTroubleshootContent(it)
    }
}