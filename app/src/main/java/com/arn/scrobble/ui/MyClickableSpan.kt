package com.arn.scrobble.ui

import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView

class MyClickableSpan(
    private val start: Int,
    private val end: Int,
    private val callback: (String) -> Unit
) : ClickableSpan() {
    override fun onClick(textView: View) {
        textView as TextView
        val text = textView.text.substring(start, end)
        callback(text)
    }
}