package com.arn.scrobble.onboarding

import kotlinx.coroutines.flow.MutableSharedFlow


object WebViewEventFlows {
    val cookies = MutableSharedFlow<Pair<String, Array<String>>>(extraBufferCapacity = 1)
    val pageLoaded = MutableSharedFlow<String>(extraBufferCapacity = 1)
}
