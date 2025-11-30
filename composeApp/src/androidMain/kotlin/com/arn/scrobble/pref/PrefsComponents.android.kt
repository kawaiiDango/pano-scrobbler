package com.arn.scrobble.pref

import androidx.compose.runtime.Composable
import com.arn.scrobble.themes.AppPreviewTheme
import com.arn.scrobble.utils.AndroidStuff

actual fun filterAppList(
    packageNames: Set<String>,
    seenAppsMap: Map<String, String>,
): List<String> {
    return packageNames.filter { AndroidStuff.isPackageInstalled(it) }
}