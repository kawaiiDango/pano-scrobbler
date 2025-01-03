package com.arn.scrobble.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.ui.InfoText
import com.arn.scrobble.ui.VerifyButton
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.api_url
import pano_scrobbler.composeapp.generated.resources.listenbrainz_info
import pano_scrobbler.composeapp.generated.resources.password
import pano_scrobbler.composeapp.generated.resources.pref_token_label
import pano_scrobbler.composeapp.generated.resources.server_url
import pano_scrobbler.composeapp.generated.resources.username

@Composable
fun ListenBrainzLoginScreen(
    hasCustomApiRoot: Boolean,
    onDone: () -> Unit,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
    modifier: Modifier = Modifier,
) {
    var token by remember { mutableStateOf("") }
    var apiRoot by remember { mutableStateOf("https://") }
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val doLogin = {
        if (hasCustomApiRoot) {
            viewModel.listenBrainzLogin(apiRoot, token)
        } else {
            viewModel.listenBrainzLogin(token = token)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {

        if (hasCustomApiRoot) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = apiRoot,
                singleLine = true,
                onValueChange = { apiRoot = it },
                label = { Text(stringResource(Res.string.api_url)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                )
            )
        } else {
            InfoText(
                text = stringResource(
                    Res.string.listenbrainz_info,
                    "https://listenbrainz.org/profile"
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        PlatformStuff.openInBrowser("https://listenbrainz.org/profile")
                    }
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = token,
            singleLine = true,
            onValueChange = { token = it },
            label = { Text(stringResource(Res.string.pref_token_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    doLogin()
                }
            )
        )

        VerifyButton(
            onDone = onDone,
            doStuff = doLogin,
            result = result
        )
    }
}


@Composable
fun GnufmLoginScreen(
    onDone: () -> Unit,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
    modifier: Modifier = Modifier,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var apiRoot by remember { mutableStateOf("https://") }
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val doLogin = {
        viewModel.gnufmLogin(apiRoot, username, password)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            value = apiRoot,
            onValueChange = { apiRoot = it },
            label = { Text(stringResource(Res.string.api_url)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(Res.string.username)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            )
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.password)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    doLogin()
                }
            )
        )

        VerifyButton(
            onDone = onDone,
            doStuff = doLogin,
            result = result
        )
    }
}

@Composable
fun MalojaLoginScreen(
    onDone: () -> Unit,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
    modifier: Modifier = Modifier,
) {
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    var token by remember { mutableStateOf("") }
    var apiRoot by remember { mutableStateOf("https://") }
    val doLogin = {
        viewModel.malojaLogin(apiRoot, token)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiRoot,
            singleLine = true,
            onValueChange = { apiRoot = it },
            label = { Text(stringResource(Res.string.api_url)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = token,
            singleLine = true,
            onValueChange = { token = it },
            label = { Text(stringResource(Res.string.pref_token_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    doLogin()
                }
            )
        )

        VerifyButton(
            result = result,
            onDone = onDone,
            doStuff = doLogin
        )
    }
}

@Composable
fun PleromaLoginScreen(
    onNavigateToWebview: (String, UserAccountTemp, PleromaOauthClientCreds) -> Unit,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
    modifier: Modifier = Modifier,
) {
    var apiRoot by remember { mutableStateOf("https://") }
    val result by viewModel.pleromaCredsResult.collectAsStateWithLifecycle(null)
    val onSubmit = {
        viewModel.pleromaCreateApp(apiRoot)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiRoot,
            singleLine = true,
            onValueChange = { apiRoot = it },
            label = { Text(stringResource(Res.string.server_url)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSubmit()
                }
            )
        )

        VerifyButton(
            onDone = {
                val creds = result?.getOrNull() ?: return@VerifyButton
                val _apiRoot = if (apiRoot.endsWith('/')) apiRoot else "$apiRoot/"

                onNavigateToWebview(
                    "${_apiRoot}oauth/authorize?client_id=${creds.client_id}&redirect_uri=${
                        creds.redirect_uri
                    }&response_type=code&scope=read+write",
                    UserAccountTemp(AccountType.PLEROMA, "", _apiRoot),
                    creds
                )
            },
            doStuff = onSubmit,
            result = result
        )
    }
}