package com.arn.scrobble.discordrpc

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.media.PlayingTrackInfo
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.accountTypeStringRes
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.http.encodeURLPathPart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.profile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private sealed interface DiscordActivity {
    // https://discord.com/developers/docs/social-sdk/classdiscordpp_1_1ActivityAssets.html
    data class Activity(
        val discordClientId: String,
        val hash: Int,
        val scrobbleData: ScrobbleData,
        val name: String,
        // If specified, must be a string between 2 and 128 characters.
        val state: String,
        // If specified, must be a string between 2 and 128 characters.
        val details: String,
        // If specified, must be a string between 2 and 128 characters.
        val largeText: String,
        val startTimeMillis: Long,
        val durationMillis: Long?,
        // If specified, must be a string between 1 and 300 characters.
        val artUrl: String,
        // If specified, must be a string between 2 and 256 characters.
        val detailsUrl: String,
        val statusLine: Int,
        val buttonText: String,
        val buttonUrl: String,
        // null = don't clear, >0 = clear after secs
//        val keepTill: Long?
        val isPlaying: Boolean,
        val showPausedForSecs: Int,
        val canFetchArt: Boolean
    ) : DiscordActivity

    data object Clear : DiscordActivity
    data object Stop : DiscordActivity
}

private enum class DiscordRpcStatus {
    ACTIVITY_SET,
    ACTIVITY_CLEARED,
    ACTIVITY_STOPPED,
    FAILED
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

    private val pausedHash = MutableStateFlow<Int?>(null)

    private val _lastStatus = MutableStateFlow(DiscordRpcStatus.ACTIVITY_STOPPED)
    val wasSuccessful = _lastStatus.map { it == DiscordRpcStatus.ACTIVITY_SET }
    private val retryDelay = 10.seconds

