package com.arn.scrobble.providers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking


class AutomationProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    override fun delete(
        p0: Uri,
        p1: String?,
        p2: Array<out String?>?
    ): Int = 0

    override fun getType(p0: Uri): String? = null

    override fun insert(p0: Uri, p1: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        sortOrder: String?
    ): Cursor? {
        val cursor = MatrixCursor(arrayOf("result"))

        if (!PlatformStuff.billingRepository.isLicenseValid) {
            cursor.addRow(arrayOf("License is invalid"))
            return cursor
        }

        val mainPrefs = PlatformStuff.mainPrefs
        val allowList = runBlocking {
            mainPrefs.data.map { it.allowedAutomationPackages }.first()
        }

        if (callingPackage !in allowList) {
            cursor.addRow(arrayOf("Not in allowlist"))
            return cursor
        }

        when (val command = uri.pathSegments?.first()) {
            ENABLE, DISABLE -> {
                runBlocking {
                    mainPrefs.updateData { it.copy(scrobblerEnabled = command == ENABLE) }
                }
            }

            LOVE, UNLOVE -> {
                val event = PlayingTrackNotifyEvent.TrackLovedUnloved(
                    hash = null,
                    loved = command == LOVE
                )

                notifyPlayingTrackEvent(event)
            }

            CANCEL -> {
                val event = PlayingTrackNotifyEvent.TrackCancelled(
                    hash = null,
                    showUnscrobbledNotification = false,
                    markAsScrobbled = true
                )

                notifyPlayingTrackEvent(event)
            }

            ALLOWLIST, BLOCKLIST -> {
                if (uri.pathSegments.getOrNull(1) != null) {
                    val appId = uri.pathSegments[1]
                    val allow = command == ALLOWLIST
                    runBlocking {
                        mainPrefs.updateData { it.allowOrBlockAppCopied(appId, allow) }
                    }
                } else {
                    cursor.addRow(arrayOf("Package name is missing"))
                    return cursor
                }
            }

            else -> {
                cursor.addRow(arrayOf("Unknown command"))
                return cursor
            }
        }

        cursor.addRow(arrayOf("Ok"))
        return cursor
    }

    override fun update(
        p0: Uri,
        p1: ContentValues?,
        p2: String?,
        p3: Array<out String?>?
    ): Int = 0

    companion object {
        val ENABLE = "enable"
        val DISABLE = "disable"
        val LOVE = "love"
        val UNLOVE = "unlove"
        val CANCEL = "cancel"
        val ALLOWLIST = "allowlist"
        val BLOCKLIST = "blocklist"
    }
}