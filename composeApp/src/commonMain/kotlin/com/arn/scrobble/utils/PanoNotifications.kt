package com.arn.scrobble.utils

import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.updates.UpdateAction

expect object PanoNotifications {
    suspend fun notifyScrobble(event: PlayingTrackNotifyEvent.TrackPlaying)

    suspend fun notifyError(event: PlayingTrackNotifyEvent.Error)

    suspend fun notifyAppDetected(appId: String, appLabel: String)

    suspend fun notifyUnscrobbled(scrobbleData: ScrobbleData, hash: Int)

    suspend fun notifyDigest(timePeriod: TimePeriod, resultsList: List<Pair<Int, String>>)

    suspend fun notifyUpdater(updateAction: UpdateAction)

    fun removeNotificationByTag(tag: String)
}