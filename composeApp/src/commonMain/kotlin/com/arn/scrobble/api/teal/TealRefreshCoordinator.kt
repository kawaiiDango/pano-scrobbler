package com.arn.scrobble.api.teal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class TealRefreshCoordinator(private val lockFile: File? = null) {
    private val mutex = Mutex()

    suspend fun <T> rotate(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        var locked = false
        var file: RandomAccessFile? = null
        var fileLock: FileLock? = null
        try {
            // Keep acquisitions in this scope even if timeout/cancellation wins as they complete.
            val acquired = withTimeoutOrNull(10.seconds) {
                mutex.lock()
                locked = true
                if (lockFile != null) {
                    val opened = RandomAccessFile(lockFile, "rw").also { file = it }
                    while (fileLock == null) {
                        fileLock = opened.channel.tryLock()
                        if (fileLock == null) delay(100.milliseconds)
                    }
                }
                true
            }
            if (acquired != true) throw IOException("Timed out waiting to refresh the teal.fm session.")
            block()
        } finally {
            try {
                try { fileLock?.release() } finally { file?.close() }
            } finally {
                if (locked) mutex.unlock()
            }
        }
    }

    companion object {
        val processLocal = TealRefreshCoordinator()
    }
}
