package com.arn.scrobble.pref

import com.arn.scrobble.utils.AndroidStuff

actual fun filterAppList(
    packageNames: Set<String>,
    seenAppsMap: Map<String, String>,
): List<String> {
    return packageNames.filter { AndroidStuff.isPackageInstalled(it) }
}