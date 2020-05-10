package com.arn.scrobble.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import com.arn.scrobble.R
import com.arn.scrobble.Stuff

class MyClickableSpan(private val start: Int, private val end: Int) : ClickableSpan() {
    override fun onClick(textView: View) {
        textView as TextView
        val text = textView.text.substring(start, end)
        val clipboard = textView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("intent", text)
        clipboard.setPrimaryClip(clip)
        Stuff.toast(textView.context, textView.context.getString(R.string.copied))
    }
}