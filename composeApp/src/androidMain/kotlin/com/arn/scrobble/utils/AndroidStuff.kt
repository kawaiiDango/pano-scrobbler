package com.arn.scrobble.utils

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.work.ForegroundInfo
import co.touchlab.kermit.Logger
import com.arn.scrobble.R
import com.arn.scrobble.main.MainActivity
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.pref.WidgetPrefsMigration1
import com.arn.scrobble.pref.WidgetPrefsSerializer
import com.arn.scrobble.utils.PlatformStuff.isNonPlayBuild
import com.arn.scrobble.utils.Stuff.SCROBBLER_PROCESS_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import java.io.File
import java.util.Locale

object AndroidStuff {
    lateinit var application: Application

    const val updateCurrentOrImmutable =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    val updateCurrentOrMutable =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

    val canShowPersistentNotiIfEnabled =
        Build.VERSION.SDK_INT in Build.VERSION_CODES.O..Build.VERSION_CODES.TIRAMISU || isNonPlayBuild

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


    fun createForegroundInfoNotification(title: String): ForegroundInfo {
        val context = application
        val intent = Intent(context, MainActivity::class.java)
        val launchIntent = PendingIntent.getActivity(
            context, 8, intent,
            updateCurrentOrImmutable
        )
        val notification = NotificationCompat.Builder(context, Stuff.CHANNEL_NOTI_PENDING)
            .setSmallIcon(R.drawable.vd_noti_persistent)
            .setPriority(Notification.PRIORITY_MIN)
            .setContentIntent(launchIntent)
            .apply { color = context.getColor(R.color.pinkNoti) }
            .setContentTitle(title)
            .build()

        return ForegroundInfo(
            title.hashCode(),
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else
                0
        )
    }


    val forcePersistentNoti by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU &&
                Build.MANUFACTURER.lowercase(Locale.ENGLISH) in arrayOf(
            Stuff.MANUFACTURER_HUAWEI,
            Stuff.MANUFACTURER_XIAOMI,
            Stuff.MANUFACTURER_SAMSUNG,
        )
    }


    val notificationManager by lazy {
        ContextCompat.getSystemService(application, NotificationManager::class.java)!!
    }


    val widgetPrefs by lazy {
        MultiProcessDataStoreFactory.create(
            serializer = WidgetPrefsSerializer,
            migrations = listOf(
                WidgetPrefsMigration1(),
            ),
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

    fun actionsToString(actions: Long): String {
        var s = "[\n"
        if (actions and PlaybackState.ACTION_PREPARE != 0L) {
            s += "\tACTION_PREPARE\n"
        }
        if (actions and PlaybackState.ACTION_PREPARE_FROM_MEDIA_ID != 0L) {
            s += "\tACTION_PREPARE_FROM_MEDIA_ID\n"
        }
        if (actions and PlaybackState.ACTION_PREPARE_FROM_SEARCH != 0L) {
            s += "\tACTION_PREPARE_FROM_SEARCH\n"
        }
        if (actions and PlaybackState.ACTION_PREPARE_FROM_URI != 0L) {
            s += "\tACTION_PREPARE_FROM_URI\n"
        }
        if (actions and PlaybackState.ACTION_PLAY != 0L) {
            s += "\tACTION_PLAY\n"
        }
        if (actions and PlaybackState.ACTION_PLAY_FROM_MEDIA_ID != 0L) {
            s += "\tACTION_PLAY_FROM_MEDIA_ID\n"
        }
        if (actions and PlaybackState.ACTION_PLAY_FROM_SEARCH != 0L) {
            s += "\tACTION_PLAY_FROM_SEARCH\n"
        }
        if (actions and PlaybackState.ACTION_PLAY_FROM_URI != 0L) {
            s += "\tACTION_PLAY_FROM_URI\n"
        }
        if (actions and PlaybackState.ACTION_PLAY_PAUSE != 0L) {
            s += "\tACTION_PLAY_PAUSE\n"
        }
        if (actions and PlaybackState.ACTION_PAUSE != 0L) {
            s += "\tACTION_PAUSE\n"
        }
        if (actions and PlaybackState.ACTION_STOP != 0L) {
            s += "\tACTION_STOP\n"
        }
        if (actions and PlaybackState.ACTION_SEEK_TO != 0L) {
            s += "\tACTION_SEEK_TO\n"
        }
        if (actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) {
            s += "\tACTION_SKIP_TO_NEXT\n"
        }
        if (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) {
            s += "\tACTION_SKIP_TO_PREVIOUS\n"
        }
        if (actions and PlaybackState.ACTION_SKIP_TO_QUEUE_ITEM != 0L) {
            s += "\tACTION_SKIP_TO_QUEUE_ITEM\n"
        }
        if (actions and PlaybackState.ACTION_FAST_FORWARD != 0L) {
            s += "\tACTION_FAST_FORWARD\n"
        }
        if (actions and PlaybackState.ACTION_REWIND != 0L) {
            s += "\tACTION_REWIND\n"
        }
        if (actions and PlaybackState.ACTION_SET_RATING != 0L) {
            s += "\tACTION_SET_RATING\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SET_REPEAT_MODE != 0L) {
            s += "\tACTION_SET_REPEAT_MODE\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE != 0L) {
            s += "\tACTION_SET_SHUFFLE_MODE\n"
        }
        if (actions and PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED != 0L) {
            s += "\tACTION_SET_CAPTIONING_ENABLED\n"
        }
        s += "]"
        return s
    }


    @RequiresApi(Build.VERSION_CODES.R)
    fun getScrobblerExitReasons(
        afterTime: Long = -1,
        printAll: Boolean = false,
    ): List<ApplicationExitInfo> {
        return try {
            val activityManager =
                ContextCompat.getSystemService(application, ActivityManager::class.java)!!
            val exitReasons = activityManager.getHistoricalProcessExitReasons(null, 0, 30)

            exitReasons.filter {
                it.processName == "${application.packageName}:$SCROBBLER_PROCESS_NAME"
//                        && it.reason == ApplicationExitInfo.REASON_OTHER
                        && it.timestamp > afterTime
            }.also {
                if (printAll) {
                    it.take(5).forEachIndexed { index, applicationExitInfo ->
                        Logger.w("${index + 1}. $applicationExitInfo", tag = "exitReasons")
                    }
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
        // Caused by java.lang.IllegalArgumentException at getHistoricalProcessExitReasons
        // Comparison method violates its general contract!
        // probably a samsung bug
    }


    fun getNotificationAction(
        icon: Int,
        emoji: String,
        text: String,
        pIntent: PendingIntent,
    ): NotificationCompat.Action {
        return NotificationCompat.Action(icon, text, pIntent)
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            application.packageManager.getPackageInfo(packageName, 0) != null
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