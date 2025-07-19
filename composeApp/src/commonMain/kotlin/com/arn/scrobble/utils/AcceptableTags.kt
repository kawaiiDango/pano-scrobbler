package com.arn.scrobble.utils

import kotlinx.coroutines.runBlocking
import pano_scrobbler.composeapp.generated.resources.Res

object AcceptableTags {

    private val tagFragments by lazy {
        runBlocking {
            Res.readBytes("files/everynoise_genres.txt")
                .decodeToString()
                .lines()
                .toSet()
        }
    }

    fun isAcceptable(lastfmTag: String, hiddenTags: Set<String>): Boolean {
        val lastfmTagLower = lastfmTag.lowercase()
        return lastfmTagLower.isNotEmpty() &&
                lastfmTagLower.split(" ", "-").any { it in tagFragments } &&
                lastfmTagLower !in hiddenTags
    }
}