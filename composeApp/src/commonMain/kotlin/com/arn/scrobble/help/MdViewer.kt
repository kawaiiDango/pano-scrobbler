package com.arn.scrobble.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.PlatformStuff

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MdViewer(
    mdItems: List<MdItem>,
    modifier: Modifier = Modifier,
    highlightText: String? = null,
) {
    PanoLazyColumn(
        contentPadding = panoContentPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        items(mdItems, key = { it.header }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = it.header.toAnnotatedString(
                        highlightText = highlightText,
                        highlightColor = MaterialTheme.colorScheme.tertiary
                    ),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )

                SelectionContainer {
                    Text(
                        text = it.content.toAnnotatedString(
                            highlightText = highlightText,
                            highlightColor = MaterialTheme.colorScheme.tertiary,
                            entities = it.entities
                        ),
                    )
                }
            }
        }
    }
}

private fun String.toAnnotatedString(
    highlightText: String?,
    highlightColor: Color,
    entities: List<MdEntity> = emptyList(),
): AnnotatedString {
    val text = this

    return buildAnnotatedString {
        append(text)

        entities
            .sortedBy { it.start }
            .forEach { e ->
                val s = e.start.coerceIn(0, text.length)
                val en = e.end.coerceIn(0, text.length)
                if (s >= en) return@forEach

                when (e.type) {
                    MdEntityType.bold -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        s,
                        en
                    )

                    MdEntityType.italic -> addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        s,
                        en
                    )

                    MdEntityType.code -> addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                        ), s, en
                    )

                    MdEntityType.link -> {
                        addStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline
                            ), s, en
                        )
                        e.url?.let { url ->
                            addLink(
                                LinkAnnotation.Url(url) {
                                    PlatformStuff.openInBrowser(url)
                                },
                                start = s,
                                end = en
                            )
                        }
                    }

                    null -> {}
                }
            }

        if (!highlightText.isNullOrBlank()) {
            var searchIndex = text.indexOf(highlightText, ignoreCase = true)
            while (searchIndex >= 0) {
                val s = searchIndex.coerceIn(0, text.length)
                val en = (searchIndex + highlightText.length).coerceIn(0, text.length)
                addStyle(SpanStyle(color = highlightColor), s, en)
                searchIndex =
                    text.indexOf(
                        highlightText,
                        searchIndex + highlightText.length,
                        ignoreCase = true
                    )
            }
        }
    }
}