package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.PanoOutlinedSecureTextField
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.ui.PanoToggleButtonGroup
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

        var typeEditable by rememberSaveable { mutableStateOf(proxy.type) }
        val hostEditable = rememberTextFieldState(proxy.host)
        val portEditable = rememberTextFieldState(proxy.port.toString())
        val userEditable = rememberTextFieldState(proxy.user)
        val passEditable = rememberTextFieldState(proxy.pass)

        var isValid by remember { mutableStateOf(true) }
        val isEnabled = typeEditable != MainPrefs.ProxyPrefs.Type.SYSTEM

        fun validate(): Boolean {
            val portInt = portEditable.text.toString().toIntOrNull() ?: return false

            return try {
                HttpUrl.Builder()
                    .host(hostEditable.text.toString())
                    .username(userEditable.text.toString())
                    .password(passEditable.text.toString())
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
            isValid = hostEditable.text.isNotBlank() && validate()
        }

        DisposableEffect(Unit) {
            onDispose {
                if (validate()) {
                    Stuff.appScope.launch {
                        PlatformStuff.mainPrefs.updateData {
                            it.copy(
                                proxy = MainPrefs.ProxyPrefs(
                                    type = typeEditable,
                                    host = hostEditable.text.toString(),
                                    port = portEditable.text.toString().toInt(),
                                    user = userEditable.text.toString(),
                                    pass = passEditable.text.toString(),
                                )
                            )
                        }
                    }
                }
            }
        }

        PanoToggleButtonGroup(
            listOf(
                stringResource(Res.string.system),
                stringResource(Res.string.proxy_http),
                stringResource(Res.string.proxy_socks5),
            ),
            selectedIndex = typeEditable.ordinal,
            onSelected = { index ->
                typeEditable = when (index) {
                    MainPrefs.ProxyPrefs.Type.SOCKS5.ordinal -> MainPrefs.ProxyPrefs.Type.SOCKS5
                    MainPrefs.ProxyPrefs.Type.HTTP.ordinal -> MainPrefs.ProxyPrefs.Type.HTTP
                    else -> MainPrefs.ProxyPrefs.Type.SYSTEM
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
                hostEditable,
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
                portEditable,
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
                userEditable,
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

            PanoOutlinedSecureTextField(
                passEditable,
                label = {
                    Text(stringResource(Res.string.password))
                },
                enabled = isEnabled,
                isError = !isValid,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}