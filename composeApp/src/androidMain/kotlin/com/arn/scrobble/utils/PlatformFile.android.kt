package com.arn.scrobble.utils

import android.content.Intent
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.InputStream
import java.io.OutputStream

actual class PlatformFile actual constructor(private val fileUri: String) {

    actual val uri = fileUri

    private val documentFile by lazy {
        DocumentFile.fromSingleUri(AndroidStuff.application, fileUri.toUri())!!
    }

    private val contentResolver by lazy { AndroidStuff.application.contentResolver }


    actual fun isFileOk() =
        documentFile.exists() && documentFile.canWrite() && documentFile.canRead()

    actual fun getFileName() = documentFile.name!!

    actual fun length() = documentFile.length()

    actual fun lastModified() = documentFile.lastModified()

    actual suspend fun writeAppend(block: suspend (OutputStream) -> Unit) {
        contentResolver.openOutputStream(documentFile.uri, "wa")?.use {
            block(it)
        }
    }

    actual suspend fun overwrite(block: suspend (OutputStream) -> Unit) {
        contentResolver.openOutputStream(documentFile.uri, "w")?.use {
            block(it)
        }
    }

    actual suspend fun read(block: suspend (InputStream) -> Unit) {
        contentResolver.openInputStream(documentFile.uri)?.use {
            block(it)
        }
    }

    actual fun takePersistableUriPermission(readWrite: Boolean) {
        val flags = if (readWrite) {
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        } else {
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        AndroidStuff.application.contentResolver.takePersistableUriPermission(
            documentFile.uri,
            flags
        )
    }

    actual fun releasePersistableUriPermission() {
        AndroidStuff.application.contentResolver.releasePersistableUriPermission(
            documentFile.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

}