package com.arn.scrobble.utils

import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R

object AcceptableTags {

    private val tagFragments by lazy {
        PlatformStuff.application.resources
            .openRawResource(R.raw.everynoise_genres)
            .bufferedReader()
            .readLines()
            .toSet()
    }

    fun isAcceptable(lastfmTag: String, hiddenTags: Set<String>): Boolean {
        val lastfmTagLower = lastfmTag.lowercase()
        return lastfmTagLower.isNotEmpty() &&
                lastfmTagLower.split(" ", "-").any { it in tagFragments } &&
                lastfmTagLower !in hiddenTags
    }
}

