package com.arn.scrobble.automation

import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.VariantStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object Automation {
    const val ENABLE = "scrobbler-on"
    const val DISABLE = "scrobbler-off"
    const val LOVE = "love"
    const val UNLOVE = "unlove"
    const val CANCEL = "cancel"
    const val ALLOWLIST = "allowlist"
    const val BLOCKLIST = "blocklist"

    const val DESKTOP_FOCUS_EXISTING = "focus-existing"
    const val ANDROID_NOW_PLAYING = "now-playing"

    const val PREFIX = "com.arn.scrobble.automation"

    fun executeAction(
        command: String,
        arg: String?,
        callingPackage: String?
    ): Boolean {
        if (!VariantStuff.billingRepository.isLicenseValid) {
            return false
        }

        val mainPrefs = PlatformStuff.mainPrefs

        if (!PlatformStuff.isDesktop && callingPackage !in runBlocking {
                mainPrefs.data.map { it.allowedAutomationPackages }.first()
            }) {
            return false
        }

        when (command) {
            ENABLE, DISABLE -> {
                runBlocking {
                    mainPrefs.updateData { it.copy(scrobblerEnabled = (command == ENABLE)) }
                }
            }

            LOVE, UNLOVE -> {
                val event = PlayingTrackNotifyEvent.TrackLovedUnloved(
                    hash = null,
                    loved = (command == LOVE)
                )

                notifyPlayingTrackEvent(event)
            }

            CANCEL -> {
                val event = PlayingTrackNotifyEvent.TrackCancelled(
                    hash = null,
                    showUnscrobbledNotification = false,
                )

                notifyPlayingTrackEvent(event)
            }

            ALLOWLIST, BLOCKLIST -> {
                if (arg != null) {
                    val appId = arg
                    val allow = (command == ALLOWLIST)
                    runBlocking {
                        mainPrefs.updateData { it.allowOrBlockAppCopied(appId, allow) }
                    }
                } else {
                    return false
                }
            }

            else -> {
                return false
            }
        }

        return true
    }
}