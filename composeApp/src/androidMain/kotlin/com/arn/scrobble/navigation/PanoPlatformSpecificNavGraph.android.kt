package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.pref.AutomationScreen
import com.arn.scrobble.ui.panoContentPadding
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.pref_automation

actual fun NavGraphBuilder.panoPlatformSpecificNavGraph(
    onSetTitle: (String, String?) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
) {

    @Composable
    fun onSetTitleString(destId: String, title: String) {
        DisposableEffect(title) {
            onSetTitle(destId, title)

            onDispose {
                onSetTitle(destId, null)
            }
        }
    }

    @Composable
    fun modifier() = Modifier
        .fillMaxSize()

    composable<PanoRoute.Automation> {
        onSetTitleString(it.id, stringResource(Res.string.pref_automation))

        val arguments = it.toRoute<PanoRoute.Automation>()

        AutomationScreen(
            allowedPackages = arguments.allowedPackages,
            onNavigate = navigate,
            modifier = modifier().padding(panoContentPadding())
        )
    }
}