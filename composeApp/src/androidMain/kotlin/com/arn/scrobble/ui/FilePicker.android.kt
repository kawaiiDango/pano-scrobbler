package com.arn.scrobble.ui

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arn.scrobble.utils.AndroidStuff.applicationContext
import com.arn.scrobble.utils.PlatformFile
import java.io.IOException

@Composable
actual fun FilePicker(
    show: Boolean,
    mode: FilePickerMode,
    type: FileType,
    onDismiss: () -> Unit,
    onFilePicked: (PlatformFile) -> Unit,
) {
    val extensions by remember(type) {
        mutableStateOf(
            getExtensionsForFilePicker(type)
        )
    }

    val createMimeType = when (type) {
        FileType.LOG -> "text/plain"
        FileType.JSON -> "application/json"
        FileType.PHOTO -> "image/jpeg"
        else -> null
    }

    val openMimeType = when (type) {
        FileType.LOG -> "text/plain"
        FileType.JSON -> "application/json"
        FileType.PHOTO -> "image/*"
        else -> "*/*"
    }

    val createLauncher = if (mode is FilePickerMode.Save) {
        rememberLauncherForActivityResult(
            if (createMimeType != null) {
                ActivityResultContracts.CreateDocument(createMimeType)
            } else {
                ActivityResultContracts.CreateDocument()
            }
        ) { uri: Uri? ->
            if (uri != null) {
                onFilePicked(PlatformFile(uri.toString()))
            }
            onDismiss()
        }
    } else {
        null
    }

    val openLauncher = if (mode is FilePickerMode.Open) {
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                onFilePicked(PlatformFile(uri.toString()))
            }
            onDismiss()
        }
    } else {
        null
    }

    val imagePickerLauncher =
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable() && type == FileType.PHOTO) {
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    onFilePicked(PlatformFile(uri.toString()))
                }
                onDismiss()
            }
        } else {
            null
        }


    fun addExtensionIfNeeded(fileName: String) =
        if (createMimeType != null)
            fileName
        else
            fileName + extensions.first()


    LaunchedEffect(show) {
        if (show) {
            when (mode) {
                is FilePickerMode.Save -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && type == FileType.PHOTO) {
                        savePictureQ(mode.title, createMimeType!!) { uri ->
                            onFilePicked(PlatformFile(uri))
                        }
                    } else {
                        createLauncher!!.launch(addExtensionIfNeeded(mode.title))
                    }
                }

                is FilePickerMode.Open -> {
                    if (type == FileType.PHOTO && imagePickerLauncher != null) {
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        openLauncher!!.launch(arrayOf(openMimeType))
                    }
                }
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
@Throws(IOException::class)
private fun savePictureQ(
    displayName: String,
    mimeType: String,
    onSave: (String) -> Unit,
) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    var uri: Uri? = null

    runCatching {
        with(applicationContext.contentResolver) {
            insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.also {
                onSave(it.toString())
                uri = it // Keep uri reference so it can be removed on failure
//                openOutputStream(it)?.use {
//                    block(it)
//                } ?: throw java.io.IOException("Failed to open output stream.")

            } ?: throw IOException("Failed to create new MediaStore record.")
        }
    }.getOrElse {
        uri?.let { orphanUri ->
            // Don't leave an orphan entry in the MediaStore
            applicationContext.contentResolver.delete(orphanUri, null, null)
        }
        throw it
    }
}