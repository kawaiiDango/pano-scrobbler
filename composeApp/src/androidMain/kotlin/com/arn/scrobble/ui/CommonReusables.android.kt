package com.arn.scrobble.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import coil3.compose.AsyncImage
import com.arn.scrobble.pref.AppItem

@Composable
actual fun getActivityOrNull(): Any? {
    return LocalActivity.current
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