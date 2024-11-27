package com.arn.scrobble.onboarding

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.Keep
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.PersistentNotificationService
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.ui.BottomSheetDialogParent
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
private fun FixItContent(
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val showDkmaLayout = remember { !Stuff.isTv }
    val showNotiPersistent by PlatformStuff.mainPrefs.data.map {
        !it.notiPersistent && !Stuff.isTv &&
                Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.TIRAMISU
    }
        .collectAsStateWithLifecycle(false)

    var exitReason: String? by remember { mutableStateOf(null) }

    val batteryIntent = remember {
        if (Stuff.isTv) {
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
            Stuff.getScrobblerExitReasons(lastKillCheckTime, false)
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
            text = stringResource(id = R.string.fix_it_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(id = R.string.fix_it_desc),
        )

        if (!showDkmaLayout && batteryIntent == null && !showNotiPersistent) {
            Text(
                text = stringResource(id = R.string.not_found),
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
                    text = stringResource(id = R.string.fix_it_startup_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        Stuff.openInBrowser("https://dontkillmyapp.com")
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = stringResource(id = R.string.fix_it_action))
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
                    text = stringResource(id = if (Stuff.isTv) R.string.fix_it_energy_title else R.string.fix_it_battery_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        context.toast(
                            context.getString(
                                R.string.check_nls,
                                if (Stuff.isTv) context.getString(R.string.special_app_access)
                                else
                                    context.getString(R.string.app_name)
                            )
                        )
                        context.startActivity(batteryIntent)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = stringResource(id = R.string.fix_it_action))
                }
            }
        }

        if (showNotiPersistent) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.show_persistent_noti),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            PlatformStuff.mainPrefs.updateData { it.copy(notiPersistent = true) }
                        }
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, PersistentNotificationService::class.java)
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(text = stringResource(id = R.string.appwidget_show))
                }
            }
        }

        if (exitReason != null) {
            Text(
                text = stringResource(R.string.kill_reason, "\n" + exitReason),
            )
        }
    }
}

@Composable
fun FixItScreen(
    onDismiss: () -> Unit
) {
    BottomSheetDialogParent(
        onDismiss = onDismiss
    ) { FixItContent(it) }
}