package com.arn.scrobble.onboarding

import android.webkit.CookieManager

actual fun WebViewVM.platformClear() {
    CookieManager.getInstance().removeAllCookies(null)
}

actual fun WebViewVM.platformInit() {
}