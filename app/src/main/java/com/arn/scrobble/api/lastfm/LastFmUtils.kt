package com.arn.scrobble.api.lastfm

class FmException(
    val code: Int,
    override val message: String
) : Exception(message)

class ScrobbleIgnoredException(
    val scrobbleTimeSecs: Int,
    val altAction: () -> Unit
) : Exception("Scrobble ignored")

