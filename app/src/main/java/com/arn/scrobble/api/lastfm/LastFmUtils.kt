package com.arn.scrobble.api.lastfm

open class ApiException(
    val code: Int,
    val description: String,
    override val cause: Throwable? = null
) : Exception("$description ($code)", cause)

class ScrobbleIgnoredException(
    val scrobbleTime: Long,
    val altAction: () -> Unit
) : ApiException(-1, "Scrobble ignored")

