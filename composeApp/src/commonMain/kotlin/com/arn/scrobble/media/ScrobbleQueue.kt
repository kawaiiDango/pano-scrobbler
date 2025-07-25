package com.arn.scrobble.media

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.parse_error
import pano_scrobbler.composeapp.generated.resources.scrobble_ignored
import kotlin.math.min


class ScrobbleQueue(
    private val scope: CoroutineScope,
) {
    // delays scrobbling this hash until it becomes null again
    private var lockedHash: Int? = null

    private val tickEveryMs = 500L

    private val scrobbleTasks = mutableMapOf<Int, Job>()

    // ticker, only handles empty messages and messagePQ
    // required because uptimeMillis pauses / slows down in deep sleep

    private fun prune() {
        scrobbleTasks.entries.removeAll { (hash, job) ->
            job.isCancelled || job.isCompleted
        }
    }

    fun shutdown() {
        scrobbleTasks.forEach { (_, job) ->
            job.cancel()
        }

        scrobbleTasks.clear()
    }

    fun has(hash: Int) =
        scrobbleTasks[hash]?.let { !(it.isCancelled || it.isCompleted) } == true

    fun setLockedHash(hash: Int?) {
        lockedHash = hash
    }

    fun scrobble(
        trackInfo: PlayingTrackInfo,
        appIsAllowListed: Boolean,
        delay: Long,
    ) {
        if (trackInfo.title.isEmpty() || has(trackInfo.hash))
            return

        val submitAtTime = PlatformStuff.monotonicTimeMs() + delay
        val hash = trackInfo.hash
        val prevPlayStartTime = if (trackInfo.preprocessed) trackInfo.playStartTime else null
        trackInfo.prepareForScrobbling()
        val scrobbleData = trackInfo.toScrobbleData(useOriginals = false)
        val origScrobbleData = trackInfo.toScrobbleData(useOriginals = true)

        suspend fun nowPlayingAndSubmit(sd: ScrobbleData) {
            Logger.d { "will submit in ${submitAtTime - PlatformStuff.monotonicTimeMs()}ms" }

            val submitNowPlaying = PlatformStuff.mainPrefs.data.map { it.submitNowPlaying }.first()

            // now playing for a new track or after that of the previously paused track has expired
            if (
                Stuff.isOnline &&
                submitNowPlaying &&
                (prevPlayStartTime == null ||
                        (System.currentTimeMillis() - prevPlayStartTime) > min(
                    trackInfo.durationMillis,
                    4 * 60 * 1000L // 4 minutes
                ))
            ) {
                val npResults =
                    withTimeout(submitAtTime - PlatformStuff.monotonicTimeMs() - 5000) {
                        ScrobbleEverywhere.nowPlaying(sd)
                    }

                if (npResults.values.any { !it.isSuccess }) {
                    notifyScrobbleError(
                        npResults,
                        sd,
                        hash
                    )
                }
            }

            // tick every n milliseconds
            while (submitAtTime > PlatformStuff.monotonicTimeMs() || hash == lockedHash) {
                delay(tickEveryMs)
            }

            // launch it in a separate scope, so that it does not get cancelled
            scope.launch(Dispatchers.IO) {
                ScrobbleEverywhere.scrobble(sd)
            }

            notifyPlayingTrackEvent(
                PlayingTrackNotifyEvent.TrackScrobbling(
                    hash = hash,
                    scrobbleData = sd,
                    origScrobbleData = origScrobbleData,
                    nowPlaying = false,
                    userLoved = trackInfo.userLoved,
                    userPlayCount = trackInfo.userPlayCount
                )
            )
        }

        notifyPlayingTrackEvent(
            PlayingTrackNotifyEvent.TrackScrobbling(
                hash = hash,
                scrobbleData = scrobbleData,
                origScrobbleData = origScrobbleData,
                nowPlaying = true,
                userLoved = trackInfo.userLoved,
                userPlayCount = trackInfo.userPlayCount
            )
        )

        if (!appIsAllowListed) {
            PanoNotifications.notifyAppDetected(
                trackInfo.appId,
                PlatformStuff.loadApplicationLabel(trackInfo.appId)
            )
        }

        prune()
        scrobbleTasks[trackInfo.hash]?.cancel()
        scrobbleTasks[trackInfo.hash] = scope.launch(Dispatchers.IO) {
            // some players put the previous song and then switch to the current song in like 150ms
            // potentially wasting an api call. sleep and throw cancellation exception in that case
            delay(Stuff.META_WAIT)

            if (trackInfo.preprocessed) {
                nowPlayingAndSubmit(trackInfo.toScrobbleData(false))
                return@launch
            }

            val preprocessResult = ScrobbleEverywhere.preprocessMetadata(
                scrobbleData,
                trackInfo.extras
            )

            when {
                preprocessResult.blockPlayerAction != null -> {
                    notifyPlayingTrackEvent(
                        PlayingTrackNotifyEvent.TrackCancelled(
                            hash = hash,
                            blockPlayerAction = preprocessResult.blockPlayerAction,
                            showUnscrobbledNotification = false
                        )
                    )
                }

                preprocessResult.titleParseFailed -> {
                    notifyPlayingTrackEvent(
                        PlayingTrackNotifyEvent.Error(
                            hash = hash,
                            scrobbleError = ScrobbleError(
                                getString(Res.string.parse_error),
                                null,
                                trackInfo.appId,
                                canFixMetadata = true
                            ),
                            scrobbleData = scrobbleData.copy(albumArtist = "")
                        )
                    )
                }

                else -> {
                    trackInfo.putPreprocessedData(preprocessResult.scrobbleData)

                    notifyPlayingTrackEvent(
                        PlayingTrackNotifyEvent.TrackScrobbling(
                            hash = hash,
                            scrobbleData = preprocessResult.scrobbleData,
                            origScrobbleData = origScrobbleData,
                            nowPlaying = true,
                            userLoved = trackInfo.userLoved,
                            userPlayCount = trackInfo.userPlayCount
                        )
                    )

                    nowPlayingAndSubmit(preprocessResult.scrobbleData)
                }
            }
        }
    }


    private suspend fun notifyScrobbleError(
        scrobbleResults: Map<Scrobblable, Result<ScrobbleIgnored>>,
        scrobbleData: ScrobbleData,
        hash: Int
    ) {
        val failedTextLines = mutableListOf<String>()
        var ignored = false
        scrobbleResults.forEach { (scrobblable, result) ->
            if (result.isFailure) {
                val exception = scrobbleResults[scrobblable]?.exceptionOrNull()

                val errMsg = exception?.redactedMessage
                failedTextLines += scrobblable.userAccount.type.name + ": $errMsg"
            } else if (result.isSuccess && result.getOrThrow().ignored) {
                failedTextLines += scrobblable.userAccount.type.name + ": " +
                        getString(Res.string.scrobble_ignored)
                ignored = true
            }
        }

        if (failedTextLines.isNotEmpty()) {
            val failedText = failedTextLines.joinToString("\n")
            Logger.w { "failedText= $failedText" }
            if (ignored && scrobbleData.appId != null) {
                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.Error(
                        hash = hash,
                        scrobbleData = scrobbleData,
                        scrobbleError = ScrobbleError(
                            failedText,
                            null,
                            scrobbleData.appId,
                            canFixMetadata = true
                        ),
                    )
                )
            }
        }
    }

    fun remove(hash: Int) {
        if (hash == lockedHash) {
            Logger.d { "$hash locked" }
            return
        }

        scrobbleTasks.remove(hash)?.cancel()
        Logger.d { "$hash cancelled" }
    }
}