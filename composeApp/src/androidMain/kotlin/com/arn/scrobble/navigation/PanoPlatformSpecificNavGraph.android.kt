package com.arn.scrobble.navigation

import androidx.navigation.NavGraphBuilder
import com.arn.scrobble.main.MainViewModel

actual fun NavGraphBuilder.panoPlatformSpecificNavGraph(
    onSetTitle: (String, String?) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
) {
}