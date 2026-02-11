package com.arn.scrobble

import androidx.annotation.Keep
import co.touchlab.kermit.Logger
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.media.CommonPlaybackState
import com.arn.scrobble.media.DesktopMediaListener
import com.arn.scrobble.media.MetadataInfo
import com.arn.scrobble.media.PlaybackInfo
import com.arn.scrobble.media.ScrobbleQueue
import com.arn.scrobble.media.SessionInfo
import com.arn.scrobble.media.listenForPlayingTrackEvents
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PanoTrayUtils
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Keep
object PanoNativeComponents {
    private var desktopMediaListener: DesktopMediaListener? = null
    val onFilePickedFlow = MutableSharedFlow<Pair<Int, String>>(extraBufferCapacity = 1)
    val onDarkModeChangeFlow = MutableStateFlow(true)
    var isMediaListenerRunning = false
        private set

    @Suppress("UnsafeDynamicallyLoadedCode")
    fun load() {
        System.load(DesktopStuff.getLibraryPath("pano_native_components"))

        val logFilePath = DesktopStuff.logsDir.resolve("pano-native-components.log")
        setLogFilePath(logFilePath.absolutePath)
    }

    fun init() {
        val scrobbleQueue = ScrobbleQueue(GlobalScope)
        desktopMediaListener = DesktopMediaListener(
            GlobalScope,
            scrobbleQueue
        )

        desktopMediaListener!!.start()

        GlobalScope.launch {
            listenForPlayingTrackEvents(scrobbleQueue, desktopMediaListener!!)
        }
    }

    fun startListeningMediaInThread() {
        Thread {
            isMediaListenerRunning = true
            startListeningMedia()
            isMediaListenerRunning = false
            Logger.i("startListeningMediaInThread finished")
        }.apply {
            name = "MediaListenerThread"
        }
            .start()
    }


    // jni callbacks


    @JvmStatic
    fun onActiveSessionsChanged(uniqueAppIds: Array<String>, appNames: Array<String>) {
        val sessionInfos = uniqueAppIds
            .zip(appNames)
            .map { (uniqueAppId, appName) ->
                SessionInfo(
                    rawAppId = uniqueAppId,
                    appName = appName,
                )
            }

        desktopMediaListener?.platformActiveSessionsChanged(sessionInfos)
    }

    @JvmStatic
    fun onMetadataChanged(
        uniqueAppId: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        trackNumber: Int,
        duration: Long,
        artUrl: String,
        trackUrl: String,
    ) {
        val artUrl = artUrl.ifEmpty { null }
            ?.takeIf { it.toHttpUrlOrNull()?.topPrivateDomain() != null }
        val normalizedUrlHost = trackUrl.ifEmpty { null }
            ?.toHttpUrlOrNull()
            ?.let {
                if (it.host in Stuff.mprisUrlSubdomains)
                    it.host
                else
                    it.topPrivateDomain()
            }

        val metadataInfo = MetadataInfo(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            trackNumber = trackNumber,
            duration = duration,

            // always null for windows
            artUrl = artUrl,
            normalizedUrlHost = normalizedUrlHost,
        )

        desktopMediaListener?.platformMetadataChanged(uniqueAppId, metadataInfo)
    }

    @JvmStatic
    fun onPlaybackStateChanged(
        uniqueAppId: String,
        state: String,
        position: Long,
        canSkip: Boolean
    ) {
        val playbackInfo = PlaybackInfo(
            state = CommonPlaybackState.valueOf(state),
            position = position,
            canSkip = canSkip
        )
        desktopMediaListener?.platformPlaybackStateChanged(uniqueAppId, playbackInfo)
    }

    @JvmStatic
    fun onTrayMenuItemClicked(id: String) {
        PanoTrayUtils.onTrayMenuItemClickedFn(id)
    }

    @JvmStatic
    fun onReceiveIpcCommand(command: String, arg: String) {
        if (command == Automation.DESKTOP_FOCUS_EXISTING) {
            PanoTrayUtils.onTrayMenuItemClickedFn(PanoTrayUtils.ItemId.Open.name)
            notify(
                "Already running",
                "Please close the existing instance before starting a new one."
            )
        } else {
            val wasSuccessful = Automation.executeAction(command, arg.ifEmpty { null }, null)
            if (!wasSuccessful) {
                Logger.w("command '$command' failed")
            }
        }
    }

    @JvmStatic
    fun onFilePicked(requestId: Int, uri: String) {
        onFilePickedFlow.tryEmit(requestId to uri)
    }

    @JvmStatic
    fun onDarkModeChange(isDarkMode: Boolean) {
        onDarkModeChangeFlow.value = isDarkMode
    }

    @JvmStatic
    fun isAppIdAllowed(appId: String): Boolean {
        return desktopMediaListener?.shouldScrobble(appId) == true
    }

    // external

    @JvmStatic
    private external fun setLogFilePath(path: String)

    @JvmStatic
    external fun stopListeningMedia()

    @JvmStatic
    external fun refreshSessions()

    @JvmStatic
    private external fun startListeningMedia()

    @JvmStatic
    external fun skip(appId: String)

    @JvmStatic
    external fun mute(appId: String)

    @JvmStatic
    external fun unmute(appId: String)

    @JvmStatic
    external fun notify(title: String, body: String)

    @JvmStatic
    external fun setTray(
        tooltip: String,
        argb: IntArray,
        iconSize: Int,
        menuItemIds: Array<String>,
        menuItemTexts: Array<String>
    )

    @JvmStatic
    external fun getMachineId(): String

    @JvmStatic
    external fun setEnvironmentVariable(key: String, value: String)

    @JvmStatic
    external fun applyDarkModeToWindow(handle: Long)

    @JvmStatic
    external fun sendIpcCommand(command: String, arg: String): Boolean

    @JvmStatic
    external fun isFileLocked(path: String): Boolean

    @JvmStatic
    external fun xdgFileChooser(
        requestId: Int,
        save: Boolean,
        title: String,
        fileName: String,
        filters: Array<String>
    )

    @JvmStatic
    external fun updateDiscordActivity(
        clientId: String,
        name: String,
        state: String,
        details: String,
        largeText: String,
        startTime: Long,
        endTime: Long,
        artUrl: String,
        detailsUrl: String,
        isPlaying: Boolean,
        statusLine: Int,
        buttonText: String,
        buttonUrl: String,
    ): Boolean

    @JvmStatic
    external fun clearDiscordActivity(shutdown: Boolean): Boolean
}