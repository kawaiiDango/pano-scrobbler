package com.arn.scrobble.media

import android.content.Intent
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.Stuff


actual fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent) {
    if (!AndroidStuff.isMainProcess) {
        if (globalTrackEventFlow.subscriptionCount.value > 0) {
            globalTrackEventFlow.tryEmit(event)
        }
        // else scrobbler service is not running, do nothing
    } else {
        val context = AndroidStuff.application
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