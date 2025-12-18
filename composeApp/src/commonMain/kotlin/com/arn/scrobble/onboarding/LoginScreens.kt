package com.arn.scrobble.onboarding

import androidx.compose.foundation.border
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.InfoText
import com.arn.scrobble.ui.VerifyButton
import com.arn.scrobble.ui.testTagsAsResId
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
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
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    var token by rememberSaveable { mutableStateOf("") }
    var apiRoot by rememberSaveable { mutableStateOf("https://") }
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
                icon = Icons.OpenInBrowser,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        PlatformStuff.openInBrowser("https://listenbrainz.org/profile")
                    }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium
                    )
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
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var apiRoot by rememberSaveable { mutableStateOf("") }
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val doLogin = {
        viewModel.gnufmLogin(apiRoot, username, password)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.testTagsAsResId()
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().testTag("login_url"),
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
            modifier = Modifier.fillMaxWidth().testTag("login_username"),
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
            modifier = Modifier.fillMaxWidth().testTag("login_password"),
            singleLine = true,
            value = password,
            onValueChange = { password = it },
            visualTransformation = remember { PasswordVisualTransformation() },
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

//@Composable
//fun MalojaLoginScreen(
//    onDone: () -> Unit,
//    modifier: Modifier = Modifier,
//    viewModel: LoginViewModel = viewModel { LoginViewModel() },
//) {
//    val result by viewModel.result.collectAsStateWithLifecycle(null)
//    var token by rememberSaveable { mutableStateOf("") }
//    var apiRoot by rememberSaveable { mutableStateOf("https://") }
//    val doLogin = {
//        viewModel.malojaLogin(apiRoot, token)
//    }
//
//    Column(
//        verticalArrangement = Arrangement.spacedBy(16.dp),
//        modifier = modifier
//    ) {
//        OutlinedTextField(
//            modifier = Modifier.fillMaxWidth(),
//            value = apiRoot,
//            singleLine = true,
//            onValueChange = { apiRoot = it },
//            label = { Text(stringResource(Res.string.api_url)) },
//            keyboardOptions = KeyboardOptions(
//                keyboardType = KeyboardType.Uri,
//                capitalization = KeyboardCapitalization.None,
//                imeAction = ImeAction.Next
//            )
//        )
//
//        OutlinedTextField(
//            modifier = Modifier.fillMaxWidth(),
//            value = token,
//            singleLine = true,
//            onValueChange = { token = it },
//            label = { Text(stringResource(Res.string.pref_token_label)) },
//            keyboardOptions = KeyboardOptions(
//                keyboardType = KeyboardType.Text,
//                capitalization = KeyboardCapitalization.None,
//                imeAction = ImeAction.Done
//            ),
//            keyboardActions = KeyboardActions(
//                onDone = {
//                    doLogin()
//                }
//            )
//        )
//
//        VerifyButton(
//            result = result,
//            onDone = onDone,
//            doStuff = doLogin
//        )
//    }
//}

@Composable
fun PleromaLoginScreen(
    onBackAndThenNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    var apiRoot by rememberSaveable { mutableStateOf("https://") }
    val result by viewModel.pleromaCredsResult.collectAsStateWithLifecycle(null)
    val redirectUri = remember {
        if (PlatformStuff.isTv)
            Stuff.DEEPLINK_SCHEME + "://auth/pleroma"
        else
            "urn:ietf:wg:oauth:2.0:oob"
    }
    val onSubmit = {
        viewModel.pleromaCreateApp(apiRoot, redirectUri)
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

                val userAccountTemp = UserAccountTemp(AccountType.PLEROMA, "", _apiRoot)
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