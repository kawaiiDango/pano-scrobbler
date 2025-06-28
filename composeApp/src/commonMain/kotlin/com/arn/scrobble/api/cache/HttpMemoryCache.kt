package com.arn.scrobble.api.cache

import androidx.collection.LruCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HttpMemoryCache(maxSize: Int) : CacheStorage {
    private val cache = LruCache<Url, CachedResponseData>(maxSize)
    private val mutex = Mutex()

    override suspend fun store(
        url: Url,
        data: CachedResponseData
    ) {
        if (data.statusCode.isSuccess()) {
            mutex.withLock {
                cache.put(url, data)
            }
        }
    }

    override suspend fun find(url: Url, varyKeys: Map<String, String>): CachedResponseData? {
        return mutex.withLock {
            val entry = cache[url]

            if (entry != null && entry.expires < GMTDate()) {
                cache.remove(url)
                null
            } else {
                entry
            }
        }
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> {
        return find(url, emptyMap())?.let { setOf(it) } ?: emptySet()
    }

    override suspend fun remove(
        url: Url,
        varyKeys: Map<String, String>
    ) {
        mutex.withLock {
            cache.remove(url)
        }
    }

    override suspend fun removeAll(url: Url) {
        mutex.withLock {
            remove(url, emptyMap())
        }
    }
}