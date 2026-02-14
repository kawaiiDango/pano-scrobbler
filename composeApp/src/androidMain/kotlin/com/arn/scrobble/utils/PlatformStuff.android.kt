package com.arn.scrobble.utils

import android.app.ActivityManager
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.net.toUri
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.room.ExperimentalRoomApi
import androidx.room.Room
import androidx.room.RoomDatabase
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.media.NLService
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.AndroidStuff.applicationContext
import com.arn.scrobble.utils.AndroidStuff.toast
import com.arn.scrobble.utils.Stuff.globalSnackbarFlow
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.copied
import pano_scrobbler.composeapp.generated.resources.no_browser
import pano_scrobbler.composeapp.generated.resources.no_player
import pano_scrobbler.composeapp.generated.resources.tv_url_notice
import java.io.File
import java.io.OutputStream
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

actual object PlatformStuff {

    actual val mainPrefs by lazy {
        MultiProcessDataStoreFactory.create(
            serializer = MainPrefs.dataStoreSerializer,
            migrations = MainPrefs.migrations(),
            corruptionHandler = null,
            produceFile = {
                File(filesDir, MainPrefs.FILE_NAME)
            },
        )
    }

    actual val filesDir by lazy { applicationContext.filesDir!! }

    actual val cacheDir by lazy { applicationContext.cacheDir!! }

    actual val logsDir by lazy { File(filesDir, "logs").also { it.mkdirs() } }

    actual fun getDeviceIdentifier(): String {
        val name = Build.BRAND + "|" + Build.MODEL + "|" + Build.DEVICE + "|" + Build.BOARD
        return Stuff.sha256Truncated(name)
    }

    actual val isJava8OrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    actual val hasSystemLocaleStore = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    actual fun isNotificationListenerEnabled(): Boolean {
        // adapted from NotificationManagerCompat.java

        val enabledNotificationListeners = try {
            Settings.Secure.getString(
                applicationContext.contentResolver,
                "enabled_notification_listeners"
            )
        } catch (e: SecurityException) {
            return false
        }

        val nlsComponentStr = "${applicationContext.packageName}/${NLService::class.qualifiedName}"
        // check for the exact component name instead of just package name
        return enabledNotificationListeners?.split(":")?.any { it == nlsComponentStr } == true
    }

    actual val isTv by lazy {
        val uiModeManager =
            applicationContext.getSystemService(UiModeManager::class.java)!!
        uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    actual val supportsDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    actual const val isDesktop = false

    actual const val noUpdateCheck = true


    actual fun isScrobblerRunning(): Boolean {
        val serviceComponent = ComponentName(applicationContext, NLService::class.java)
        val manager =
            applicationContext.getSystemService(ActivityManager::class.java)!!
        val nlsService = try {
            manager.getRunningServices(Integer.MAX_VALUE)?.find { it.service == serviceComponent }
        } catch (e: SecurityException) {
            Logger.e(e) { "isScrobblerRunning: no permission to get running services" }
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
                        longDuration = true
                    )
                )
            }
            return
        }

        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            applicationContext.startActivity(browserIntent)
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
        appId: String?,
    ) {
        val searchUrlTemplate = mainPrefs.data.map { p ->
            p.searchUrlTemplate.takeIf { !p.usePlayFromSearchP }
        }.first()

        val searchQuery = when (musicEntry) {
            is Artist -> musicEntry.name
            is Album -> musicEntry.artist!!.name + " " + musicEntry.name
            is Track -> musicEntry.artist.name + " " + musicEntry.name
        }

        if (searchQuery.isBlank())
            return

        if (searchUrlTemplate != null) {
            val searchUrl = searchUrlTemplate.replace(
                "\$query",
                searchQuery.encodeURLPath()
            )
            openInBrowser(searchUrl)
            return
        }

        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            when (musicEntry) {
                is Artist -> {
                    putExtra(
                        MediaStore.EXTRA_MEDIA_FOCUS,
                        MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE
                    )
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, musicEntry.name)
                }

                is Album -> {
                    putExtra(
                        MediaStore.EXTRA_MEDIA_FOCUS,
                        MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE
                    )
                    putExtra(MediaStore.EXTRA_MEDIA_ARTIST, musicEntry.artist!!.name)
                    putExtra(MediaStore.EXTRA_MEDIA_ALBUM, musicEntry.name)
                }

                is Track -> {
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

            putExtra(SearchManager.QUERY, searchQuery)

            if (appId != null)
                `package` = appId
        }
        try {
            applicationContext.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            if (appId != null) {
                try {
                    intent.`package` = null
                    applicationContext.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    applicationContext.toast(Res.string.no_player)
                }
            } else
                applicationContext.toast(Res.string.no_player)
        }
    }

    @OptIn(ExperimentalRoomApi::class)
    actual fun getDatabaseBuilder(): RoomDatabase.Builder<PanoDb> {
        val dbFile = applicationContext.getDatabasePath("pendingScrobbles")
        return Room.databaseBuilder<PanoDb>(
            context = applicationContext,
            name = dbFile.absolutePath
        )
//            .setDriver(AndroidSQLiteDriver())
            .enableMultiInstanceInvalidation()
            .setAutoCloseTimeout(7, TimeUnit.MINUTES)
    }

    private val appLabelCache = mutableMapOf<String, String>()
    actual fun loadApplicationLabel(appId: String): String {
        return appLabelCache[appId] ?: try {
            val label = applicationContext.packageManager.getApplicationLabel(
                applicationContext.packageManager.getApplicationInfo(appId, 0)
            ).toString()
            appLabelCache[appId] = label
            label

        } catch (e: PackageManager.NameNotFoundException) {
            appLabelCache[appId] = ""
            ""
        }
    }

    actual fun copyToClipboard(text: String) {
        val clipboard =
            applicationContext.getSystemService(ClipboardManager::class.java)!!
        val clip = ClipData.newPlainText(BuildKonfig.APP_NAME, text)
        clipboard.setPrimaryClip(clip)

        // Starting in Android 13, the system displays a standard visual confirmation when content is added to the clipboard.

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            GlobalScope.launch {
                globalSnackbarFlow.tryEmit(
                    PanoSnackbarVisuals(
                        getString(Res.string.copied),
                    )
                )
            }
        }
    }

    actual suspend fun writeBitmapToStream(imageBitmap: ImageBitmap, stream: OutputStream) {
        withContext(Dispatchers.IO) {
            stream.use {
                imageBitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 95, it)
            }
        }
    }

    actual fun getLocalIpAddresses(): List<String> {
        val connectivityManager =
            applicationContext.getSystemService(ConnectivityManager::class.java)!!
        val activeNetwork = connectivityManager.activeNetwork
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)

        return linkProperties?.linkAddresses
            ?.filter {
                val inetAddress = it.address
                !inetAddress.isLoopbackAddress && inetAddress is Inet4Address
            }
            ?.mapNotNull { it.address.hostAddress }
            .orEmpty()
    }

    actual fun monotonicTimeMs() = SystemClock.elapsedRealtime()

    actual fun getSystemSocksProxy(): Pair<String, Int>? = null
}