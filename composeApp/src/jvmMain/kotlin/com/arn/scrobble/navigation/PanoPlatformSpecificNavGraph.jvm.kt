package com.arn.scrobble.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

actual fun EntryProviderScope<PanoRoute>.panoPlatformSpecificNavGraph(
    onSetTitle: (PanoRoute, String) -> Unit,
    navigate: (PanoRoute) -> Unit,
    goBack: () -> Unit,
) {
    @Composable
    fun onSetTitleRes(route: PanoRoute, resId: StringResource) {
        val title = stringResource(resId)

        LaunchedEffect(resId) {
            onSetTitle(route, title)
        }
    }

}