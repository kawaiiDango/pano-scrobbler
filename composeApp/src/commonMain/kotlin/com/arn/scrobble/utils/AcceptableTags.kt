package com.arn.scrobble.utils

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pano_scrobbler.composeapp.generated.resources.Res

object AcceptableTags {

    @OptIn(ExperimentalResourceApi::class)
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