package com.arn.scrobble.crashreporter


abstract class BaseCrashReporter {
    open val isAvailable: Boolean = false

    open fun config(
        keysMap: Map<String, String> = emptyMap(),
    ) {
    }
}