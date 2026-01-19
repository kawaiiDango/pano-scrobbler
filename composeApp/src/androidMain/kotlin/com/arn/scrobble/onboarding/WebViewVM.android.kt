package com.arn.scrobble.onboarding

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebView
import co.touchlab.kermit.Logger

actual fun WebViewVM.platformClear() {
    try {
        CookieManager.getInstance().removeAllCookies(null)
    } catch (e: Exception) {
        Logger.e { "WebView unavailable" }
    }
}

actual fun WebViewVM.platformInit() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        WebView.getCurrentWebViewPackage() == null
    ) {
        Logger.e { "WebView unavailable" }
        loginState.value = WebViewLoginState.Unavailable
    }
}