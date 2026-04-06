package com.arn.scrobble.onboarding

import android.webkit.CookieManager
import androidx.webkit.WebViewCompat
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.utils.AndroidStuff

actual fun WebViewVM.platformClear() {
    try {
        CookieManager.getInstance().removeAllCookies(null)
    } catch (e: Exception) {
        Logger.e { "WebView unavailable" }
    }
}

actual fun WebViewVM.platformInit() {
    if (WebViewCompat.getCurrentWebViewPackage(AndroidStuff.applicationContext) == null) {
        Logger.e { "WebView unavailable" }
        loginState.value = WebViewLoginState.Unavailable
    } else {
        val proxy = Requesters.proxy.value
        if (proxy.enabled && !proxy.hasAuth)
            WebViewProxyOverride.setSocksProxy(proxy.host, proxy.port)
        else if (proxy.enabled && proxy.hasAuth) {
            val tunnelPort = startProxyRelay(proxy)

            WebViewProxyOverride.setSocksProxy("127.0.0.1", tunnelPort)
        } else
            WebViewProxyOverride.clearProxy()
    }
}