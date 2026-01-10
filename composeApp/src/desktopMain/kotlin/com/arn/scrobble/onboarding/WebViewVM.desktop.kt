package com.arn.scrobble.onboarding

import co.touchlab.kermit.Logger
import com.arn.scrobble.DesktopWebView

actual fun WebViewVM.platformClear() {
    if (DesktopWebView.inited)
        DesktopWebView.deleteAndQuitP()
}

actual fun WebViewVM.platformInit() {
    try {
        DesktopWebView.load()
        DesktopWebView.init()
        DesktopWebView.setCallbackFlow(callbackUrlAndCookies)
    } catch (e: UnsatisfiedLinkError) {
        Logger.e(e) { "WebView unavailable" }
        loginState.value = WebViewLoginState.Unavailable
    }
}