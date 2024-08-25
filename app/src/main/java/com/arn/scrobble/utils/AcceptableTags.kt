package com.arn.scrobble.utils

import com.arn.scrobble.R
import com.arn.scrobble.main.App

object AcceptableTags {

    private val tagFragments by lazy {
        App.application.resources
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