    fun start() {
        PanoNotifications.playingTrackTrayInfo
            .mapLatest {
                delay(500.milliseconds) // debounce the source
                it.values
                    .filterIsInstance<PlayingTrackNotifyEvent.TrackPlaying>()
                    .firstOrNull()
                    ?.takeIf { it.preprocessed }
            }
            .combine(
                PlatformStuff.mainPrefs.data.map { it.discordRpc }.distinctUntilChanged()
            ) { event, settings ->
                val activity: DiscordActivity

                if (!settings.enabled) {
                    activity = DiscordActivity.Stop
                } else if (event != null) {
                    var buttonText = "via " + BuildKonfig.APP_NAME
                    var buttonUrl = Stuff.REPO_URL

                    suspend fun setProfileUrlAndText(accountType: AccountType) {
                        val profileUrl = Scrobblables.all
                            .find { it.userAccount.type == accountType }
                            ?.userAccount?.user?.url

                        if (profileUrl != null) {
                            buttonUrl = profileUrl
                            buttonText = getString(accountTypeStringRes(accountType).first) + " " +
                                    getString(Res.string.profile)
                        }
                    }

                    val buttonType =
                        MainPrefs.DiscordRpcPrefs.ButtonType.entries.find { it.name == settings.buttonType }
                            ?: MainPrefs.DiscordRpcPrefs.ButtonType.PANO_SCROBBLER

                    when (buttonType) {
                        MainPrefs.DiscordRpcPrefs.ButtonType.NONE -> {
                            buttonText = ""
                            buttonUrl = ""
                        }

                        MainPrefs.DiscordRpcPrefs.ButtonType.LASTFM_PROFILE -> {
                            setProfileUrlAndText(AccountType.LASTFM)
                        }

                        MainPrefs.DiscordRpcPrefs.ButtonType.LISTENBRAINZ_PROFILE -> {
                            setProfileUrlAndText(AccountType.LISTENBRAINZ)
                        }

                        MainPrefs.DiscordRpcPrefs.ButtonType.LIBREFM_PROFILE -> {
                            setProfileUrlAndText(AccountType.LIBREFM)
                        }

                        MainPrefs.DiscordRpcPrefs.ButtonType.PANO_SCROBBLER -> {
                        }
                    }

                    pausedHash.value = null

                    activity = transform(
                        appName = PlatformStuff.loadApplicationLabel(event.scrobbleData.appId.orEmpty()),
                        trackPlaying = event,
                        buttonUrl = buttonUrl,
                        buttonText = buttonText,
                        settings = settings
                    )

                } else {
                    activity = DiscordActivity.Clear
                }

                activity
            }.scan(
                Pair<DiscordActivity?, DiscordActivity?>(null, null)
            ) { accumulator, current ->
                // accumulator.second is the previous value
                val previous = accumulator.first

                Logger.d { "DiscordRpc: current=$current,\nprevious=$previous" }

                val newCurrent = when (current) {
                    is DiscordActivity.Clear if previous == null -> null
                    is DiscordActivity.Clear if previous is DiscordActivity.Activity &&
                            previous.hash == pausedHash.value &&
                            previous.showPausedForSecs > 0
                        -> previous.copy(isPlaying = false)

                    else -> current
                }

                Pair(newCurrent, previous)
            }.map { (current, previous) ->
                current
            }
            .distinctUntilChanged()
            .mapLatest { activity ->
                Logger.d { "DiscordRpc: activity=$activity" }

                activity ?: return@mapLatest

                var succ = _lastStatus.value != DiscordRpcStatus.FAILED

                do {
                    if (!succ)
                        delay(retryDelay)
                    succ = updateActivity(activity)
                    _lastStatus.value = when {
                        succ && activity is DiscordActivity.Activity -> DiscordRpcStatus.ACTIVITY_SET
                        succ && activity is DiscordActivity.Clear -> DiscordRpcStatus.ACTIVITY_CLEARED
                        succ && activity is DiscordActivity.Stop -> DiscordRpcStatus.ACTIVITY_STOPPED
                        else -> DiscordRpcStatus.FAILED
                    }

                    // don't retry Clear or Stop
                    if (activity !is DiscordActivity.Activity)
                        break

                    if (succ &&
                        activity.artUrl.isEmpty() &&
                        activity.isPlaying &&
                        activity.canFetchArt
                    )
                        fetchArt(activity.scrobbleData, activity.hash)

                    if (!succ) {
                        Logger.d { "DiscordRpc: retrying" }
                    } else if (!activity.isPlaying && activity.showPausedForSecs > 0) {
                        withTimeoutOrNull(activity.showPausedForSecs.seconds) {
                            pausedHash.first { it != activity.hash }
                        }
                        succ = updateActivity(DiscordActivity.Clear)
                        _lastStatus.value = if (succ)
                            DiscordRpcStatus.ACTIVITY_CLEARED
                        else
                            DiscordRpcStatus.FAILED
                    }
                } while (!succ)
            }
            .launchIn(Stuff.appScope)
    }

    private fun updateActivity(activity: DiscordActivity): Boolean =
        when (activity) {
            is DiscordActivity.Activity -> {
                val now = System.currentTimeMillis()

                val startTimeMillis = if (activity.isPlaying || activity.durationMillis == null)
                    activity.startTimeMillis
                else
                    now - activity.durationMillis

                val startTimeSecs = startTimeMillis / 1000

                val endTimeSecs =
                    if (activity.durationMillis != null && activity.durationMillis > 0) {
                        if (activity.isPlaying)
                            startTimeSecs + (activity.durationMillis / 1000)
                        else
                            now / 1000
                    } else {
                        0
                    }

                PanoNativeComponents.updateDiscordActivity(
                    clientId = activity.discordClientId,
                    name = activity.name,
                    state = activity.state,
                    details = activity.details,
                    largeText = activity.largeText,
                    startTime = startTimeSecs,
                    endTime = endTimeSecs,
                    artUrl = activity.artUrl,
                    detailsUrl = activity.detailsUrl,
                    isPlaying = activity.isPlaying,
                    statusLine = activity.statusLine,
                    buttonText = activity.buttonText,
                    buttonUrl = activity.buttonUrl,
                )

            }

            is DiscordActivity.Clear -> {
                PanoNativeComponents.clearDiscordActivity(false)
            }

            is DiscordActivity.Stop -> {
                PanoNativeComponents.clearDiscordActivity(true)
            }
        }

