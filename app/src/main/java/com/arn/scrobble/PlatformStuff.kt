package com.arn.scrobble

import android.app.Application
import android.app.NotificationManager
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.datastore.core.MultiProcessDataStoreFactory
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.MainPrefsMigration5
import com.arn.scrobble.pref.MainPrefsSerializer
import io.ktor.util.encodeBase64
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest

object PlatformStuff {
    // not a leak
    lateinit var application: Application

    val mainPrefs by lazy {
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

    val notificationManager by lazy {
        ContextCompat.getSystemService(
            application,
            NotificationManager::class.java
        )!!
    }

    val filesDir by lazy { application.filesDir!! }

    val cacheDir by lazy { application.cacheDir!! }

    fun getDeviceIdentifier(): String {
        val name = Build.BRAND + "|" + Build.MODEL + "|" + Build.DEVICE + "|" + Build.BOARD
        return name.sha256()
    }

    private fun String.sha256() =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .encodeBase64()

    @RequiresApi(Build.VERSION_CODES.Q)
    @Throws(IOException::class)
    fun savePictureQ(
        displayName: String,
        mimeType: String,
        block: (OutputStream) -> Unit,
    ) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        var uri: Uri? = null

        runCatching {
            with(application.contentResolver) {
                insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                    uri = it // Keep uri reference so it can be removed on failure
                    openOutputStream(it)?.use {
                        block(it)
                    } ?: throw IOException("Failed to open output stream.")

                } ?: throw IOException("Failed to create new MediaStore record.")
            }
        }.getOrElse {
            uri?.let { orphanUri ->
                // Don't leave an orphan entry in the MediaStore
                application.contentResolver.delete(orphanUri, null, null)
            }

            throw it
        }
    }
}