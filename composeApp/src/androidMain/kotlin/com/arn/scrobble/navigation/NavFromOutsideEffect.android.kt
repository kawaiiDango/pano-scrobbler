package com.arn.scrobble.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer

@Composable
actual fun NavFromOutsideEffect(
    onNavigate: (PanoRoute) -> Unit,
    isAndroidDialogActivity: Boolean,
) {
    val activity = LocalActivity.current as? ComponentActivity
    val activityRestored = LocalActivityRestoredFlag.current

    DisposableEffect(Unit) {
        val consumer = Consumer<Intent> {
            val route = if (isAndroidDialogActivity)
                DeepLinkUtils.parseDialogDeepLink(it)
            else
                DeepLinkUtils.parseDeepLink(it)

            if (route != null)
                onNavigate(route)
        }

        activity?.intent?.let {
            if (!activityRestored)
                consumer.accept(it)
        }

        activity?.addOnNewIntentListener(consumer)

        onDispose {
            activity?.removeOnNewIntentListener(consumer)
        }
    }
}