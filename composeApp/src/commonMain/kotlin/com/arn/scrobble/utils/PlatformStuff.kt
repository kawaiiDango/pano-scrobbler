package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.datastore.core.DataStore
import androidx.room.RoomDatabase
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.pref.MainPrefs
import java.io.File
import java.io.OutputStream

expect object PlatformStuff {

    val mainPrefs: DataStore<MainPrefs>

    val filesDir: File

    val cacheDir: File

    fun getDeviceIdentifier(): String

    val isJava8OrGreater: Boolean

    val supportsDynamicColors: Boolean

    fun isNotificationListenerEnabled(): Boolean

    val isTv: Boolean

    val isDesktop: Boolean

    val noUpdateCheck: Boolean

    val hasSystemLocaleStore: Boolean

    fun isScrobblerRunning(): Boolean

    fun openInBrowser(url: String)

    suspend fun launchSearchIntent(
        musicEntry: MusicEntry,
        appId: String?,
    )

    fun getDatabaseBuilder(): RoomDatabase.Builder<PanoDb>

    fun loadApplicationLabel(appId: String): String

    fun copyToClipboard(text: String)

    suspend fun writeBitmapToStream(imageBitmap: ImageBitmap, stream: OutputStream)

    fun getLocalIpAddresses(): List<String>

    fun monotonicTimeMs(): Long

    fun getSystemSocksProxy(): Pair<String, Int>?
}