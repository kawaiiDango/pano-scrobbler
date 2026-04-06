package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.OutlinedToggleButtons
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.host
import pano_scrobbler.composeapp.generated.resources.password
import pano_scrobbler.composeapp.generated.resources.port
import pano_scrobbler.composeapp.generated.resources.proxy_socks5
import pano_scrobbler.composeapp.generated.resources.system
import pano_scrobbler.composeapp.generated.resources.username

@Composable
fun ProxyPrefDialog(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        val proxy by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.proxy }

        var enabledEditable by remember { mutableStateOf(proxy.enabled) }
        var hostEditable by remember { mutableStateOf(proxy.host) }
        var portEditable by remember { mutableStateOf(proxy.port.toString()) }
        var userEditable by remember { mutableStateOf(proxy.user) }
        var passEditable by remember { mutableStateOf(proxy.pass) }

        var isValid by remember { mutableStateOf(true) }

        fun validate(): Boolean {
            val portInt = portEditable.toIntOrNull() ?: return false

            return try {
                HttpUrl.Builder()
                    .host(hostEditable)
                    .username(userEditable)
                    .password(passEditable)
                    .port(portInt)
                    .scheme("http")
                    .build()
                true
            } catch (e: Exception) {
                false
            }
        }

        LaunchedEffect(hostEditable, portEditable, userEditable, passEditable) {
            delay(500) // debounce
            isValid = hostEditable.isNotBlank() && validate()
        }

        DisposableEffect(Unit) {
            onDispose {
                if (validate()) {
                    Stuff.appScope.launch {
                        PlatformStuff.mainPrefs.updateData {
                            it.copy(
                                proxy = MainPrefs.ProxySettings(
                                    enabled = enabledEditable,
                                    host = hostEditable,
                                    port = portEditable.toInt(),
                                    user = userEditable,
                                    pass = passEditable,
                                )
                            )
                        }
                    }
                }
            }
        }

        OutlinedToggleButtons(
            listOf(
                stringResource(Res.string.system),
                stringResource(Res.string.proxy_socks5)
            ),
            selectedIndex = if (enabledEditable) 1 else 0,
            onSelected = { index ->
                enabledEditable = index == 1
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            PanoOutlinedTextField(
                value = hostEditable,
                onValueChange = { hostEditable = it.trim() },
                label = {
                    Text(stringResource(Res.string.host))
                },
                enabled = enabledEditable,
                singleLine = true,
                isError = !isValid,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            PanoOutlinedTextField(
                value = portEditable,
                onValueChange = { portEditable = it.trim() },
                label = {
                    Text(stringResource(Res.string.port))
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                enabled = enabledEditable,
                singleLine = true,
                isError = !isValid,
                modifier = Modifier
                    .width(120.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            PanoOutlinedTextField(
                value = userEditable,
                onValueChange = { userEditable = it },
                label = {
                    Text(stringResource(Res.string.username))
                },
                enabled = enabledEditable,
                singleLine = true,
                isError = !isValid,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            PanoOutlinedTextField(
                value = passEditable,
                onValueChange = { passEditable = it },
                label = {
                    Text(stringResource(Res.string.password))
                },
                visualTransformation = PasswordVisualTransformation(),
                enabled = enabledEditable,
                singleLine = true,
                isError = !isValid,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}