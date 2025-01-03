package com.arn.scrobble.webview.web

import co.touchlab.kermit.Logger
import javafx.concurrent.Worker.State.CANCELLED
import javafx.concurrent.Worker.State.FAILED
import javafx.concurrent.Worker.State.READY
import javafx.concurrent.Worker.State.RUNNING
import javafx.concurrent.Worker.State.SCHEDULED
import javafx.concurrent.Worker.State.SUCCEEDED
import javafx.scene.web.WebEngine

/**
 * Created By Kevin Zou On 2023/9/12
 */
internal fun WebEngine.getCurrentUrl(): String? {
    if (history.entries.size <= 0) return null
    return history.entries[history.currentIndex].url
}

internal fun WebEngine.stopLoading() {
    loadWorker.cancel()
}

internal fun WebEngine.goForward() {
    if (canGoForward()) {
        history.go(1)
    }
}

internal fun WebEngine.goBack() {
    if (canGoBack()) {
        history.go(-1)
    }
}

internal fun WebEngine.canGoBack(): Boolean {
    return history.maxSize > 0 && history.currentIndex != 0
}

internal fun WebEngine.canGoForward(): Boolean {
    return history.maxSize > 0 && history.currentIndex != history.maxSize - 1
}

internal fun WebEngine.addLoadListener(state: WebViewState, navigator: WebViewNavigator) {
    titleProperty().addListener { _, _, newValue ->
        Logger.d { "titleProperty: $newValue" }
        state.pageTitle = newValue
    }

    loadWorker.stateProperty().addListener { _, _, newValue ->
        when (newValue) {

            READY, SCHEDULED -> {
                Logger.d { "READY or SCHEDULED" }
                state.loadingState = LoadingState.Initializing
                state.errorsForCurrentRequest.clear()
            }

            RUNNING -> {
                state.loadingState = LoadingState.Loading(0f)
            }

            SUCCEEDED -> {
                Logger.d { "SUCCEEDED ${getCurrentUrl()}" }
                navigator.canGoBack = canGoBack()
                navigator.canGoForward = canGoForward()
                state.loadingState = LoadingState.Finished
                state.lastLoadedUrl = getCurrentUrl()
            }

            FAILED, CANCELLED -> {
                state.loadingState = LoadingState.Finished
                state.errorsForCurrentRequest.add(
                    WebViewError(
                        code = 404,
                        description = "Failed to load url: ${getCurrentUrl()}"
                    )
                )
            }
        }
    }

    loadWorker.progressProperty().addListener { _, _, newValue ->
        if (newValue.toFloat() < 0f) return@addListener
        state.loadingState = LoadingState.Loading(newValue.toFloat())
    }

    history.currentIndexProperty().addListener { _, _, _ ->
        val currentUrl = getCurrentUrl()
        if (currentUrl != null) {
            state.lastLoadedUrl = currentUrl
        }
    }
}