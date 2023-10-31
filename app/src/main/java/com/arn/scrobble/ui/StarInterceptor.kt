package com.arn.scrobble.ui

import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult
import com.arn.scrobble.R

class StarInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val imgUrl = chain.request.data as? String
        return if (imgUrl != null && starPatterns.any { imgUrl.contains(it) }) {
            chain.proceed(
                ImageRequest.Builder(chain.request)
                    .data(R.drawable.vd_album_filled)
                    .allowHardware(false)
                    .build()
            )
        } else
            chain.proceed(chain.request)
    }

    companion object {
        val starPatterns = arrayOf(
            "2a96cbd8b46e442fc41c2b86b821562f"
        )
    }
}
