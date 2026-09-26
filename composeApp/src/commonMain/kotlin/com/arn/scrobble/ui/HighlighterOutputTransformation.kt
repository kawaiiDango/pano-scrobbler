package com.arn.scrobble.ui

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

class HighlighterOutputTransformation(
    private val stringsToHighlight: List<String>,
    private val highlightColor: Color
) : OutputTransformation {

    fun highlightToAnnotatedString(text: String): AnnotatedString {
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

    override fun TextFieldBuffer.transformOutput() {
        val text = asCharSequence()
        stringsToHighlight.forEach { strToHighlight ->
            var startIndex = text.indexOf(strToHighlight, 0)
            while (startIndex >= 0) {
                val endIndex = startIndex + strToHighlight.length
                addStyle(
                    spanStyle = SpanStyle(color = highlightColor, fontWeight = FontWeight.Bold),
                    start = startIndex,
                    end = endIndex
                )
                startIndex = text.indexOf(strToHighlight, endIndex)
            }
        }
    }
}