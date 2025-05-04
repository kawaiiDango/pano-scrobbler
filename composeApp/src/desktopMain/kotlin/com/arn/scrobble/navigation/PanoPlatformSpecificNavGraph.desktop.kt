package com.arn.scrobble.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.arn.scrobble.main.MainViewModel
import kotlin.system.exitProcess

actual fun NavGraphBuilder.panoPlatformSpecificNavGraph(
    onSetTitle: (String?) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
) {
    dialog<PanoRoute.Exit> {
        LaunchedEffect(Unit) {
            exitProcess(0)
        }
    }
}