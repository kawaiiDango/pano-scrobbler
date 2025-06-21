package com.arn.scrobble.media

actual fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent) {
    if (globalTrackEventFlow.subscriptionCount.value > 0) {
        globalTrackEventFlow.tryEmit(event)
    }
}