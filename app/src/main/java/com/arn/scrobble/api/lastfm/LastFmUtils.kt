package com.arn.scrobble.api.lastfm

class FmException(
    val code: Int,
    override val message: String
) : Exception(message)


