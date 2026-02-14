package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.Stuff.stateInWithCache
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI


actual object PlatformStuff {

    actual const val isJava8OrGreater = true

    actual fun isNotificationListenerEnabled() = true

    actual const val isTv = false

    actual const val supportsDynamicColors = false

    actual const val isDesktop = true

    actual val noUpdateCheck
        get() =
            DesktopStuff.noUpdateCheck

    actual const val hasSystemLocaleStore = false

    actual val filesDir by lazy {
        File(DesktopStuff.appDataRoot, "data").also { it.mkdirs() }
    }

    actual val cacheDir by lazy {
        File(DesktopStuff.appDataRoot, "cache").also { it.mkdirs() }
    }

    actual val logsDir by lazy { File(DesktopStuff.appDataRoot, "logs").also { it.mkdirs() } }

    actual val mainPrefs by lazy {
        DataStoreFactory.create(
            serializer = MainPrefs.dataStoreSerializer,
            migrations = MainPrefs.migrations(),
            corruptionHandler = null,
            produceFile = {
                File(filesDir, MainPrefs.FILE_NAME)
            },
        )
    }

    actual fun openInBrowser(url: String) {
        val desktop = Desktop.getDesktop()
        if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(URI(url))
        } else {
            if (DesktopStuff.os == DesktopStuff.Os.Linux) {
                try {
                    Logger.i { "Attempting to open with xdg-open" }
                    ProcessBuilder("xdg-open", url).start()
                } catch (e: IOException) {
                    val snackbarData = PanoSnackbarVisuals(
                        message = "Failed to open with xdg-open: $url",
                        isError = true,
                    )
                    Stuff.globalSnackbarFlow.tryEmit(snackbarData)
                } catch (e: InterruptedException) {
                    Logger.w("Interrupted while waiting for xdg-open", e)
                }
            } else {
                val snackbarData = PanoSnackbarVisuals(
                    message = "Failed to open URL: $url",
                    isError = true,
                )
                Stuff.globalSnackbarFlow.tryEmit(snackbarData)
            }
        }
    }

    actual fun isScrobblerRunning(): Boolean {
        return PanoNativeComponents.isMediaListenerRunning
    }

    actual fun getDeviceIdentifier(): String {
        return Stuff.sha256Truncated(PanoNativeComponents.getMachineId())
    }

    actual suspend fun launchSearchIntent(
        musicEntry: MusicEntry,
        appId: String?,
    ) {
        val searchUrlTemplate = mainPrefs.data.map { it.searchUrlTemplate }.first()

        var searchQuery = when (musicEntry) {
            is Artist -> {
                musicEntry.name
            }

            is Album -> {
                musicEntry.artist!!.name + " " + musicEntry.name
            }

            is Track -> {
                musicEntry.artist.name + " " + musicEntry.name
            }
        }

        searchQuery = searchQuery.encodeURLPath()
        val searchUrl = searchUrlTemplate.replace(
            "\$query",
            searchQuery
        )
        openInBrowser(searchUrl)
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<PanoDb> {
        val dbFile = File(filesDir, "panodb.db")
        return Room.databaseBuilder<PanoDb>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())

    }

    private val seenApps = mainPrefs.data.stateInWithCache(GlobalScope) { it.seenApps }
    actual fun loadApplicationLabel(appId: String): String =
        seenApps.value[appId] ?: ""

    actual fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
        Stuff.globalSnackbarFlow.tryEmit(
            PanoSnackbarVisuals("Copied")
        )
    }

    actual suspend fun writeBitmapToStream(imageBitmap: ImageBitmap, stream: OutputStream) =
        withContext(Dispatchers.IO) {
            val image = imageBitmap.asSkiaBitmap().let { Image.makeFromBitmap(it) }
            stream.use {
                try {
                    val data = image.encodeToData(
                        EncodedImageFormat.JPEG,
                        95
                    )!!
                    it.write(data.bytes)
                } catch (e: Exception) {
                    Logger.e(e) { "Failed to write bitmap to stream" }
                }
            }
        }

    actual fun getLocalIpAddresses(): List<String> {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.asSequence() }
                .filter {
                    !it.isLoopbackAddress && it is Inet4Address
                }
                .mapNotNull {
                    it.hostAddress
                }
                .toList()
        } catch (e: Exception) {
            Logger.e(e) { "Failed to get local IP address" }
            emptyList()
        }
    }

    actual fun monotonicTimeMs(): Long = System.nanoTime() / 1_000_000L

    actual fun getSystemSocksProxy(): Pair<String, Int>? {
        val socksUrl = "socket://www.example.com"
        val proxySelector = ProxySelector.getDefault()
        val proxies = proxySelector.select(URI(socksUrl))

        if (proxies.isNotEmpty()) {
            val firstProxy = proxies[0]
            if (firstProxy.type() == Proxy.Type.SOCKS &&
                firstProxy.address() != null
//                        firstProxy.protocolVersion() == 5
            ) {
                val addr = firstProxy?.address()
                if (addr is InetSocketAddress) {
                    return addr.hostName to addr.port
                }
            }
        }

        return null
    }
}