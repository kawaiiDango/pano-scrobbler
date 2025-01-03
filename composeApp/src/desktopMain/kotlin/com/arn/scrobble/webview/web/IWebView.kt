package com.arn.scrobble.webview.web

/**
 * Created By Kevin Zou On 2023/9/5
 */

interface IWebView {
    /**
     * True when the web view is able to navigate backwards, false otherwise.
     */
    fun canGoBack(): Boolean

    /**
     * True when the web view is able to navigate forwards, false otherwise.
     */
    fun canGoForward(): Boolean

    fun loadUrl(url: String, additionalHttpHeaders: Map<String, String> = emptyMap())

    fun loadHtml(
        html: String? = null,
        baseUrl: String? = null,
        mimeType: String? = "text/html",
        encoding: String? = "utf-8",
        historyUrl: String? = null,
    )

    fun postUrl(
        url: String,
        postData: ByteArray,
    )

    /**
     * Navigates the webview back to the previous page.
     */
    fun goBack()

    /**
     * Navigates the webview forward after going back from a page.
     */
    fun goForward()

    /**
     * Reloads the current page in the webview.
     */
    fun reload()

    /**
     * Stops the current page load (if one is loading).
     */
    fun stopLoading()

    fun evaluateJavaScript(script: String, callback: ((String) -> Unit)? = null)
}