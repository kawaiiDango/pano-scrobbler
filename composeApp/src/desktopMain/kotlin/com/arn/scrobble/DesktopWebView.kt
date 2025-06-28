package com.arn.scrobble

import com.arn.scrobble.onboarding.WebViewEventFlows
import com.arn.scrobble.utils.DesktopStuff

object DesktopWebView {

    private var inited = false

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
        }.start()
    }

    // jni callbacks

    @JvmStatic
    fun onWebViewCookies(url: String, cookies: Array<String>) {
        WebViewEventFlows.cookies.tryEmit(url to cookies)
    }

    @JvmStatic
    fun onWebViewUrlLoaded(url: String) {
        WebViewEventFlows.pageLoaded.tryEmit(url)
    }


    @JvmStatic
    private external fun startEventLoop()

    @JvmStatic
    external fun launchWebView(url: String, callbackPrefix: String, dataDir: String)

    @JvmStatic
    external fun getWebViewCookiesFor(url: String)

    @JvmStatic
    external fun quitWebView()
}