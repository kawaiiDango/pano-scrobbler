package com.arn.scrobble.media

import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.BlockPlayerAction
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class ScrobbleError(
    val title: String,
    val description: String?,
    val appId: String,
    val canFixMetadata: Boolean,
)

@Serializable
sealed interface PlayingTrackNotifyEvent {
    interface PlayingTrackState {
        val scrobbleData: ScrobbleData
    }

    @Serializable
    data class Error(
        val hash: Int,
        override val scrobbleData: ScrobbleData,
        val scrobbleError: ScrobbleError,
    ) : PlayingTrackNotifyEvent, PlayingTrackState

    @Serializable
    data class TrackScrobbling(
        override val scrobbleData: ScrobbleData,
        val origScrobbleData: ScrobbleData,
        val hash: Int,
        val nowPlaying: Boolean,
        val userLoved: Boolean,
        val userPlayCount: Int,
    ) : PlayingTrackNotifyEvent, PlayingTrackState

    @Serializable
    data class TrackCancelled(
        val hash: Int?,
        val showUnscrobbledNotification: Boolean,
        val blockPlayerAction: BlockPlayerAction = BlockPlayerAction.ignore,
    ) : PlayingTrackNotifyEvent

    @Serializable
    data class TrackScrobbleLocked(
        val hash: Int,
        val locked: Boolean,
    ) : PlayingTrackNotifyEvent

    @Serializable
    data class TrackLovedUnloved(
        val hash: Int?,
        val loved: Boolean,
    ) : PlayingTrackNotifyEvent

    @Serializable
    data class AppAllowedBlocked(
        val appId: String,
        val allowed: Boolean,
    ) : PlayingTrackNotifyEvent
}

val globalTrackEventFlow by lazy { MutableSharedFlow<PlayingTrackNotifyEvent>(extraBufferCapacity = 10) }

suspend fun listenForPlayingTrackEvents(
    scrobbleQueue: ScrobbleQueue,
    mediaListener: MediaListener,
) {
    if (globalTrackEventFlow.subscriptionCount.value > 0) {
        return
    }

    globalTrackEventFlow.collect { event ->
        when (event) {
            is PlayingTrackNotifyEvent.Error -> {
                PanoNotifications.notifyError(event)

                if (event.scrobbleError.canFixMetadata) {
                    scrobbleQueue.remove(event.hash)
                    mediaListener.findTrackInfoByHash(event.hash)?.markAsScrobbled()
                }
            }

            is PlayingTrackNotifyEvent.TrackScrobbling -> {
                if (!event.nowPlaying) {
                    val trackInfo = mediaListener.findTrackInfoByHash(event.hash)
                    if (trackInfo != null && trackInfo.userPlayCount > 0)
                        trackInfo.updateUserProps(userPlayCount = trackInfo.userPlayCount + 1)
                    trackInfo?.markAsScrobbled()
                }

                PanoNotifications.notifyScrobble(event)
            }

            is PlayingTrackNotifyEvent.TrackCancelled -> {
                val trackInfo = if (event.hash == null) {
                    mediaListener.findPlayingTrackInfo()
                } else {
                    mediaListener.findTrackInfoByHash(event.hash)
                }

                trackInfo ?: return@collect

                val hash = trackInfo.hash

                if (event.blockPlayerAction == BlockPlayerAction.skip) {
                    mediaListener.skip(hash)
                } else if (event.blockPlayerAction == BlockPlayerAction.mute) {
                    mediaListener.mute(hash)
                }

                if (!scrobbleQueue.has(hash)) {
                    return@collect
                } else {
                    trackInfo.markAsScrobbled()
                    scrobbleQueue.remove(hash)
                }


                if (event.showUnscrobbledNotification && event.hash != null) {
                    PanoNotifications.notifyUnscrobbled(
                        trackInfo.toScrobbleData(true),
                        event.hash
                    )
                } else {
                    PanoNotifications.removeNotificationByTag(trackInfo.appId)
                }
            }

            is PlayingTrackNotifyEvent.TrackScrobbleLocked -> {
                if (event.hash != -1) {
                    if (event.locked) {
                        scrobbleQueue.setLockedHash(event.hash)
                    } else {
                        scrobbleQueue.setLockedHash(null)
                    }
                }
            }

            is PlayingTrackNotifyEvent.TrackLovedUnloved -> {
                val trackInfo = if (event.hash == null) {
                    mediaListener.findPlayingTrackInfo()
                } else {
                    mediaListener.findTrackInfoByHash(event.hash)
                }

                trackInfo ?: return@collect

                if (trackInfo.artist.isEmpty() || trackInfo.title.isEmpty())
                    return@collect

                val scrobbleData = trackInfo.toScrobbleData(false)
                val origScrobbleData = trackInfo.toScrobbleData(true)

                mediaListener.scope.launch(Dispatchers.IO) {
                    ScrobbleEverywhere.loveOrUnlove(scrobbleData.toTrack(), event.loved)
                }

                trackInfo.updateUserProps(userLoved = event.loved)

                val scrobbleEvent = PlayingTrackNotifyEvent.TrackScrobbling(
                    scrobbleData = scrobbleData,
                    origScrobbleData = origScrobbleData,
                    hash = trackInfo.hash,
                    nowPlaying = scrobbleQueue.has(trackInfo.hash),
                    userLoved = event.loved,
                    userPlayCount = trackInfo.userPlayCount,
                )

                PanoNotifications.notifyScrobble(scrobbleEvent)

                val linkHeartButtonToRating =
                    PlatformStuff.mainPrefs.data.map { it.linkHeartButtonToRating }.first()

                if (linkHeartButtonToRating && PlatformStuff.billingRepository.isLicenseValid) {
                    if (event.loved)
                        mediaListener.love(trackInfo.hash)
                    else
                        mediaListener.unlove(trackInfo.hash)
                }
            }

            is PlayingTrackNotifyEvent.AppAllowedBlocked -> {
                PlatformStuff.mainPrefs.updateData {
                    it.allowOrBlockAppCopied(event.appId, event.allowed)
                }

                PanoNotifications.removeNotificationByTag(Stuff.CHANNEL_NOTI_NEW_APP)
            }
        }
    }

}

expect fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent)