package com.arn.scrobble.api.atproto

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.CustomCachePlugin
import com.arn.scrobble.api.cache.ExpirationPolicy
import com.arn.scrobble.api.cache.installPanoHttpCache
import com.arn.scrobble.api.lastfm.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.HttpSendPipeline
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.http.parseHeaderValue
import io.ktor.util.pipeline.PipelinePhase
import java.io.File
import java.util.concurrent.TimeUnit

internal const val AT_RECENTS_PATH = "/xrpc/com.atproto.repo.listRecords"
internal const val AT_RECENTS_MAX_BYTES = 2 * 1024 * 1024L
private val playCollections = setOf(AccountType.TEAL.playCollection, AccountType.ROCKSKY.playCollection)
private val privateDirectives = setOf("private", "no-store")

/** Built from the PDS transport to keep its network restrictions, but only for public play reads. */
internal fun HttpClient.atprotoRecentsClient(cacheDir: File): HttpClient = config {
    followRedirects = false
    installPanoHttpCache(cacheDir, publicOnly = true)
    install(CustomCachePlugin) {
        policy = object : ExpirationPolicy {
            override fun getExpirationTime(url: Url) = TimeUnit.DAYS.toMillis(7)
        }
        maxBodyBytes = AT_RECENTS_MAX_BYTES
        shouldStore = { response ->
            response.headers.getAll(HttpHeaders.CacheControl).orEmpty().flatMap { parseHeaderValue(it) }
                .none { it.value.substringBefore('=').trim().lowercase() in privateDirectives }
        }
    }
}.apply {
    requestPipeline.intercept(HttpRequestPipeline.Before) {
        check(context.method == HttpMethod.Get && context.url.encodedPath == AT_RECENTS_PATH &&
            context.url.parameters.getAll("collection")?.singleOrNull() in playCollections &&
            listOf(HttpHeaders.Authorization, HttpHeaders.Cookie).none(context.headers::contains)) {
            "The public AT Protocol Recents client only supports anonymous play history reads"
        }
    }
    // Ktor can attempt revalidation of a matching entry. A preview must never reach the engine.
    val cacheOnly = PipelinePhase("AtprotoRecentsCacheOnly")
    sendPipeline.insertPhaseBefore(HttpSendPipeline.Engine, cacheOnly)
    sendPipeline.intercept(cacheOnly) {
        if (parseHeaderValue(context.headers[HttpHeaders.CacheControl]).any { it.value == "only-if-cached" }) {
            throw ApiException(504, "AT Protocol history is not cached")
        }
    }
}
