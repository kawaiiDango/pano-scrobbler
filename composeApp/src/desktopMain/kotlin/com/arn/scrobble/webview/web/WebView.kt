package com.arn.scrobble.webview.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView

/**
 * Created By Kevin Zou On 2023/8/31
 *
 * A wrapper around the Android View WebView to provide a basic WebView composable.
 *
 * @param state The webview state holder where the Uri to load is defined.
 * @param modifier A compose modifier
 * @param captureBackPresses Set to true to have this Composable capture back presses and navigate
 * the WebView back.
 * @param navigator An optional navigator object that can be used to control the WebView's
 * navigation from outside the composable.
 * @param onCreated Called when the WebView is first created.
 * @param onDispose Called when the WebView is destroyed.
 * @sample sample.BasicWebViewSample
 */
@Composable
fun WebView(
    state: WebViewState,
    modifier: Modifier = Modifier,
    captureBackPresses: Boolean = true,
    navigator: WebViewNavigator = rememberWebViewNavigator(),
    onCreated: () -> Unit = {},
    onDispose: () -> Unit = {},
) {
    val webView = state.webView

    webView?.let { wv ->
        LaunchedEffect(wv, navigator) {
            with(navigator) {
                wv.handleNavigationEvents()
            }
        }

        LaunchedEffect(wv, state) {
            snapshotFlow { state.content }.collect { content ->
                when (content) {
                    is WebContent.Url -> {
                        state.lastLoadedUrl = content.url
                        wv.loadUrl(content.url, content.additionalHttpHeaders)
                    }

                    is WebContent.Data -> {
                        wv.loadHtml(
                            content.data,
                            content.baseUrl,
                            content.mimeType,
                            content.encoding,
                            content.historyUrl
                        )
                    }

                    is WebContent.Post -> {
                        wv.postUrl(
                            content.url,
                            content.postData
                        )
                    }

                    is WebContent.NavigatorOnly -> {
                        // NO-OP
                    }
                }
            }
        }
    }

    ActualDesktopWebView(
        state = state,
        modifier = modifier,
        navigator = navigator,
        onCreated = onCreated,
        onDispose = onDispose,
    )
}


@Composable
private fun ActualDesktopWebView(
    state: WebViewState,
    modifier: Modifier,
    navigator: WebViewNavigator,
    onCreated: () -> Unit,
    onDispose: () -> Unit,
) {
    val currentOnDispose by rememberUpdatedState(onDispose)

    DisposableEffect(Unit) {
        onDispose {
            currentOnDispose()
        }
    }

    SwingPanel(
        factory = {
            JFXPanel().apply {
                Platform.runLater {
                    val webView = WebView().apply {
                        isVisible = true
                        engine.addLoadListener(state, navigator)
                        engine.isJavaScriptEnabled = state.webSettings.isJavaScriptEnabled
                    }
                    val root = StackPane()
                    root.children.add(webView)
                    this.scene = Scene(root)
                    state.webView = DesktopWebView(webView)
                    onCreated()
                }
            }
        },
        background = Color.Transparent,
        modifier = modifier,
    )
}

//@Composable
//expect fun ActualWebView(
//    state: WebViewState,
//    modifier: Modifier = Modifier,
//    captureBackPresses: Boolean = true,
//    navigator: WebViewNavigator = rememberWebViewNavigator(),
//    onCreated: () -> Unit = {},
//    onDispose: () -> Unit = {},
//)

