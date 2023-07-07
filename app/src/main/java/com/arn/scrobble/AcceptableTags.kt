package com.arn.scrobble

object AcceptableTags {

    private val tagFragments by lazy {
        App.context.resources
            .openRawResource(R.raw.everynoise_genres)
            .bufferedReader()
            .readLines()
            .toSet()
    }

    fun isAcceptable(lastfmTag: String) =
        lastfmTag.isNotEmpty() &&
                lastfmTag.lowercase().split(" ", "-").any { it in tagFragments }
}

