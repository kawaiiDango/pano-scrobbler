package com.arn.scrobble.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        contentDescription = appItem?.label,
        modifier = modifier
    )
}