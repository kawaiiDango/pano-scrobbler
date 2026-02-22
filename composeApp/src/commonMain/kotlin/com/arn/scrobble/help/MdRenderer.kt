package com.arn.scrobble.help

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.arn.scrobble.utils.PlatformStuff

// ─── Block → AnnotatedString ──────────────────────────────────

fun MdNode.Block.toAnnotatedString(style: MarkdownStyle = MarkdownStyle()): AnnotatedString =
    buildAnnotatedString {
        when (val block = this@toAnnotatedString) {
            is MdNode.Block.Heading -> appendInlineNodes(block.children, style)
            is MdNode.Block.Paragraph -> appendInlineNodes(block.children, style)
            is MdNode.Block.CodeBlock -> withStyle(style.code) { append(block.code) }
            is MdNode.Block.UnorderedList -> withBulletList {
                block.items.forEach {
                    withBulletListItem {
                        // bullets don't render on desktop
                        if (PlatformStuff.isDesktop)
                            append(style.bulletText)

                        appendInlineNodes(it.children, style)
                    }
                }
            }
        }
    }

// ─── Helpers ──────────────────────────────────────────────────

private fun AnnotatedString.Builder.appendInlineNodes(
    nodes: List<MdNode.InlineNode>,
    style: MarkdownStyle,
) {
    for (node in nodes) {
        when (node) {
            is MdNode.InlineNode.TextSpan -> append(node.text)
            is MdNode.InlineNode.HardBreak -> append("\n")
            is MdNode.InlineNode.BoldSpan -> withStyle(style.bold) {
                appendInlineNodes(
                    node.children,
                    style
                )
            }

            is MdNode.InlineNode.ItalicSpan -> withStyle(style.italic) {
                appendInlineNodes(
                    node.children,
                    style
                )
            }

            is MdNode.InlineNode.BoldItalicSpan -> withStyle(style.boldItalic) {
                appendInlineNodes(node.children, style)
            }

            is MdNode.InlineNode.InlineCode -> withStyle(style.code) { append(node.code) }
            is MdNode.InlineNode.LinkSpan -> withLink(
                LinkAnnotation.Url(node.url, linkInteractionListener = {
                    PlatformStuff.openInBrowser(node.url)
                })
            ) {
                withStyle(style.link) { appendInlineNodes(node.children, style) }
            }
        }
    }
}