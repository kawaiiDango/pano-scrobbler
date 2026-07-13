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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.arn.scrobble.navigation.enumSaver
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
import pano_scrobbler.composeapp.generated.resources.proxy_http
import pano_scrobbler.composeapp.generated.resources.proxy_socks5
import pano_scrobbler.composeapp.generated.resources.system
import pano_scrobbler.composeapp.generated.resources.username
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ProxyPrefDialog(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        val proxy by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.proxy }

        var typeEditable by rememberSaveable(saver = enumSaver()) { mutableStateOf(proxy.type) }
        var hostEditable by rememberSaveable { mutableStateOf(proxy.host) }
        var portEditable by rememberSaveable { mutableStateOf(proxy.port.toString()) }
        var userEditable by rememberSaveable { mutableStateOf(proxy.user) }
        var passEditable by rememberSaveable { mutableStateOf(proxy.pass) }

        var isValid by remember { mutableStateOf(true) }
        val isEnabled = typeEditable != MainPrefs.ProxySettings.Type.SYSTEM

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
            delay(500.milliseconds) // debounce
            isValid = hostEditable.isNotBlank() && validate()
        }

        DisposableEffect(Unit) {
            onDispose {
                if (validate()) {
                    Stuff.appScope.launch {
                        PlatformStuff.mainPrefs.updateData {
                            it.copy(
                                proxy = MainPrefs.ProxySettings(
                                    type = typeEditable,
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
                stringResource(Res.string.proxy_http),
                stringResource(Res.string.proxy_socks5),
            ),
            selectedIndex = typeEditable.ordinal,
            onSelected = { index ->
                typeEditable = when (index) {
                    MainPrefs.ProxySettings.Type.SOCKS5.ordinal -> MainPrefs.ProxySettings.Type.SOCKS5
                    MainPrefs.ProxySettings.Type.HTTP.ordinal -> MainPrefs.ProxySettings.Type.HTTP
                    else -> MainPrefs.ProxySettings.Type.SYSTEM
                }
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
                enabled = isEnabled,
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
                enabled = isEnabled,
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
                enabled = isEnabled,
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
                enabled = isEnabled,
                singleLine = true,
                isError = !isValid,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}