package com.arn.scrobble.onboarding

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Warning
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.main.ScrobblerState
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.enumSaver
import com.arn.scrobble.pref.AppListSaveType
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.testTagsAsResId
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add_exception
import pano_scrobbler.composeapp.generated.resources.check_nls
import pano_scrobbler.composeapp.generated.resources.choose_apps
import pano_scrobbler.composeapp.generated.resources.fix_it_battery_title
import pano_scrobbler.composeapp.generated.resources.fix_it_startup_title
import pano_scrobbler.composeapp.generated.resources.grant_notification_access
import pano_scrobbler.composeapp.generated.resources.grant_notification_access_desc
import pano_scrobbler.composeapp.generated.resources.notification_access_tv
import pano_scrobbler.composeapp.generated.resources.persistent_noti_desc
import pano_scrobbler.composeapp.generated.resources.persistent_noti_fgs
import pano_scrobbler.composeapp.generated.resources.persistent_noti_oems
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.pref_scrobble_from
import pano_scrobbler.composeapp.generated.resources.send_notifications
import pano_scrobbler.composeapp.generated.resources.send_notifications_desc
import pano_scrobbler.composeapp.generated.resources.will_not_scrobble


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun NotificationPermissionStep(
    isDone: Boolean,
    isExpanded: Boolean,
    onDone: () -> Unit,
) {
    fun checkPermission() {
        if (AndroidStuff.applicationContext.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onDone()
        }
    }

    LaunchedEffect(Unit) {
        checkPermission()
    }

    val permRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted)
            notifyPlayingTrackEvent(PlayingTrackNotifyEvent.RepostFgNoti)
        onDone()
    }

    VerticalStepperItem(
        titleRes = Res.string.send_notifications,
        description = stringResource(Res.string.send_notifications_desc),
        onSkip = onDone,
        openAction = {
            permRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        isDone = isDone,
        isExpanded = isExpanded,
    )
}

@Composable
private fun NotificationListenerStep(
    navigate: (PanoRoute) -> Unit,
    isDone: Boolean,
    isExpanded: Boolean,
    scrobblerState: ScrobblerState,
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    var warningShown by rememberSaveable { mutableStateOf(false) }
    val notiPersistent by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.notiPersistent }
    val scope = rememberCoroutineScope()

    LaunchedEffect(scrobblerState) {
        // on resume
        if (scrobblerState != ScrobblerState.NLSDisabled && scrobblerState != ScrobblerState.Unknown) {
            onDone()
        }
    }

    VerticalStepperItem(
        titleRes = Res.string.grant_notification_access,
        description = stringResource(Res.string.grant_notification_access_desc) +
                if (PlatformStuff.isTv) "\n" + stringResource(Res.string.notification_access_tv)
                else "",
        openAction = {
            val intent = if (PlatformStuff.isTv &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
//                && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
            )
                Intent().setComponent(
                    ComponentName(Stuff.PACKAGE_TV_SETTINGS, Stuff.ACTIVITY_TV_SETTINGS)
                )
            else
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

            if (AndroidStuff.applicationContext.packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                ) != null
            ) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                AndroidStuff.applicationContext.startActivity(intent)
            } else {
                navigate(PanoRoute.Help("[FAQ-nc]"))
            }
        },
        isDone = isDone,
        isExpanded = isExpanded,
        onSkip = { warningShown = true },
        additionalContent = {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .toggleable(
                        value = notiPersistent,
                        onValueChange = {
                            scope.launch {
                                PlatformStuff.mainPrefs.updateData {
                                    it.copy(notiPersistent = !it.notiPersistent)
                                }
                            }
                        },
                        role = Role.Checkbox
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = notiPersistent,
                    onCheckedChange = null // null recommended for accessibility with screenreaders
                )

                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.persistent_noti_fgs),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(
                            Res.string.persistent_noti_desc,
                            stringResource(Res.string.persistent_noti_oems)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    )

    if (warningShown) {
        AlertDialogOk(
            text = stringResource(Res.string.will_not_scrobble),
            icon = Icons.Warning,
            onConfirmation = {
                warningShown = false
                onSkip()
            },
            onDismissRequest = { warningShown = false },
        )
    }
}

