package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds

@Composable
expect fun WebViewScreen(
    initialUrl: String,
    userAccountTemp: UserAccountTemp? = null,
    pleromaOauthClientCreds: PleromaOauthClientCreds? = null,
    onSetTitle: (String) -> Unit,
    onBack: () -> Unit,
    bottomContent: @Composable ColumnScope.() -> Unit = {},
    viewModel: WebViewVM = viewModel { WebViewVM() },
    modifier: Modifier = Modifier,
)