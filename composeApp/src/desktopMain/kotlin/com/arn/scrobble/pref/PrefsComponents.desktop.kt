package com.arn.scrobble.pref

actual fun filterAppList(
    packageNames: Set<String>,
    seenAppsMap: Map<String, String>,
): List<String> {
    return packageNames.filter { seenAppsMap.containsKey(it) }
}