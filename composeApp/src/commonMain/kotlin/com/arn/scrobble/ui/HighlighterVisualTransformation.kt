package com.arn.scrobble.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class HighlighterVisualTransformation(
    private val stringsToHighlight: List<String>,
    private val highlightColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = AnnotatedString.Builder(text.text)
        stringsToHighlight.forEach { strToHighlight ->
            var startIndex = text.text.indexOf(strToHighlight, 0)
            while (startIndex >= 0) {
                val endIndex = startIndex + strToHighlight.length
                annotatedString.addStyle(
                    style = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold),
                    start = startIndex,
                    end = endIndex
                )
                startIndex = text.text.indexOf(strToHighlight, endIndex)
            }
        }
        return TransformedText(annotatedString.toAnnotatedString(), OffsetMapping.Identity)
    }
}