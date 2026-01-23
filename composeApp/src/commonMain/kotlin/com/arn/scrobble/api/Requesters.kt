package com.arn.scrobble.api

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.cache.HttpMemoryCache
import com.arn.scrobble.api.cache.HybridCacheStorage
import com.arn.scrobble.api.deezer.DeezerRequester
import com.arn.scrobble.api.itunes.ItunesRequester
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.LastFmUnauthedRequester
import com.arn.scrobble.api.lastfm.LastfmExpirationPolicy
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageEntries
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.spotify.SpotifyRequester
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import kotlin.coroutines.cancellation.CancellationException


object Requesters {
    val spotifyRequester by lazy { SpotifyRequester() }
    val itunesRequester by lazy { ItunesRequester() }
    val deezerRequester by lazy { DeezerRequester() }

    val lastfmUnauthedRequester by lazy { LastFmUnauthedRequester() }

    val baseKtorClient by lazy {
        HttpClient(OkHttp) {

            engine {
                dispatcher = Dispatchers.IO

                // fix to tunnel dns through socks5 proxy if set at system level
                PlatformStuff.getSystemSocksProxy()?.let { (host, port) ->
                    Logger.i { "SOCKS proxy detected at $host:$port, applying fix" }
                    proxy = ProxyBuilder.socks(host, port)
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 40 * 1000L
            }

            install(UserAgent) {
                agent = BuildKonfig.APP_NAME + " " + BuildKonfig.VER_NAME
            }
        }
    }

    val genericKtorClient by lazy {
        baseKtorClient.config {
            install(ContentNegotiation) {
                json(Stuff.myJson)
            }

            if (!Stuff.isRunningInTest) {
                install(HttpCache) {
                    val cacheFile = File(PlatformStuff.cacheDir, "ktor")
                    val fileCache = FileStorage(cacheFile)
                    val memoryCache = HttpMemoryCache(25)
                    val hybridCache = HybridCacheStorage(
                        memoryCache = memoryCache,
                        fileCache = fileCache,
                    )
                    publicStorage(hybridCache)
                }
            }

            install(CustomCachePlugin) {
                policy = LastfmExpirationPolicy()
            }

            install(HttpCallValidator) {
                validateResponse { response ->
                    if (response.status.isSuccess()) return@validateResponse
                    try {
                        val errorResponse = response.parseJsonBody<ApiErrorResponse>()
                        throw ApiException(errorResponse.code, errorResponse.message)
                    } catch (e: SerializationException) {
                        throw ApiException(response.status.value, response.status.description, e)
                    }
                }
            }

//            expectSuccess = true
            // https://youtrack.jetbrains.com/issue/KTOR-4225
        }
    }


    suspend inline fun <reified T> HttpClient.getResult(
        urlString: String = "",
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ) = try {
        withContext(Dispatchers.IO) {
            val resp = get(urlString, block)
            try {
                val body = if (T::class == String::class)
                    resp.bodyAsText() as T
                else
                    resp.parseJsonBody<T>()
                Result.success(body)
            } catch (e: SerializationException) {
                val errorResponse = resp.parseJsonBody<ApiErrorResponse>()
                Result.failure(ApiException(errorResponse.code, errorResponse.message, e))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend inline fun <reified T> HttpClient.postResult(
        urlString: String = "",
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ) = try {
        withContext(Dispatchers.IO) {
            val resp = post(urlString, block)
            try {
                val body = if (T::class == String::class)
                    resp.bodyAsText() as T
                else
                    resp.parseJsonBody<T>()
                Result.success(body)
            } catch (e: SerializationException) {
                val errorResponse = resp.parseJsonBody<ApiErrorResponse>()
                Result.failure(ApiException(errorResponse.code, errorResponse.message, e))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend inline fun <reified T, U> HttpClient.getPageResult(
        urlString: String = "",
        crossinline transform: (T) -> PageEntries<U>,
        crossinline pageAttrTransform: (T) -> PageAttr? = { null },
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ) = try {
        withContext(Dispatchers.IO) {
            val resp = get(urlString, block)
            try {
                // listenbrainz empty charts
                if (resp.status.value == 204) {
                    return@withContext Result.success(
                        PageResult(
                            PageAttr(1, 1, 0),
                            emptyList(),
                        )
                    )
                }

                val body = resp.parseJsonBody<T>()
                val pageEntries = transform(body)
                val customPageAttr = pageAttrTransform(body)
                val pr = PageResult(
                    customPageAttr
                        ?: pageEntries.attr
                        ?: PageAttr(
                            page = 1,
                            totalPages = 1,
                            total = pageEntries.entries.size,
                        ),
                    pageEntries.entries,
                )
                Result.success(pr)
            } catch (e: SerializationException) {
                val errorResponse = resp.parseJsonBody<ApiErrorResponse>()
                Result.failure(ApiException(errorResponse.code, errorResponse.message, e))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun HttpClient.postString(
        url: String,
        body: String,
    ): String {
        return withContext(Dispatchers.IO) {
            post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
                .bodyAsText()
        }
    }

    // doing it like this avoids reflection according to graalvm
    suspend inline fun <reified T> HttpResponse.parseJsonBody(): T {
        return bodyAsChannel().toInputStream().use {
            Stuff.myJson.decodeFromStream<T>(it)
        }
    }

    inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
        contentType(ContentType.Application.Json)
        val jsonBody = Stuff.myJson.encodeToString(body)
        setBody(jsonBody)
    }
}