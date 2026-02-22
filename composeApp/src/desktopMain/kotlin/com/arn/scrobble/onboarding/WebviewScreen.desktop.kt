package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.Stuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.login_in_browser

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
    val title = stringResource(Res.string.login_in_browser)
    var statusText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        onSetTitle(title)

        if (DesktopWebView.inited)
            DesktopWebView.launchWebView(
                initialUrl,
                Stuff.DEEPLINK_SCHEME,
                "https://www.last.fm/",
                DesktopStuff.webViewDir.absolutePath
            )

        viewModel.loginState.collect { loginState ->
            handleWebViewStatus(
                loginState,
                onNavigate = onNavigate,
                onSetStatusText = { statusText = it },
                onBack = onBack,
            )
        }
    }

    Column(
        modifier = modifier
    ) {
        ButtonWithIcon(
            onClick = {
                viewModel.webViewHelp()
            },
            icon = Icons.AutoMirrored.Help,
            text = stringResource(Res.string.help),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )

        SelectionContainer {
            Text(text = statusText)
        }
    }
}