package com.arn.scrobble.webview.setting

/**
 * Created By Kevin Zou On 2023/9/20
 */
class WebSettings {
    var isJavaScriptEnabled = false

    /**
     * Android platform specific settings
     */
    val androidWebSettings = PlatformWebSettings.AndroidWebSettings()

    /**
     * Desktop platform specific settings
     */
    val desktopWebSettings = PlatformWebSettings.DesktopWebSettings

    /**
     * iOS platform specific settings
     */
    val iOSWebSettings = PlatformWebSettings.IOSWebSettings

}

