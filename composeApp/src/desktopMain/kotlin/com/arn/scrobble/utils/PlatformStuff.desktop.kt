package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.text.AnnotatedString
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
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MainPrefsSerializer
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.Stuff.billingClientData
import com.arn.scrobble.utils.Stuff.globalSnackbarFlow
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.copied
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URI

actual object PlatformStuff {

    actual val isDebug = true

    actual val isJava8OrGreater = true

    actual fun isNotificationListenerEnabled() = true

    actual val isTv = false

    actual val supportsDynamicColors = false

    actual val isDesktop = true

    actual val isNonPlayBuild = true

    actual val platformSubstring = when (DesktopStuff.os) {
        DesktopStuff.Os.Macos -> "macos"
        DesktopStuff.Os.Windows -> "windows"
        DesktopStuff.Os.Linux -> "linux"
    }

    actual fun isDkmaNeeded() = false

    actual val isTestLab = false

    actual val filesDir by lazy {
        File(DesktopStuff.appDataRoot, "data")
    }

    actual val cacheDir by lazy {
        File(DesktopStuff.appDataRoot, "cache")
    }

    actual val mainPrefs by lazy {
        DataStoreFactory.create(
            serializer = MainPrefsSerializer,
            corruptionHandler = null,
            produceFile = {
                File(filesDir, MainPrefs.FILE_NAME)
            },
        )
    }

    actual val billingRepository: BaseBillingRepository by lazy {
        BillingRepository(null, billingClientData)
    }

    actual fun openInBrowser(url: String) {
        try {
            Desktop.getDesktop().browse(URI(url))
        } catch (e: IOException) {
            Logger.e("Error opening an URL in browser")
        }
    }

    actual fun String.toHtmlAnnotatedString() =
        AnnotatedString(this) // todo implement if possible


    actual fun isScrobblerRunning(): Boolean {
        // todo implement after porting NLService
        return true
    }

    actual fun getDeviceIdentifier(): String {
        val name = PanoNativeComponents.getMachineId()
        return name.sha256()
    }

    actual suspend fun launchSearchIntent(
        musicEntry: MusicEntry,
        pkgName: String?,
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

    actual fun isNotiChannelEnabled(channelId: String): Boolean {
        // todo implement
        return false
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<PanoDb> {
        val dbFile = File(filesDir, "panodb.db")
        return Room.databaseBuilder<PanoDb>(
            name = dbFile.absolutePath
        )
            .setDriver(BundledSQLiteDriver())

    }

    actual fun loadApplicationLabel(pkgName: String): String = pkgName

    actual fun getWebviewCookies(uri: String): Map<String, String> {
        val cookiesStr = CookieHandler.getDefault().get(URI(uri), emptyMap()).values.firstOrNull()

        return cookiesStr
            ?.joinToString("; ")
            ?.split(";")
            ?.associate {
                val (name, value) = it.trim().split("=", limit = 2)
                name to value
            } ?: emptyMap()
    }

    actual fun clearWebviewCookies() {
        val cookieManager = CookieHandler.getDefault() as? CookieManager
        cookieManager?.cookieStore?.removeAll()
    }

    actual fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
        GlobalScope.launch {
            globalSnackbarFlow.emit(
                PanoSnackbarVisuals(
                    message = getString(Res.string.copied),
                    isError = false,
                )
            )
        }
    }

    actual fun writeBitmapToStream(imageBitmap: ImageBitmap, stream: OutputStream) {
        val image = imageBitmap.asSkiaBitmap().let { Image.makeFromBitmap(it) }
        stream.use {
            try {
                val data = image.encodeToData(
                    EncodedImageFormat.JPEG,
                    95
                )!!
                it.write(data.bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual suspend fun promptForReview(activity: Any?) {
    }

    actual fun getWifiIpAddress(): String? = null

    actual fun monotonicTimeMs(): Long = System.nanoTime() / 1_000_000L
}