package com.arn.scrobble.media

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.ScrobbleResult
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.parse_error
import pano_scrobbler.composeapp.generated.resources.scrobble_ignored
import kotlin.math.min


class ScrobbleQueue(
    private val scope: CoroutineScope,
) {
    class NetworkRequestNeededException(cause: Throwable? = null) :
        IllegalStateException("Network request needed", cause)

    // delays scrobbling this hash until it becomes null again
    private var lockedHash: Int? = null

    private val tickEveryMs = 500L

    private val scrobbleTasks = mutableMapOf<Int, Job>()

    private val fetchAdditionalMetadataTimestamps = ArrayDeque<Long>()
    private val fetchAdditionalMetadataMutex = Mutex()

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

    private suspend fun canFetchAdditionalMetadata() {
        // was still getting java.util.NoSuchElementException: ArrayDeque is empty, so use lock
        return fetchAdditionalMetadataMutex.withLock {
            val now = System.currentTimeMillis()
            // Remove timestamps older than n seconds
            while (now - (fetchAdditionalMetadataTimestamps.firstOrNull() ?: now) > 1 * 60_000) {
                fetchAdditionalMetadataTimestamps.removeFirstOrNull()
            }
            val can = fetchAdditionalMetadataTimestamps.size < 2
            fetchAdditionalMetadataTimestamps.addLast(System.currentTimeMillis())

            if (!can)
                throw NetworkRequestNeededException()
        }
    }

    fun scrobble(
        trackInfo: PlayingTrackInfo,
        appIsAllowListed: Boolean,
        delay: Long,
        timestampOverride: Long? = null,
    ) {
        if (trackInfo.title.isEmpty() || has(trackInfo.hash))
            return

        val submitAtTime = PlatformStuff.monotonicTimeMs() + delay
        val hash = trackInfo.hash
        val prevPlayStartTime =
            if (trackInfo.scrobbledState > PlayingTrackInfo.ScrobbledState.PREPROCESSED)
                trackInfo.playStartTime
            else
                null
        trackInfo.prepareForScrobbling()
        val scrobbleData = trackInfo.toScrobbleData(useOriginals = false)
            .let {
                if (timestampOverride != null) {
                    it.copy(timestamp = timestampOverride)
                } else {
                    it
                }
            }
        val origScrobbleData = trackInfo.toScrobbleData(useOriginals = true)

        suspend fun nowPlayingAndSubmit(
            sd: ScrobbleData,
            fetchAdditionalMetadata: Boolean
        ) = coroutineScope {
            Logger.d { "will submit in ${submitAtTime - PlatformStuff.monotonicTimeMs()}ms" }

            val submitNowPlaying =
                PlatformStuff.mainPrefs.data.map { it.submitNowPlaying }.first()

            // now playing for a new track or after that of the previously paused track has expired
            var lastfmNpSucc = false
            if (
                timestampOverride == null &&
                submitNowPlaying &&
                (prevPlayStartTime == null ||
                        (System.currentTimeMillis() - prevPlayStartTime) > min(
                    trackInfo.durationMillis * 3 / 4, // 3/4 of the track duration to prevent edge cases
                    4 * 60 * 1000L // 4 minutes
                ))
            ) {
                val npResults =
                    withTimeoutOrNull(submitAtTime - PlatformStuff.monotonicTimeMs() - 5000) {
                        ScrobbleEverywhere.nowPlaying(sd)
                    }

                // listenbrainz msid for now playing
                val msid = npResults?.firstNotNullOfOrNull { (k, v) ->
                    if (k.userAccount.type == AccountType.LISTENBRAINZ && v.isSuccess)
                        v.getOrThrow().msid
                    else
                        null
                }

                trackInfo.nowPlayingSubmitted(msid)

                if (msid != null)
                    notifyPlayingTrackEvent(trackInfo.toTrackPlayingEvent())


                if (npResults != null && npResults.values.any { !it.isSuccess }) {
                    notifyScrobbleError(
                        notiKey = trackInfo.uniqueId,
                        scrobbleResults = npResults,
                        scrobbleData = sd,
                        hash = hash
                    )
                }

                lastfmNpSucc = npResults?.any { (k, v) ->
                    k.userAccount.type == AccountType.LASTFM && v.isSuccess
                } == true

            }

            // discord rpc album art
            val npArtFetchJob =
                if (PlatformStuff.isDesktop &&
                    PlatformStuff.mainPrefs.data.map { it.discordRpc.enabled }.first() &&
                    lastfmNpSucc &&
                    trackInfo.artUrl == null
                ) {
                    launch(Dispatchers.IO) {
                        if (shouldFetchNpArtUrl().firstOrNull { it } == true) {
                            val additionalMetadata = ScrobbleEverywhere.fetchAdditionalMetadata(
                                scrobbleData,
                                ::canFetchAdditionalMetadata,
                                true,
                            )

                            if (additionalMetadata.artUrl != null) {
                                Logger.d { "fetched artUrl for now playing: ${additionalMetadata.artUrl}" }
                                trackInfo.setArtUrl(additionalMetadata.artUrl)
                                notifyPlayingTrackEvent(trackInfo.toTrackPlayingEvent())
                            }
                        }
                    }
                } else
                    null

            // tick every n milliseconds
            while (submitAtTime > PlatformStuff.monotonicTimeMs() || hash == lockedHash) {
                delay(tickEveryMs)
            }

            npArtFetchJob?.cancel()

            // launch it in a separate scope, so that it does not get cancelled
            scope.launch(Dispatchers.IO) {
                val scrobbleSd = if (fetchAdditionalMetadata) {
                    val additionalMetadata = ScrobbleEverywhere.fetchAdditionalMetadata(
                        scrobbleData,
                        { }
                    )

                    ScrobbleEverywhere.preprocessMetadata(
                        additionalMetadata.scrobbleData ?: scrobbleData,
                        trackInfo.normalizedUrlHost
                    ).scrobbleData
                } else {
                    sd
                }

                ScrobbleEverywhere.scrobble(scrobbleSd)
            }

            trackInfo.scrobbled()

            notifyPlayingTrackEvent(
                trackInfo.toTrackPlayingEvent()
            )
        }

        notifyPlayingTrackEvent(
            trackInfo.toTrackPlayingEvent().copy(
                scrobbleData = scrobbleData,
                nowPlaying = true,
            )
        )

        if (!appIsAllowListed) {
            scope.launch {
                PanoNotifications.notifyAppDetected(
                    trackInfo.appId,
                    PlatformStuff.loadApplicationLabel(trackInfo.appId)
                )
            }
        }

        prune()
        scrobbleTasks[trackInfo.hash]?.cancel()
        scrobbleTasks[trackInfo.hash] = scope.launch(Dispatchers.IO) {
            // some players put the previous song and then switch to the current song in like 150ms
            // potentially wasting an api call. sleep and throw cancellation exception in that case
            delay(Stuff.META_WAIT)

            if (trackInfo.scrobbledState in
                PlayingTrackInfo.ScrobbledState.PREPROCESSED..PlayingTrackInfo.ScrobbledState.ADDITIONAL_METADATA_FETCHED
            ) {
                nowPlayingAndSubmit(
                    trackInfo.toScrobbleData(false),
                    trackInfo.scrobbledState == PlayingTrackInfo.ScrobbledState.PREPROCESSED
                )
                return@launch
            }

            val additionalMeta = ScrobbleEverywhere.fetchAdditionalMetadata(
                scrobbleData,
                ::canFetchAdditionalMetadata
            )

            val preprocessResult = ScrobbleEverywhere.preprocessMetadata(
                additionalMeta.scrobbleData ?: scrobbleData,
                trackInfo.normalizedUrlHost
            )

            when {
                preprocessResult.blockPlayerAction != null -> {
                    notifyPlayingTrackEvent(
                        PlayingTrackNotifyEvent.TrackCancelled(
                            hash = hash,
                            showUnscrobbledNotification = false,
                            blockedMetadata = BlockedMetadata(blockPlayerAction = preprocessResult.blockPlayerAction),
                        )
                    )
                }

                preprocessResult.titleParseFailed -> {
                    notifyPlayingTrackEvent(
                        PlayingTrackNotifyEvent.Error(
                            notiKey = trackInfo.uniqueId,
                            hash = hash,
                            scrobbleError = ScrobbleError(
                                getString(Res.string.parse_error),
                                null,
                                trackInfo.appId,
                                canFixMetadata = true
                            ),
                            scrobbleData = scrobbleData.copy(albumArtist = ""),
                            msid = null,
                        )
                    )
                }

                else -> {
                    trackInfo.putPreprocessedData(
                        preprocessResult.scrobbleData,
                        !additionalMeta.shouldFetchAgain
                    )

                    if (additionalMeta.artUrl != null) {
                        trackInfo.setArtUrl(additionalMeta.artUrl)
                    }

                    notifyPlayingTrackEvent(

                        trackInfo.toTrackPlayingEvent().copy(
                            origScrobbleData = additionalMeta.scrobbleData ?: origScrobbleData,
                            nowPlaying = true,
                        )
                    )

                    nowPlayingAndSubmit(
                        preprocessResult.scrobbleData,
                        additionalMeta.shouldFetchAgain
                    )
                }
            }
        }
    }


    private suspend fun notifyScrobbleError(
        notiKey: String,
        scrobbleResults: Map<Scrobblable, Result<ScrobbleResult>>,
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
                        notiKey = notiKey,
                        hash = hash,
                        scrobbleData = scrobbleData,
                        scrobbleError = ScrobbleError(
                            failedText,
                            null,
                            scrobbleData.appId,
                            canFixMetadata = true,
                        ),
                        msid = null,
                    )
                )
            }
        }
    }

    fun remove(hash: Int) {
        if (hash == lockedHash) {
            Logger.d { "${hash.toHexString()} locked" }
            return
        }

        if (scrobbleTasks.remove(hash)?.cancel() != null) {
            Logger.d { "${hash.toHexString()} cancelled" }
        }
    }
}