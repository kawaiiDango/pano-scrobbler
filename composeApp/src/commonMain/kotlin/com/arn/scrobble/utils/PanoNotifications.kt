package com.arn.scrobble.utils

import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.media.PlayingTrackInfo
import com.arn.scrobble.media.ScrobbleError

expect object PanoNotifications {
    fun notifyScrobble(trackInfo: PlayingTrackInfo, nowPlaying: Boolean)

    fun notifyBadMeta(trackInfo: PlayingTrackInfo, scrobbleError: ScrobbleError)

    fun notifyOtherError(trackInfo: PlayingTrackInfo, scrobbleError: ScrobbleError)

    fun notifyAppDetected(appId: String, appLabel: String)

    fun notifyUnscrobbled(trackInfo: PlayingTrackInfo)

    suspend fun notifyDigest(timePeriod: TimePeriod, resultsList: List<Pair<Int, String>>)

    fun removeNotificationByTag(tag: String)
}