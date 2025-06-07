package com.arn.scrobble

import androidx.annotation.Keep
import co.touchlab.kermit.Logger
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.media.DesktopMediaListener
import com.arn.scrobble.media.MetadataInfo
import com.arn.scrobble.media.PlaybackInfo
import com.arn.scrobble.media.ScrobbleQueue
import com.arn.scrobble.media.SessionInfo
import com.arn.scrobble.media.listenForPlayingTrackEvents
import com.arn.scrobble.onboarding.WebViewEvent
import com.arn.scrobble.onboarding.WebViewEventFlows
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PanoTrayUtils
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Keep
object PanoNativeComponents {
    var desktopMediaListener: DesktopMediaListener? = null
    private const val TAG = "pano_native_components"

    fun load() {
        System.loadLibrary("pano_native_components")
    }

    fun init() {
        val scrobbleQueue = ScrobbleQueue(GlobalScope)
        desktopMediaListener = DesktopMediaListener(
            GlobalScope,
            scrobbleQueue
        )

        Thread {
            startEventLoop()
        }.start()

        desktopMediaListener!!.start()

        GlobalScope.launch {
            listenForPlayingTrackEvents(scrobbleQueue, desktopMediaListener!!)
        }
    }

    fun startListeningMediaInThread() {
        Thread {
            startListeningMedia()
            Logger.i("startListeningMediaInThread finished")
        }.start()
    }


    // jni callbacks


    @JvmStatic
    fun onActiveSessionsChanged(json: String) {
        val sessionInfos = Stuff.myJson.decodeFromString<List<SessionInfo>>(json)
        desktopMediaListener?.platformActiveSessionsChanged(sessionInfos)
    }

    @JvmStatic
    fun onMetadataChanged(json: String) {
        val metadataInfo = Stuff.myJson.decodeFromString<MetadataInfo>(json)
        desktopMediaListener?.platformMetadataChanged(metadataInfo)
    }

    @JvmStatic
    fun onPlaybackStateChanged(json: String) {
        val playbackInfo = Stuff.myJson.decodeFromString<PlaybackInfo>(json)
        desktopMediaListener?.platformPlaybackStateChanged(playbackInfo)
    }

    @JvmStatic
    fun onTrayMenuItemClicked(id: String) {
        PanoTrayUtils.onTrayMenuItemClickedFn(id)
    }

    @JvmStatic
    fun onWebViewCookies(event: String) {
        val event = Stuff.myJson.decodeFromString<WebViewEvent>(event)
        GlobalScope.launch {
            WebViewEventFlows.event.emit(event)
        }
    }

    @JvmStatic
    fun onWebViewPageLoad(url: String) {
        GlobalScope.launch {
            WebViewEventFlows.pageLoaded.emit(url)
        }
    }

    @JvmStatic
    fun onLogInfo(msg: String) {
        Logger.i(msg, tag = TAG)
    }

    @JvmStatic
    fun onLogWarn(msg: String) {
        Logger.w(msg, tag = TAG)
    }

    @JvmStatic
    fun onReceiveAutomationCommand(command: String, arg: String) {
        val wasSuccessFul = Automation.executeAction(command, arg.ifEmpty { null }, null)
        if (!wasSuccessFul) {
            Logger.w("command '$command' failed")
        }
    }


    // external

    @JvmStatic
    external fun stopListeningMedia()

    @JvmStatic
    external fun setAllowedAppIds(appIds: Array<String>)

    @JvmStatic
    private external fun startEventLoop()

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
    external fun isSingleInstance(): Boolean

    @JvmStatic
    external fun applyDarkModeToWindow(handle: Long)

    @JvmStatic
    external fun launchWebView(url: String, callbackPrefix: String, dataDir: String)

    @JvmStatic
    external fun getWebViewCookiesFor(url: String)

    @JvmStatic
    external fun quitWebView()

    @JvmStatic
    external fun sendAutomationCommand(command: String, arg: String)
}