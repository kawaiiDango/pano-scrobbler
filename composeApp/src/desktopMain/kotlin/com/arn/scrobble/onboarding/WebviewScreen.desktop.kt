package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.desktop_webview_message
import pano_scrobbler.composeapp.generated.resources.pref_login

@Composable
actual fun WebViewScreen(
    initialUrl: String,
    userAccountTemp: UserAccountTemp?,
    pleromaOauthClientCreds: PleromaOauthClientCreds?,
    onSetTitle: (String?) -> Unit,
    onBack: () -> Unit,
    bottomContent: @Composable ColumnScope.() -> Unit,
    viewModel: WebViewVM,
    modifier: Modifier,
) {
    var callbackHandled by remember { mutableStateOf(false) }

    val title = stringResource(Res.string.pref_login)

    DisposableEffect(Unit) {
        onSetTitle(title)

        PanoNativeComponents.launchWebView(
            initialUrl,
            Stuff.DEEPLINK_PROTOCOL_NAME,
            DesktopStuff.webViewDir.absolutePath
        )

        onDispose {
            onSetTitle(null)
            PanoNativeComponents.quitWebView()
        }
    }

    LaunchedEffect(Unit) {
        WebViewEventFlows.pageLoaded.collect { url ->
            val urlObj = Url(url)
            if (urlObj.protocol.name == Stuff.DEEPLINK_PROTOCOL_NAME && urlObj.segments.isNotEmpty()) {
                callbackHandled =
                    viewModel.handleCallbackUrl(
                        urlObj,
                        userAccountTemp!!,
                        pleromaOauthClientCreds
                    )
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
        Text(
            text = if (callbackHandled) {
                "⏳"
            } else {
                stringResource(Res.string.desktop_webview_message)
            },
        )

        bottomContent()
    }
}