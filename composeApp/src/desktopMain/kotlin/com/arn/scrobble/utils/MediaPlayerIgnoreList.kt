package com.arn.scrobble.utils

// Media player prefixes to ignore by default (e.g. spammy apps)
private val ignoredPlayerPrefixes = listOf("org.mpris.MediaPlayer2.kdeconnect")

/**
 * Checks if a media player app ID should be ignored.
 * Handles both full app IDs and normalized IDs.
 */
fun isAppIgnored(appId: String): Boolean {
    val normalizedId = if (appId.startsWith("org.mpris.MediaPlayer2.")) {
        appId.substring("org.mpris.MediaPlayer2.".length)
    } else {
        appId
    }
    return ignoredPlayerPrefixes.any { prefix ->
        appId.startsWith(prefix) || normalizedId.startsWith(prefix.substring("org.mpris.MediaPlayer2.".length))
    }
}
