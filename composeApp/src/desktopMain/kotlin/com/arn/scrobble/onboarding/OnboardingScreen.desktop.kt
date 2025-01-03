package com.arn.scrobble.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.pref_login
import pano_scrobbler.composeapp.generated.resources.pref_privacy_policy
import pano_scrobbler.composeapp.generated.resources.privacy_policy_link


@Composable
actual fun OnboardingScreen(
    onNavigate: (PanoRoute) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    val isLoggedIn by Scrobblables.current.map { it != null }.collectAsStateWithLifecycle(false)

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onDone()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val privacyPolicyLink = stringResource(Res.string.privacy_policy_link)

        TextButton(
            onClick = {
                PlatformStuff.openInBrowser(privacyPolicyLink)
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp)
                .alpha(0.75f)
        ) {
            Text(text = stringResource(Res.string.pref_privacy_policy))
        }

        VerticalStepperItem(
            titleRes = Res.string.pref_login,
            descriptionRes = null,
            openAction = {},
            isDone = isLoggedIn,
            isExpanded = true,
            onSkip = null,
        ) {
            ButtonStepperForLogin(navigate = onNavigate)
        }
    }
}