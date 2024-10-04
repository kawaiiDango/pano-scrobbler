package com.arn.scrobble.onboarding

import android.Manifest
import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.ButtonWithDropdown
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.map


private enum class OnboardingStepType {
    LOGIN,
    NOTIFICATION_LISTENER,
    DKMA,
    CHOOSE_APPS,
    SEND_NOTIFICATIONS,
}

@Composable
fun ButtonsStepper(
    onOpenClick: () -> Unit,
    onSkipClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.End)
    ) {
        if (onSkipClick != null) {
            TextButton(onClick = onSkipClick) {
                Text(text = stringResource(R.string.skip))
            }
        }
        OutlinedButton(onClick = onOpenClick) {
            Text(text = stringResource(R.string.fix_it_action))
        }
    }
}

@Composable
private fun ButtonStepperForLogin(navigate: (PanoRoute) -> Unit) {
    val accountTypesToStrings = remember {
        AccountType.entries.filterNot {
            it == AccountType.LASTFM || Stuff.isTv && it == AccountType.FILE
        }.associateWith {
            Scrobblables.getString(it)
        }
    }

    Row {
        ButtonWithDropdown(
            onMainButtonClick = {
                navigate(LoginDestinations.route(AccountType.LASTFM))
            },
            onItemClick = {
                navigate(LoginDestinations.route(it))
            },
            itemToTexts = accountTypesToStrings,
            text = stringResource(R.string.lastfm),
        )
    }
}

@Composable
private fun VerticalStepperItem(
    titleRes: Int,
    descriptionRes: Int?,
    openAction: () -> Unit,
    isDone: Boolean,
    isExpanded: Boolean,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    buttonsContent: @Composable () -> Unit = {
        ButtonsStepper(
            onOpenClick = openAction,
            onSkipClick = onSkip
        )
    },
) {

    val icon = if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = horizontalOverscanPadding())
            .then(
                if (isExpanded)
                    Modifier.alpha(1f)
                else
                    Modifier.alpha(0.5f)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )


            AnimatedVisibility(isExpanded) {
                Column(
                    modifier = modifier.fillMaxWidth()
                ) {
                    if (descriptionRes != null) {
                        Text(
                            text = stringResource(descriptionRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    buttonsContent()
                }
            }
        }
    }
}

@TargetApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun NotificationPermissionStep(
    isDone: Boolean,
    isExpanded: Boolean,
    onDone: () -> Unit,
) {
    fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                PlatformStuff.application,
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
        titleRes = R.string.send_notifications,
        descriptionRes = R.string.send_notifications_desc,
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
) {
    var warningShown by remember { mutableStateOf(false) }

    val tvLink = stringResource(R.string.tv_link)
    val toastText = stringResource(
        R.string.check_nls,
        if (Stuff.isTv)
            stringResource(R.string.special_app_access)
        else
            stringResource(R.string.app_name)
    )

    LifecycleResumeEffect(Unit) {
        // on resume
        if (Stuff.isNotificationListenerEnabled()) {
            onDone()
        }

        //on pause
        onPauseOrDispose { }
    }

    VerticalStepperItem(
        titleRes = R.string.grant_notification_access,
        descriptionRes = R.string.grant_notification_access_desc,
        openAction = {
            val intent = if (Stuff.isTv &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q
            )
                Intent().setComponent(
                    ComponentName(Stuff.PACKAGE_TV_SETTINGS, Stuff.ACTIVITY_TV_SETTINGS)
                )
            else
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

            if (PlatformStuff.application.packageManager.resolveActivity(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                ) != null
            ) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                PlatformStuff.application.startActivity(intent)
                Toast.makeText(PlatformStuff.application, toastText, Toast.LENGTH_SHORT).show()
            } else {
                navigate(PanoRoute.WebView(tvLink))
            }
        },
        isDone = isDone,
        isExpanded = isExpanded,
        onSkip = { warningShown = true },
    )

    if (warningShown) {
        AlertDialogOk(
            text = stringResource(R.string.will_not_scrobble),
            icon = Icons.Outlined.WarningAmber,
            onConfirmation = {
                warningShown = false
                onDone()
            },
            onDismissRequest = { warningShown = false },
        )
    }
}

@Composable
fun OnboardingScreen(
    onNavigate: (PanoRoute) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // make these lists and not maps otherwise the order gets messed up
    val steps = rememberSaveable {
        listOfNotNull(
            OnboardingStepType.LOGIN,
            OnboardingStepType.NOTIFICATION_LISTENER,
            if (Stuff.isDkmaNeeded() && !Stuff.isNotificationListenerEnabled())
                OnboardingStepType.DKMA
            else null,
            OnboardingStepType.CHOOSE_APPS,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                OnboardingStepType.SEND_NOTIFICATIONS
            else null,
        )
    }

    val doneStatus = rememberSaveable(
        saver = listSaver(
            save = { it.map { it }.toList() },
            restore = { mutableStateListOf(*it.toTypedArray()) }
        )
    ) {
        mutableStateListOf(*steps.map { false }.toTypedArray())
    }

    fun markAsDone(step: OnboardingStepType) {
        doneStatus[steps.indexOf(step)] = true
    }

    var currentStep by remember { mutableStateOf(steps.first()) }

    val isLoggedIn by Scrobblables.current.map { it != null }.collectAsState(false)
    val appListWasRun by PlatformStuff.mainPrefs.data.map { it.appListWasRun }.collectAsState(false)

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

    // change sometimes does not trigger this unless using .toList(), compose bug?
    LaunchedEffect(doneStatus.toList()) {
        if (doneStatus.all { it }) {
            onDone()
        } else {
            currentStep = steps.firstOrNull { !doneStatus[steps.indexOf(it)] } ?: steps.last()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val privacyPolicyLink = stringResource(R.string.privacy_policy_link)

        TextButton(
            onClick = {
                onNavigate(PanoRoute.WebView(privacyPolicyLink))
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp)
                .alpha(0.75f)
        ) {
            Text(text = stringResource(R.string.pref_privacy_policy))
        }

        steps.indices.forEach { i ->

            val step = steps[i]
            val isDone = doneStatus[i]

            when (step) {
                OnboardingStepType.LOGIN -> {
                    VerticalStepperItem(
                        titleRes = R.string.pref_login,
                        descriptionRes = null,
                        openAction = {},
                        isDone = isDone,
                        isExpanded = step == currentStep,
                        onSkip = null,
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
                        }
                    )
                }

                OnboardingStepType.DKMA -> {

                    VerticalStepperItem(
                        titleRes = R.string.allow_background,
                        descriptionRes = null,
                        openAction = {
                            Stuff.openInBrowser(
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
                        titleRes = R.string.pref_scrobble_from,
                        descriptionRes = R.string.choose_apps,
                        openAction = {
                            onNavigate(
                                PanoRoute.AppList(
                                    preSelectedPackages = emptyList(),
                                    hasPreSelection = false,
                                    isSingleSelect = false,
                                )
                            )
                        },
                        isDone = isDone,
                        isExpanded = step == currentStep,
                    )
                }

                OnboardingStepType.SEND_NOTIFICATIONS -> {
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