package com.arn.scrobble.help

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.panoContentPadding

@Composable
fun MdText(
    blocks: List<MdNode.Block>,
    modifier: Modifier = Modifier,
    markdownStyle: MarkdownStyle = MarkdownStyle(),
    contentPadding: PaddingValues = panoContentPadding(),
) {
    PanoLazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        itemsIndexed(
            blocks,
            // Stable key: block index is fine since the list is immutable per Markdown input
            key = { index, _ -> index },
            contentType = { _, block ->
                block::class
            }
        ) { _, block ->
            val annotated = remember(block, markdownStyle) {
                block.toAnnotatedString(markdownStyle)
            }
            
            val interactionSource = remember { MutableInteractionSource() }
            val itemModifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .indication(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                )
                .focusable(interactionSource = interactionSource)

            when (block) {
                is MdNode.Block.CodeBlock ->
                    SelectionContainer(
                        modifier = itemModifier
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = annotated,
                        )
                    }

                is MdNode.Block.Heading ->
                    Text(
                        text = annotated,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineLarge
                            2 -> MaterialTheme.typography.headlineMedium
                            3 -> MaterialTheme.typography.headlineSmall
                            4 -> MaterialTheme.typography.titleMedium
                            5 -> MaterialTheme.typography.titleSmall
                            else -> MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = itemModifier
                            .padding(vertical = 8.dp),
                    )

                is MdNode.Block.Paragraph,
                is MdNode.Block.UnorderedList ->
                    Text(
                        text = annotated,
                        modifier = itemModifier
                            .padding(vertical = 4.dp),
                    )
            }
        }
    }
}