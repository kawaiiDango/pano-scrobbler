package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.ui.BottomSheetDialogParent
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.changelog

@Composable
private fun ChangelogContent(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(Res.string.changelog),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = BuildKonfig.CHANGELOG,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun ChangelogScreen(
    onDismiss: () -> Unit,
) {
    BottomSheetDialogParent(
        onDismiss = onDismiss
    ) { ChangelogContent(it) }
}