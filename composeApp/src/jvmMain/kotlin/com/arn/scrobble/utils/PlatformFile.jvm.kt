package com.arn.scrobble.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

actual class PlatformFile actual constructor(private val fileUri: String) {

    private val file by lazy { File(URI(fileUri)) }

    actual val uri = fileUri

    actual suspend fun isWritable() = withContext(Dispatchers.IO) {
        file.exists() && file.canWrite() && file.canRead()
    }

    actual fun name(): String = file.name

    actual fun length() = file.length()

    actual fun lastModified() = file.lastModified()

    actual suspend fun writeAppend(block: suspend (OutputStream) -> Unit) {
        withContext(Dispatchers.IO) {
            FileOutputStream(file, true).use {
                block(it)
            }
        }
    }

    actual suspend fun overwrite(block: suspend (OutputStream) -> Unit) {
        withContext(Dispatchers.IO) {
            FileOutputStream(file, false).use {
                block(it)
            }
        }
    }

    actual suspend fun read(block: suspend (InputStream) -> Unit) {
        withContext(Dispatchers.IO) {
            FileInputStream(file).use {
                block(it)
            }
        }
    }

    actual suspend fun readLastNBytes(n: Long, block: suspend (InputStream, Boolean) -> Unit) {
        withContext(Dispatchers.IO) {
            FileInputStream(file).use { fis ->
                val length = file.length()
                val nExceedsLength = length > n

                if (nExceedsLength)
                    fis.channel.position(length - n)
                block(fis, nExceedsLength)
            }
        }
    }

    actual fun takePersistableUriPermission(readWrite: Boolean) {
    }

    actual fun releasePersistableUriPermission(readWrite: Boolean) {
    }

}