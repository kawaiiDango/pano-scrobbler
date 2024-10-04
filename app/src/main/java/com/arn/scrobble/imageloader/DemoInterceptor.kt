package com.arn.scrobble.imageloader

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.transformations

class DemoInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val imgUrl = chain.request.data as? String
        return if (imgUrl != null &&
            (imgUrl.endsWith(".webp") || imgUrl.endsWith(".gif") || imgUrl.startsWith("https://i.scdn.co"))
        ) {
            chain.withRequest(
                chain.request.newBuilder()
                    .transformations(CrystallizeTransformation(chain.request.context))
                    .build()
            ).proceed()
        } else
            chain.proceed()
    }
}