package com.arn.scrobble.utils

import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.media.PlayingTrackNotifyEvent

expect object PanoNotifications {
    fun notifyScrobble(event: PlayingTrackNotifyEvent.TrackScrobbling)

    fun notifyError(event: PlayingTrackNotifyEvent.Error)

    fun notifyAppDetected(appId: String, appLabel: String)

    fun notifyUnscrobbled(scrobbleData: ScrobbleData, hash: Int)

    suspend fun notifyDigest(timePeriod: TimePeriod, resultsList: List<Pair<Int, String>>)

    fun removeNotificationByTag(tag: String)
}