package com.arn.scrobble.utils

import pano_scrobbler.composeapp.generated.resources.Res

object AcceptableTags {

    private var tagFragments: Set<Int>? = null
    suspend fun isAcceptable(lastfmTag: String, hiddenTags: Set<String>): Boolean {
        val _tagFragments = tagFragments ?: Res.readBytes("files/everynoise_genres.txt")
            .decodeToString()
            .lineSequence()
            .map { it.hashCode() }
            .toHashSet()
            .also {
                tagFragments = it
            }

        val lastfmTagLower = lastfmTag.lowercase()
        return lastfmTagLower.isNotEmpty() &&
                lastfmTagLower.split(" ", "-").any { it.hashCode() in _tagFragments } &&
                lastfmTagLower !in hiddenTags
    }
}