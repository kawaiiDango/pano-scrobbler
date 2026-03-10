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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pano_scrobbler.composeapp.generated.resources.Res
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory


class ImportVM : ViewModel() {

    private val _serverAddress = MutableStateFlow<String?>(null)
    val serverAddress = _serverAddress.asStateFlow()
    private val _serverResult = MutableStateFlow<Result<String>?>(null)
    val serverResult = _serverResult.asStateFlow()
    private val _availableImportTypes = MutableStateFlow<Set<ImExporter.ImportTypes>?>(null)
    val availableImportTypes = _availableImportTypes.asStateFlow()
    private val _importResult = MutableSharedFlow<Result<Unit>>()
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
                        val encodedAddress = IpPortCode.encode(localIp, randomPort)
                        server = ImportServer(
                            localIp,
                            randomPort,
                            "import",
                            readKeystore()
                        ) {
                            _availableImportTypes.value = imExporter.createImportTypes(it)
                        }
                        server?.start()

                        _serverResult.value = Result.success(encodedAddress)
                    } catch (e: Exception) {
                        Logger.e(e) { "Failed to start import server" }
                        _serverResult.value = Result.failure(e)
                    }
                }
        }
    }

    fun setServerAddress(address: String?) {
        _availableImportTypes.value = null
        _serverAddress.value = address
    }

    fun import(
        userImportTypes: Set<ImExporter.ImportTypes>,
        writeMode: ImExporter.WriteMode,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_availableImportTypes.value != null) {
                val imported = imExporter.import(userImportTypes, writeMode)

                if (!imported)
                    _importResult.emit(Result.failure(IOException("Import failed")))
                else
                    _importResult.emit(Result.success(Unit))

                _availableImportTypes.value = null
            }
        }
    }

    fun setPlatformFile(platformFile: PlatformFile) {
        viewModelScope.launch(Dispatchers.IO) {
            if (platformFile.isWritable()) {
                platformFile.read {
                    _availableImportTypes.value = imExporter.createImportTypes(it)
                }
            } else {
                _importResult.emit(Result.failure(IOException("File is not readable")))
            }
        }
    }

    override fun onCleared() {
        server?.stop()
        super.onCleared()
    }

    private class ImportServer(
        hostname: String,
        port: Int,
        private val path: String,
        ks: KeyStore,
        private val onImport: (InputStream) -> Unit,
    ) : NanoHTTPD(hostname, port) {

        init {
            val kmf =
                KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
                )
            kmf.init(
                ks,
                Stuff.xorWithKey(
                    Stuff.EMBEDDED_SERVER_KS,
                    BuildKonfig.APP_ID
                ).toCharArray()
            )
            makeSecure(makeSSLSocketFactory(ks, kmf), null)

            tempFileManagerFactory = MyTempFileManagerFactory()
        }

        override fun serve(session: IHTTPSession): Response {
            return if (session.method == Method.POST && session.uri == "/$path") {
                val contentLength = session.headers["content-length"]!!.toInt()
                val lis = LimitedInputStream(session.inputStream, contentLength)
                onImport(lis)

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

        private class MyTempFileManagerFactory : TempFileManagerFactory {
            override fun create() = MyTempFileManager()
        }

        class MyTempFileManager : TempFileManager {
            private val tmpdir = PlatformStuff.cacheDir.resolve("nanohttpd").apply { mkdirs() }
            private val tempFiles = mutableListOf<TempFile>()

            override fun clear() {
                tempFiles.forEach { file ->
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        Logger.w("could not delete file", e)
                    }
                }
                tempFiles.clear()
            }

            override fun createTempFile(filename_hint: String?): TempFile {
                val tempFile = DefaultTempFile(tmpdir)
                this.tempFiles.add(tempFile)
                return tempFile
            }
        }

    }

    companion object {
        suspend fun readKeystore(): KeyStore {
            val ks = KeyStore.getInstance("PKCS12")
            Res.readBytes("files/pano-embedded-server-ks.bin")
                .let {
                    Stuff.xorWithKeyBytes(
                        it,
                        BuildKonfig.APP_ID.toByteArray()
                    )
                }
                .inputStream()
                .use {
                    ks.load(
                        it, Stuff.xorWithKey(
                            Stuff.EMBEDDED_SERVER_KS,
                            BuildKonfig.APP_ID
                        ).toCharArray()
                    )
                }

            return ks
        }
    }
}