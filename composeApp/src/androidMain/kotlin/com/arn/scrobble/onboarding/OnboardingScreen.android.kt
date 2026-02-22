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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Warning
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.enumSaver
import com.arn.scrobble.pref.AppListSaveType
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.testTagsAsResId
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.allow_background
import pano_scrobbler.composeapp.generated.resources.check_nls
import pano_scrobbler.composeapp.generated.resources.choose_apps
import pano_scrobbler.composeapp.generated.resources.grant_notification_access
import pano_scrobbler.composeapp.generated.resources.grant_notification_access_desc
import pano_scrobbler.composeapp.generated.resources.notification_access_tv
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.pref_privacy_policy
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
//        if (isGranted)
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
    onDone: () -> Unit,
    onSkip: () -> Unit
) {
    var warningShown by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val toastText = stringResource(
        Res.string.check_nls,
        BuildKonfig.APP_NAME
    )

    LifecycleResumeEffect(Unit) {
        // on resume
        if (PlatformStuff.isNotificationListenerEnabled()) {
            onDone()
        }

        //on pause
        onPauseOrDispose { }
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
    modifier: Modifier,
) {
    // make these lists and not maps otherwise the order gets messed up
    val steps = rememberSaveable {
        listOfNotNull(
            OnboardingStepType.LOGIN,
            OnboardingStepType.NOTIFICATION_LISTENER,
            if (!PlatformStuff.isTv && AndroidStuff.isDkmaNeeded() && !PlatformStuff.isNotificationListenerEnabled())
                OnboardingStepType.DKMA
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
            onDone()
        } else {
            currentStep = steps.firstOrNull { !doneStatus[steps.indexOf(it)] } ?: steps.last()
        }
    }

    Column(
        modifier = modifier.testTagsAsResId(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        TextButton(
            onClick = {
                onNavigate(PanoRoute.PrivacyPolicy)
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp)
                .alpha(0.75f)
                .testTag("button_privacy_policy")
        ) {
            Text(text = stringResource(Res.string.pref_privacy_policy))
        }

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
                    ) {
                        ButtonStepperForLogin(navigate = onNavigate)
                    }
                }

                OnboardingStepType.NOTIFICATION_LISTENER -> {
                    NotificationListenerStep(
                        navigate = onNavigate,
                        isDone = isDone,
                        isExpanded = step == currentStep,
                        onDone = {
                            markAsDone(OnboardingStepType.NOTIFICATION_LISTENER)
                        },
                        onSkip = onDone
                    )
                }

                OnboardingStepType.DKMA -> {
                    VerticalStepperItem(
                        titleRes = Res.string.allow_background,
                        description = "https://dontkillmyapp.com/",
                        openAction = {
                            PlatformStuff.openInBrowser(
                                "https://dontkillmyapp.com/" + Build.MANUFACTURER.lowercase()
                            )
                            markAsDone(OnboardingStepType.DKMA)
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
            }
        }
    }
}