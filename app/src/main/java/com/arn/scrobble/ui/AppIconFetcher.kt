package com.arn.scrobble.ui

import coil3.ImageLoader
import coil3.asCoilImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.arn.scrobble.BuildConfig
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
        val icon = withContext(Dispatchers.IO) {
            if (!firstRequestReturned)
                lock.withLock {
                    options.context.packageManager.getApplicationIcon(data.packageName) // is BitmapDrawable
                }
            else
                options.context.packageManager.getApplicationIcon(data.packageName)
        }

        firstRequestReturned = true

//        if (icon !is BitmapDrawable) // just in case
//            icon = BitmapDrawable(
//                options.context.resources,
//                icon.toBitmap()
//            )


        return ImageFetchResult(
            icon.asCoilImage(),
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


data class PackageName(val packageName: String) {
    override fun toString(): String {
        return if (BuildConfig.DEBUG)
            super.toString()
        else
            "PackageName(packageName=redacted)"
    }
}


