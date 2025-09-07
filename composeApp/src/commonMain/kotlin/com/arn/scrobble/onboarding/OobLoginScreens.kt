package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.UserAccountTemp
import com.arn.scrobble.api.pleroma.PleromaOauthClientCreds
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.VerifyButton
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.pref_imexport_code


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OobLastfmLibrefmLoginScreen(
    userAccountTemp: UserAccountTemp,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    val result by viewModel.result.collectAsStateWithLifecycle(null)
    var url by rememberSaveable { mutableStateOf<String?>(null) }
    var exception by remember(result) { mutableStateOf(result?.exceptionOrNull()) }

    LaunchedEffect(Unit) {
        val apiKey = if (userAccountTemp.type == AccountType.LASTFM)
            Stuff.LAST_KEY
        else
            Stuff.LIBREFM_KEY

        val apiSecret = if (userAccountTemp.type == AccountType.LASTFM)
            Stuff.LAST_SECRET
        else
            Stuff.LIBREFM_KEY

        withContext(Dispatchers.IO) {
            Requesters.lastfmUnauthedRequester.getToken(
                userAccountTemp.apiRoot ?: Stuff.LASTFM_API_ROOT,
                apiKey,
                apiSecret,
            )
        }.onSuccess {
            url = if (userAccountTemp.type == AccountType.LASTFM)
                "https://www.last.fm/api/auth?api_key=$apiKey&token=${it.token}"
            else
                "https://libre.fm/api/auth/?api_key=$apiKey&token=${it.token}"

            if (!PlatformStuff.isTv) {
                PlatformStuff.openInBrowser(url!!)
            }

            viewModel.lastfmOobLogin(
                userAccountTemp,
                it.token,
            )
        }.onFailure {
            exception = it
        }
    }

    LaunchedEffect(result) {
        if (result?.isSuccess == true) {
            onBack()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        if (PlatformStuff.isTv && url != null) {
            QrCodeCanvas(
                url = url!!,
                modifier = Modifier.weight(0.8f),
            )
        }

        if (exception != null) {
            exception?.printStackTrace()
            ErrorText(exception?.redactedMessage)
        } else
            CircularWavyProgressIndicator()
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