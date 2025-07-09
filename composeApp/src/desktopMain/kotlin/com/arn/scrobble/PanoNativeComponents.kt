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
import kotlinx.coroutines.launch

@Keep
object PanoNativeComponents {
    var desktopMediaListener: DesktopMediaListener? = null

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
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            trackNumber = trackNumber,
            duration = duration
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
        } else {
            val wasSuccessFul = Automation.executeAction(command, arg.ifEmpty { null }, null)
            if (!wasSuccessFul) {
                Logger.w("command '$command' failed")
            }
        }
    }


    // external

    @JvmStatic
    external fun stopListeningMedia()

    @JvmStatic
    external fun setAllowedAppIds(appIds: Array<String>)

    @JvmStatic
    external fun albumArtEnabled(enabled: Boolean)

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
    external fun notify(
        title: String,
        body: String,
        iconPath: String = DesktopStuff.iconPath ?: "",
    )

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

    external fun sendIpcCommand(command: String, arg: String): Boolean

    @JvmStatic
    external fun getSystemLocale(): String

    @JvmStatic
    external fun isFileLocked(path: String): Boolean

}