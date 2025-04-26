package com.arn.scrobble

import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.launch

class PanoNativeComponents(
    private val _onActiveSessionsChanged: (List<SessionInfo>) -> Unit,
    private val _onMetadataChanged: (MetadataInfo) -> Unit,
    private val _onPlaybackStateChanged: (PlaybackInfo) -> Unit,
    private val _onTrayMenuItemClicked: (String) -> Unit,
) {

    fun onLogInfo(msg: String) {
        Logger.i(msg, tag = TAG)
    }

    fun onLogWarn(msg: String) {
        Logger.w(msg, tag = TAG)
    }

    fun onActiveSessionsChanged(json: String) {
        val sessionInfos = Stuff.myJson.decodeFromString<List<SessionInfo>>(json)
        _onActiveSessionsChanged(sessionInfos)
    }

    fun onMetadataChanged(json: String) {
        val metadataInfo = Stuff.myJson.decodeFromString<MetadataInfo>(json)
        _onMetadataChanged(metadataInfo)
    }

    fun onPlaybackStateChanged(json: String) {
        val playbackInfo = Stuff.myJson.decodeFromString<PlaybackInfo>(json)
        _onPlaybackStateChanged(playbackInfo)
    }

    fun onTrayMenuItemClicked(id: String) {
        _onTrayMenuItemClicked(id)
    }

    companion object {
        private const val TAG = "native_components"

        fun load() {
            System.loadLibrary("native_components")
        }

        fun init() {
            System.loadLibrary("native_components")

            val scrobbleQueue = ScrobbleQueue(GlobalScope)
            val desktopMediaListener = DesktopMediaListener(
                GlobalScope,
                scrobbleQueue
            )
            val instance = PanoNativeComponents(
                _onActiveSessionsChanged = desktopMediaListener::platformActiveSessionsChanged,
                _onMetadataChanged = desktopMediaListener::platformMetadataChanged,
                _onPlaybackStateChanged = desktopMediaListener::platformPlaybackStateChanged,
                _onTrayMenuItemClicked = PanoTrayUtils::onTrayMenuItemClickedFn
            )

            Thread {
                startEventLoop(instance)
            }.start()
            
            desktopMediaListener.start()

            GlobalScope.launch {
                listenForPlayingTrackEvents(scrobbleQueue, desktopMediaListener)
            }
        }

        fun startListeningMediaInThread() {
            Thread {
                startListeningMedia()
                Logger.i("startListeningMediaInThread finished")
            }.start()
        }

        @JvmStatic
        external fun stopListeningMedia()

        @JvmStatic
        external fun setAllowedAppIds(appIds: Array<String>)

        @JvmStatic
        private external fun startEventLoop(callback: PanoNativeComponents)

        @JvmStatic
        external fun stopEventLoop()

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
            iconPath: String = DesktopStuff.iconPath
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
        external fun addRemoveStartupWin(exePath: String, add: Boolean): Boolean

        @JvmStatic
        external fun isAddedToStartupWin(exePath: String): Boolean

        @JvmStatic
        external fun setEnvironmentVariable(key: String, value: String)
    }
}