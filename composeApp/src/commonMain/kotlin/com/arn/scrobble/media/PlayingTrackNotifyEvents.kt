package com.arn.scrobble.media

import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.db.BlockPlayerAction
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.db.BlockedMetadataDao
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNot
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
        val notiKey: String
        val scrobbleData: ScrobbleData
    }

    @Serializable
    data class Error(
        val hash: Int,
        override val notiKey: String,
        override val scrobbleData: ScrobbleData,
        val scrobbleError: ScrobbleError,
    ) : PlayingTrackNotifyEvent, PlayingTrackState

    @Serializable
    data class TrackPlaying(
        override val notiKey: String,
        override val scrobbleData: ScrobbleData,
        val origScrobbleData: ScrobbleData,
        val hash: Int,
        val nowPlaying: Boolean,
        val userLoved: Boolean,
        val userPlayCount: Int,
        val artUrl: String?,
        val timelineStartTime: Long,
        val preprocessed: Boolean,
    ) : PlayingTrackNotifyEvent, PlayingTrackState

    @Serializable
    data class TrackCancelled(
        val hash: Int?,
        val showUnscrobbledNotification: Boolean,
        val blockedMetadata: BlockedMetadata = BlockedMetadata(blockPlayerAction = BlockPlayerAction.ignore),
    ) : PlayingTrackNotifyEvent

    @Serializable
    data class TrackScrobbleLocked(
        val hash: Int,
        val locked: Boolean,
    ) : PlayingTrackNotifyEvent

    @Serializable
    data class CurrentTrackLovedUnloved(
        val loved: Boolean,
    ) : PlayingTrackNotifyEvent

    @Serializable
    data class TrackLovedUnloved(
        override val notiKey: String,
        override val scrobbleData: ScrobbleData,
        val hash: Int,
        val loved: Boolean,
    ) : PlayingTrackNotifyEvent, PlayingTrackState

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
                    mediaListener.findTrackerByHash(event.hash)?.trackInfo?.scrobbled()
                }
            }

            is PlayingTrackNotifyEvent.TrackPlaying -> {
                val trackInfo = mediaListener.findTrackerByHash(event.hash)?.trackInfo
                if (!event.nowPlaying) {
                    if (trackInfo != null && trackInfo.userPlayCount > 0)
                        trackInfo.updateUserProps(userPlayCount = trackInfo.userPlayCount + 1)
                }

                PanoNotifications.notifyScrobble(event)
            }

            is PlayingTrackNotifyEvent.TrackCancelled -> {
                val tracker = if (event.hash == null) {
                    mediaListener.findPlayingTracker()
                } else {
                    mediaListener.findTrackerByHash(event.hash)
                }

                val trackInfo = tracker?.trackInfo
                trackInfo ?: return@collect

                val shouldCancel = if (event.hash == null)
                    BlockedMetadataDao.shouldBlock(
                        event.blockedMetadata,
                        trackInfo.toScrobbleData(false)
                    )
                else
                    true

                if (!shouldCancel) {
                    return@collect
                }

                val hash = trackInfo.hash
                val blockPlayerAction = event.blockedMetadata.blockPlayerAction

                if (blockPlayerAction == BlockPlayerAction.skip) {
                    tracker.skip()
                } else if (blockPlayerAction == BlockPlayerAction.mute) {
                    mediaListener.mute(hash)
                }

                trackInfo.cancelled()

                if (scrobbleQueue.has(hash)) {
                    scrobbleQueue.remove(hash)
                }


                if (event.showUnscrobbledNotification && event.hash != null) {
                    PanoNotifications.notifyUnscrobbled(
                        trackInfo.uniqueId,
                        trackInfo.toScrobbleData(true),
                        event.hash
                    )
                } else {
                    PanoNotifications.removeNotificationByKey(trackInfo.uniqueId)
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

            is PlayingTrackNotifyEvent.CurrentTrackLovedUnloved,
            is PlayingTrackNotifyEvent.TrackLovedUnloved -> {

                val tracker: MediaListener.SessionTracker?
                val scrobbleData: ScrobbleData
                val loved: Boolean

                when (event) {
                    is PlayingTrackNotifyEvent.CurrentTrackLovedUnloved -> {
                        tracker = mediaListener.findPlayingTracker()
                        scrobbleData = tracker?.trackInfo?.toScrobbleData(false) ?: return@collect
                        loved = event.loved
                    }

                    is PlayingTrackNotifyEvent.TrackLovedUnloved -> {
                        tracker = mediaListener.findTrackerByHash(event.hash)
                        scrobbleData = event.scrobbleData
                        loved = event.loved
                    }
                }

                if (scrobbleData.artist.isEmpty() || scrobbleData.track.isEmpty())
                    return@collect

                mediaListener.scope.launch(Dispatchers.IO) {
                    ScrobbleEverywhere.loveOrUnlove(scrobbleData.toTrack(), loved)
                }

                if (tracker == null) {
                    // todo maybe show the new love/unlove state in notification instead of removing it
                    PanoNotifications.removeNotificationByKey(scrobbleData.appId!!)
                    return@collect
                }

                tracker.trackInfo.updateUserProps(userLoved = loved)

                val linkHeartButtonToRating =
                    PlatformStuff.mainPrefs.data.map { it.linkHeartButtonToRating }.first()

                if (linkHeartButtonToRating &&
                    VariantStuff.billingRepository.licenseState.filterNot { it == LicenseState.UNKNOWN }
                        .first() == LicenseState.VALID
                ) {
                    if (loved)
                        tracker.love()
                    else
                        tracker.unlove()
                }

                globalTrackEventFlow.emit(tracker.trackInfo.toTrackPlayingEvent())
            }

            is PlayingTrackNotifyEvent.AppAllowedBlocked -> {
                PlatformStuff.mainPrefs.updateData {
                    it.allowOrBlockAppCopied(event.appId, event.allowed)
                }

                PanoNotifications.removeNotificationByKey(Stuff.CHANNEL_NOTI_NEW_APP)
            }
        }
    }

}

expect fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent)

expect fun getNowPlayingFromMainProcess(): Pair<ScrobbleData, Int>?

expect fun shouldFetchNpArtUrl(): Flow<Boolean>