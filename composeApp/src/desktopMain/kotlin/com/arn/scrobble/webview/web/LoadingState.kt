package com.arn.scrobble.webview.web

/**
 * Created By Kevin Zou On 2023/9/5
 * Sealed class for constraining possible loading states.
 * See [Loading] and [Finished].
 */
sealed class LoadingState {
    /**
     * Describes a WebView that has not yet loaded for the first time.
     */
    data object Initializing : LoadingState()

    /**
     * Describes a webview between `onPageStarted` and `onPageFinished` events, contains a
     * [progress] property which is updated by the webview.
     */
    data class Loading(val progress: Float) : LoadingState()

    /**
     * Describes a webview that has finished loading content.
     */
    data object Finished : LoadingState()
}