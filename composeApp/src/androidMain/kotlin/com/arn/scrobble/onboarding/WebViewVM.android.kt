package com.arn.scrobble.onboarding

import android.webkit.CookieManager
import androidx.webkit.WebViewCompat
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.pref.MainPrefs
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
        when (proxy.type) {
            MainPrefs.ProxySettings.Type.SOCKS5 if !proxy.hasAuth ->
                WebViewProxyOverride.setProxy(true, proxy.host, proxy.port)

            MainPrefs.ProxySettings.Type.SOCKS5 if proxy.hasAuth ->
                WebViewProxyOverride.setProxy(true, "127.0.0.1", startProxyRelay(proxy))

            MainPrefs.ProxySettings.Type.HTTP ->
                WebViewProxyOverride.setProxy(false, proxy.host, proxy.port)

            else ->
                WebViewProxyOverride.clearProxy()
        }
    }
}