package com.arn.scrobble.onboarding

import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.MutableState

open class PanoWebViewChromeClient : WebChromeClient() {
    lateinit var pageTitleState: MutableState<String>

    override fun onReceivedTitle(view: WebView, title: String?) {
        super.onReceivedTitle(view, title)
        pageTitleState.value = title ?: "-"
    }
}