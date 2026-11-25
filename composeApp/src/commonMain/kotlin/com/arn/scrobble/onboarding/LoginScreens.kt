package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.PanoOutlinedSecureTextField
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.ui.VerifyButton
import com.arn.scrobble.ui.testTagsAsResId
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.tealfm_handle
import pano_scrobbler.composeapp.generated.resources.tealfm_handle_hint
import pano_scrobbler.composeapp.generated.resources.tealfm_app_password
import pano_scrobbler.composeapp.generated.resources.tealfm_login_help
import pano_scrobbler.composeapp.generated.resources.tealfm_create_app_password
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.api_url
import pano_scrobbler.composeapp.generated.resources.listenbrainz_info
import pano_scrobbler.composeapp.generated.resources.password
import pano_scrobbler.composeapp.generated.resources.pref_token_label
import pano_scrobbler.composeapp.generated.resources.server_url
import pano_scrobbler.composeapp.generated.resources.username

@Composable
fun ListenBrainzLoginScreen(
    customServerSlot: Int?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    val token = rememberTextFieldState()
    val apiRoot = rememberTextFieldState("https://")
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val doLogin = {
        if (customServerSlot != null) {
            viewModel.listenBrainzLogin(
                token.text.toString(),
                customServerSlot,
                apiRoot.text.toString()
            )
        } else {
            viewModel.listenBrainzLogin(token.text.toString(), customServerSlot)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {

        if (customServerSlot != null) {
            PanoOutlinedTextField(
                apiRoot,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(Res.string.api_url)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                )
            )
        } else {
            ButtonWithIcon(
                onClick = {
                    PlatformStuff.openInBrowser("https://listenbrainz.org/profile")
                },
                text = stringResource(
                    Res.string.listenbrainz_info,
                    "listenbrainz.org/profile"
                ),
                icon = Icons.OpenInBrowser,
                maxLines = 3,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }

        PanoOutlinedTextField(
            token,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.pref_token_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = { performDefaultAction ->
                doLogin()
                performDefaultAction()
            }
        )

        VerifyButton(
            onDone = onDone,
            doStuff = doLogin,
            onTrustAll = if (customServerSlot != null) {
                {
                    viewModel.listenBrainzLogin(
                        token.text.toString(),
                        customServerSlot,
                        apiRoot.text.toString(),
                        true
                    )
                }
            } else null,
            result = result
        )
    }
}


@Composable
fun GnufmLoginScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    val username = rememberTextFieldState()
    val password = rememberTextFieldState()
    val apiRoot = rememberTextFieldState()
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val doLogin = {
        viewModel.gnufmLogin(
            apiRoot.text.toString(),
            username.text.toString(),
            password.text.toString()
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.testTagsAsResId()
    ) {
        PanoOutlinedTextField(
            apiRoot,
            modifier = Modifier.fillMaxWidth().testTag("login_url"),
            singleLine = true,
            label = { Text(stringResource(Res.string.api_url)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            )
        )
        PanoOutlinedTextField(
            username,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("login_username"),
            label = { Text(stringResource(Res.string.username)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            )
        )
        PanoOutlinedSecureTextField(
            password,
            modifier = Modifier.fillMaxWidth().testTag("login_password"),
            label = { Text(stringResource(Res.string.password)) },
            onKeyboardAction = { performDefaultAction ->
                doLogin()
                performDefaultAction()
            }
        )

        VerifyButton(
            onDone = onDone,
            doStuff = doLogin,
            result = result
        )
    }
}

@Composable
fun PleromaLoginScreen(
    onBackAndThenNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    val apiRoot = rememberTextFieldState("https://")
    val result by viewModel.pleromaCredsResult.collectAsStateWithLifecycle(null)
    val redirectUri = remember {
        if (PlatformStuff.isTv)
            Stuff.DEEPLINK_SCHEME + "://auth/pleroma"
        else
            "urn:ietf:wg:oauth:2.0:oob"
    }
    val onSubmit = {
        viewModel.pleromaCreateApp(apiRoot.text.toString(), redirectUri)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        PanoOutlinedTextField(
            apiRoot,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.server_url)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = { performDefaultAction ->
                onSubmit()
                performDefaultAction()
            }
        )

        VerifyButton(
            onDone = {
                val creds = result?.getOrNull() ?: return@VerifyButton
                val _apiRoot = if (apiRoot.text.endsWith('/')) apiRoot.text else "${apiRoot.text}/"

                val userAccountTemp = UserAccountTemp(AccountType.PLEROMA, "", _apiRoot.toString())
                val url =
                    "${_apiRoot}oauth/authorize?client_id=${creds.client_id}&redirect_uri=${
                        creds.redirect_uri
                    }&response_type=code&scope=read+write"

                val route = if (PlatformStuff.isTv) {
                    PanoRoute.WebView(url, userAccountTemp, creds)
                } else {
                    PanoRoute.OobPleromaAuth(url, userAccountTemp, creds)
                }

                onBackAndThenNavigate(route)
            },
            doStuff = onSubmit,
            result = result
        )
    }
}

@Composable
fun TealLoginScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    val identifier = rememberTextFieldState()
    val appPassword = remember { TextFieldState() }
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val doLogin = { viewModel.tealLogin(identifier.text.toString(), appPassword.text.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier.testTagsAsResId()) {
        val help = stringResource(Res.string.tealfm_login_help)
        Text(androidx.compose.ui.text.buildAnnotatedString {
            append(help)
            val start = help.indexOf("not")
            if (start >= 0) addStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), start, start + 3)
        })
        ButtonWithIcon(
            onClick = { PlatformStuff.openInBrowser("https://bsky.app/settings/app-passwords") },
            text = stringResource(Res.string.tealfm_create_app_password),
            icon = Icons.OpenInBrowser,
            maxLines = 3,
        )
        PanoOutlinedTextField(
            identifier,
            modifier = Modifier.fillMaxWidth().testTag("teal_handle"),
            singleLine = true,
            label = { Text(stringResource(Res.string.tealfm_handle)) },
            supportingText = { Text(stringResource(Res.string.tealfm_handle_hint)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next,
            ),
        )
        PanoOutlinedSecureTextField(
            appPassword,
            modifier = Modifier.fillMaxWidth().testTag("teal_app_password"),
            label = { Text(stringResource(Res.string.tealfm_app_password)) },
            onKeyboardAction = { performDefaultAction -> doLogin(); performDefaultAction() },
        )
        VerifyButton(
            onDone = { appPassword.edit { replace(0, length, "") }; onDone() },
            doStuff = doLogin,
            result = result,
        )
    }
}
