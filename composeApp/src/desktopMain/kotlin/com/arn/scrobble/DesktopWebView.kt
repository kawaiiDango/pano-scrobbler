package com.arn.scrobble

import com.arn.scrobble.utils.DesktopStuff
import kotlinx.coroutines.flow.MutableSharedFlow

object DesktopWebView {

    var inited = false
        private set
    private var callbackUrlAndCookies: MutableSharedFlow<Pair<String, Map<String, String>>>? = null

    @Suppress("UnsafeDynamicallyLoadedCode")
    fun load() {
        System.load(DesktopStuff.getLibraryPath("native_webview"))
    }

    fun init() {
        if (inited) return
        inited = true
        // Start the event loop in a separate thread
        Thread {
            startEventLoop()
        }.apply {
            name = "WebviewEventLoopThread"
        }
            .start()
    }

    fun setCallbackFlow(flow: MutableSharedFlow<Pair<String, Map<String, String>>>) {
        callbackUrlAndCookies = flow
    }

    fun deleteAndQuitP() {
        callbackUrlAndCookies = null
        deleteAndQuit()
    }

    // jni callbacks

    @JvmStatic
    fun onCallback(url: String, cookies: Array<String>) {
        val cookiesMap = cookies.associate {
            val (name, value) = it.split("=", limit = 2)
            name to value
        }
        callbackUrlAndCookies?.tryEmit(url to cookiesMap)
    }

    @JvmStatic
    private external fun startEventLoop()

    @JvmStatic
    external fun launchWebView(
        url: String,
        callbackPrefix: String,
        cookiesUrl: String,
        dataDir: String
    )

    @JvmStatic
    private external fun deleteAndQuit()
}