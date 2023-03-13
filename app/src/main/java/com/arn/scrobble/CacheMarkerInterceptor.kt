package com.arn.scrobble

import io.ktor.client.statement.HttpResponse
import okhttp3.Interceptor
import okhttp3.Response

class CacheMarkerInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        if (response.cacheResponse != null) {
            response = response.newBuilder()
                .header(CACHE_MARKER_HEADER, "true")
                .build()
        }

        return response
    }

    companion object {
        private const val CACHE_MARKER_HEADER = "X-FROM-CACHE"

        val HttpResponse.isFromCache
            get() = headers.contains(CACHE_MARKER_HEADER, "true")

    }
}