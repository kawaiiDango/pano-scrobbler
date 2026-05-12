package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.navigation.enumSaver
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.add_to_app_launcher
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.run_on_start


@Composable
actual fun OnboardingScreen(
    onNavigate: (PanoRoute) -> Unit,
    onDone: () -> Unit,
    mainViewModel: MainViewModel,
    modifier: Modifier,
) {
    val isLoggedIn by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.scrobbleAccounts.isNotEmpty() }

    val steps = rememberSaveable {
        listOfNotNull(
            OnboardingStepType.LOGIN,
            if (DesktopStuff.os == DesktopStuff.Os.Linux)
                OnboardingStepType.AUTOSTART
            else null,
            if (DesktopStuff.os == DesktopStuff.Os.Linux && System.getenv("APPIMAGE") != null)
                OnboardingStepType.APP_LAUNCHER
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

    LaunchedEffect(doneStatus.toList()) {
        if (doneStatus.all { it }) {
            mainViewModel.updateScrobblerServiceState(true)
            onDone()
        } else {
            currentStep = steps.firstOrNull { !doneStatus[steps.indexOf(it)] } ?: steps.last()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            markAsDone(OnboardingStepType.LOGIN)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        OnboardingTopRow(
            onNavigate = onNavigate,
            showProxySettings = true,
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

                OnboardingStepType.AUTOSTART -> {
                    VerticalStepperItem(
                        titleRes = Res.string.run_on_start,
                        description = null,
                        openAction = {
                            PanoNativeComponents.autoStartLinux(true)
                            markAsDone(OnboardingStepType.AUTOSTART)
                        },
                        isDone = isDone,
                        onSkip = {
                            markAsDone(OnboardingStepType.AUTOSTART)
                        },
                        isExpanded = step == currentStep
                    )
                }

                OnboardingStepType.APP_LAUNCHER -> {
                    VerticalStepperItem(
                        titleRes = Res.string.add_to_app_launcher,
                        description = null,
                        openAction = {
                            DesktopStuff.addAppImageToAppLauncher()
                            markAsDone(OnboardingStepType.APP_LAUNCHER)
                        },
                        isDone = isDone,
                        onSkip = {
                            markAsDone(OnboardingStepType.APP_LAUNCHER)
                        },
                        isExpanded = step == currentStep
                    )
                }

                else -> {}
            }
        }
    }
}