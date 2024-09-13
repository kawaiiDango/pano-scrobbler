package com.arn.scrobble.onboarding

import android.os.Bundle
import androidx.annotation.Keep
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.friends.UserAccountTemp
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.InfoText
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle

@Composable
private fun LoginVerifyButton(
    modifier: Modifier = Modifier,
    dologin: () -> Unit,
    result: Result<*>?,
    onDone: () -> Unit,
) {
    var verifying by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val networkErrorStr = stringResource(R.string.network_error)

    LaunchedEffect(result) {
        result?.onFailure {
            verifying = false
            errorText = it.message ?: networkErrorStr
        }?.onSuccess {
            verifying = false
            errorText = null
            onDone()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .height(60.dp)
                .align(Alignment.End)
        ) {
            if (verifying) {
                CircularProgressIndicator()
            } else {
                OutlinedButton(
                    onClick = {
                        verifying = true
                        errorText = null
                        dologin()
                    }
                ) {
                    Text(stringResource(R.string.login_submit), maxLines = 1)
                }
            }
        }

        ErrorText(errorText)
    }
}

@Composable
private fun ListenBrainzLoginContent(
    hasCustomApiRoot: Boolean,
    onDone: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
            .verticalScroll(rememberScrollState())
    ) {

        if (hasCustomApiRoot) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = apiRoot,
                singleLine = true,
                onValueChange = { apiRoot = it },
                label = { Text(stringResource(R.string.api_url)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                )
            )
        } else {
            InfoText(
                text = stringResource(
                    R.string.listenbrainz_info,
                    "https://listenbrainz.org/profile"
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        Stuff.openInBrowser(context, "https://listenbrainz.org/profile")
                    }
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = token,
            singleLine = true,
            onValueChange = { token = it },
            label = { Text(stringResource(R.string.pref_token_label)) },
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

        LoginVerifyButton(
            onDone = onDone,
            dologin = doLogin,
            result = result
        )
    }
}


@Composable
private fun GnufmLoginContent(
    onDone: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
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
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            value = apiRoot,
            onValueChange = { apiRoot = it },
            label = { Text(stringResource(R.string.api_url)) },
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
            label = { Text(stringResource(R.string.username)) },
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
            label = { Text(stringResource(R.string.password)) },
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

        LoginVerifyButton(
            onDone = onDone,
            dologin = doLogin,
            result = result
        )
    }
}

@Composable
private fun MalojaLoginContent(
    onDone: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
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
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiRoot,
            singleLine = true,
            onValueChange = { apiRoot = it },
            label = { Text(stringResource(R.string.api_url)) },
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
            label = { Text(stringResource(R.string.pref_token_label)) },
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

        LoginVerifyButton(
            result = result,
            onDone = onDone,
            dologin = doLogin
        )
    }
}

@Composable
private fun PleromaLoginContent(
    navigateToWebview: (Bundle) -> Unit,
    viewModel: LoginViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    var apiRoot by remember { mutableStateOf("https://") }
    val result by viewModel.pleromaCredsResult.collectAsStateWithLifecycle(null)
    val onSubmit = {
        viewModel.pleromaCreateApp(apiRoot)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiRoot,
            singleLine = true,
            onValueChange = { apiRoot = it },
            label = { Text(stringResource(R.string.server_url)) },
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

        LoginVerifyButton(
            onDone = {
                val creds = result?.getOrNull() ?: return@LoginVerifyButton
                val _apiRoot = if (apiRoot.endsWith('/')) apiRoot else "$apiRoot/"

                Bundle().apply {
                    putString(
                        Stuff.ARG_URL,
                        "${_apiRoot}oauth/authorize?client_id=${creds.client_id}&redirect_uri=${
                            creds.redirect_uri
                        }&response_type=code&scope=read+write"
                    )
                    putSingle(UserAccountTemp(AccountType.PLEROMA, "", _apiRoot))
                    putSingle(creds)

                    navigateToWebview(this)
                }
            },
            dologin = onSubmit,
            result = result
        )
    }
}


@Keep
@Composable
fun ListenBrainzLoginScreen() {
    val fragment = LocalFragment.current
    ScreenParent {
        ListenBrainzLoginContent(hasCustomApiRoot = false, onDone = {
            fragment.findNavController().popBackStack()
        }, modifier = it)
    }
}

@Keep
@Composable
fun CustomListenBrainzLoginScreen() {
    val fragment = LocalFragment.current
    ScreenParent {
        ListenBrainzLoginContent(hasCustomApiRoot = true, onDone = {
            fragment.findNavController().popBackStack()
        }, modifier = it)
    }
}

@Keep
@Composable
fun PleromaLoginScreen() {
    val fragment = LocalFragment.current
    ScreenParent {
        PleromaLoginContent(navigateToWebview = { args ->
            fragment.findNavController().navigate(R.id.webViewFragment, args)
            // todo needs to pop twice
        }, modifier = it)
    }
}

@Keep
@Composable
fun MalojaLoginScreen() {
    val fragment = LocalFragment.current
    ScreenParent {
        MalojaLoginContent(onDone = {
            fragment.findNavController().popBackStack()
        }, modifier = it)
    }
}

@Keep
@Composable
fun GnufmLoginScreen() {
    val fragment = LocalFragment.current
    ScreenParent {
        GnufmLoginContent(onDone = {
            fragment.findNavController().popBackStack()
        }, modifier = it)
    }
}