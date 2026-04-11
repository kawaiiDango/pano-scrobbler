package com.arn.scrobble.automation

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.service.notification.NotificationListenerService
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.media.NLService
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking


class AutomationProvider : ContentProvider() {

    // this does not get triggered on app start on some chinese OEMSs
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
    ): Cursor {
        val callingPackage = callingPackage ?: return createCursor(false)
        val command = uri.pathSegments?.first() ?: return createCursor(false)
        val arg = uri.pathSegments.getOrNull(1)

        if (command == Automation.ANDROID_NOW_PLAYING && callingPackage == BuildKonfig.APP_ID) {
            return nowPlayingDataCursor()
        } else if (command == Automation.ANDROID_REQUEST_REBIND && callingPackage == BuildKonfig.APP_ID) {
            return requestRebind()
        }

        val wasSuccessful = Automation.executeAction(
            command,
            arg,
            callingPackage
        )

        if (wasSuccessful) {
            runBlocking(Dispatchers.Main) {
                context?.toast(command)
            }
        }

        return createCursor(wasSuccessful)
    }

    private fun createCursor(successful: Boolean): Cursor {
        val cursor = MatrixCursor(arrayOf("result"))
        cursor.addRow(
            arrayOf(
                if (successful)
                    "Ok"
                else
                    "Failed"
            )
        )

        return cursor
    }

    private fun nowPlayingDataCursor(): Cursor {
        val data = PanoNotifications.getNowPlayingFromBackgroundProcess()
        val cursor = MatrixCursor(
            arrayOf(
                PlayingTrackNotifyEvent.TrackPlaying::origScrobbleData.name,
                PlayingTrackNotifyEvent.TrackPlaying::hash.name,
            )
        )

        if (data != null) {
            cursor.addRow(
                arrayOf(
                    Stuff.myJson.encodeToString(data.first),
                    data.second.toString(),
                )
            )
        }

        return cursor
    }

    override fun update(
        p0: Uri,
        p1: ContentValues?,
        p2: String?,
        p3: Array<out String?>?
    ): Int = 0

    // triggering a content provider query has more chances to start the content provider's process
    // than directly calling requestRebind from main process on some Chinese OEMSs
    private fun requestRebind(): Cursor {
        val res = try {
            NotificationListenerService.requestRebind(
                ComponentName(BuildKonfig.APP_ID, NLService::class.java.name)
            )
            true
        } catch (e: Exception) {
            Logger.w(e) { "requestRebind failed" }
            false
        }

        return createCursor(res)
    }
}