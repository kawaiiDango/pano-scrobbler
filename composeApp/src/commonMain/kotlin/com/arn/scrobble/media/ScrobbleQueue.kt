package com.arn.scrobble.media

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.PriorityQueue
import kotlin.math.min


class ScrobbleQueue(
    private val scope: CoroutineScope,
) {
    private val delayPercentAndSecs =
        PlatformStuff.mainPrefs.data.map { it.delayPercentP to it.delaySecsP }
            .stateIn(
                scope,
                SharingStarted.Lazily,
                Stuff.mainPrefsInitialValue.let { it.delayPercentP to it.delaySecsP })

    // delays scrobbling this hash until it becomes null again
    var lockedHash: Int? = null

    private val tickEveryMs = 500L

    private val tracksCopyPQ = PriorityQueue<PlayingTrackInfo>(20) { a, b ->
        (a.scrobbleAtMonotonicTime - b.scrobbleAtMonotonicTime).toInt()
    }

    private var tickerJob: Job? = null
    private var lastNpTask: Job? = null


    // ticker, only handles empty messages and messagePQ
    // required because uptimeMillis pauses / slows down in deep sleep

    private fun startTickerIfNeeded() {
        if (tickerJob?.isActive == true) return

        tickerJob = scope.launch {
            while (tracksCopyPQ.isNotEmpty()) {
                delay(tickEveryMs)
                val queuedMessage = tracksCopyPQ.peek()
                if (queuedMessage != null && queuedMessage.hash != lockedHash &&
                    queuedMessage.scrobbleAtMonotonicTime <= PlatformStuff.monotonicTimeMs()
                ) {
                    tracksCopyPQ.remove(queuedMessage)
                    submitScrobble(queuedMessage)
                }
            }
        }
    }

    fun shutdown() {
        tickerJob?.cancel()
        tracksCopyPQ.clear()
    }

    fun has(hash: Int) = tracksCopyPQ.any { it.hash == hash }

    fun reschedule(hash: Int, newElapsedRealtime: Long) {
        tracksCopyPQ.find { it.hash == hash }
            ?.scrobbleAtMonotonicTime = newElapsedRealtime
    }

    fun addScrobble(trackInfo: PlayingTrackInfo) =
        trackInfo.copy().also {
            tracksCopyPQ.add(it)
            startTickerIfNeeded()
        }

    fun nowPlaying(
        trackInfo: PlayingTrackInfo,
        appIsAllowListed: Boolean,
        fixedDelay: Long? = null,
    ) {
        if (trackInfo.title.isEmpty() || has(trackInfo.hash))
            return

        val (delayPercent, delaySecs) = delayPercentAndSecs.value

        trackInfo.artist = MetadataUtils.sanitizeArtist(trackInfo.artist)
        trackInfo.album = MetadataUtils.sanitizeAlbum(trackInfo.album)
        trackInfo.albumArtist = MetadataUtils.sanitizeAlbumArtist(trackInfo.albumArtist)
        trackInfo.userPlayCount = 0
        trackInfo.userLoved = false

        trackInfo.isPlaying = true

        var finalDelay: Long
        if (fixedDelay == null) {
            val delayMillis = delaySecs.toLong() * 1000
            val delayFraction = delayPercent / 100.0
            val delayMillisFraction = if (trackInfo.durationMillis > 0)
                (trackInfo.durationMillis * delayFraction).toLong()
            else
                Long.MAX_VALUE

            finalDelay = min(delayMillisFraction, delayMillis)
                .coerceAtLeast(10 * 1000) // don't scrobble < 10 seconds

            finalDelay = (finalDelay - trackInfo.timePlayed)
                .coerceAtLeast(1000)// deal with negative or 0 delay
        } else {
            finalDelay = fixedDelay
        }

        val submitTime = PlatformStuff.monotonicTimeMs() + finalDelay
        trackInfo.scrobbleAtMonotonicTime = submitTime
        val trackInfoCopy = addScrobble(trackInfo)

        lastNpTask?.cancel()
        lastNpTask = scope.launch(Dispatchers.IO) {
            ScrobbleEverywhere.scrobble(true, trackInfoCopy)
        }

        PanoNotifications.notifyScrobble(trackInfo, nowPlaying = true)
        if (!appIsAllowListed) {
            PanoNotifications.notifyAppDetected(
                trackInfo.appId,
                PlatformStuff.loadApplicationLabel(trackInfo.appId)
            )
        }
    }

    private fun submitScrobble(trackInfoCopy: PlayingTrackInfo) {
        // if it somehow reached here, don't scrobble
//        if (mediaListener.hasOtherPlayingControllers(trackInfoCopy) == true && trackInfoCopy.hasBlockedTag)
//            return

        scope.launch(Dispatchers.IO) {
            notifyPlayingTrackEvent(PlayingTrackNotifyEvent.TrackScrobbled(trackInfoCopy))
            ScrobbleEverywhere.scrobble(false, trackInfoCopy)
        }

    }

    fun remove(hash: Int) {
        if (hash == lockedHash) return

        Logger.d { "$hash cancelled" }
        tracksCopyPQ.removeAll { it.hash == hash }
    }
}