package com.arn.scrobble.imageloader

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.size.Precision
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.utils.Stuff
import java.io.File

private val musicEntryImageInterceptor = MusicEntryImageInterceptor()

fun clearMusicEntryImageCache(entry: MusicEntry) {
    musicEntryImageInterceptor.clearCacheForEntry(entry)
}

fun newImageLoader(
    context: PlatformContext,
): ImageLoader {
    return ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder()
                // Set the max size to 25% of the app's available memory.
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .components {
            add(MusicEntryMapper())
            add(musicEntryImageInterceptor)
            add(StarMapper())

            if (Stuff.isInDemoMode)
                add(DemoInterceptor())

            additionalComponents()
        }
        // Show a short crossfade when loading images asynchronously.
        .crossfade(true)
        .precision(Precision.INEXACT)
        .additionalOptions()
        .build()
}

private fun ImageLoader.Builder.additionalOptions(): ImageLoader.Builder {
    allowHardware(false)
    diskCache {
        DiskCache.Builder()
            .directory(File(PlatformStuff.cacheDir, "image_cache"))
            .maxSizeBytes(50 * 1024 * 1024) // 50MB
            .build()
    }
    return this
}

private fun ComponentRegistry.Builder.additionalComponents(): ComponentRegistry.Builder {
    add(AppIconKeyer())
    add(AppIconFetcher.Factory())
    return this
}