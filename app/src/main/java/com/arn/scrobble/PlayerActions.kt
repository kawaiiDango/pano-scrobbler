package com.arn.scrobble

import android.media.Rating
import android.media.session.MediaController

object PlayerActions {

    fun List<MediaController>.skip() {
        forEach {
            it.transportControls.skipToNext()
        }
    }

    // doesn't work in spotify, s2
    // works in youtube, yt music, poweramp,
    fun List<MediaController>.love() {
        forEach {
            val rating = when (it.ratingType) {
                Rating.RATING_THUMB_UP_DOWN -> Rating.newThumbRating(true)
                Rating.RATING_HEART -> Rating.newHeartRating(true)
                Rating.RATING_3_STARS -> Rating.newStarRating(
                    Rating.RATING_3_STARS,
                    3F
                )

                Rating.RATING_4_STARS -> Rating.newStarRating(Rating.RATING_4_STARS, 4F)
                Rating.RATING_5_STARS -> Rating.newStarRating(Rating.RATING_5_STARS, 5F)
                Rating.RATING_PERCENTAGE -> Rating.newPercentageRating(100F)
                else -> null
            }
            if (rating != null)
                it.transportControls.setRating(rating)

            Stuff.logD { "Rating type for ${it.packageName}: ${it.ratingType}" }
        }
    }

    fun List<MediaController>.unlove() {
        forEach {
            val ratingSupported = it.ratingType != Rating.RATING_NONE
            if (ratingSupported)
                it.transportControls.setRating(Rating.newUnratedRating(it.ratingType))
        }
    }
}