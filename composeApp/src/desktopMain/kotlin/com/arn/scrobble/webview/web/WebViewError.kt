package com.arn.scrobble.webview.web

import androidx.compose.runtime.Immutable

/**
 * Created By Kevin Zou On 2023/9/5
 * A wrapper class to hold errors from the WebView.
 */
@Immutable
data class WebViewError(
    /**
     * The request the error came from.
     */
    val code: Int,
    /**
     * The error that was reported.
     */
    val description: String,
)