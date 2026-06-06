package com.arn.scrobble.imageloader

import coil3.map.Mapper
import coil3.request.Options

class StarMapper : Mapper<String, String> {

    override fun map(data: String, options: Options): String? {
        if (STAR_PATTERN in data)
            throw StarException()
        return null
    }

    class StarException : IllegalStateException("Got a star")

    companion object {
        const val STAR_PATTERN = "2a96cbd8b46e442fc41c2b86b821562f"
    }
}