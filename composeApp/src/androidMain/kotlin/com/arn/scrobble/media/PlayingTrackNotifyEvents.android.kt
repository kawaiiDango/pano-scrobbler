package com.arn.scrobble.media

import android.content.Intent
import androidx.core.net.toUri
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff


actual fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent) {
    if (!AndroidStuff.isMainProcess) {
        if (globalTrackEventFlow.subscriptionCount.value > 0) {
            globalTrackEventFlow.tryEmit(event)
        }
        // else scrobbler service is not running, do nothing
    } else {
        val context = AndroidStuff.applicationContext
        val intent = Intent(PlayingTrackEventReceiver.BROADCAST_PLAYING_TRACK_EVENT)
            .setPackage(context.packageName)
            .putExtra(
                PlayingTrackEventReceiver.EXTRA_EVENT,
                Stuff.myJson.encodeToString(event)
            ).putExtra(
                PlayingTrackEventReceiver.EXTRA_EVENT_TYPE,
                event::class.simpleName
            )
        context.sendBroadcast(intent)
    }
}

actual fun getNowPlayingFromMainProcess(): Pair<ScrobbleData, Int>? {
    if (!AndroidStuff.isMainProcess)
        return null

    val cr = AndroidStuff.applicationContext.contentResolver

    if (cr == null)
        return null

    val cursor = cr.query(
        "content://${Automation.PREFIX}/${Automation.ANDROID_NOW_PLAYING}".toUri(),
        null,
        null,
        null,
        null
    ) ?: return null

    while (cursor.moveToNext()) {
        val sdColIdx =
            cursor.getColumnIndex(PlayingTrackNotifyEvent.TrackPlaying::origScrobbleData.name)
        val hashColIdx = cursor.getColumnIndex(PlayingTrackNotifyEvent.TrackPlaying::hash.name)

        if (sdColIdx != -1 && hashColIdx != -1) {
            val sd = cursor.getString(sdColIdx)
            val hash = cursor.getString(hashColIdx)
            if (sd != null && hash != null) {
                cursor.close()
                return Stuff.myJson.decodeFromString<ScrobbleData>(sd) to hash.toInt()
            }
        }
    }

    cursor.close()
    return null
}

actual suspend fun shouldFetchNpArtUrl(): Boolean {
    return false
}