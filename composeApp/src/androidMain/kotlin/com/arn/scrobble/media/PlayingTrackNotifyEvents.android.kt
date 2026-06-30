package com.arn.scrobble.media

import android.os.CancellationSignal
import android.os.OperationCanceledException
import androidx.core.net.toUri
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


actual fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent) {
    if (!AndroidStuff.isMainProcess) {
        if (globalTrackEventFlow.subscriptionCount.value > 0) {
            globalTrackEventFlow.tryEmit(event)
        }
        // else scrobbler service is not running, do nothing
    } else {
        val context = AndroidStuff.applicationContext

        val intent = PlayingTrackEventReceiver.createIntent(context, event)
        context.sendBroadcast(intent)
    }
}

actual fun getNowPlayingFromMainProcess(): PlayingTrackNotifyEvent.TrackPlaying? {
    if (!AndroidStuff.isMainProcess)
        return null

    val cr = AndroidStuff.applicationContext.contentResolver ?: return null
    val cancellationSignal = CancellationSignal()

    // wait for 500ms max to prevent ANR
    val cancelJob = Stuff.appScope.launch {
        delay(500.milliseconds)
        cancellationSignal.cancel()
    }

    val cursor = try {
        cr.query(
            "content://${Automation.PREFIX}/${Automation.ANDROID_NOW_PLAYING}".toUri(),
            null,
            null,
            null,
            null,
            cancellationSignal
        ).also {
            cancelJob.cancel()
        }
    } catch (e: OperationCanceledException) {
        null
    } ?: return null

    while (cursor.moveToNext()) {
        val trackPlayingIdx = cursor.getColumnIndex("result")

        if (trackPlayingIdx != -1) {
            val event = cursor.getString(trackPlayingIdx)
            if (event != null) {
                cursor.close()
                return Stuff.myJson.decodeFromString<PlayingTrackNotifyEvent.TrackPlaying>(event)
            }
        }
    }

    cursor.close()
    return null
}

actual fun shouldFetchNpArtUrl(): Flow<Boolean> {
    return emptyFlow()
}