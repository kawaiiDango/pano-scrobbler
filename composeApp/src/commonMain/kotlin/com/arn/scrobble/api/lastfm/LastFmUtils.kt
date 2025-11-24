package com.arn.scrobble.api.lastfm

import androidx.annotation.Keep
import co.touchlab.kermit.Logger

@Keep
open class ApiException(
    val code: Int,
    val description: String,
    override val cause: Throwable? = null,
) : Exception(
    description + (if (code != -1) " ($code)" else ""),
    cause
) {

    init {
        reportRateLimitErrors()
    }

    private fun reportRateLimitErrors() {
        // report rate limit errors to crashlytics
        if (code in arrayOf(29, 9, 429)) {
            Logger.w(cause ?: this) { description }
        }
    }
}

class ScrobbleIgnoredException(
    val scrobbleTime: Long,
) : ApiException(-1, "Scrobble ignored")

