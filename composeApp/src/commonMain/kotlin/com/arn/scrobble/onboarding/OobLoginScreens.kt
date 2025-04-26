package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.ui.VerifyButton
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.pref_imexport_code


@Composable
fun OobLibrefmLoginScreen(
    userAccountTemp: UserAccountTemp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    var token by rememberSaveable { mutableStateOf<String?>(null) }
    val result by viewModel.result.collectAsStateWithLifecycle(null)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            Requesters.lastfmUnauthedRequester.getToken(
                userAccountTemp.apiRoot ?: Stuff.LIBREFM_API_ROOT,
                Stuff.LIBREFM_KEY,
                Stuff.LIBREFM_KEY,
            )
        }.onSuccess {
            val url = "https://libre.fm/api/auth/?api_key=${Stuff.LIBREFM_KEY}&token=${it.token}"
            token = it.token
            PlatformStuff.openInBrowser(url)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        VerifyButton(
            buttonText = stringResource(Res.string.done),
            onDone = onBack,
            doStuff = {
                token?.let { token ->
                    viewModel.lastfmLogin(userAccountTemp, token)
                }
            },
            result = result,
        )
    }
}

@Composable
fun OobPleromaLoginScreen(
    url: String,
    userAccountTemp: UserAccountTemp,
    pleromaCreds: PleromaOauthClientCreds,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    var code by rememberSaveable { mutableStateOf("") }
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    val onSubmit = {
        viewModel.pleromaLogin(userAccountTemp, pleromaCreds, code)
    }

    LaunchedEffect(Unit) {
        PlatformStuff.openInBrowser(url)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = code,
            singleLine = true,
            onValueChange = { code = it },
            label = { Text(stringResource(Res.string.pref_imexport_code)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
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
            onDone = onBack,
            doStuff = onSubmit,
            result = result
        )
    }
}