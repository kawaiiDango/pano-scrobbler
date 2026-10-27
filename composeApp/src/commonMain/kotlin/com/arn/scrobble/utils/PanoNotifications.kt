package com.arn.scrobble.utils

import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.updates.UpdateAction
import kotlinx.coroutines.flow.StateFlow

expect object PanoNotifications {
    val forcePersistentNoti: Boolean
    val playingTrackStates: StateFlow<Map<String, PlayingTrackNotifyEvent.PlayingTrackState>>
    suspend fun notifyScrobble(event: PlayingTrackNotifyEvent.TrackPlaying)

    suspend fun notifyError(event: PlayingTrackNotifyEvent.Error)

    suspend fun notifyAppDetected(appId: String, appLabel: String)

    suspend fun notifyUnscrobbled(notiKey: String, scrobbleData: ScrobbleData, hash: Int)

    suspend fun notifyDigest(lastfmPeriod: LastfmPeriod, title: String, text: String)

    suspend fun notifyUpdater(updateAction: UpdateAction)

    fun repostFgNotiIfNeeded()

    fun isNotiChannelEnabled(channelId: String): Boolean

    fun removeNotificationByKey(key: String)
}