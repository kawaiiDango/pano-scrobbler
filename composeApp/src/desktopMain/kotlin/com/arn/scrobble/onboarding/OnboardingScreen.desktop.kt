package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.pref_login


@Composable
actual fun OnboardingScreen(
    onNavigate: (PanoRoute) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    val isLoggedIn by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.scrobbleAccounts.isNotEmpty() }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onDone()
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

        VerticalStepperItem(
            titleRes = Res.string.pref_login,
            description = null,
            openAction = {},
            isDone = isLoggedIn,
            isExpanded = true,
        ) {
            ButtonStepperForLogin(navigate = onNavigate)
        }
    }
}