package com.arn.scrobble.webview.web

import co.touchlab.kermit.Logger
import javafx.application.Platform
import javafx.scene.web.WebView

/**
 * Created By Kevin Zou On 2023/9/12
 */
class DesktopWebView(private val webView: WebView) : IWebView {
    private val engine = webView.engine

    override fun canGoBack() = engine.canGoBack()

    override fun canGoForward() = engine.canGoForward()

    override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
        Platform.runLater {
            engine.load(url)
        }
    }

    override fun loadHtml(
        html: String?,
        baseUrl: String?,
        mimeType: String?,
        encoding: String?,
        historyUrl: String?,
    ) {
        Platform.runLater {
            engine.loadContent(html)
        }
    }

    override fun postUrl(url: String, postData: ByteArray) = Platform.runLater {
        engine.loadContent(postData.toString(), "text/html")
    }

    override fun goBack() = Platform.runLater {
        engine.goBack()
    }

    override fun goForward() = Platform.runLater {
        engine.goForward()
    }

    override fun reload() = Platform.runLater {
        engine.reload()
    }

    override fun stopLoading() = Platform.runLater {
        engine.stopLoading()
    }

    override fun evaluateJavaScript(script: String, callback: ((String) -> Unit)?) =
        Platform.runLater {
            Logger.d {
                "evaluateJavaScript: $script"
            }
            val res = engine.executeScript(script)
            callback?.invoke(res.toString())
        }
}