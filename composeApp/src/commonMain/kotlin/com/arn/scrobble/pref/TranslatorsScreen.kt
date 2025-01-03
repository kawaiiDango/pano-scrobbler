package com.arn.scrobble.pref

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.horizontalOverscanPadding

@Composable
fun TranslatorsScreen(
    viewModel: TranslatorsVM = viewModel { TranslatorsVM() },
    modifier: Modifier = Modifier,
) {
    val translators by viewModel.translators.collectAsStateWithLifecycle()

    PanoLazyColumn(
        modifier = modifier
    ) {
        items(translators, key = { it }) {
            TranslatorItem(it)
        }
    }
}

@Composable
private fun TranslatorItem(translator: String) {
    val interactionSource = remember { MutableInteractionSource() }

    Text(
        style = MaterialTheme.typography.bodyLarge,
        text = translator,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .indication(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = horizontalOverscanPadding(), vertical = 16.dp)

    )
}