@Composable
actual fun OnboardingScreen(
    onNavigate: (PanoRoute) -> Unit,
    onDone: () -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier,
) {
    val scrobblerState by mainViewModel.scrobblerStateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val startupMgrIntentPkg by
    rememberSaveable { mutableStateOf(AndroidStuff.findStartupMgrIntentPkg(context)) }

    val needsBatteryOptimizationIgnore = remember {
        if (!PlatformStuff.isTv && startupMgrIntentPkg != null)
            Build.MANUFACTURER.lowercase() in arrayOf(
                Stuff.MANUFACTURER_XIAOMI,
            )
        else
            false
    }

    // make these lists and not maps otherwise the order gets messed up
    val steps = rememberSaveable {
        listOfNotNull(
            OnboardingStepType.LOGIN,
            OnboardingStepType.NOTIFICATION_LISTENER,
//            if (!PlatformStuff.isTv && AndroidStuff.isDkmaNeeded() && scrobblerState == ScrobblerState.NLSDisabled)
//                OnboardingStepType.DKMA
//            else null,
            if (startupMgrIntentPkg != null && !PlatformStuff.isTv)
                OnboardingStepType.AUTOSTART
            else null,
            if (needsBatteryOptimizationIgnore)
                OnboardingStepType.BATTERY_OPTIMIZATIONS_IGNORE
            else null,
            OnboardingStepType.CHOOSE_APPS,
            if (!PlatformStuff.isTv && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                OnboardingStepType.SEND_NOTIFICATIONS
            else null,
        )
    }

    val doneStatus = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf(*it.toTypedArray()) }
        )
    ) {
        mutableStateListOf(*steps.map { false }.toTypedArray())
    }

    fun markAsDone(step: OnboardingStepType) {
        doneStatus[steps.indexOf(step)] = true
    }

    var currentStep by rememberSaveable(saver = enumSaver()) { mutableStateOf(steps.first()) }

    val isLoggedIn by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.scrobbleAccounts.isNotEmpty() }
    val appListWasRun by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.appListWasRun }
    val showProxySettings = remember { WebViewProxyOverride.isWebViewProxyOverrideSupported() }
    val checkAppString = stringResource(Res.string.check_nls, BuildKonfig.APP_NAME)

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            markAsDone(OnboardingStepType.LOGIN)
        }
    }

    LaunchedEffect(appListWasRun) {
        if (appListWasRun) {
            markAsDone(OnboardingStepType.CHOOSE_APPS)
        }
    }

    LaunchedEffect(doneStatus.toList()) {
        if (doneStatus.all { it }) {
            mainViewModel.updateScrobblerServiceState(true)
            onDone()
        } else {
            currentStep = steps.firstOrNull { !doneStatus[steps.indexOf(it)] } ?: steps.last()
        }
    }

    LifecycleStartEffect(Unit) {
        if (scrobblerState == ScrobblerState.NLSDisabled || scrobblerState == ScrobblerState.Unknown) {
            mainViewModel.updateScrobblerServiceState(false)
        }

        onStopOrDispose { }
    }

    if (OnboardingStepType.BATTERY_OPTIMIZATIONS_IGNORE in steps) {
        LifecycleStartEffect(Unit) {
            if (AndroidStuff.isIgnoringBatteryOptimizations()) {
                markAsDone(OnboardingStepType.BATTERY_OPTIMIZATIONS_IGNORE)
            }

            onStopOrDispose { }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                PanoNotifications.createChannels()
            }
        }
    }

    Column(
        modifier = modifier.testTagsAsResId(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        OnboardingTopRow(
            onNavigate = onNavigate,
            showProxySettings = showProxySettings,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp)
                .alpha(0.75f)
        )

        steps.indices.forEach { i ->

            val step = steps[i]
            val isDone = doneStatus[i]

            when (step) {
                OnboardingStepType.LOGIN -> {
                    VerticalStepperItem(
                        titleRes = Res.string.pref_login,
                        description = null,
                        openAction = {},
                        isDone = isDone,
                        isExpanded = step == currentStep,
                        buttonsContent = {
                            ButtonStepperForLogin(navigate = onNavigate)
                        }
                    )
                }

                OnboardingStepType.NOTIFICATION_LISTENER -> {
                    NotificationListenerStep(
                        navigate = onNavigate,
                        isDone = isDone,
                        isExpanded = step == currentStep,
                        scrobblerState = scrobblerState,
                        onDone = {
                            markAsDone(OnboardingStepType.NOTIFICATION_LISTENER)
                        },
                        onSkip = onDone
                    )
                }

                // their own app does this https://www.mi.com/global/support/faq/details/KA-517222/

                OnboardingStepType.AUTOSTART -> {
                    VerticalStepperItem(
                        titleRes = Res.string.fix_it_startup_title,
                        description = stringResource(Res.string.persistent_noti_fgs),
                        openButtonText = stringResource(Res.string.add_exception),
                        openAction = {
                            if (startupMgrIntentPkg != null) {
                                val launched =
                                    AndroidStuff.getStartupMgrIntents(startupMgrIntentPkg!!)
                                        .any { intent ->
                                            try {
                                                context.startActivity(intent)
                                                true
                                            } catch (e: Exception) {
                                                Logger.e("Failed to open autostart intent", e)
                                                false
                                            }
                                        }

                                if (!launched) {
                                    PlatformStuff.openInBrowser("https://dontkillmyapp.com/" + Build.MANUFACTURER.lowercase())
                                } else {
                                    context.toast(checkAppString)
                                }
                            }
                            markAsDone(OnboardingStepType.AUTOSTART)
                        },
                        isDone = isDone,
                        isExpanded = step == currentStep,
                    )
                }

                OnboardingStepType.BATTERY_OPTIMIZATIONS_IGNORE -> {
                    VerticalStepperItem(
                        titleRes = Res.string.fix_it_battery_title,
                        description = stringResource(Res.string.persistent_noti_fgs),
                        openButtonText = stringResource(Res.string.add_exception),
                        openAction = {
                            val intent =
                                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                            try {
                                context.startActivity(intent)
                                context.toast(checkAppString)
                            } catch (e: Exception) {
                                Logger.e("Failed to open battery optimization settings", e)
                                markAsDone(OnboardingStepType.BATTERY_OPTIMIZATIONS_IGNORE)
                            }

                        },
                        isDone = isDone,
                        isExpanded = step == currentStep,
                    )
                }

                OnboardingStepType.CHOOSE_APPS -> {
                    VerticalStepperItem(
                        titleRes = Res.string.pref_scrobble_from,
                        description = stringResource(Res.string.choose_apps),
                        openAction = {
                            onNavigate(
                                PanoRoute.AppList(
                                    saveType = AppListSaveType.Scrobbling,
                                    preSelectedPackages = emptyList(),
                                    isSingleSelect = false,
                                )
                            )
                        },
                        isDone = isDone,
                        isExpanded = step == currentStep,
                    )
                }

                OnboardingStepType.SEND_NOTIFICATIONS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        NotificationPermissionStep(
                            isDone = isDone,
                            isExpanded = step == currentStep,
                            onDone = {
                                markAsDone(OnboardingStepType.SEND_NOTIFICATIONS)
                            }
                        )
                    }
                }

                else -> {}
            }
        }
    }
}