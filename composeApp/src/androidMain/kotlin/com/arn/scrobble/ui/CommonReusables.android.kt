package com.arn.scrobble.ui

import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImage
import com.arn.scrobble.pref.AppItem

@Composable
actual fun getActivityOrNull(): Any? {
    return LocalActivity.current
}

@Composable
actual fun ApplyWindowBlur(behind: Int, bg: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val dialogWindowProvider = LocalView.current.parent
    val density = LocalDensity.current

    SideEffect {
        val window = (dialogWindowProvider as? DialogWindowProvider)?.window ?: return@SideEffect

        if (behind > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val attributes = window.attributes
            attributes.blurBehindRadius = with(density) { behind.dp.roundToPx() }
            attributes.dimAmount = 0.01f  // near-zero, but satisfies the compositor
            window.attributes = attributes
        }

        if (bg > 0)
            window.setBackgroundBlurRadius(with(density) { bg.dp.roundToPx() })
    }
}

@Composable
actual fun isImeVisible(): Boolean {
    return WindowInsets.isImeVisible
}

@Composable
actual fun AppIcon(
    appItem: AppItem?,
    modifier: Modifier,
) {
    AsyncImage(
        model = appItem?.appId?.let { PackageName(it) },
        placeholder = placeholderPainter(),
        contentDescription = appItem?.friendlyLabel,
        modifier = modifier
    )
}

actual fun Modifier.testTagsAsResId() = semantics {
    testTagsAsResourceId = true
}