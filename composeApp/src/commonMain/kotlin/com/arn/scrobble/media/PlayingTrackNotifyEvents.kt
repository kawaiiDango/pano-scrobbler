package com.arn.scrobble.media

import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

data class ScrobbleError(
    val title: String,
    val description: String?,
    val appId: String,
    val canFixMetadata: Boolean,
)

sealed interface PlayingTrackNotifyEvent {
    data class Error(
        val trackInfo: PlayingTrackInfo,
        val scrobbleError: ScrobbleError,
    ) : PlayingTrackNotifyEvent

    data class TrackInfoUpdated(
        val trackInfo: PlayingTrackInfo,
    ) : PlayingTrackNotifyEvent

    data class TrackScrobbled(
        val trackInfo: PlayingTrackInfo,
    ) : PlayingTrackNotifyEvent

    @Serializable
    data class TrackBlocked(
        val trackInfo: PlayingTrackInfo,
        val blockedMetadata: BlockedMetadata,
    ) : PlayingTrackNotifyEvent

    @Serializable
    data class TrackCancelled(
        val hash: Int?,
        val showUnscrobbledNotification: Boolean,
        val markAsScrobbled: Boolean,
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

private val globalTrackEventFlow by lazy { MutableSharedFlow<PlayingTrackNotifyEvent>() }
private var collectorRegistered = false

suspend fun listenForPlayingTrackEvents(
    scrobbleQueue: ScrobbleQueue,
    mediaListener: MediaListener,
) {
    if (collectorRegistered)
        return

    collectorRegistered = true

    globalTrackEventFlow.collectLatest { event ->
        when (event) {
            is PlayingTrackNotifyEvent.Error -> {
                if (event.scrobbleError.canFixMetadata) {
                    PanoNotifications.notifyBadMeta(
                        event.trackInfo,
                        event.scrobbleError
                    )
                    scrobbleQueue.remove(event.trackInfo.hash)
                    mediaListener.findTrackInfoByHash(event.trackInfo.hash)?.markAsScrobbled()
                } else {
                    PanoNotifications.notifyOtherError(
                        event.trackInfo,
                        event.scrobbleError
                    )
                }
            }

            is PlayingTrackNotifyEvent.TrackInfoUpdated -> {
                val trackInfo =
                    mediaListener.findTrackInfoByHash(event.trackInfo.hash)
                        ?: return@collectLatest
                trackInfo.updateMetaFrom(event.trackInfo)
                PanoNotifications.notifyScrobble(
                    trackInfo,
                    nowPlaying = scrobbleQueue.has(trackInfo.hash)
                )
            }

            is PlayingTrackNotifyEvent.TrackScrobbled -> {
                val trackInfo =
                    mediaListener.findTrackInfoByHash(event.trackInfo.hash)
                        ?: return@collectLatest
                if (trackInfo.userPlayCount > 0)
                    trackInfo.userPlayCount++
                trackInfo.markAsScrobbled()

                PanoNotifications.notifyScrobble(
                    trackInfo,
                    nowPlaying = false
                )
            }

            is PlayingTrackNotifyEvent.TrackBlocked -> {
                val hash = event.trackInfo.hash
                val blockedMetadata = event.blockedMetadata
                if (blockedMetadata.skip) {
                    mediaListener.skip(hash)
                } else if (blockedMetadata.mute) {
                    mediaListener.mute(hash)
                }

                val trackInfo = mediaListener.findPlayingTrackInfo() ?: return@collectLatest
                if (!scrobbleQueue.has(hash))
                    return@collectLatest
                PanoNotifications.removeNotificationByTag(trackInfo.appId)
                trackInfo.markAsScrobbled()
                trackInfo.isPlaying = false
            }

            is PlayingTrackNotifyEvent.TrackCancelled -> {
                val trackInfo = if (event.hash == null) {
                    mediaListener.findPlayingTrackInfo()
                } else {
                    mediaListener.findTrackInfoByHash(event.hash)
                }

                trackInfo ?: return@collectLatest

                val hash = trackInfo.hash
                if (!scrobbleQueue.has(hash)) {
                    return@collectLatest
                } else {
                    if (event.markAsScrobbled) {
                        trackInfo.markAsScrobbled()
                    }

                    scrobbleQueue.remove(hash)
                }

                trackInfo.isPlaying = false

                if (event.showUnscrobbledNotification) {
                    PanoNotifications.notifyUnscrobbled(trackInfo)
                } else {
                    PanoNotifications.removeNotificationByTag(trackInfo.appId)
                }
            }

            is PlayingTrackNotifyEvent.TrackScrobbleLocked -> {
                if (event.hash != -1) {
                    if (event.locked) {
                        scrobbleQueue.lockedHash = event.hash
                    } else {
                        scrobbleQueue.lockedHash = null
                    }
                }
            }

            is PlayingTrackNotifyEvent.TrackLovedUnloved -> {
                val trackInfo = if (event.hash == null) {
                    mediaListener.findPlayingTrackInfo()
                } else {
                    mediaListener.findTrackInfoByHash(event.hash)
                }

                trackInfo ?: return@collectLatest

                if (trackInfo.artist.isEmpty() || trackInfo.title.isEmpty())
                    return@collectLatest

//                if (hash == 0) {
//                    // called from automation app
//                    toast(
//                        (if (loved)
//                            "♥"
//                        else
//                            "\uD83D\uDC94"
//                                ) + Stuff.formatBigHyphen(
//                            trackInfo.artist,
//                            trackInfo.title
//                        )
//                    )
//                }

                mediaListener.scope.launch(Dispatchers.IO) {
                    ScrobbleEverywhere.loveOrUnlove(trackInfo.toTrack(), event.loved)
                }

                trackInfo.userLoved = event.loved
                PanoNotifications.notifyScrobble(
                    trackInfo,
                    nowPlaying = scrobbleQueue.has(trackInfo.hash)
                )

                val linkHeartButtonToRating =
                    PlatformStuff.mainPrefs.data.mapLatest { it.linkHeartButtonToRating }.first()

                if (linkHeartButtonToRating && PlatformStuff.billingRepository.isLicenseValid)

                    if (event.loved)
                        mediaListener.love(trackInfo.hash)
                    else
                        mediaListener.unlove(trackInfo.hash)
            }

            is PlayingTrackNotifyEvent.AppAllowedBlocked -> {
                val appId = event.appId
                //create copies
                val aSet =
                    PlatformStuff.mainPrefs.data.map { it.allowedPackages }.first().toMutableSet()
                val bSet =
                    PlatformStuff.mainPrefs.data.map { it.blockedPackages }.first().toMutableSet()

                if (event.allowed)
                    aSet += appId
                else
                    bSet += appId
                bSet.removeAll(aSet) // allowlist takes over blocklist for conflicts

                PlatformStuff.mainPrefs.updateData {
                    it.copy(
                        allowedPackages = aSet,
                        blockedPackages = bSet
                    )
                }

                PanoNotifications.removeNotificationByTag(Stuff.CHANNEL_NOTI_NEW_APP)
            }
        }
    }

}

fun notifyPlayingTrackEvent(event: PlayingTrackNotifyEvent) {
    if (collectorRegistered) {
        GlobalScope.launch {
            globalTrackEventFlow.emit(event)
        }
    } else {
        notifyPlayingTrackEventWithIpc(event)
    }
}

expect fun notifyPlayingTrackEventWithIpc(event: PlayingTrackNotifyEvent)