package com.arn.scrobble.help

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Lightweight Markdown AST.
 *
 * Supported elements:
 * - Headings (h1–h6)
 * - Bold (**text** / __text__)
 * - Italic (*text* / _text_)
 * - Bold+Italic (***text*** / ___text___)
 * - Links [text](url)
 * - Inline code (`code`)
 * - Fenced code blocks (``` with optional language tag)
 * - Unordered lists (-, *, +)
 * - Paragraphs (everything else)
 */

// ─── Inline nodes ─────────────────────────────────────────────

sealed interface MdNode {
    sealed interface InlineNode : MdNode {
        data class TextSpan(val text: String) : InlineNode
        data class BoldSpan(val children: List<InlineNode>) : InlineNode
        data class ItalicSpan(val children: List<InlineNode>) : InlineNode
        data class BoldItalicSpan(val children: List<InlineNode>) : InlineNode
        data class InlineCode(val code: String) : InlineNode
        data class LinkSpan(val children: List<InlineNode>, val url: String) : InlineNode
        data object HardBreak : InlineNode
    }

// ─── Block nodes ──────────────────────────────────────────────

    sealed interface Block : MdNode {

        data class Heading(val level: Int, val children: List<InlineNode>) : Block
        data class Paragraph(val children: List<InlineNode>) : Block
        data class CodeBlock(val language: String, val code: String) : Block
        data class UnorderedList(val items: List<ListItem>) : Block
    }

    data class ListItem(val children: List<InlineNode>) : MdNode
}

@Immutable
data class MarkdownStyle(
    val bold: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
    val italic: SpanStyle = SpanStyle(fontStyle = FontStyle.Italic),
    val boldItalic: SpanStyle = SpanStyle(
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Italic,
    ),
    val code: SpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
    ),
    val link: SpanStyle = SpanStyle(
        textDecoration = TextDecoration.Underline,
    ),
    val bulletText: String = "• ",
)