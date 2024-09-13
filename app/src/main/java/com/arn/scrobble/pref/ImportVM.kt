package com.arn.scrobble.pref

import android.net.ConnectivityManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.Tokens
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory


class ImportVM : ViewModel() {

    private val _serverAddress = MutableStateFlow<Result<String>?>(null)
    val serverAddress = _serverAddress.asStateFlow()
    private val _inputStream = MutableStateFlow<InputStream?>(null)
    val inputStream = _inputStream.filterNotNull()
    private val _importResult = MutableSharedFlow<Boolean>()
    val importResult = _importResult.asSharedFlow()
    private val imExporter by lazy { ImExporter() }

    private var server: ImportServer? = null

    fun import(
        editsMode: EditsMode,
        settings: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _inputStream.value?.let {
                val imported = imExporter.import(it, editsMode, settings)
                _importResult.emit(imported)
            }
            _inputStream.value = null
        }
    }

    fun setInputStream(inputStream: InputStream?) {
        _inputStream.value = inputStream
    }

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

            try {
                val base26Address = Base26Utils.encodeIpPort(wifiIpAddress, randomPort)
                val server = ImportServer(randomPort, base26Address) { postData ->
                    _inputStream.tryEmit(postData.byteInputStream())
                }
                server.start()

                _serverAddress.value = Result.success(base26Address)
            } catch (e: Exception) {
                _serverAddress.value = Result.failure(e)
                e.printStackTrace()
            }

        }
    }

    private fun getWifiIpAddress(): String? {
        val connectivityManager =
            ContextCompat.getSystemService(
                PlatformStuff.application,
                ConnectivityManager::class.java
            )!!
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
        _inputStream.value?.close()
        _inputStream.value = null
        server?.stop()
        super.onCleared()
    }

    private class ImportServer(
        port: Int,
        private val path: String,
        private val onImport: (String) -> Unit
    ) : NanoHTTPD(port) {
        init {
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            PlatformStuff.application.resources.openRawResource(R.raw.embedded_server_bks).use {
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