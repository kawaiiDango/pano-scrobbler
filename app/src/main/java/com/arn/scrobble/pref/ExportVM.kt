package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class ExportVM : ViewModel() {

    private val _result = MutableStateFlow<Result<Unit>?>(null)
    val result = _result.asStateFlow()
    private val imExporter by lazy { ImExporter() }
    private val ktorClient by lazy { buildKtorClient() }

    fun exportToFile(outputStream: OutputStream?, privateData: Boolean) {
        outputStream ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val exported = if (privateData)
                imExporter.exportPrivateData(outputStream)
            else
                imExporter.export(outputStream)

            if (!exported)
                _result.value = Result.failure(IOException("Export failed"))
            else
                Logger.i { "Exported" }
        }
    }

    fun exportToServer(base26Address: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val outputStream = ByteArrayOutputStream()
            val exported = imExporter.export(outputStream)
            if (exported) {
                val jsonBody = outputStream.toString()
                _result.value = runCatching {
                    val (ip, port) = Base26Utils.decodeIpPort(base26Address)

                    val req = ktorClient.post("https://$ip:$port/$base26Address") {
                        setBody(jsonBody)
                    }

                    if (req.status.isSuccess())
                        Unit
                    else
                        throw IOException(req.body<String>())
                }.onFailure {
                    if (BuildConfig.DEBUG)
                        it.printStackTrace()
                }
            }
        }
    }

    private fun buildKtorClient(): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                    val keyStoreStream =
                        PlatformStuff.application.resources.openRawResource(R.raw.embedded_server_bks)
                    keyStore.load(keyStoreStream, null)

                    val trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    trustManagerFactory.init(keyStore)
                    val trustManagers = trustManagerFactory.trustManagers
                    val x509TrustManager = trustManagers[0] as X509TrustManager

                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, arrayOf(x509TrustManager), SecureRandom())

                    sslSocketFactory(sslContext.socketFactory, x509TrustManager)

                    hostnameVerifier { _, _ -> true }
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 5 * 1000L
            }

            //            install(ContentNegotiation) {
            //                json(Stuff.myJson)
            //            }
        }
    }

}