package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.utils.Stuff
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewNavigator
import com.kevinnzou.web.rememberWebViewState
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.use_browser

@Composable
actual fun WebViewScreen(
    initialUrl: String,
    userAccountTemp: UserAccountTemp?,
    creds: PleromaOauthClientCreds?,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    bottomContent: @Composable ColumnScope.() -> Unit,
    viewModel: WebViewVM,
    modifier: Modifier,
) {
    val webViewState = rememberWebViewState(initialUrl)
    val navigator = rememberWebViewNavigator()
    val useBrowserText = stringResource(Res.string.use_browser)

    LaunchedEffect(webViewState.pageTitle) {
        onTitleChange(webViewState.pageTitle ?: "-")
    }

    LaunchedEffect(webViewState.errorsForCurrentRequest) {
        val error = webViewState.errorsForCurrentRequest.find { it.request?.isForMainFrame == true }
        if (error != null) {
            val errorMsg = error.error.description.toString()
            val errorMsgHtml = "<html><body><div align=\"center\">$errorMsg</div></body></html>"
            navigator.loadHtml(
                errorMsgHtml,
                baseUrl = "about:blank",
                mimeType = "text/html",
                historyUrl = null
            )
        }
    }

    LaunchedEffect(webViewState.lastLoadedUrl) {
        val lastLoadedUrl = webViewState.lastLoadedUrl ?: ""

        if (Stuff.disallowedWebviewUrls.any { lastLoadedUrl.startsWith(it) }) {
            val errorMsgHtml =
                "<html><body><div align=\"center\"><h3>$useBrowserText</h3></div></body></html>"

            navigator.loadHtml(
                errorMsgHtml,
                baseUrl = "about:blank",
                mimeType = "text/html",
                historyUrl = null
            )
        } else {
            val lastLoadedUrlObj = Url(lastLoadedUrl)

            if (lastLoadedUrlObj.protocol.name == Stuff.DEEPLINK_PROTOCOL_NAME && lastLoadedUrlObj.segments.isNotEmpty()) {
                val callbackHandled =
                    viewModel.handleCallbackUrl(lastLoadedUrlObj, userAccountTemp!!, creds)

                if (callbackHandled) {
                    val loadingMsg = "⏳"
                    val loadingMsgHtml =
                        "<html><body><div align=\"center\"><h1>$loadingMsg</h1></div></body></html>"
                    navigator.loadHtml(
                        loadingMsgHtml,
                        baseUrl = "about:blank",
                        mimeType = "text/html",
                        historyUrl = null
                    )
                    onTitleChange("...")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.callbackProcessed.collect { done ->
            if (done) {
                onBack()
            }
        }
    }

    Column(
        modifier = modifier
    ) {
        WebView(
            state = webViewState,
            modifier = Modifier.weight(1f),
            captureBackPresses = false,
            navigator = navigator,
        )

        bottomContent()
    }
}