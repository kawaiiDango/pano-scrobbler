package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.desktop_webview_not_loaded

@Composable
expect fun WebViewScreen(
    initialUrl: String,
    onSetTitle: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    userAccountTemp: UserAccountTemp? = null,
    pleromaOauthClientCreds: PleromaOauthClientCreds? = null,
    viewModel: WebViewVM = viewModel {
        WebViewVM(
            userAccountTemp,
            pleromaOauthClientCreds
        )
    },
)

suspend fun handleWebViewStatus(
    webViewLoginState: WebViewLoginState,
    onSetStatusText: (String) -> Unit,
    onBack: () -> Unit,
) {
    when (webViewLoginState) {
        WebViewLoginState.None -> {
            onSetStatusText("")
        }

        WebViewLoginState.Unavailable -> {
            onSetStatusText(getString(Res.string.desktop_webview_not_loaded))
        }

        WebViewLoginState.Processing -> {
            onSetStatusText("⏳")
        }

        WebViewLoginState.Success -> {
            onBack()
        }

        is WebViewLoginState.Failed -> {
            onSetStatusText(webViewLoginState.errorMsg)
        }
    }
}