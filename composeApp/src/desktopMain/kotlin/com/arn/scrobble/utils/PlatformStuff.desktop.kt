package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.datastore.core.DataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.DesktopWebView
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.onboarding.WebViewEventFlows
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MainPrefsSerializer
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.Stuff.billingClientData
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.OutputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URI

actual object PlatformStuff {

    actual val isDebug = BuildKonfig.DEBUG

    actual val isJava8OrGreater = true

    actual fun isNotificationListenerEnabled() = true

    actual val isTv = false

    actual val supportsDynamicColors = false

    actual val isDesktop = true

    actual val isNonPlayBuild = true

    actual val platformSubstring = DesktopStuff.os.name.lowercase()

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
        val desktop = Desktop.getDesktop()
        if (!Desktop.isDesktopSupported() || !desktop.isSupported(Desktop.Action.BROWSE)) {
            val snackbarData = PanoSnackbarVisuals(
                message = "Failed to open URL: $url",
                isError = true,
            )
            Stuff.globalSnackbarFlow.tryEmit(snackbarData)
            return
        }
        desktop.browse(URI(url))
    }

    actual fun String.toHtmlAnnotatedString() =
        AnnotatedString(this) // todo implement if possible


    actual fun isScrobblerRunning(): Boolean {
        // todo implement after porting NLService
        return true
    }

    actual fun getDeviceIdentifier(): String {
        return PanoNativeComponents.getMachineId().sha256Truncated()
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

    actual fun loadApplicationLabel(appId: String): String = appId

    actual suspend fun getWebviewCookies(uri: String): Map<String, String> {
        val maybeCookies = withTimeoutOrNull(1_000) {
            WebViewEventFlows.cookies.onStart {
                DesktopWebView.getWebViewCookiesFor(uri)
            }.first()
        }

        if (maybeCookies != null) {
            val (incomingUri, cookies) = maybeCookies

            if (incomingUri.startsWith(uri))
                return cookies.associate {
                    val (name, value) = it.split("=", limit = 2)
                    name to value
                }
        } else {
            Logger.e("WebViewEvent timed out")
        }

        return emptyMap()
    }

    actual fun clearWebviewCookies() {
        val cookieManager = CookieHandler.getDefault() as? CookieManager
        cookieManager?.cookieStore?.removeAll()
    }

    actual fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
        Stuff.globalSnackbarFlow.tryEmit(
            PanoSnackbarVisuals(
                message = "Copied",
                isError = false,
            )
        )
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