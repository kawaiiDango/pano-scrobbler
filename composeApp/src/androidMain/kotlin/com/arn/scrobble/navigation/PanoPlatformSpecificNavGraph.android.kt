package com.arn.scrobble.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.onboarding.FixItDialog

actual fun EntryProviderScope<PanoRoute>.panoPlatformSpecificNavGraph(
    onSetTitle: (PanoRoute, String) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goBack: () -> Unit,
    mainViewModel: MainViewModel,
) {
    modalEntry<PanoRoute.Modal.FixIt> {
        FixItDialog(
            modifier = modalModifier(),
        )
    }
}