package com.arn.scrobble.imageloader

import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.arn.scrobble.ui.PackageName
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
            icon = icon.toBitmap().toDrawable(options.context.resources)

        return ImageFetchResult(
            icon.asImage(),
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


