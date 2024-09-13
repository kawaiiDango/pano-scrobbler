package com.arn.scrobble.pref

import androidx.annotation.Keep
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.ui.ExtraBottomSpace
import com.arn.scrobble.ui.ScreenParent

@Preview(showBackground = true)
@Composable
private fun TranslatorsScreenContents(
    viewModel: TranslatorsVM = viewModel(),
    modifier: Modifier = Modifier
) {
    val translators = remember { viewModel.translators }

    LazyColumn(
        modifier = modifier
    ) {
        items(translators) { translator ->
            TranslatorItem(translator)
        }

        item("spacer") {
            ExtraBottomSpace()
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
            .padding(16.dp)

    )
}

@Keep
@Composable
fun TranslatorsScreen() {
    ScreenParent { TranslatorsScreenContents(modifier = it) }
}