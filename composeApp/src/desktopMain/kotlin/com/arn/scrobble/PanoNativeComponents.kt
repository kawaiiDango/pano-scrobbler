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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Keep
object PanoNativeComponents {
    var desktopMediaListener: DesktopMediaListener? = null
    val onFilePickedFlow = MutableSharedFlow<Pair<Int, String>>(extraBufferCapacity = 1)
    val onDarkModeChangeFlow = MutableStateFlow<Boolean>(true)

    @Suppress("UnsafeDynamicallyLoadedCode")
    fun load() {
        System.load(DesktopStuff.getLibraryPath("pano_native_components"))
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
            startListeningMedia()
            Logger.i("startListeningMediaInThread finished")
        }.apply {
            name = "MediaListenerThread"
        }
            .start()
    }


    // jni callbacks


    @JvmStatic
    fun onActiveSessionsChanged(appIds: Array<String>, appNames: Array<String>) {
        val sessionInfos = appIds.zip(appNames).map { (appId, appName) ->
            SessionInfo(
                appId = appId,
                appName = appName,
            )
        }

        desktopMediaListener?.platformActiveSessionsChanged(sessionInfos)
    }

    @JvmStatic
    fun onMetadataChanged(
        appId: String,
        trackId: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        trackNumber: Int,
        duration: Long,
        artUrl: String,
        artBytes: ByteArray,
    ) {
        val metadataInfo = MetadataInfo(
            appId = appId,
            trackId = trackId, // always empty for windows
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            trackNumber = trackNumber,
            duration = duration,
            artUrl = artUrl.takeIf { it.startsWith("https://") }.orEmpty(),
        )

        desktopMediaListener?.platformMetadataChanged(metadataInfo)
    }

    @JvmStatic
    fun onPlaybackStateChanged(appId: String, state: String, position: Long, canSkip: Boolean) {
        val playbackInfo = PlaybackInfo(
            appId = appId,
            state = CommonPlaybackState.valueOf(state),
            position = position,
            canSkip = canSkip
        )
        desktopMediaListener?.platformPlaybackStateChanged(playbackInfo)
    }

    @JvmStatic
    fun onTrayMenuItemClicked(id: String) {
        PanoTrayUtils.onTrayMenuItemClickedFn(id)
    }

    @JvmStatic
    fun onReceiveIpcCommand(command: String, arg: String) {
        if (command == Automation.DESKTOP_FOCUS_EXISTING) {
            PanoTrayUtils.onTrayMenuItemClickedFn(PanoTrayUtils.ItemId.Open.name)
            PanoNativeComponents.notify(
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

    // external

    @JvmStatic
    external fun stopListeningMedia()

    @JvmStatic
    external fun setAllowedAppIds(appIds: Array<String>)

    @JvmStatic
    private external fun startListeningMedia()

    @JvmStatic
    external fun ping(input: String): String

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
        state: String,
        details: String,
        largeText: String,
        startTime: Long,
        endTime: Long,
        artUrl: String,
        isPlaying: Boolean,
        statusLine: Int,
        buttonTexts: Array<String>,
        buttonUrls: Array<String>,
    ): Boolean

    @JvmStatic
    external fun clearDiscordActivity(): Boolean

    @JvmStatic
    external fun stopDiscordActivity(): Boolean
}