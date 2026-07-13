package com.arn.scrobble.api

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.cache.HttpMemoryCache
import com.arn.scrobble.api.cache.HybridCacheStorage
import com.arn.scrobble.api.deezer.DeezerRequester
import com.arn.scrobble.api.itunes.ItunesRequester
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.DefaultExpirationPolicy
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.LastFmUnauthedRequester
import com.arn.scrobble.api.lastfm.PageAttr
import com.arn.scrobble.api.lastfm.PageEntries
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.spotify.SpotifyRequester
import com.arn.scrobble.imageloader.PanoImageLoader
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.stateInWithCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Credentials
import java.io.File
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
import kotlin.coroutines.cancellation.CancellationException


object Requesters {
    val proxy = PlatformStuff.mainPrefs.data.stateInWithCache(Stuff.appScope) { it.proxy }

    private val proxyAuthenticator = object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication? {
            val p = proxy.value
                .takeIf { it.type == MainPrefs.ProxySettings.Type.SOCKS5 && it.hasAuth }
                ?: return null

            if (requestingProtocol.equals("SOCKS5", ignoreCase = true) &&
                requestingPort == p.port &&
                requestingHost.equals(p.host, ignoreCase = true)
            ) {
                return PasswordAuthentication(p.user, p.pass.toCharArray())
            }
            return null
        }
    }

    private var proxyAuthenticatorSet = false

    init {
        // invalidate clients when proxy settings change
        Stuff.appScope.launch {
            proxy.drop(1).collect {
                invalidateAll()
            }
        }
    }

    val spotifyRequester by lazy { SpotifyRequester() }
    val itunesRequester by lazy { ItunesRequester() }
    val deezerRequester by lazy { DeezerRequester() }

    val lastfmUnauthedRequester by lazy { LastFmUnauthedRequester() }

    private fun createBaseKtorClient(trustAll: Boolean) = invalidatableLazy {
        HttpClient(OkHttp) {
            val proxyVal = proxy.value

            val proxyJvm = when (proxyVal.type) {
                MainPrefs.ProxySettings.Type.SYSTEM -> {
                    PlatformStuff.getSystemSocksProxy()
                }

                MainPrefs.ProxySettings.Type.HTTP -> {
                    Proxy(
                        Proxy.Type.HTTP, InetSocketAddress.createUnresolved(
                            proxyVal.host,
                            proxyVal.port
                        )
                    )
                }

                MainPrefs.ProxySettings.Type.SOCKS5 -> {
                    if (!proxyAuthenticatorSet && proxyVal.hasAuth) {
                        proxyAuthenticatorSet = true
                        Authenticator.setDefault(proxyAuthenticator)
                    }

                    Proxy(
                        Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(
                            proxyVal.host,
                            proxyVal.port
                        )
                    )
                }
            }

            engine {
                dispatcher = Dispatchers.IO
                proxy = proxyJvm

                config {
                    if (proxyVal.type == MainPrefs.ProxySettings.Type.HTTP && proxyVal.hasAuth) {
                        proxyAuthenticator { _, response ->
                            val credential =
                                Credentials.basic(proxyVal.user, proxyVal.pass)

                            response.request.newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build()
                        }
                    }

                    if (trustAll) {
                        val localNetworkTlsTrustManager = object : X509TrustManager {
                            override fun checkClientTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?
                            ) = Unit

                            override fun checkServerTrusted(
                                chain: Array<out X509Certificate>?,
                                authType: String?
                            ) = Unit

                            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                        }

                        val sslContext = SSLContext.getInstance("TLS")
                        sslContext.init(null, arrayOf(localNetworkTlsTrustManager), null)
                        sslSocketFactory(sslContext.socketFactory, localNetworkTlsTrustManager)
                        // Self-signed certs may not carry a matching CN/SAN.
                        hostnameVerifier { _, _ -> true }
                    }
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

    private val _baseKtorClient = createBaseKtorClient(false)
    val baseKtorClient by _baseKtorClient

    private fun createGenericKtorClient(trustAll: Boolean) = invalidatableLazy {
        val baseClient = if (trustAll) {
            val c by createBaseKtorClient(true)
            c
        } else
            baseKtorClient

        baseClient.config {
            install(ContentNegotiation) {
                json(Stuff.myJson)
            }

            if (!Stuff.isRunningInTest) {
                install(HttpCache) {
                    val cacheFile = File(PlatformStuff.cacheDir, "ktor")
                    val memoryCache = HttpMemoryCache(25)
                    val hybridCache = HybridCacheStorage(
                        memoryCache = memoryCache,
                        cacheFile,
                    )
                    publicStorage(hybridCache)
                }
            }

            install(CustomCachePlugin) {
                policy = DefaultExpirationPolicy()
            }

            install(HttpCallValidator) {
                validateResponse { response ->
                    if (response.status.isSuccess()) return@validateResponse
                    try {
                        val errorResponse = response.parseJsonBody<ApiErrorResponse>()
                        throw ApiException(errorResponse.code, errorResponse.message)
                    } catch (e: SerializationException) {
                        throw ApiException(response.status.value, response.status.description)
                    }
                }
            }

//            expectSuccess = true
            // https://youtrack.jetbrains.com/issue/KTOR-4225
        }
    }

    private val _genericKtorClient = createGenericKtorClient(false)
    val genericKtorClient by _genericKtorClient

    private val _genericKtorTrustAllClient = createGenericKtorClient(true)
    val genericKtorTrustAllClient by _genericKtorTrustAllClient

    private fun invalidateAll() {
        _baseKtorClient.invalidate()
        _genericKtorClient.invalidate()
        _genericKtorTrustAllClient.invalidate()
        spotifyRequester.invalidateClient()
        LastFm.LastfmUnscrobbler.invalidateClient()
        PanoImageLoader.invalidate()
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
                throw ApiException(errorResponse.code, errorResponse.message)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        e.fillInStackTrace()
        reportRateLimitErrors(e)
        Result.failure(e)
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
                throw ApiException(errorResponse.code, errorResponse.message)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        e.fillInStackTrace()
        reportRateLimitErrors(e)
        Result.failure(e)
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
                throw ApiException(errorResponse.code, errorResponse.message)
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        e.fillInStackTrace()
        reportRateLimitErrors(e)
        Result.failure(e)
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


    fun reportRateLimitErrors(e: ApiException) {
        // report rate limit errors to crashlytics
        if (e.code in arrayOf(29, 9, 429)) {
            Logger.w(e) { e.description }
        }
    }
}