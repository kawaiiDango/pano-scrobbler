package com.arn.scrobble.onboarding

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable

@Serializable
data class WebViewEvent(
    val url: String,
    val cookies: List<String>,
)

object WebViewEventFlows {
    val event = MutableSharedFlow<WebViewEvent>()
    val pageLoaded = MutableSharedFlow<String>()
}
