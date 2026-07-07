package com.arn.scrobble.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val initials = remember(name) {
        // if this is like a package name, use the last part
        if (' ' !in name && '.' in name)
            name.substringAfterLast('.')
                .take(1)
                .uppercase()
                .takeIf { it.isNotEmpty() }
        else
            null
    }
    AvatarOrInitials(
        avatarUrl = null,
        avatarName = name,
        initials = initials,
        textStyle = MaterialTheme.typography.titleSmall,
        modifier = modifier.clip(CircleShape),
    )
}

actual fun Modifier.testTagsAsResId() = this

@Composable
actual fun isImeVisible() = false