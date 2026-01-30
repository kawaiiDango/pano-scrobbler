package com.arn.scrobble.discordrpc

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private sealed interface DiscordActivity {
    data class Activity(
        val discordClientId: String,
        val appId: String,
        val hash: Int,
        val name: String,
        val state: String,
        val details: String,
        val largeText: String,
        val startTimeMillis: Long,
        val durationMillis: Long?,
        val artUrl: String,
        val detailsUrl: String,
        val statusLine: Int,
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

                            val startTimeSecs = startTimeMillis / 1000

                            val endTimeSecs =
                                if (activity.durationMillis != null && activity.durationMillis > 0) {
                                    if (isPlaying)
                                        startTimeSecs + (activity.durationMillis / 1000)
                                    else
                                        now / 1000
                                } else {
                                    0
                                }

                            success = PanoNativeComponents.updateDiscordActivity(
                                clientId = activity.discordClientId,
                                name = activity.name,
                                state = activity.state,
                                details = activity.details,
                                largeText = activity.largeText,
                                startTime = startTimeSecs,
                                endTime = endTimeSecs,
                                artUrl = activity.artUrl,
                                detailsUrl = activity.detailsUrl,
                                isPlaying = isPlaying,
                                statusLine = activity.statusLine,
                                buttonText = "via " + BuildKonfig.APP_NAME,
                                buttonUrl = Stuff.REPO_URL,
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
                        PanoNativeComponents.clearDiscordActivity(false)
                        _wasSuccessful.value = null
                    }

                    is DiscordActivity.Stop -> {
                        PanoNativeComponents.clearDiscordActivity(true)
                        _wasSuccessful.value = null
                    }
                }
            }.launchIn(GlobalScope)

        combine(
            PanoNotifications.playingTrackTrayInfo.mapLatest { it.values.firstOrNull() },
            PlatformStuff.mainPrefs.data.mapLatest { it.discordRpc }
        ) { event, settings ->

            if (!settings.enabled) {
                discordActivity.value = DiscordActivity.Stop
            } else if (event == null) {
                discordActivityKeepTill.value =
                    System.currentTimeMillis() + settings.showPausedForSecs * 1000L
            } else {
                discordActivityKeepTill.value = null

                if (event is PlayingTrackNotifyEvent.TrackPlaying && event.preprocessed)
                    discordActivity.value =
                        transform(
                            appId = event.scrobbleData.appId.orEmpty(),
                            appName = PlatformStuff.loadApplicationLabel(event.scrobbleData.appId.orEmpty()),
                            trackPlaying = event,
                            settings = settings
                        )
            }
        }.launchIn(GlobalScope)
    }

    private fun transform(
        appId: String,
        appName: String,
        trackPlaying: PlayingTrackNotifyEvent.TrackPlaying,
        settings: MainPrefs.DiscordRpcSettings,
    ): DiscordActivity.Activity {
        val hash = trackPlaying.hash
        val state = formatLine(settings.line2Format, trackPlaying, appName)
        val details = formatLine(settings.line1Format, trackPlaying, appName)
        val largeText = formatLine(settings.line3Format, trackPlaying, appName)
        val name = formatLine(settings.nameFormat, trackPlaying, appName)
        val startTimeMillis =
            trackPlaying.timelineStartTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        val durationMillis = trackPlaying.scrobbleData.duration
        val artUrl = trackPlaying.artUrl?.takeIf { settings.albumArt }.orEmpty()
        val statusLine = settings.statusLine
        val detailsUrl = if (settings.detailsUrl)
            "https://www.last.fm/music/${trackPlaying.scrobbleData.artist.encodeURLPathPart()}/_/${trackPlaying.scrobbleData.track.encodeURLPathPart()}"
        else
            ""

        return DiscordActivity.Activity(
            discordClientId = Stuff.DISCORD_CLIENT_ID,
            appId = appId,
            hash = hash,
            name = name,
            state = state,
            details = details,
            largeText = largeText,
            startTimeMillis = startTimeMillis,
            durationMillis = durationMillis,
            artUrl = artUrl,
            detailsUrl = detailsUrl,
            statusLine = statusLine,
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
        trackPlaying: PlayingTrackNotifyEvent.TrackPlaying,
        appName: String,
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
                        AppItem(it, appName).friendlyLabel
                    }.orEmpty()
                }

                else -> match.value
            }
        }
    }
}