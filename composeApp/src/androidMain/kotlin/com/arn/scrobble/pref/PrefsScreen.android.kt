package com.arn.scrobble.pref

import android.app.PendingIntent
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arn.scrobble.MasterSwitchQS
import com.arn.scrobble.R
import com.arn.scrobble.media.NLService
import com.arn.scrobble.media.PersistentNotificationService
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.widget.ChartsWidgetConfigActivity
import com.arn.scrobble.widget.ChartsWidgetProvider
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.fix_it_desc
import pano_scrobbler.composeapp.generated.resources.pref_crashlytics_enabled
import pano_scrobbler.composeapp.generated.resources.pref_intents
import pano_scrobbler.composeapp.generated.resources.pref_master_qs_add
import pano_scrobbler.composeapp.generated.resources.pref_master_qs_already_addded
import pano_scrobbler.composeapp.generated.resources.pref_noti
import pano_scrobbler.composeapp.generated.resources.pref_widget_charts
import pano_scrobbler.composeapp.generated.resources.scrobbler_off
import pano_scrobbler.composeapp.generated.resources.scrobbler_on
import pano_scrobbler.composeapp.generated.resources.show_persistent_noti

actual fun prefQuickSettings(listScope: LazyListScope, scrobblerEnabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PlatformStuff.isTv) {
        listScope.item("master_qs_add") {
            val scrobblerEnabledText =
                stringResource(if (scrobblerEnabled) Res.string.scrobbler_on else Res.string.scrobbler_off)
            val context = AndroidStuff.application
            val scope = rememberCoroutineScope()
            TextPref(
                text = stringResource(Res.string.pref_master_qs_add),
                onClick = {
                    val statusBarManager =
                        ContextCompat.getSystemService(
                            context,
                            StatusBarManager::class.java
                        )
                            ?: return@TextPref
                    statusBarManager.requestAddTileService(
                        ComponentName(context, MasterSwitchQS::class.java),
                        scrobblerEnabledText,
                        Icon.createWithResource(context, R.drawable.vd_noti),
                        context.mainExecutor
                    ) { result ->
                        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                            scope.launch {
                                Stuff.globalSnackbarFlow.emit(
                                    PanoSnackbarVisuals(
                                        message = getString(Res.string.pref_master_qs_already_addded),
                                        isError = true
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

actual fun prefCrashReporter(listScope: LazyListScope, crashReporterEnabled: Boolean) {
    if (!PlatformStuff.isNonPlayBuild) {
        listScope.item(MainPrefs::crashReporterEnabled.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_crashlytics_enabled),
                value = crashReporterEnabled,
                copyToSave = {
                    copy(crashReporterEnabled = it)
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun requestPinWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val pi = PendingIntent.getActivity(
            context,
            30,
            Intent(context, ChartsWidgetConfigActivity::class.java)
                .apply { putExtra(Stuff.EXTRA_PINNED, true) },
            AndroidStuff.updateCurrentOrMutable
        )

        val myProvider =
            ComponentName(context, ChartsWidgetProvider::class.java)
        appWidgetManager.requestPinAppWidget(myProvider, null, pi)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun launchNotificationsActivity(context: Context) {
    val intent = Intent().apply {
        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntentsDescDialog(
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
    ) {
        val items = remember {
            listOf(
                NLService.iSCROBBLER_ON,
                NLService.iSCROBBLER_OFF,
                NLService.iLOVE,
                NLService.iUNLOVE,
                NLService.iCANCEL,
            )
        }

        items.forEach { item ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        PlatformStuff.copyToClipboard(item)
                        onDismissRequest()
                    }
                    .padding(16.dp)
            )
        }
    }
}

actual fun prefChartsWidget(listScope: LazyListScope) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !PlatformStuff.isTv) {
        listScope.item("widget") {
            val context = LocalContext.current

            TextPref(
                text = stringResource(Res.string.pref_widget_charts),
                onClick = {
                    requestPinWidget(context)
                }
            )
        }
    }
}

actual fun prefIntents(listScope: LazyListScope) {
    if (!PlatformStuff.isTv) {
        listScope.item("intents") {
            var showIntentsDescDialog by remember { mutableStateOf(false) }
            TextPref(
                text = stringResource(Res.string.pref_intents),
                onClick = {
                    showIntentsDescDialog = true
                }
            )

            if (showIntentsDescDialog)
                IntentsDescDialog(
                    onDismissRequest = { showIntentsDescDialog = false },
                )
        }
    }
}

actual fun prefNotifications(listScope: LazyListScope) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !PlatformStuff.isTv) {
        listScope.item("notifications") {
            val context = LocalContext.current
            TextPref(
                text = stringResource(Res.string.pref_noti),
                onClick = {
                    launchNotificationsActivity(context)
                }
            )
        }
    }
}

actual fun prefPersistentNoti(listScope: LazyListScope, notiEnabled: Boolean) {
    if (AndroidStuff.canShowPersistentNotiIfEnabled) {
        listScope.item(MainPrefs::notiPersistent.name) {
            SwitchPref(
                text = stringResource(Res.string.show_persistent_noti),
                summary = stringResource(Res.string.fix_it_desc),
                value = notiEnabled,
                copyToSave = {
                    if (it) {
                        PersistentNotificationService.start()
                    } else {
                        PersistentNotificationService.stop()
                    }
                    copy(notiPersistent = it)
                }
            )
        }
    }
}

actual fun addToStartup(
    listScope: LazyListScope,
    isAdded: Boolean,
    onAddedChanged: (Boolean) -> Unit,
) {
    // no-op
}

actual suspend fun isAddedToStartup() = false
