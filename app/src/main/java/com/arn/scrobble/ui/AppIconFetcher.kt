package com.arn.scrobble.ui

import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppIconFetcher : Fetcher<PackageName> {

    private val lock = Mutex()
    private var firstRequestReturned = false
    // flooding packageManager with too many requests before the first one has returned
    // causes a huge delay (5+ secs) on oxygenos

    override suspend fun fetch(
        pool: BitmapPool,
        data: PackageName,
        size: Size,
        options: Options
    ): FetchResult {
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

    override fun key(data: PackageName) = "package:" + data.packageName

    override fun handles(data: PackageName) = true
}

data class PackageName(val packageName: String)
