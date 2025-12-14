package com.arn.scrobble.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.arn.scrobble.main.MainViewModel

expect fun EntryProviderScope<PanoRoute>.panoPlatformSpecificNavGraph(
    onSetTitle: (PanoRoute, String) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goBack: () -> Unit,
)