    // call this only on isPlaying=true. The art is from now playing
    private suspend fun fetchArt(scrobbleData: ScrobbleData, hash: Int) {
        val additionalMetadata = ScrobbleEverywhere.fetchNowPlayingAlbumArt(scrobbleData)

        notifyPlayingTrackEvent(
            PlayingTrackNotifyEvent.ArtUrlFetched(
                hash,
                additionalMetadata.artUrl.orEmpty()
            )
        )

        if (!additionalMetadata.artUrl.isNullOrEmpty()) {
            Logger.d { "DiscordRpc artUrl fetched: ${additionalMetadata.artUrl}" }
        } else {
            Logger.e { "DiscordRpc artUrl: ${additionalMetadata.artUrl}" }
        }
    }

    private fun transform(
        appName: String,
        trackPlaying: PlayingTrackNotifyEvent.TrackPlaying,
        buttonUrl: String,
        buttonText: String,
        settings: MainPrefs.DiscordRpcPrefs,
    ): DiscordActivity.Activity {
        val hash = trackPlaying.hash
        val state = formatLine(settings.line2Format, trackPlaying, appName, settings.lovedState)
        val details = formatLine(settings.line1Format, trackPlaying, appName, settings.lovedState)
        val largeText = formatLine(settings.line3Format, trackPlaying, appName, settings.lovedState)
        val name = formatLine(settings.nameFormat, trackPlaying, appName, settings.lovedState)
        val startTimeMillis =
            trackPlaying.timelineStartTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        val durationMillis = trackPlaying.scrobbleData.duration
        val artUrl = trackPlaying.artUrlState.takeIf { settings.albumArt }?.url.orEmpty()
        val statusLine = settings.statusLine
        val detailsUrl = if (settings.detailsUrl)
            "https://www.last.fm/music/${trackPlaying.scrobbleData.artist.encodeURLPathPart()}/_/${trackPlaying.scrobbleData.track.encodeURLPathPart()}"
        else
            ""

        return DiscordActivity.Activity(
            discordClientId = Stuff.DISCORD_CLIENT_ID,
            hash = hash,
            scrobbleData = trackPlaying.scrobbleData,
            name = name.clamp(2, 128),
            state = state.clamp(2, 128),
            details = details.clamp(2, 128),
            largeText = largeText.clamp(2, 128),
            startTimeMillis = startTimeMillis,
            durationMillis = durationMillis,
            artUrl = artUrl.takeIf { it.length in 1..300 }.orEmpty(),
            detailsUrl = detailsUrl.takeIf { it.length in 2..256 }.orEmpty(),
            statusLine = statusLine,
            buttonText = buttonText,
            buttonUrl = buttonUrl,
            showPausedForSecs = settings.showPausedForSecs,
            isPlaying = true,
            canFetchArt = settings.albumArt &&
                    settings.albumArtFromNowPlaying &&
                    trackPlaying.artUrlState == PlayingTrackInfo.ArtUrlState.CanFetch
        )
    }

    fun paused(hash: Int) {
        pausedHash.value = hash
    }

    fun clearPaused(hash: Int) {
        if (pausedHash.value == hash)
            pausedHash.value = null
    }

    private fun String.clamp(min: Int, max: Int): String {
        require(min <= max) { "min ($min) must be <= max ($max)" }
        return when {
            length < min -> padEnd(min)
            length > max -> take(max)
            else -> this
        }
    }

    private fun formatLine(
        template: String,
        trackPlaying: PlayingTrackNotifyEvent.TrackPlaying,
        appName: String,
        showLoved: Boolean,
    ): String {
        return template.replace(placeholderRegex) { match ->
            when (match.groupValues.getOrNull(1)) {
                DiscordRpcPlaceholder.artist.name -> trackPlaying.scrobbleData.artist
                DiscordRpcPlaceholder.title.name -> (if (trackPlaying.userLoved && showLoved) "❤ " else "") +
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