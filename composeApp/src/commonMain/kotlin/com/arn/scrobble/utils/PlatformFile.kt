package com.arn.scrobble.utils

import java.io.InputStream
import java.io.OutputStream

expect class PlatformFile(fileUri: String) {
    val uri: String

    suspend fun isWritable(): Boolean

    fun name(): String

    suspend fun writeAppend(block: suspend (OutputStream) -> Unit)

    suspend fun overwrite(block: suspend (OutputStream) -> Unit)

    suspend fun read(block: suspend (InputStream) -> Unit)

    suspend fun readLastNBytes(n: Long, block: suspend (InputStream, Boolean) -> Unit)

    fun length(): Long

    fun lastModified(): Long

    fun takePersistableUriPermission(readWrite: Boolean)

    fun releasePersistableUriPermission(readWrite: Boolean)
}