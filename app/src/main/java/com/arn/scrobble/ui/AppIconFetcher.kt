package com.arn.scrobble.ui

import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppIconFetcher(
    private val data: PackageName,
    private val options: Options
) : Fetcher {

    private val lock = Mutex()
    private var firstRequestReturned = false
    // flooding packageManager with too many requests before the first one has returned
    // causes a huge delay (5+ secs) on oxygenos

    override suspend fun fetch(): FetchResult {
        var icon = withContext(Dispatchers.IO) {
            if (!firstRequestReturned)
                lock.withLock {
                    options.context.packageManager.getApplicationIcon(data.packageName) // is BitmapDrawable
                }
            else
                options.context.packageManager.getApplicationIcon(data.packageName)
        }

        firstRequestReturned = true

        if (icon !is BitmapDrawable) // just in case
            icon = BitmapDrawable(
                options.context.resources,
                icon.toBitmap()
            )

        return DrawableResult(
            icon,
            false,
            DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<PackageName> {

        override fun create(
            data: PackageName,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return AppIconFetcher(data, options)
        }

    }
}

data class PackageName(val packageName: String)
