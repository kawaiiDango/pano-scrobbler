package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.PlatformFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val ktorClient = viewModelScope.async(start = CoroutineStart.LAZY) {
        val ks = ImportVM.readKeystore()
        buildKtorClient(ks)
    }

    fun exportToFile(platformFile: PlatformFile, privateData: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            var exported = false
            platformFile.overwrite {
                exported = if (privateData)
                    imExporter.exportPrivateData(it)
                else
                    imExporter.export(it)
            }

            if (!exported) {
                _result.value = Result.failure(IOException("Export failed"))
            } else {
                _result.value = Result.success(Unit)
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
                    val (ip, port) = IpPortCode.decode(base26Address)

                    val req = ktorClient.await()
                        .post("https://$ip:$port/$base26Address") {
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

    private fun buildKtorClient(ks: KeyStore): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
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