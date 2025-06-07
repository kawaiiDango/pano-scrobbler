package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.datastore.core.DataStore
import androidx.room.RoomDatabase
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MainPrefs
import java.io.File
import java.io.OutputStream

expect object PlatformStuff {

    val mainPrefs: DataStore<MainPrefs>

    val billingRepository: BaseBillingRepository

    val filesDir: File

    val cacheDir: File

    fun getDeviceIdentifier(): String

    val isDebug: Boolean

    val isJava8OrGreater: Boolean

    val supportsDynamicColors: Boolean

    fun isNotificationListenerEnabled(): Boolean

    val isTv: Boolean

    val isDesktop: Boolean

    val isNonPlayBuild: Boolean

    val platformSubstring: String

    val isTestLab: Boolean

    fun String.toHtmlAnnotatedString(): AnnotatedString

    fun isScrobblerRunning(): Boolean

    fun openInBrowser(url: String)

    suspend fun launchSearchIntent(
        musicEntry: MusicEntry,
        pkgName: String?,
    )

    fun isNotiChannelEnabled(channelId: String): Boolean

    fun getDatabaseBuilder(): RoomDatabase.Builder<PanoDb>

    fun loadApplicationLabel(pkgName: String): String

    suspend fun getWebviewCookies(uri: String): Map<String, String>

    fun clearWebviewCookies()

    fun copyToClipboard(text: String)

    fun writeBitmapToStream(imageBitmap: ImageBitmap, stream: OutputStream)

    suspend fun promptForReview(activity: Any?)

    fun getWifiIpAddress(): String?

    fun monotonicTimeMs(): Long
}