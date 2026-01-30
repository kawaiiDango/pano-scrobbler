package com.arn.scrobble.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.arn.scrobble.pref.AppItem

@Composable
actual fun getActivityOrNull(): Any? {
    return null
}

@Composable
actual fun AppIcon(
    appItem: AppItem?,
    modifier: Modifier,
) {
    val name = appItem?.friendlyLabel?.ifEmpty { "*" } ?: "*"

    AvatarOrInitials(
        avatarUrl = null,
        avatarName = name,
        textStyle = MaterialTheme.typography.titleSmall,
        modifier = modifier.clip(CircleShape),
    )
}

actual fun Modifier.testTagsAsResId() = this

@Composable
actual fun isImeVisible() = false