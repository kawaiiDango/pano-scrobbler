package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.mic.MicScrobbleScreen
import com.arn.scrobble.ui.addColumnPadding
import com.arn.scrobble.utils.Stuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.scrobble_from_mic

actual fun NavGraphBuilder.panoPlatformSpecificNavGraph(
    onSetTitle: (String?) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
) {

    @Composable
    fun modifier() = Modifier
        .fillMaxSize()
//        .background(MaterialTheme.colorScheme.background)

    composable<PanoRoute.MicScrobble>(
        deepLinks = listOf(
            navDeepLink {
                uriPattern =
                    Stuff.DEEPLINK_BASE_PATH + "/" + PanoRoute.MicScrobble::class.simpleName
            },
        )
    ) {
        onSetTitle(stringResource(Res.string.scrobble_from_mic))

        MicScrobbleScreen(
            modifier = modifier().addColumnPadding()
        )
    }

}