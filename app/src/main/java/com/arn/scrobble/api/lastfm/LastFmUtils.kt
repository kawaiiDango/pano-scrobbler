package com.arn.scrobble.api.lastfm

import timber.log.Timber

open class ApiException(
    val code: Int,
    val description: String,
    override val cause: Throwable? = null
) : Exception("$description ($code)", cause) {
    init {
        // report rate limit errors to crashlytics
        if (code in arrayOf(29, 9, 429)) {
            Timber.w(this)
            // todo fix warning
        }
    }
}

class ScrobbleIgnoredException(
    val scrobbleTime: Long,
    val altAction: () -> Unit
) : ApiException(-1, "Scrobble ignored")

