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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.arn.scrobble.MasterSwitchQS
import com.arn.scrobble.R
import com.arn.scrobble.media.PersistentNotificationService
import com.arn.scrobble.navigation.PanoRoute
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
import pano_scrobbler.composeapp.generated.resources.grant_notification_access
import pano_scrobbler.composeapp.generated.resources.pref_master
import pano_scrobbler.composeapp.generated.resources.pref_master_qs_add
import pano_scrobbler.composeapp.generated.resources.pref_master_qs_already_addded
import pano_scrobbler.composeapp.generated.resources.pref_noti
import pano_scrobbler.composeapp.generated.resources.pref_offline_info
import pano_scrobbler.composeapp.generated.resources.pref_widget_charts
import pano_scrobbler.composeapp.generated.resources.scrobbler_off
import pano_scrobbler.composeapp.generated.resources.scrobbler_on
import pano_scrobbler.composeapp.generated.resources.show_persistent_noti

actual object PlatformSpecificPrefs {
    actual fun prefQuickSettings(listScope: LazyListScope, scrobblerEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PlatformStuff.isTv) {
            listScope.item("master_qs_add") {
                val scrobblerEnabledText =
                    stringResource(if (scrobblerEnabled) Res.string.scrobbler_on else Res.string.scrobbler_off)
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                TextPref(
                    text = stringResource(Res.string.pref_master_qs_add),
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
                val context = LocalContext.current

                SwitchPref(
                    text = stringResource(Res.string.show_persistent_noti),
                    summary = stringResource(Res.string.fix_it_desc),
                    value = notiEnabled,
                    copyToSave = {
                        if (it) {
                            PersistentNotificationService.start(context)
                        } else {
                            PersistentNotificationService.stop(context)
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

    actual fun discordRpc(listScope: LazyListScope, onNavigate: (PanoRoute) -> Unit) {
        // no-op
    }

    actual fun tidalSteelSeries(listScope: LazyListScope, enabled: Boolean) {
    }

    actual fun deezerApi(listScope: LazyListScope, enabled: Boolean) {
    }

    actual fun prefScrobbler(
        listScope: LazyListScope,
        scrobblerEnabled: Boolean,
        nlsEnabled: Boolean,
        onNavigate: (PanoRoute) -> Unit,
    ) {
        listScope.item(MainPrefs::scrobblerEnabled.name) {
            SwitchPref(
                text = stringResource(Res.string.pref_master),
                summary = if (!nlsEnabled)
                    stringResource(Res.string.grant_notification_access)
                else
                    stringResource(Res.string.pref_offline_info),
                value = scrobblerEnabled && nlsEnabled,
                copyToSave = {
                    if (!nlsEnabled) {
                        onNavigate(PanoRoute.Onboarding)
                        this
                    } else
                        copy(scrobblerEnabled = it)
                }
            )
        }
    }
}