package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arn.scrobble.DesktopWebView
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.automirrored.Help
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.desktop_webview_not_loaded
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.login_in_browser

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
    val title = stringResource(Res.string.login_in_browser)
    val webViewNotLoadedMessage = stringResource(Res.string.desktop_webview_not_loaded)
    var statusText by remember { mutableStateOf("") }
    var helpButtonShown by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onSetTitle(title)

        var webViewLoaded = false
        try {
            DesktopWebView.load()
            DesktopWebView.init()

            webViewLoaded = true
            DesktopWebView.launchWebView(
                initialUrl,
                Stuff.DEEPLINK_SCHEME,
                DesktopStuff.webViewDir.absolutePath
            )

            statusText = ""
        } catch (e: UnsatisfiedLinkError) {
            // Handle the case where the native library is not found
            statusText = webViewNotLoadedMessage
        }


        onDispose {
            if (webViewLoaded)
                DesktopWebView.quitWebView()
        }
    }

    LaunchedEffect(Unit) {
        WebViewEventFlows.pageLoaded.collect { url ->
            val urlObj = Url(url)
            if (urlObj.protocol.name == Stuff.DEEPLINK_SCHEME && urlObj.segments.isNotEmpty()) {
                val callbackHandled =
                    viewModel.handleCallbackUrl(
                        urlObj,
                        userAccountTemp!!,
                        pleromaOauthClientCreds
                    )

                if (callbackHandled) {
                    statusText = "⏳"
                }

                DesktopWebView.quitWebView()
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

        if (helpButtonShown) {
            ButtonWithIcon(
                onClick = {
                    helpButtonShown = false
                    statusText = webViewNotLoadedMessage
                },
                icon = Icons.AutoMirrored.Help,
                text = stringResource(Res.string.help),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }

        SelectionContainer {
            Text(text = statusText)
        }

        bottomContent()
    }
}