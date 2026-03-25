package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arn.scrobble.ui.OutlinedToggleButtons
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.host
import pano_scrobbler.composeapp.generated.resources.port
import pano_scrobbler.composeapp.generated.resources.proxy_socks5
import pano_scrobbler.composeapp.generated.resources.system

@Composable
fun ProxyPrefDialog(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        val enabled by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.customProxyEnabled }
        val host by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.proxyHost }
        val port by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.proxyPort }

        var enabledEditable by remember { mutableStateOf(enabled) }
        var hostEditable by remember { mutableStateOf(host) }
        var portEditable by remember { mutableIntStateOf(port) }

        val hostValid = hostEditable.isNotBlank()
        val portValid = portEditable in 1..65535

        DisposableEffect(Unit) {
            onDispose {
                if (hostValid && portValid) {
                    Stuff.appScope.launch {
                        PlatformStuff.mainPrefs.updateData {
                            it.copy(
                                customProxyEnabled = enabledEditable,
                                proxyHost = hostEditable,
                                proxyPort = portEditable
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
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            PanoOutlinedTextField(
                value = hostEditable,
                onValueChange = { hostEditable = it },
                label = {
                    Text(stringResource(Res.string.host))
                },
                enabled = enabledEditable,
                singleLine = true,
                isError = !hostValid,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            PanoOutlinedTextField(
                value = if (portEditable == 0) "" else portEditable.toString(),
                onValueChange = { portEditable = it.toIntOrNull() ?: 0 },
                label = {
                    Text(stringResource(Res.string.port))
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                enabled = enabledEditable,
                singleLine = true,
                isError = !portValid,
                modifier = Modifier
                    .width(120.dp)
            )
        }
    }
}