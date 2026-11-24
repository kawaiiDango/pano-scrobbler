package com.arn.scrobble.api

import com.arn.scrobble.api.cache.ExpirationPolicy
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.CacheControl
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import java.io.IOException

class CustomCachePlugin(
    private val policy: ExpirationPolicy,
    private val shouldStore: (HttpResponse) -> Boolean,
    private val maxBodyBytes: Long?,
) {
    class Config {
        lateinit var policy: ExpirationPolicy
        var shouldStore: (HttpResponse) -> Boolean = { true }
        var maxBodyBytes: Long? = null

        fun build(): CustomCachePlugin = CustomCachePlugin(policy, shouldStore, maxBodyBytes)
    }

    companion object Feature : HttpClientPlugin<Config, CustomCachePlugin> {
        override val key: AttributeKey<CustomCachePlugin> = AttributeKey("CustomCachePlugin")

        override fun prepare(block: Config.() -> Unit): CustomCachePlugin {
            return Config().apply(block).build()
        }

        @InternalAPI
        override fun install(plugin: CustomCachePlugin, scope: HttpClient) {

            scope.receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
                val cacheTime = plugin.policy.getExpirationTime(response.request.url)
                if (cacheTime <= 0 || response.request.method != HttpMethod.Get) {
                    proceedWith(response)
                } else {
                    val shouldStore = plugin.shouldStore(response)
                    val cacheControl = if (shouldStore) CacheControl.MaxAge((cacheTime / 1000).toInt()).toString()
                    else "no-store"
                    // HttpCache buffers a successful response before the caller can read it.
                    val originalContent = response.rawContent
                    val cappedContent = if (shouldStore && plugin.maxBodyBytes != null && response.status.isSuccess()) {
                        val body = originalContent.readBuffer(plugin.maxBodyBytes + 1)
                        if (body.size > plugin.maxBodyBytes) {
                            val error = IOException("HTTP cache response too large")
                            originalContent.cancel(error)
                            throw error
                        }
                        ByteReadChannel(body.readByteArray())
                    } else originalContent

                    proceedWith(object : HttpResponse() {
                        override val call = response.call

                        override val rawContent = cappedContent
                        override val coroutineContext = response.coroutineContext
                        override val headers: Headers = HeadersBuilder().apply {
                            appendAll(response.headers)
                            remove(HttpHeaders.CacheControl)
                            remove(HttpHeaders.Pragma)
                            append(HttpHeaders.CacheControl, cacheControl)
                        }.build()
                        override val requestTime = response.requestTime
                        override val responseTime = response.responseTime
                        override val status: HttpStatusCode = response.status
                        override val version: HttpProtocolVersion = response.version
                    })
                }
            }
        }
    }
}