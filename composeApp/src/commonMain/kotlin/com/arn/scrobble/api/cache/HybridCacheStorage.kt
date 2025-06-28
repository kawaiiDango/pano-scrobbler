package com.arn.scrobble.api.cache

import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url

class HybridCacheStorage(
    private val memoryCache: CacheStorage,
    private val fileCache: CacheStorage,
    private val minAgeSeconds: Int = 300,
) : CacheStorage {
    override suspend fun store(url: Url, data: CachedResponseData) {
        val maxAgeSeconds = data.headers["Cache-Control"]
            ?.split(",")
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("max-age=") }
            ?.substringAfter("=")
            ?.toIntOrNull() ?: 0

        return if (maxAgeSeconds > 0 && maxAgeSeconds <= minAgeSeconds) {
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
}