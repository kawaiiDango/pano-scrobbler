package com.arn.scrobble.navigation

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import com.arn.scrobble.onboarding.FixItDialog
import com.arn.scrobble.ui.navModal

actual fun EntryProviderScope<PanoRoute>.panoPlatformSpecificNavGraph(
    onSetTitle: (PanoRoute, String) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goBack: () -> Unit,
) {
    modalEntry<PanoRoute.Modal.FixIt> { route ->

        FixItDialog(
            killedReason = route.killedReason,
            onNavigate = navigate,
            modifier = Modifier.navModal(),
        )
    }
}