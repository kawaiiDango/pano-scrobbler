package com.arn.scrobble.discordrpc

import co.touchlab.kermit.Logger
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private sealed interface DiscordActivity {
    data class Activity(
        val discordClientId: String,
        val appId: String,
        val hash: Int,
        val state: String,
        val details: String,
        val largeText: String,
        val startTimeMillis: Long,
        val durationMillis: Long?,
        val artUrl: String,
        val statusLine: Int,
        val buttonsTexts: Array<String>,
        val buttonUrls: Array<String>,
    ) : DiscordActivity

    data object Clear : DiscordActivity
    data object Stop : DiscordActivity
}

object DiscordRpc {
    private val placeholderRegex by lazy {
        DiscordRpcPlaceholder.entries
            .joinToString(
                prefix = "\\$(",
                postfix = ")",
                separator = "|",
                transform = { it.name })
            .toRegex()
    }
    private val seenApps =
        PlatformStuff.mainPrefs.data.mapLatest { it.seenApps }
            .stateIn(
                GlobalScope,
                SharingStarted.Eagerly,
                Stuff.mainPrefsInitialValue.seenApps
            )

    private val discordActivity = MutableStateFlow<DiscordActivity>(DiscordActivity.Stop)

    // null = don't clear, >0 = clear after secs
    private val discordActivityKeepTill = MutableStateFlow<Long?>(null)

    private val _wasSuccessful = MutableStateFlow<Boolean?>(null)
    val wasSuccessFul = _wasSuccessful.asStateFlow()
    private val retryDelay = 7.seconds

    fun start() {
        combine(discordActivity, discordActivityKeepTill) { activity, keepTill ->
            activity to keepTill
        }
            .mapLatest { (activity, keepTill) ->
                delay(
                    if (_wasSuccessful.value != false)
                        500.milliseconds
                    else
                        retryDelay
                )

                when (activity) {
                    is DiscordActivity.Activity -> {
                        var success = false

                        while (!success) {
                            val now = System.currentTimeMillis()
                            val isPlaying = keepTill == null

                            val startTimeMillis = if (isPlaying || activity.durationMillis == null)
                                activity.startTimeMillis
                            else
                                now - activity.durationMillis
                            val endTimeMillis =
                                if (activity.durationMillis != null && activity.durationMillis > 0) {
                                    if (isPlaying)
                                        startTimeMillis + activity.durationMillis
                                    else
                                        now
                                } else {
                                    0
                                }

                            success = PanoNativeComponents.updateDiscordActivity(
                                clientId = activity.discordClientId,
                                state = activity.state,
                                details = activity.details,
                                largeText = activity.largeText,
                                startTime = startTimeMillis / 1000,
                                endTime = endTimeMillis / 1000,
                                artUrl = activity.artUrl,
                                isPlaying = isPlaying,
                                statusLine = activity.statusLine,
                                buttonTexts = activity.buttonsTexts,
                                buttonUrls = activity.buttonUrls,
                            )

                            _wasSuccessful.value = success

                            Logger.d { activity.toString() }

                            if (!success) {
                                Logger.d { "Failed to update Discord activity, retrying" }
                                delay(10.seconds)
                            } else {

                                if (!isPlaying) {
                                    delay(keepTill - now)
                                    discordActivity.emit(DiscordActivity.Clear)
                                }
                            }
                        }
                    }

                    is DiscordActivity.Clear -> {
                        PanoNativeComponents.clearDiscordActivity()
                        _wasSuccessful.value = null
                    }

                    is DiscordActivity.Stop -> {
                        PanoNativeComponents.stopDiscordActivity()
                        _wasSuccessful.value = null
                    }
                }
            }.launchIn(GlobalScope)

        combine(
            PanoNotifications.playingTrackTrayInfo.mapLatest { it.entries.firstOrNull()?.toPair() },
            PlatformStuff.mainPrefs.data.mapLatest { it.discordRpc }
        ) { appIdToEvent, settings ->

            if (!settings.enabled) {
                discordActivity.value = DiscordActivity.Stop
            } else if (appIdToEvent == null) {
                discordActivityKeepTill.value =
                    System.currentTimeMillis() + settings.showPausedForSecs * 1000L
            } else {
                discordActivityKeepTill.value = null
                val (appId, event) = appIdToEvent

                if (event is PlayingTrackNotifyEvent.TrackPlaying)
                    discordActivity.value = transform(appId, event, settings)
            }
        }.launchIn(GlobalScope)
    }

    private fun transform(
        appId: String,
        trackPlaying: PlayingTrackNotifyEvent.TrackPlaying,
        settings: MainPrefs.DiscordRpcSettings,
    ): DiscordActivity.Activity {
        val hash = trackPlaying.hash
        val state = formatLine(settings.line2Format, trackPlaying)
        val details = formatLine(settings.line1Format, trackPlaying)
        val largeText = formatLine(settings.line3Format, trackPlaying)
        val startTimeMillis =
            trackPlaying.timelineStartTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        val durationMillis = trackPlaying.scrobbleData.duration
        val artUrl = trackPlaying.artUrl?.takeIf { settings.albumArt }.orEmpty()
        val statusLine = settings.statusLine
        val buttonTexts = mutableListOf<String>()
        val buttonUrls = mutableListOf<String>()

        if (settings.showUrlButton) {
            buttonTexts.add("On last.fm")
            buttonUrls.add("https://www.last.fm/music/${trackPlaying.scrobbleData.artist.encodeURLPathPart()}/_/${trackPlaying.scrobbleData.track.encodeURLPathPart()}")
        }

        return DiscordActivity.Activity(
            discordClientId = Stuff.DISCORD_CLIENT_ID,
            appId = appId,
            hash = hash,
            state = state,
            details = details,
            largeText = largeText,
            startTimeMillis = startTimeMillis,
            durationMillis = durationMillis,
            artUrl = artUrl,
            statusLine = statusLine,
            buttonsTexts = buttonTexts.toTypedArray(),
            buttonUrls = buttonUrls.toTypedArray(),
        )
    }

    fun clearDiscordActivity(appId: String) {
        if ((discordActivity.value as? DiscordActivity.Activity)?.appId == appId)
            discordActivity.value = DiscordActivity.Clear
    }

    fun clearDiscordActivity(hash: Int?) {
        val activity = discordActivity.value as? DiscordActivity.Activity
        if (activity != null && (hash == null || hash == activity.hash)) {
            discordActivity.value = DiscordActivity.Clear
        }
    }

    private fun formatLine(
        template: String,
        trackPlaying: PlayingTrackNotifyEvent.TrackPlaying
    ): String {
        return template.replace(placeholderRegex) { match ->
            when (match.groupValues.getOrNull(1)) {
                DiscordRpcPlaceholder.artist.name -> trackPlaying.scrobbleData.artist
                DiscordRpcPlaceholder.title.name -> (if (trackPlaying.userLoved) "❤ " else "") +
                        trackPlaying.scrobbleData.track

                DiscordRpcPlaceholder.albumArtist.name -> trackPlaying.scrobbleData.albumArtist
                    .orEmpty().ifEmpty { trackPlaying.scrobbleData.artist }

                DiscordRpcPlaceholder.album.name -> trackPlaying.scrobbleData.album.orEmpty()
                DiscordRpcPlaceholder.mediaPlayer.name -> {
                    trackPlaying.scrobbleData.appId?.let {
                        AppItem(
                            it,
                            seenApps.value[it].orEmpty()
                        ).friendlyLabel
                    }.orEmpty()
                }

                else -> match.value
            }
        }
    }
}