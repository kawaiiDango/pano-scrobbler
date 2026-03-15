package com.arn.scrobble.api.lastfm

import androidx.annotation.Keep

@Keep
open class ApiException(
    val code: Int,
    val description: String,
    override val cause: Throwable? = null,
) : Exception(
    description + (if (code != -1) " ($code)" else ""),
    cause
)

class ScrobbleIgnoredException(
    val scrobbleTime: Long,
) : ApiException(-1, "Scrobble ignored")

