package com.arn.scrobble.imageloader

import coil3.map.Mapper
import coil3.request.Options
import org.jetbrains.compose.resources.ExperimentalResourceApi

class StarMapper : Mapper<String, String> {

    @OptIn(ExperimentalResourceApi::class)
    override fun map(data: String, options: Options): String? {
        if (STAR_PATTERN in data)
            throw IllegalStateException("Got a star")
        return null
    }

    companion object {
        const val STAR_PATTERN = "2a96cbd8b46e442fc41c2b86b821562f"
    }
}