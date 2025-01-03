package com.arn.scrobble

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform