package com.arn.scrobble.pref

import com.arn.scrobble.utils.Stuff
import kotlinx.serialization.Serializable

@Serializable
data class AppItem(
    val appId: String,
    val label: String,
) {
    val friendlyLabel: String
        get() = when (appId) {
            Stuff.PACKAGE_PIXEL_NP,
            Stuff.PACKAGE_PIXEL_NP_R,
            Stuff.PACKAGE_PIXEL_NP_AMM -> "Pixel Now Playing"

            Stuff.PACKAGE_FIREFOX_WIN -> "Mozilla Firefox"

            Stuff.PACKAGE_KDE_CONNECT_LINUX -> "KDE Connect"

            else -> label
                .ifEmpty {
                    if (appId.endsWith(EXE_SUFFIX))
                        appId.dropLast(EXE_SUFFIX.length)
                    else if (appId.startsWith(MPSRIS_PREFIX))
                        appId.substring(MPSRIS_PREFIX.length)
                    else
                        appId
                }
        }

    companion object {
        private const val EXE_SUFFIX = ".exe"
        private const val MPSRIS_PREFIX = "org.mpris.MediaPlayer2."
    }
}

data class AppList(
    val musicPlayers: List<AppItem> = emptyList(),
    val otherApps: List<AppItem> = emptyList()
)