package com.arn.scrobble.onboarding

import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableSharedFlow

class PanoWebViewClient(
    private val disableNavigation: Boolean,
) : WebViewClient() {
    private var firstLoadFinished = false
    lateinit var callbackUrlAndCookies: MutableSharedFlow<Pair<String, Map<String, String>>>

    override fun shouldOverrideUrlLoading(
        webView: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = if (request.isForMainFrame)
            request.url?.toString()
        else
            null
        url ?: return false

        if (disableNavigation && firstLoadFinished ||
            Stuff.disallowedWebviewUrls.any { url.startsWith(it) }
        ) {
            PlatformStuff.openInBrowser(url)
            return true
        } else {
            val urlObj = Url(url)

            if (urlObj.protocol.name == Stuff.DEEPLINK_SCHEME && urlObj.segments.isNotEmpty()) {
                val cookies = if (urlObj.segments.lastOrNull() == "lastfm") {
                    CookieManager.getInstance()
                        .getCookie(Stuff.LASTFM_URL)
                        .orEmpty()
                        .split(";").mapNotNull { cookie ->
                            val pair = cookie.trim().split("=", limit = 2)
                            if (pair.size == 2 && pair.all { it.isNotEmpty() }) {
                                pair[0] to pair[1]
                            } else
                                null
                        }.toMap()
                } else {
                    emptyMap()
                }
                callbackUrlAndCookies.tryEmit(url to cookies)

                webView.loadUrl("about:blank")

                return true
            }
        }

        return false
    }

    override fun onReceivedError(
        webView: WebView,
        request: WebResourceRequest,
        error: WebResourceError?
    ) {
        if (request.isForMainFrame) {
            val msg = error?.description.toString()
            val htmlData =
                "<html><body><div align=\"center\">$msg</div></body></html>"
            webView.loadDataWithBaseURL("about:blank", htmlData, "text/html", "UTF-8", null)
        }

        super.onReceivedError(webView, request, error)
    }

    override fun onPageFinished(webView: WebView, url: String?) {
        firstLoadFinished = true
        super.onPageFinished(webView, url)
    }
}