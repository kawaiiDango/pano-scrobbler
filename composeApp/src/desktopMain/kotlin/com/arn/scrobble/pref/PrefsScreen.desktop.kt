package com.arn.scrobble.pref

import androidx.compose.foundation.lazy.LazyListScope
import com.arn.scrobble.utils.DesktopStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.run_on_start

actual fun prefQuickSettings(listScope: LazyListScope, scrobblerEnabled: Boolean) {
    // no-op
}

actual fun prefCrashReporter(listScope: LazyListScope, crashReporterEnabled: Boolean) {
    // no-op
}

actual fun prefChartsWidget(listScope: LazyListScope) {
    // no-op
}

actual fun prefNotifications(listScope: LazyListScope) {
    // no-op
}

actual fun prefIntents(listScope: LazyListScope) {
    // no-op
}

actual fun addToStartup(
    listScope: LazyListScope,
    isAdded: Boolean,
    onAddedChanged: (Boolean) -> Unit,
) {
    listScope.item("startup") {
        SwitchPref(
            text = stringResource(Res.string.run_on_start),
            value = isAdded,
            copyToSave = {
                DesktopStuff.addOrRemoveFromStartup(it)
                onAddedChanged(it)
                this
            }
        )
    }
}

actual suspend fun isAddedToStartup() = DesktopStuff.isAddedToStartup()