package com.arn.scrobble.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.SearchManager
import android.app.UiModeManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.CookieManager
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.billing.BaseBillingRepository
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.media.NLService
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MainPrefsMigration5
import com.arn.scrobble.pref.MainPrefsSerializer
import com.arn.scrobble.review.ReviewPrompter
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.AndroidStuff.application
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.Stuff.globalSnackbarFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.copied
import pano_scrobbler.composeapp.generated.resources.no_browser
import pano_scrobbler.composeapp.generated.resources.no_player
import pano_scrobbler.composeapp.generated.resources.tv_url_notice
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address

actual object PlatformStuff {

    actual val mainPrefs by lazy {
        MultiProcessDataStoreFactory.create(
            serializer = MainPrefsSerializer,
            migrations = listOf(
                MainPrefsMigration5(),
            ),
            corruptionHandler = null,
            produceFile = {
                File(filesDir, MainPrefs.FILE_NAME)
            },
        )
    }

    actual val billingRepository: BaseBillingRepository by lazy {
        BillingRepository(application, Stuff.billingClientData)
    }

    actual val filesDir by lazy { application.filesDir!! }

    actual val cacheDir by lazy { application.cacheDir!! }

    actual fun getDeviceIdentifier(): String {
        val name = Build.BRAND + "|" + Build.MODEL + "|" + Build.DEVICE + "|" + Build.BOARD
        return name.sha256()
    }

    actual val isDebug = BuildConfig.DEBUG

    actual val isJava8OrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    actual fun isNotificationListenerEnabled() =
        NotificationManagerCompat.getEnabledListenerPackages(application)
            .any { it == application.packageName }

    actual val isTv by lazy {
        val uiModeManager = ContextCompat.getSystemService(application, UiModeManager::class.java)!!
        uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    actual val supportsDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    actual val isDesktop = false

    actual val isNonPlayBuild = ExtrasConsts.isNonPlayBuild

    actual val platformSubstring = "android"

    actual fun isDkmaNeeded(): Boolean {
        val packages = Stuff.STARTUPMGR_INTENTS.map { it.first }.toSet()
        return packages.any {
            try {
                application.packageManager.getApplicationInfo(it, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    actual val isTestLab by lazy {
        Settings.System.getString(
            application.contentResolver,
            "firebase.test.lab"
        ) == "true"
    }

    actual fun String.toHtmlAnnotatedString() = AnnotatedString.fromHtml(this)


    actual fun isScrobblerRunning(): Boolean {
        val serviceComponent = ComponentName(application, NLService::class.java)
        val manager = ContextCompat.getSystemService(application, ActivityManager::class.java)!!
        val nlsService = try {
            manager.getRunningServices(Integer.MAX_VALUE)?.find { it.service == serviceComponent }
        } catch (e: SecurityException) {
            return true // just assume true to suppress the error message, if we don't have permission
        }

        nlsService ?: return false

        Logger.i(
            "${NLService::class.simpleName} - clientCount: ${nlsService.clientCount} process:${nlsService.process}"
        )

        return nlsService.clientCount > 0
    }


    actual fun openInBrowser(url: String) {
        if (isTv) {
            GlobalScope.launch {
                globalSnackbarFlow.emit(
                    PanoSnackbarVisuals(
                        message = getString(Res.string.tv_url_notice) + "\n" + url,
                        isError = false,
                        duration = SnackbarDuration.Long
                    )
                )
            }
            return
        }

        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            application.startActivity(browserIntent)
        } catch (e: ActivityNotFoundException) {
            GlobalScope.launch {
                globalSnackbarFlow.emit(
                    PanoSnackbarVisuals(
                        message = getString(Res.string.no_browser),
                        isError = true,
                    )
                )
            }
        }
    }


    actual suspend fun launchSearchIntent(
        musicEntry: MusicEntry,
        pkgName: String?,
    ) {
        val searchInSource = mainPrefs.data.map { it.searchInSource }.first()

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val searchQuery: String

            when (musicEntry) {
                is Artist -> {
                    searchQuery = musicEntry.name
                    putExtra(
                        MediaStore.EXTRA_MEDIA_FOCUS,
                        MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE
                    )
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, musicEntry.name)
                }

                is Album -> {
                    searchQuery = musicEntry.artist!!.name + " " + musicEntry.name
                    putExtra(
                        MediaStore.EXTRA_MEDIA_FOCUS,
                        MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE
                    )
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, musicEntry.artist.name)
                    putExtra(MediaStore.EXTRA_MEDIA_ALBUM, musicEntry.name)
                }

                is Track -> {
                    searchQuery = musicEntry.artist.name + " " + musicEntry.name
                    putExtra(
                        MediaStore.EXTRA_MEDIA_FOCUS,
                        MediaStore.Audio.Media.ENTRY_CONTENT_TYPE
                    )
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, musicEntry.artist.name)
                    putExtra(MediaStore.EXTRA_MEDIA_TITLE, musicEntry.name)
                    if (!musicEntry.album?.name.isNullOrEmpty()) {
                        putExtra(MediaStore.EXTRA_MEDIA_ALBUM, musicEntry.album.name)
                    }
                }
            }

            if (searchQuery.isBlank())
                return

            putExtra(SearchManager.QUERY, searchQuery)

            if (pkgName != null && billingRepository.isLicenseValid && searchInSource)
                `package` = pkgName
        }
        try {
            application.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (pkgName != null) {
                try {
                    intent.`package` = null
                    application.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    application.toast(Res.string.no_player)
                }
            } else
                application.toast(Res.string.no_player)
        }
    }

    actual fun isNotiChannelEnabled(channelId: String): Boolean {
        return when {
            isTv -> false

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                AndroidStuff.notificationManager.areNotificationsEnabled() &&
                        AndroidStuff.notificationManager.getNotificationChannel(channelId)?.importance != NotificationManager.IMPORTANCE_NONE
            }

            else -> true
        }
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<PanoDb> {
        val dbFile = application.getDatabasePath("pendingScrobbles")
        return Room.databaseBuilder<PanoDb>(
            context = application,
            name = dbFile.absolutePath
        )
            .setDriver(AndroidSQLiteDriver())
            .enableMultiInstanceInvalidation()

    }

    actual fun loadApplicationLabel(pkgName: String): String {
        return try {
            application.packageManager.getApplicationLabel(
                application.packageManager.getApplicationInfo(pkgName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkgName
        }
    }

    actual suspend fun getWebviewCookies(uri: String): Map<String, String> {
        CookieManager.getInstance().getCookie(uri)?.let {
            val map = mutableMapOf<String, String>()
            it.split(";").forEach { cookie ->
                val pair = cookie.trim().split("=", limit = 2)
                if (pair.size == 2 && pair.all { it.isNotEmpty() }) {
                    map[pair[0]] = pair[1]
                }
            }
            return map
        }
        return emptyMap()
    }

    actual fun clearWebviewCookies() {
        CookieManager.getInstance().removeAllCookies(null)
    }

    actual fun copyToClipboard(text: String) {
        val clipboard = ContextCompat.getSystemService(application, ClipboardManager::class.java)!!
        val clip = ClipData.newPlainText(BuildKonfig.APP_NAME, text)
        clipboard.setPrimaryClip(clip)
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
        stream.use {
            imageBitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 95, it)
        }
    }

    actual suspend fun promptForReview(activity: Any?) {
        ReviewPrompter(
            activity as? Activity,
            mainPrefs.data.first().lastReviewPromptTime
        ) { t ->
            mainPrefs.updateData { it.copy(lastReviewPromptTime = t) }
        }.showIfNeeded()
    }

    actual fun getWifiIpAddress(): String? {
        val connectivityManager =
            ContextCompat.getSystemService(
                application,
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

    actual fun monotonicTimeMs() = SystemClock.elapsedRealtime()
}