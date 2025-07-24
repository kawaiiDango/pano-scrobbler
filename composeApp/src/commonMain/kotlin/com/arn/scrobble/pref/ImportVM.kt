package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.Tokens
import com.arn.scrobble.utils.PlatformFile
import com.arn.scrobble.utils.PlatformStuff
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pano_scrobbler.composeapp.generated.resources.Res
import java.io.IOException
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory


class ImportVM : ViewModel() {

    private val _serverAddress = MutableStateFlow<Result<String>?>(null)
    val serverAddress = _serverAddress.asStateFlow()
    private val _jsonText = MutableStateFlow<String?>(null)
    val jsonText = _jsonText.filterNotNull()
    private val _importResult = MutableSharedFlow<Boolean>()
    val importResult = _importResult.asSharedFlow()
    private val imExporter by lazy { ImExporter() }

    private var server: ImportServer? = null

    fun import(
        editsMode: EditsMode,
        settings: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _jsonText.value?.let {
                val imported = imExporter.import(it, editsMode, settings)
                _importResult.emit(imported)
                _jsonText.value = null
            }
        }
    }

    fun setPlatformFile(platformFile: PlatformFile) {
        viewModelScope.launch(Dispatchers.IO) {
            platformFile.read {
                val fileText = it.bufferedReader().readText()
                _jsonText.emit(fileText)
            }
        }
    }

    fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            if (serverAddress.value != null) {
                return@launch
            }

            val localIp = PlatformStuff.getLocalIpAddress()
            if (localIp == null) {
                _serverAddress.value = Result.failure(IOException("No connection"))
                return@launch
            }

            val randomPort = (Base26Utils.PORT_START..Base26Utils.PORT_END).random()

            Logger.d { "Server running on: https://$localIp:$randomPort" }

            try {
                val base26Address = Base26Utils.encodeIpPort(localIp, randomPort)
                val server = ImportServer(randomPort, base26Address) { postData ->
                    _jsonText.tryEmit(postData)
                }
                server.start()

                _serverAddress.value = Result.success(base26Address)
            } catch (e: Exception) {
                Logger.e(e) { "Failed to start import server" }
                _serverAddress.value = Result.failure(e)
            }

        }
    }

    override fun onCleared() {
        _jsonText.value = null
        server?.stop()
        super.onCleared()
    }

    private class ImportServer(
        port: Int,
        private val path: String,
        private val onImport: (String) -> Unit,
    ) : NanoHTTPD(port) {
        private val keyFileExtension = if (PlatformStuff.isDesktop) "jks" else "bks"

        init {
            val ksType = KeyStore.getDefaultType()
            val ks = KeyStore.getInstance(ksType)
            runBlocking { Res.readBytes("files/pano-embedded-server-ks.$keyFileExtension") }
                .inputStream()
                .use {
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
            } else if (PlatformStuff.isDebug && session.method == Method.GET && session.uri == "/test") {
                newFixedLengthResponse("Ok")
            } else {
                return super.serve(session)
            }

        }
    }
}