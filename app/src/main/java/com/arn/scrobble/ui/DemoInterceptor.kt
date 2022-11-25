package com.arn.scrobble.ui

import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult

class DemoInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val imgUrl = chain.request.data as? String
        return if (imgUrl != null &&
            (imgUrl.endsWith(".webp") || imgUrl.endsWith(".gif") || imgUrl.startsWith("https://i.scdn.co"))
        ) {
            chain.proceed(
                ImageRequest.Builder(chain.request)
                    .transformations(CrystallizeTransformation(chain.request.context))
                    .build()
            )
        } else
            chain.proceed(chain.request)
    }
}
