package com.arn.scrobble.utils

import android.content.Intent
import android.provider.DocumentsContract
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream


actual class PlatformFile actual constructor(fileUri: String) {

    actual val uri = fileUri

    private val parsedUri = fileUri.toUri()

    private val contentResolver by lazy { AndroidStuff.applicationContext.contentResolver }

    actual suspend fun isWritable() = withContext(Dispatchers.IO) {
        val cursor = contentResolver.query(
            parsedUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_FLAGS
            ),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val flags =
                    it.getInt(it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_FLAGS))
                val canWrite = (flags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE) != 0
                canWrite
            } else {
                false
            }
        } ?: false
    }

    actual fun name(): String {
        val cursor = contentResolver.query(
            parsedUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        throw IllegalStateException("File name not found")
    }

    actual fun length(): Long {
        val cursor = contentResolver.query(
            parsedUri,
            arrayOf(DocumentsContract.Document.COLUMN_SIZE),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return 0L
    }

    actual fun lastModified(): Long {
        val cursor = contentResolver.query(
            parsedUri,
            arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return 0L
    }

    actual suspend fun writeAppend(block: suspend (OutputStream) -> Unit) {
        withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(parsedUri, "wa")?.use {
                block(it)
            }
        }
    }

    actual suspend fun overwrite(block: suspend (OutputStream) -> Unit) {
        withContext(Dispatchers.IO) {
            contentResolver.openOutputStream(parsedUri, "w")?.use {
                block(it)
            }
        }
    }

    actual suspend fun read(block: suspend (InputStream) -> Unit) {
        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(parsedUri)?.use {
                block(it)
            }
        }
    }

    actual suspend fun readLastNBytes(n: Long, block: suspend (InputStream, Boolean) -> Unit) {
        withContext(Dispatchers.IO) {
            contentResolver.openFileDescriptor(parsedUri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val length = pfd.statSize
                    val nExceedsLength = length > n

                    if (nExceedsLength)
                    // even gdrive seems to support this
                        fis.channel.position(length - n)
                    block(fis, nExceedsLength)
                }
            }
        }
    }

    actual fun takePersistableUriPermission(readWrite: Boolean) {
        val flags = if (readWrite) {
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        } else {
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        AndroidStuff.applicationContext.contentResolver.takePersistableUriPermission(
            parsedUri,
            flags
        )
    }

    actual fun releasePersistableUriPermission(readWrite: Boolean) {
        val flags = if (readWrite) {
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        } else {
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            AndroidStuff.applicationContext.contentResolver.releasePersistableUriPermission(
                parsedUri,
                flags
            )
        } catch (e: SecurityException) {
            // usually: No permission grants found for UID 10375 and Uri content://media/...
            // ignore
        }
    }
}