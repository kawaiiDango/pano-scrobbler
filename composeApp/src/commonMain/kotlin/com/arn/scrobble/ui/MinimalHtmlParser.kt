package com.arn.scrobble.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

object MinimalHtmlParser {
    val anchorTagPattern =
        """<a\s+href\s*=\s*"([^"]+)"\s*>(.*?)</a>""".toRegex(RegexOption.IGNORE_CASE)
    val urlPattern = """https?://[^\s<>]+""".toRegex()
    private val htmlEntities =
        mapOf(
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&#39;" to "'",
            "&nbsp;" to " ",
            "<br>" to "\n",
        )

    fun parseLinksToAnnotatedString(
        text: String,
        onLinkClick: ((String) -> Unit)? = null
    ) = buildAnnotatedString {
        var currentIndex = 0
        val text = text.decodeHtmlEntities()
        val anchorMatches = anchorTagPattern.findAll(text).sortedBy { it.range.first }.toList()

        anchorMatches.forEach { match ->
            // Append text before this anchor tag (with auto-linking)
            if (currentIndex < match.range.first) {
                val beforeText = text.substring(currentIndex, match.range.first)
                appendWithAutoLinks(beforeText, urlPattern, onLinkClick)
            }

            // Append the anchor tag as a link
            val url = match.groupValues[1]
            val linkText = match.groupValues[2]
                .replace("""<[^>]+>""".toRegex(), "")
                .trim()

            if (onLinkClick != null) {
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        linkInteractionListener = {
                            onLinkClick(url)
                        }
                    )
                ) {
                    append(linkText)
                }
            } else {
                append(linkText)
            }

            currentIndex = match.range.last + 1
        }

        // Append remaining text (with auto-linking)
        if (currentIndex < text.length) {
            val remainingText = text.substring(currentIndex)
            appendWithAutoLinks(remainingText, urlPattern, onLinkClick)
        }
    }

    private fun AnnotatedString.Builder.appendWithAutoLinks(
        text: String,
        urlPattern: Regex,
        onLinkClick: ((String) -> Unit)?
    ) {
        var lastIndex = 0

        urlPattern.findAll(text).forEach { match ->
            append(text.substring(lastIndex, match.range.first))

            val url = match.value
            if (onLinkClick != null) {
                // Using LinkAnnotation.Url with custom handler
                withLink(
                    LinkAnnotation.Url(
                        url = url,
                        linkInteractionListener = {
                            onLinkClick(url)
                        }
                    )
                ) {
                    append(url)
                }
            } else {
                append(url)
            }

            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    fun String.decodeHtmlEntities(): String =
        htmlEntities.entries.fold(this) { acc, (entity, replacement) ->
            acc.replace(entity, replacement, ignoreCase = true)
        }
}