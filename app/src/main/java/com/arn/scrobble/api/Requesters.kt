package com.arn.scrobble.api

import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.api.CacheMarkerInterceptor.Companion.isFromCache
import com.arn.scrobble.api.lastfm.CacheInterceptor
import com.arn.scrobble.api.lastfm.FmErrorResponse
import com.arn.scrobble.api.lastfm.FmException
import com.arn.scrobble.api.lastfm.LastFmUnauthedRequester
import com.arn.scrobble.api.lastfm.LastfmExpirationPolicy
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageEntries
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.spotify.SpotifyRequester
import com.arn.scrobble.utils.Stuff
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.Cache
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

object Requesters {
    val spotifyRequester by lazy { SpotifyRequester() }

    val lastfmUnauthedRequester by lazy { LastFmUnauthedRequester() }

    val genericKtorClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                    if (!Stuff.isRunningInTest)
                        cache(Cache(File(App.context.cacheDir, "ktor"), 10 * 1024 * 1024))
                    readTimeout(Stuff.READ_TIMEOUT_SECS, TimeUnit.SECONDS)
                }
                addNetworkInterceptor(CacheInterceptor(LastfmExpirationPolicy()))
                addInterceptor(CacheMarkerInterceptor())
            }

            install(ContentNegotiation) {
                json(Stuff.myJson)
            }

            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.ANDROID
                    level = LogLevel.ALL
                }
            }

            install(HttpCallValidator) {
                validateResponse { response ->
                    if (response.status.isSuccess()) return@validateResponse
                    val errorResponse = response.body<FmErrorResponse>()
                    throw FmException(errorResponse.code, errorResponse.message)
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
                val errorResponse = resp.body<FmErrorResponse>()
                Result.failure(FmException(errorResponse.code, errorResponse.message))
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
                val errorResponse = resp.body<FmErrorResponse>()
                Result.failure(FmException(errorResponse.code, errorResponse.message))
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
                val fromCache = resp.isFromCache
                // listenbrainz empty charts
                if (resp.status.value == 204) {
                    return@withContext Result.success(
                        PageResult(
                            PageAttr(1, 1, 0),
                            emptyList(),
                            fromCache
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
                    fromCache
                )
                Result.success(pr)
            } catch (e: JsonConvertException) {
                val errorResponse = resp.body<FmErrorResponse>()
                Result.failure(FmException(errorResponse.code, errorResponse.message))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun <T> Result<T>.toFlow() = flow {
        when {
            isSuccess -> emit(getOrThrow())
            isFailure -> App.globalExceptionFlow.emit(exceptionOrNull()!!)
        }
    }

}