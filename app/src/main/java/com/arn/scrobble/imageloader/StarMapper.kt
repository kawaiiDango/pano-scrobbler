package com.arn.scrobble.imageloader

import coil3.map.Mapper
import coil3.request.Options
import com.arn.scrobble.R

class StarMapper : Mapper<String, Int> {

    override fun map(data: String, options: Options): Int? {
        if (STAR_PATTERN in data)
            return R.drawable.vd_album_filled

        return null
    }

    companion object {
        const val STAR_PATTERN = "2a96cbd8b46e442fc41c2b86b821562f"
    }
}