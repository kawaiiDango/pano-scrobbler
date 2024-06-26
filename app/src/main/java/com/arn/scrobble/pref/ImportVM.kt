package com.arn.scrobble.pref

import android.net.ConnectivityManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.Tokens
import com.arn.scrobble.main.App
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Inet4Address
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory


class ImportVM : ViewModel() {

    private val _serverAddress = MutableStateFlow<Result<String>?>(null)
    val serverAddress = _serverAddress.asStateFlow()
    private val _postData = MutableStateFlow<String?>(null)
    val postData = _postData.asStateFlow()
    val imExporter by lazy { ImExporter() }

    private var server: ImportServer? = null

    fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            if (serverAddress.value != null) {
                return@launch
            }

            val wifiIpAddress = getWifiIpAddress()
            if (wifiIpAddress == null) {
                _serverAddress.value = Result.failure(IOException("No wifi connection"))
                return@launch
            }

            val randomPort = (Base26Utils.PORT_START..Base26Utils.PORT_END).random()
            val base26Address = Base26Utils.encodeIpPort(wifiIpAddress, randomPort)

            try {
                val server = ImportServer(randomPort, base26Address) { postData ->
                    _postData.value = postData
                }
                server.start()

                _serverAddress.value = Result.success(base26Address)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    private fun getWifiIpAddress(): String? {
        val connectivityManager =
            ContextCompat.getSystemService(App.context, ConnectivityManager::class.java)!!
        val activeNetwork = connectivityManager.activeNetwork
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)

        linkProperties?.linkAddresses?.forEach { linkAddress ->
            val inetAddress = linkAddress.address
            if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                return inetAddress.hostAddress
            }
        }

        return null
    }

    override fun onCleared() {
        server?.stop()
        super.onCleared()
    }

    class ImportServer(
        port: Int,
        private val path: String,
        private val onImport: (String) -> Unit
    ) : NanoHTTPD(port) {
        init {
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            App.context.resources.openRawResource(R.raw.embedded_server_bks).use {
                ks.load(it, Tokens.EMBEDDED_SERVER_KEYSTORE_PASSWORD.toCharArray())
            }
            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(ks, Tokens.EMBEDDED_SERVER_KEYSTORE_PASSWORD.toCharArray())
            makeSecure(makeSSLSocketFactory(ks, kmf), null)
        }

        override fun serve(session: IHTTPSession): Response {
            return if (session.method == Method.POST && session.uri == "/$path") {
                val files = mutableMapOf<String, String>()
                session.parseBody(files)
                onImport(files["postData"]!!)

                newFixedLengthResponse(
                    Status.OK,
                    MIME_PLAINTEXT,
                    "Ok"
                )
            } else if (BuildConfig.DEBUG && session.method == Method.GET && session.uri == "/test") {
                newFixedLengthResponse("Ok")
            } else {
                return super.serve(session)
            }

        }
    }
}