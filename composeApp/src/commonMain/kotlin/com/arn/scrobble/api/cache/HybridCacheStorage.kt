package com.arn.scrobble.api.cache

import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.client.plugins.cache.storage.FileStorage
import io.ktor.http.Url
import kotlinx.coroutines.sync.Semaphore
import java.io.File

class HybridCacheStorage(
    private val memoryCache: CacheStorage,
    private val cacheDir: File,
    private val minAgeSeconds: Long = 300,
    private val maxSizeMb: Long = 40,
) : CacheStorage {
    private val fileCache = FileStorage(cacheDir)
    private var initalTrimmedSemaphore = Semaphore(1)

    override suspend fun store(url: Url, data: CachedResponseData) {
        val maxAgeSeconds = data.headers["Cache-Control"]
            ?.split(",")
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("max-age=") }
            ?.substringAfter("=")
            ?.toIntOrNull() ?: 0

        if (initalTrimmedSemaphore.tryAcquire()) {
            cleanupCache()
        }

        return if (maxAgeSeconds in 1..minAgeSeconds) {
            memoryCache.store(url, data)
        } else {
            fileCache.store(url, data)
        }
    }

    override suspend fun find(url: Url, varyKeys: Map<String, String>): CachedResponseData? {
        return memoryCache.find(url, varyKeys) ?: fileCache.find(url, varyKeys)
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> {
        return memoryCache.findAll(url) + fileCache.findAll(url)
    }

    override suspend fun remove(
        url: Url,
        varyKeys: Map<String, String>
    ) {
        memoryCache.remove(url, varyKeys)
        fileCache.remove(url, varyKeys)
    }

    override suspend fun removeAll(url: Url) {
        memoryCache.removeAll(url)
        fileCache.removeAll(url)
    }

    /**
     * Cleans up the cache by removing the oldest files until within size limit.
     */
    private fun cleanupCache() {
        val files = cacheDir.listFiles { it.isFile } ?: return

        if (files.isEmpty()) return

        // Calculate total size
        val totalSize = files.sumOf { it.length() }
        val maxSizeBytes = maxSizeMb * 1024 * 1024

        if (totalSize <= maxSizeBytes) {
            return // Already within limit
        }

        // Sort by last modified time (oldest first)
        val sortedFiles = files.sortedBy { it.lastModified() }

        var currentSize = totalSize
        var deletedCount = 0

        for (file in sortedFiles) {
            if (currentSize <= maxSizeBytes) break

            val fileSize = file.length()
            if (file.delete()) {
                currentSize -= fileSize
                deletedCount++
            }
        }
    }

}