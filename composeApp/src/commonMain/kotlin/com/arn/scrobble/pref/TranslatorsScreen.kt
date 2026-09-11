package com.arn.scrobble.pref

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.myTransparentCheckableItemColors

@Composable
fun TranslatorsScreen(
    modifier: Modifier = Modifier,
    viewModel: TranslatorsVM = viewModel { TranslatorsVM() },
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

    ListItem(
        colors = ListItemDefaults.myTransparentCheckableItemColors(),
        modifier = Modifier
            .clip(ListItemDefaults.shapes().shape)
            .indication(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
            .focusable(interactionSource = interactionSource)
    ) {
        Text(
            text = translator,
        )
    }
}
