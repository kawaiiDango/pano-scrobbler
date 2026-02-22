package com.arn.scrobble.onboarding

import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.navigation.PanoRoute
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.loading

@Composable
actual fun WebViewScreen(
    initialUrl: String,
    onSetTitle: (String) -> Unit,
    onBack: () -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier,
    userAccountTemp: UserAccountTemp?,
    pleromaOauthClientCreds: PleromaOauthClientCreds?,
    viewModel: WebViewVM,
) {
    val webViewClient = remember {
        PanoWebViewClient(
            disableNavigation = userAccountTemp == null && pleromaOauthClientCreds == null
        )
    }
    val loadingStr = stringResource(Res.string.loading)
    val webViewChromeClient = remember { PanoWebViewChromeClient() }
    val pageTitleState = remember { mutableStateOf(loadingStr) }
    var statusText by remember { mutableStateOf("") }

    LaunchedEffect(pageTitleState.value) {
        onSetTitle(pageTitleState.value)
    }

    LaunchedEffect(Unit) {
        viewModel.loginState.collect { loginState ->
            handleWebViewStatus(
                loginState,
                onNavigate = onNavigate,
                onSetStatusText = { statusText = it },
                onBack = onBack,
            )
        }
    }

    if (statusText.isEmpty())
        AndroidView(
            factory = {
                WebView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.allowContentAccess = false
                    settings.allowFileAccess = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        settings.isAlgorithmicDarkeningAllowed = true
                    }

                    webViewChromeClient.pageTitleState = pageTitleState
                    webViewClient.callbackUrlAndCookies = viewModel.callbackUrlAndCookies

                    this.webViewClient = webViewClient
                    this.webChromeClient = webViewChromeClient
                    loadUrl(initialUrl)
                }
            },
            modifier = modifier
        )
    else
        Text(
            text = statusText,
            modifier = modifier
        )
}