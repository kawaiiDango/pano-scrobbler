package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.kevinnzou.web.LoadingState
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
    pleromaOauthClientCreds: PleromaOauthClientCreds?,
    onSetTitle: (String) -> Unit,
    onBack: () -> Unit,
    bottomContent: @Composable ColumnScope.() -> Unit,
    viewModel: WebViewVM,
    modifier: Modifier,
) {
    val webViewState = rememberWebViewState(initialUrl)
    val navigator = rememberWebViewNavigator()
    val useBrowserText = stringResource(Res.string.use_browser)
    val disableNavigation = remember { userAccountTemp == null && pleromaOauthClientCreds == null }
    var firstLoadComplete by remember { mutableStateOf(false) }

    LaunchedEffect(webViewState.isLoading) {
        if (webViewState.loadingState == LoadingState.Finished && !firstLoadComplete) {
            firstLoadComplete = true
        }
    }

    LaunchedEffect(webViewState.pageTitle) {
        onSetTitle(webViewState.pageTitle ?: "-")
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
        val currentUrl = webViewState.lastLoadedUrl ?: ""

        if ((disableNavigation && firstLoadComplete) ||
            Stuff.disallowedWebviewUrls.any {
                currentUrl.startsWith(it)
            }
        ) {
            if (!PlatformStuff.isTv) {
                navigator.stopLoading()
                navigator.navigateBack()
                PlatformStuff.openInBrowser(currentUrl)
            } else {
                val errorMsgHtml =
                    "<html><body><div align=\"center\"><h3>$useBrowserText</h3></div></body></html>"

                navigator.loadHtml(
                    errorMsgHtml,
                    baseUrl = "about:blank",
                    mimeType = "text/html",
                    historyUrl = null
                )
            }

        } else {
            // use lastLoadedUrl here and not getCurrentUrl()
            val lastLoadedUrlObj =
                webViewState.lastLoadedUrl?.let { Url(it) } ?: return@LaunchedEffect

            if (lastLoadedUrlObj.protocol.name == Stuff.DEEPLINK_SCHEME && lastLoadedUrlObj.segments.isNotEmpty()) {
                val callbackHandled =
                    viewModel.handleCallbackUrl(
                        lastLoadedUrlObj,
                        userAccountTemp!!,
                        pleromaOauthClientCreds
                    )

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
                    onSetTitle("...")
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