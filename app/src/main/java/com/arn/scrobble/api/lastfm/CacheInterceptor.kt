package com.arn.scrobble.api.lastfm

import com.arn.scrobble.utils.Stuff
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

// https://stackoverflow.com/questions/49453564/how-to-cache-okhttp-response-from-web-server
class CacheInterceptor(private val policy: ExpirationPolicy) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val url = request.url
        val noCache = request.cacheControl.noCache
        val cacheTimeSecs = policy.getExpirationTimeSecs(url)
        val cacheControl = if (cacheTimeSecs > 0 && request.method != "GET") {
            Stuff.logD { "CacheInterceptor: Ignoring non-GET cacheable request" }
            return chain.proceed(request)
        } else if (cacheTimeSecs > 0) {
            CacheControl.Builder()
                .maxAge(cacheTimeSecs, TimeUnit.SECONDS)
                .build()
        } else if (noCache) {
            CacheControl.Builder()
                .noCache()
                .build()
        } else {
            CacheControl.Builder()
                .noStore().build()
        }
        return chain.proceed(request)
            .newBuilder()
            .removeHeader("Pragma")
            .removeHeader("Cache-Control")
            .header("Cache-Control", cacheControl.toString())
            .build()
    }
}