package com.arn.scrobble.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.utils.PlatformFile
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pano_scrobbler.composeapp.generated.resources.Res
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory


class ImportVM : ViewModel() {

    private val _serverAddress = MutableStateFlow<String?>(null)
    val serverAddress = _serverAddress.asStateFlow()
    private val _serverResult = MutableStateFlow<Result<String>?>(null)
    val serverResult = _serverResult.asStateFlow()
    private val _jsonText = MutableStateFlow<String?>(null)
    val jsonText = _jsonText.filterNotNull()
    private val _importResult = MutableSharedFlow<Boolean>()
    val importResult = _importResult.asSharedFlow()
    private val imExporter by lazy { ImExporter() }
    val localIps by lazy { PlatformStuff.getLocalIpAddresses() }
    private var server: ImportServer? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            serverAddress
                .collect { localIp ->
                    server?.stop()

                    if (localIp == null) {
                        _serverResult.value = null
                        return@collect
                    }

                    val randomPort = (IpPortCode.PORT_BASE..IpPortCode.PORT_MAX).random()

                    Logger.d { "Server running on: https://$localIp:$randomPort" }

                    try {
                        val base26Address = IpPortCode.encode(localIp, randomPort)
                        server = ImportServer(localIp, randomPort, base26Address) { postData ->
                            _jsonText.tryEmit(postData)
                        }
                        server?.start()

                        _serverResult.value = Result.success(base26Address)
                    } catch (e: Exception) {
                        Logger.e(e) { "Failed to start import server" }
                        _serverResult.value = Result.failure(e)
                    }
                }
        }
    }

    fun setServerAddress(address: String?) {
        _serverAddress.value = address
    }

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

    override fun onCleared() {
        _jsonText.value = null
        server?.stop()
        super.onCleared()
    }

    private class ImportServer(
        hostname: String,
        port: Int,
        private val path: String,
        private val onImport: (String) -> Unit,
    ) : NanoHTTPD(hostname, port) {

        init {
            val ks = readKeystore()
            val kmf =
                KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
                )
            kmf.init(
                ks,
                Stuff.xorWithKeyBytes(
                    Stuff.EMBEDDED_SERVER_KS.decodeBase64Bytes(),
                    BuildKonfig.APP_ID.toByteArray()
                ).decodeToString().toCharArray()
            )
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
            } else if (BuildKonfig.DEBUG && session.method == Method.GET && session.uri == "/test") {
                newFixedLengthResponse("Ok")
            } else {
                super.serve(session)
            }

        }
    }

    companion object {
        fun readKeystore(): KeyStore {
            val ks = KeyStore.getInstance("PKCS12")
            runBlocking { Res.readBytes("files/pano-embedded-server-ks.bin") }
                .let {
                    Stuff.xorWithKeyBytes(
                        it,
                        BuildKonfig.APP_ID.toByteArray()
                    )
                }
                .inputStream()
                .use {
                    ks.load(
                        it, Stuff.xorWithKeyBytes(
                            Stuff.EMBEDDED_SERVER_KS.decodeBase64Bytes(),
                            BuildKonfig.APP_ID.toByteArray()
                        ).decodeToString().toCharArray()
                    )
                }

            return ks
        }
    }
}