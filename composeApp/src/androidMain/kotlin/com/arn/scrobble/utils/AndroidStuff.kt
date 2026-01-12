package com.arn.scrobble.utils

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.datastore.core.MultiProcessDataStoreFactory
import co.touchlab.kermit.Logger
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.pref.WidgetPrefsSerializer
import com.arn.scrobble.utils.Stuff.SCROBBLER_PROCESS_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import java.io.File
import kotlin.properties.Delegates

object AndroidStuff {
    lateinit var applicationContext: Context

    var isMainProcess by Delegates.notNull<Boolean>()

    const val updateCurrentOrImmutable =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    val updateCurrentOrMutable =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

    val canShowPersistentNotiIfEnabled
        get() = !PlatformStuff.isTv &&
                (Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.TIRAMISU ||
                        VariantStuff.extrasProps.hasForegroundServiceSpecialUse)

//    @RequiresApi(Build.VERSION_CODES.Q)
//    @Throws(IOException::class)
//    fun savePictureQ(
//        displayName: String,
//        mimeType: String,
//        block: (OutputStream) -> Unit,
//    ) {
//        val values = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
//            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
//            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
//        }
//
//        var uri: Uri? = null
//
//        runCatching {
//            with(application.contentResolver) {
//                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
//                    uri = it // Keep uri reference so it can be removed on failure
//                    openOutputStream(it)?.use {
//                        block(it)
//                    } ?: throw IOException("Failed to open output stream.")
//
//                } ?: throw IOException("Failed to create new MediaStore record.")
//            }
//        }.getOrElse {
//            uri?.let { orphanUri ->
//                // Don't leave an orphan entry in the MediaStore
//                application.contentResolver.delete(orphanUri, null, null)
//            }
//
//            throw it
//        }
//    }

    fun isDkmaNeeded(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return manufacturer in arrayOf(
            Stuff.MANUFACTURER_HUAWEI,
            Stuff.MANUFACTURER_XIAOMI,
            Stuff.MANUFACTURER_SAMSUNG,
            Stuff.MANUFACTURER_ONEPLUS,
            Stuff.MANUFACTURER_OPPO,
            Stuff.MANUFACTURER_MEIZU,
            Stuff.MANUFACTURER_VIVO,
        )
    }


//    val forcePersistentNoti by lazy {
//        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
//                Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU &&
//                Build.MANUFACTURER.lowercase(Locale.ENGLISH) in arrayOf(
//            Stuff.MANUFACTURER_HUAWEI,
//            Stuff.MANUFACTURER_XIAOMI,
//            Stuff.MANUFACTURER_SAMSUNG,
//        )
//    }


    val notificationManager by lazy {
        ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)!!
    }


    val widgetPrefs by lazy {
        MultiProcessDataStoreFactory.create(
            serializer = WidgetPrefsSerializer,
            corruptionHandler = null,
            produceFile = {
                File(PlatformStuff.filesDir, WidgetPrefs.FILE_NAME)
            }
        )
    }


    fun Bundle?.dump(): String {
        this ?: return "null"
        var s = ""
        for (key in keySet().sortedDescending()) {
            s += try {
                val value = get(key) ?: "null"
                "$key= $value, "
            } catch (e: Exception) {
                "$key= $e, "
            }
        }
        return s
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getScrobblerExitReasons(afterTime: Long = -1): List<ApplicationExitInfo> {
        return try {
            val activityManager =
                ContextCompat.getSystemService(applicationContext, ActivityManager::class.java)!!
            val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 30)

            exitReasons.filter {
                it.processName == "${applicationContext.packageName}:$SCROBBLER_PROCESS_NAME"
//                        && it.reason == ApplicationExitInfo.REASON_OTHER
                        && it.timestamp > afterTime
            }
        } catch (e: Exception) {
            emptyList()
        }
        // Caused by java.lang.IllegalArgumentException at getHistoricalProcessExitReasons
        // Comparison method violates its general contract!
        // probably a samsung bug
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            applicationContext.packageManager.getPackageInfo(packageName, 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun MediaMetadata.dump() {
        val data = keySet().joinToString(separator = "\n") {
            var value: String? = getString(it)
            if (value == null)
                value = getLong(it).toString()
            if (value == "0")
                value = getBitmap(it)?.toString()
            if (value == null)
                value = getRating(it)?.toString()
            "$it: $value"
        }
        Logger.d { "MediaMetadata\n$data" }
    }

    fun Context.toast(strRes: StringResource, len: Int = Toast.LENGTH_SHORT) {
        GlobalScope.launch(Dispatchers.Main) {
            toast(org.jetbrains.compose.resources.getString(strRes), len)
        }
    }

    fun Context.toast(text: String, len: Int = Toast.LENGTH_SHORT) {
        try {
            Toast.makeText(this, text, len).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun Context.toast(@StringRes textRes: Int, len: Int = Toast.LENGTH_SHORT) {
        try {
            Toast.makeText(this, textRes, len).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}