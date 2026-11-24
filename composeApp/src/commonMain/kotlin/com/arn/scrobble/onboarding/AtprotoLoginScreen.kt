package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.atproto.matches
import com.arn.scrobble.ui.PanoOutlinedSecureTextField
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.ui.VerifyButton
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.testTagsAsResId
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.atproto_app_password
import pano_scrobbler.composeapp.generated.resources.atproto_app_password_info
import pano_scrobbler.composeapp.generated.resources.atproto_connect_as
import pano_scrobbler.composeapp.generated.resources.atproto_handle
import pano_scrobbler.composeapp.generated.resources.atproto_public_history_disclosure
import pano_scrobbler.composeapp.generated.resources.atproto_reconnect_needed

@Composable
fun AtprotoLoginScreen(
    type: AccountType,
    reconnect: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    val connection by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { p ->
        p.atprotoAccount?.let { SavedLogin(it.authKey, it.user.name, p.atprotoSession?.matches(it) == true) }
    }
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val saved = connection
    val handle = key(saved?.did) { rememberTextFieldState(saved?.handle.orEmpty()) }
    // Passwords must never go into saved instance state (including Navigation's saved state).
    val password = remember(saved?.did) { TextFieldState() }
    val submit = {
        val handleText = handle.text.toString()
        val passwordText = password.text.toString()
        if (handleText.isNotBlank() && passwordText.isNotBlank()) {
            password.clearText()
        }
        viewModel.atprotoLogin(handleText, passwordText, type, saved?.did)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier.testTagsAsResId()) {
        Text(
            text = stringResource(Res.string.atproto_public_history_disclosure, accountTypeLabel(type)),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (saved != null && saved.canReuse && !reconnect) {
            VerifyButton(
                onDone = onDone,
                doStuff = { viewModel.atprotoConnectSaved(saved.did, type) },
                result = result,
                buttonText = stringResource(Res.string.atproto_connect_as, saved.handle),
            )
        } else {
            if (saved != null) {
                Text(stringResource(Res.string.atproto_reconnect_needed, saved.handle))
            }
            PanoOutlinedTextField(
                handle,
                modifier = Modifier.fillMaxWidth().testTag("atproto_handle"),
                singleLine = true,
                label = { Text(stringResource(Res.string.atproto_handle)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                ),
            )
            Text(stringResource(Res.string.atproto_app_password_info), style = MaterialTheme.typography.bodyMedium)
            PanoOutlinedSecureTextField(
                password,
                modifier = Modifier.fillMaxWidth().testTag("atproto_app_password"),
                label = { Text(stringResource(Res.string.atproto_app_password)) },
                onKeyboardAction = { performDefaultAction ->
                    submit()
                    performDefaultAction()
                },
            )
            VerifyButton(
                onDone = onDone,
                doStuff = submit,
                result = result,
            )
        }
    }
}

private data class SavedLogin(val did: String, val handle: String, val canReuse: Boolean)
