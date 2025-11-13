package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.PlatformFile
import com.arn.scrobble.utils.PlatformStuff
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pano_scrobbler.composeapp.generated.resources.Res
import java.io.ByteArrayOutputStream
import java.io.IOException
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

    private val keyFileExtension = if (PlatformStuff.isDesktop) "jks" else "bks"

    fun exportToFile(platformFile: PlatformFile, privateData: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            platformFile.overwrite {
                val exported = if (privateData)
                    imExporter.exportPrivateData(it)
                else
                    imExporter.export(it)

                if (!exported)
                    _result.value = Result.failure(IOException("Export failed"))
                else
                    Logger.i { "Exported" }
            }
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
                        throw IOException(req.bodyAsText())
                }.onFailure {
                    Logger.d(it) { "Export failed" }
                }
            }
        }
    }

    private fun buildKtorClient(): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    val ks = ImportVM.readKeystore()
                    val trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    trustManagerFactory.init(ks)
                    val trustManagers = trustManagerFactory.trustManagers
                    val x509TrustManager = trustManagers[0] as X509TrustManager

                    val sslContext = SSLContext.getInstance("TLS")
                    sslContext.init(null, arrayOf(x509TrustManager), SecureRandom())

                    sslSocketFactory(sslContext.socketFactory, x509TrustManager)

                    hostnameVerifier { _, _ -> true }
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30 * 1000L
            }

            //            install(ContentNegotiation) {
            //                json(Stuff.myJson)
            //            }
        }
    }

}