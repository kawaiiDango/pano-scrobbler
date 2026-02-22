package com.arn.scrobble.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.navigation.PanoRoute

@Composable
expect fun WebViewScreen(
    initialUrl: String,
    onSetTitle: (String) -> Unit,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
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
    onNavigate: (PanoRoute) -> Unit,
    onSetStatusText: (String) -> Unit,
    onBack: () -> Unit,
) {
    when (webViewLoginState) {
        WebViewLoginState.None -> {
            onSetStatusText("")
        }

        WebViewLoginState.Unavailable -> {
            onNavigate(PanoRoute.Help("[FAQ-wv]"))
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