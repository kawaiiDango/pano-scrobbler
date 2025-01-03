package com.arn.scrobble.navigation

import androidx.navigation.NavGraphBuilder
import com.arn.scrobble.main.MainViewModel

expect fun NavGraphBuilder.panoPlatformSpecificNavGraph(
    onSetTitle: (String?) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goUp: () -> Unit,
    mainViewModel: MainViewModel,
)