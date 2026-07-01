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

    fun highlight(text: String): AnnotatedString {
        val annotatedString = AnnotatedString.Builder(text)
        stringsToHighlight.forEach { strToHighlight ->
            var startIndex = text.indexOf(strToHighlight, 0)
            while (startIndex >= 0) {
                val endIndex = startIndex + strToHighlight.length
                annotatedString.addStyle(
                    style = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold),
                    start = startIndex,
                    end = endIndex
                )
                startIndex = text.indexOf(strToHighlight, endIndex)
            }
        }
        return annotatedString.toAnnotatedString()
    }

    override fun filter(text: AnnotatedString) =
        TransformedText(highlight(text.text), OffsetMapping.Identity)
}