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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.arn.scrobble.MasterSwitchQS
import com.arn.scrobble.R
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.widget.ChartsWidgetConfigActivity
import com.arn.scrobble.widget.ChartsWidgetProvider
import com.arn.scrobble.work.CommonWorkProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.persistent_noti_fgs
import pano_scrobbler.composeapp.generated.resources.persistent_noti_hide
import pano_scrobbler.composeapp.generated.resources.pref_master_qs_add
import pano_scrobbler.composeapp.generated.resources.pref_master_qs_already_addded
import pano_scrobbler.composeapp.generated.resources.pref_noti
import pano_scrobbler.composeapp.generated.resources.pref_widget_charts
import pano_scrobbler.composeapp.generated.resources.scrobbler_off
import pano_scrobbler.composeapp.generated.resources.scrobbler_on
import pano_scrobbler.composeapp.generated.resources.show_persistent_noti

actual object PlatformSpecificPrefs {
    actual fun prefQuickSettings(filteredItem: FilteredItem, scrobblerEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PlatformStuff.isTv) {
            filteredItem("quick_settings", Res.string.pref_master_qs_add, null) { title ->
                val scrobblerEnabledText =
                    stringResource(if (scrobblerEnabled) Res.string.scrobbler_on else Res.string.scrobbler_off)
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                TextPref(
                    text = title,
                    onClick = {
                        val statusBarManager =
                            context.getSystemService(StatusBarManager::class.java)
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

    actual fun prefChartsWidget(filteredItem: FilteredItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !PlatformStuff.isTv) {
            filteredItem("widget", Res.string.pref_widget_charts, null) { title ->
                val context = LocalContext.current

                TextPref(
                    text = title,
                    onClick = {
                        requestPinWidget(context)
                    }
                )
            }
        }
    }

    actual fun prefNotifications(filteredItem: FilteredItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !PlatformStuff.isTv) {
            filteredItem("notifications", Res.string.pref_noti, null) { title ->
                val context = LocalContext.current
                TextPref(
                    text = title,
                    onClick = {
                        launchNotificationsActivity(context)
                    }
                )
            }
        }
    }

    actual fun prefPersistentNotification(filteredItem: FilteredItem, notiPersistent: Boolean) {
        if (!PlatformStuff.isTv) {
            filteredItem(
                "persistent_notification",
                Res.string.persistent_noti_fgs,
                null
            ) { title ->
                SwitchPref(
                    text = title,
                    summary = stringResource(Res.string.show_persistent_noti) + "\n" +
                            stringResource(Res.string.persistent_noti_hide),
                    value = notiPersistent,
                    copyToSave = {
                        copy(notiPersistent = it)
                    }
                )
            }
        }
    }

    actual fun prefAutostart(filteredItem: FilteredItem) {}

    actual fun prefAddToAppLauncher(filteredItem: FilteredItem) {}

    actual fun discordRpc(filteredItem: FilteredItem, onNavigate: (PanoRoute) -> Unit) {
        // no-op
    }

    actual fun tidalSteelSeries(filteredItem: FilteredItem, enabled: Boolean) {
    }

    actual fun deezerApi(filteredItem: FilteredItem, enabled: Boolean) {
    }

    actual fun onPrefScrobblerToggled(
        scrobblerEnabled: Boolean,
    ) {
        Stuff.appScope.launch(Dispatchers.IO) {
            MasterSwitchQS.requestListeningState(AndroidStuff.applicationContext)
            if (scrobblerEnabled) {
                AndroidStuff.requestRebindFromContentProvider(AndroidStuff.applicationContext.contentResolver)
            }
        }
    }

    actual fun updateCheck(
        filteredItem: FilteredItem, enabled: Boolean,
        updateProgress: CommonWorkProgress?
    ) {
    }
}