package com.arn.scrobble.webview.setting

/**
 * Created By Kevin Zou On 2023/9/20
 */
sealed class PlatformWebSettings {
    data class AndroidWebSettings(
        /**
         * whether the WebView should support zooming using its on-screen zoom
         * controls and gestures. The particular zoom mechanisms that should be used
         * can be set with {@link #setBuiltInZoomControls}. This setting does not
         * affect zooming performed using the {@link WebView#zoomIn()} and
         * {@link WebView#zoomOut()} methods. The default is {@code true}.
         *
         * @param support whether the WebView should support zoom
         */
        var supportZoom: Boolean = true,

        /**
         * Enables or disables file access within WebView.
         * Note that this enables or disables file system access only. Assets and resources
         * are still accessible using file:///android_asset and file:///android_res.
         * <p class="note">
         * <b>Note:</b> Apps should not open {@code file://} URLs from any external source in
         * WebView, don't enable this if your app accepts arbitrary URLs from external sources.
         * It's recommended to always use
         * <a href="{@docRoot}reference/androidx/webkit/WebViewAssetLoader">
         * androidx.webkit.WebViewAssetLoader</a> to access files including assets and resources over
         * {@code http(s)://} schemes, instead of {@code file://} URLs. To prevent possible security
         * issues targeting {@link android.os.Build.VERSION_CODES#Q} and earlier, you should explicitly
         * set this value to {@code false}.
         * <p>
         * The default value is {@code true} for apps targeting
         * {@link android.os.Build.VERSION_CODES#Q} and below, and {@code false} when targeting
         * {@link android.os.Build.VERSION_CODES#R} and above.
         */
        var allowFileAccess: Boolean = false,

        /**
         * The text zoom of the page in percent. The default is 100.
         *
         * @param textZoom the text zoom in percent
         */
        var textZoom: Int = 100,

        /**
         * Whether the WebView should enable support for the &quot;viewport&quot;
         * HTML meta tag or should use a wide viewport.
         * When the value of the setting is {@code false}, the layout width is always set to the
         * width of the WebView control in device-independent (CSS) pixels.
         * When the value is {@code true} and the page contains the viewport meta tag, the value
         * of the width specified in the tag is used. If the page does not contain the tag or
         * does not provide a width, then a wide viewport will be used.
         *
         */
        var useWideViewPort: Boolean = false,

        /**
         * The standard font family name. The default is "sans-serif".
         *
         * @param font a font family name
         */
        var standardFontFamily: String = "sans-serif",

        /**
         * The default font size. The default is 16.
         *
         * @param size a non-negative integer between 1 and 72. Any number outside
         *             the specified range will be pinned.
         */
        var defaultFontSize: Int = 16,

        /**
         * Sets whether the WebView should load image resources. Note that this method
         * controls loading of all images, including those embedded using the data
         * URI scheme. Use {@link #setBlockNetworkImage} to control loading only
         * of images specified using network URI schemes. Note that if the value of this
         * setting is changed from {@code false} to {@code true}, all images resources referenced
         * by content currently displayed by the WebView are loaded automatically.
         * The default is {@code true}.
         *
         * @param flag whether the WebView should load image resources
         */
        var loadsImagesAutomatically: Boolean = true,

        /**
         * Control whether algorithmic darkening is allowed.
         *
         * <p class="note">
         * <b>Note:</b> This API and the behaviour described only apply to apps with
         * {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU}.
         *
         * <p>
         * WebView always sets the media query {@code prefers-color-scheme} according to the app's
         * theme attribute {@link android.R.styleable#Theme_isLightTheme isLightTheme}, i.e.
         * {@code prefers-color-scheme} is {@code light} if isLightTheme is true or not specified,
         * otherwise it is {@code dark}. This means that the web content's light or dark style will
         * be applied automatically to match the app's theme if the content supports it.
         *
         * <p>
         * Algorithmic darkening is disallowed by default.
         * <p>
         * If the app's theme is dark and it allows algorithmic darkening, WebView will attempt to
         * darken web content using an algorithm, if the content doesn't define its own dark styles
         * and doesn't explicitly disable darkening.
         *
         * <p>
         * If Android is applying Force Dark to WebView then WebView will ignore the value of
         * this setting and behave as if it were set to true.
         *
         * <p>
         * The deprecated {@link #setForceDark} and related API are no-ops in apps with
         * {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU},
         * but they still apply to apps with
         * {@code targetSdkVersion} &lt; {@link android.os.Build.VERSION_CODES#TIRAMISU}.
         *
         * <p>
         * The below table summarizes how APIs work with different apps.
         *
         * <table border="2" width="85%" align="center" cellpadding="5">
         *     <thead>
         *         <tr>
         *             <th>App</th>
         *             <th>Web content which uses {@code prefers-color-scheme}</th>
         *             <th>Web content which does not use {@code prefers-color-scheme}</th>
         *         </tr>
         *     </thead>
         *     <tbody>
         *     <tr>
         *         <td>App with {@code isLightTheme} True or not set</td>
         *         <td>Renders with the light theme defined by the content author.</td>
         *         <td>Renders with the default styling defined by the content author.</td>
         *     </tr>
         *     <tr>
         *         <td>App with Android forceDark in effect</td>
         *         <td>Renders with the dark theme defined by the content author.</td>
         *         <td>Renders with the styling modified to dark colors by an algorithm
         *             if allowed by the content author.</td>
         *     </tr>
         *     <tr>
         *         <td>App with {@code isLightTheme} False,
         *            {@code targetSdkVersion} &lt; {@link android.os.Build.VERSION_CODES#TIRAMISU},
         *             and has {@code FORCE_DARK_AUTO}</td>
         *         <td>Renders with the dark theme defined by the content author.</td>
         *         <td>Renders with the default styling defined by the content author.</td>
         *     </tr>
         *     <tr>
         *         <td>App with {@code isLightTheme} False,
         *            {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU},
         *             and {@code setAlgorithmicDarkening(false)}</td>
         *         <td>Renders with the dark theme defined by the content author.</td>
         *         <td>Renders with the default styling defined by the content author.</td>
         *     </tr>
         *     <tr>
         *         <td>App with {@code isLightTheme} False,
         *            {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU},
         *             and {@code setAlgorithmicDarkening(true)}</td>
         *         <td>Renders with the dark theme defined by the content author.</td>
         *         <td>Renders with the styling modified to dark colors by an algorithm if allowed
         *             by the content author.</td>
         *     </tr>
         *     </tbody>
         * </table>
         * </p>
         *
         */
        var isAlgorithmicDarkeningAllowed: Boolean = false,
        /**
         * whether Safe Browsing is enabled. Safe Browsing allows WebView to
         * protect against malware and phishing attacks by verifying the links.
         */
        var safeBrowsingEnabled: Boolean = true,
    ) : PlatformWebSettings()

    data object DesktopWebSettings : PlatformWebSettings()

    data object IOSWebSettings : PlatformWebSettings()
}