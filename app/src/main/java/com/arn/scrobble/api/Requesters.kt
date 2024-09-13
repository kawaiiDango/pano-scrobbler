package com.arn.scrobble.api

import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.ApiErrorResponse
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.LastFmUnauthedRequester
import com.arn.scrobble.api.lastfm.LastfmExpirationPolicy
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageEntries
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.spotify.SpotifyRequester
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import io.ktor.client.HttpClient
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
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
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException


object Requesters {
    val spotifyRequester by lazy { SpotifyRequester() }

    val lastfmUnauthedRequester by lazy { LastFmUnauthedRequester() }

    val genericKtorClient by lazy {
        HttpClient(OkHttp) {

            install(HttpTimeout) {
                requestTimeoutMillis = 20 * 1000L
            }

            install(ContentNegotiation) {
                json(Stuff.myJson)
            }

            if (!Stuff.isRunningInTest) {

                install(UserAgent) {
                    agent =
                        PlatformStuff.application.getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME
                }

                install(HttpCache) {
                    val cacheFile = File(PlatformStuff.application.cacheDir, "ktor")
                    cacheFile.mkdirs()
                    publicStorage(FileStorage(cacheFile))
                }
            }

            install(CustomCachePlugin) {
                policy = LastfmExpirationPolicy()
            }

            install(HttpCallValidator) {
                validateResponse { response ->
                    if (response.status.isSuccess()) return@validateResponse
                    try {
                        val errorResponse = response.body<ApiErrorResponse>()
                        throw ApiException(errorResponse.code, errorResponse.message)
                    } catch (e: NoTransformationFoundException) {
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
                val body = resp.body<T>()
                Result.success(body)
            } catch (e: JsonConvertException) {
                val errorResponse = resp.body<ApiErrorResponse>()
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
                val body = resp.body<T>()
                Result.success(body)
            } catch (e: JsonConvertException) {
                val errorResponse = resp.body<ApiErrorResponse>()
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

                val body = resp.body<T>()
                Result.success(body)
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
            } catch (e: JsonConvertException) {
                val errorResponse = resp.body<ApiErrorResponse>()
                Result.failure(ApiException(errorResponse.code, errorResponse.message, e))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}