package com.arn.scrobble.onboarding

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.media.PersistentNotificationService
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.appwidget_show
import pano_scrobbler.composeapp.generated.resources.check_nls
import pano_scrobbler.composeapp.generated.resources.fix_it_action
import pano_scrobbler.composeapp.generated.resources.fix_it_battery_title
import pano_scrobbler.composeapp.generated.resources.fix_it_desc
import pano_scrobbler.composeapp.generated.resources.fix_it_energy_title
import pano_scrobbler.composeapp.generated.resources.fix_it_startup_title
import pano_scrobbler.composeapp.generated.resources.fix_it_title
import pano_scrobbler.composeapp.generated.resources.kill_reason
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.show_persistent_noti
import pano_scrobbler.composeapp.generated.resources.special_app_access

@Composable
fun FixItDialog(
    modifier: Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showDkmaLayout = remember { !PlatformStuff.isTv }
    val canShowPersistentNoti by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        !it.notiPersistent && AndroidStuff.canShowPersistentNotiIfEnabled
    }

    var exitReason: String? by remember { mutableStateOf(null) }

    val batteryIntent = remember {
        if (PlatformStuff.isTv) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Intent().setComponent(
                    ComponentName(Stuff.PACKAGE_TV_SETTINGS, Stuff.ACTIVITY_TV_SETTINGS)
                )
            } else null
        } else Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    LaunchedEffect(Unit) {
        val mainPrefs = PlatformStuff.mainPrefs
        val lastKillCheckTime = mainPrefs.data.map { it.lastKillCheckTime }.first()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AndroidStuff.getScrobblerExitReasons(lastKillCheckTime)
                .firstOrNull()
                ?.let {
                    exitReason = it.description
                }

            mainPrefs.updateData { it.copy(lastKillCheckTime = System.currentTimeMillis()) }
            // todo: this is technically wrong
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.fix_it_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(Res.string.fix_it_desc),
        )

        if (!showDkmaLayout && batteryIntent == null && !canShowPersistentNoti) {
            Text(
                text = stringResource(Res.string.not_found),
                style = MaterialTheme.typography.titleLarge
            )
        }

        if (showDkmaLayout) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.fix_it_startup_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        PlatformStuff.openInBrowser("https://dontkillmyapp.com")
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = stringResource(Res.string.fix_it_action))
                }
            }
        }

        if (batteryIntent != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = stringResource(if (PlatformStuff.isTv) Res.string.fix_it_energy_title else Res.string.fix_it_battery_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            context.toast(
                                getString(
                                    Res.string.check_nls,
                                    if (PlatformStuff.isTv) getString(Res.string.special_app_access)
                                    else
                                        BuildKonfig.APP_NAME
                                )
                            )
                        }
                        context.startActivity(batteryIntent)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = stringResource(Res.string.fix_it_action))
                }
            }
        }

        if (canShowPersistentNoti) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.show_persistent_noti),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            PlatformStuff.mainPrefs.updateData { it.copy(notiPersistent = true) }
                        }

                        PersistentNotificationService.start()
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = stringResource(Res.string.appwidget_show))
                }
            }
        }

        if (exitReason != null) {
            Text(
                text = stringResource(Res.string.kill_reason, "\n" + exitReason),
            )
        }
    }
}