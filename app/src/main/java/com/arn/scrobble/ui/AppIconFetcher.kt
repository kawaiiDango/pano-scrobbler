package com.arn.scrobble.ui

import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.LocaleList
import androidx.collection.LruCache
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

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


data class PackageName(val packageName: String) {

    val englishLabel: String?
        get() {
            cacheIt()
            return labelCache[packageName]
        }

    val version: String?
        get() {
            cacheIt()
            return versionCache[packageName]
        }

    @Synchronized
    private fun cacheIt() {
        if (labelCache[packageName] != null)
            return

        val _label: String
        val _version: String

        try {
            val pkgInfo = App.context.packageManager.getPackageInfo(packageName, 0)
            val appInfo = pkgInfo.applicationInfo
            val configuration = Configuration(App.context.resources.configuration)
            configuration.setLocale(Locale.US)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocales(LocaleList(Locale.US))
            }
            val pkgRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                App.context.packageManager.getResourcesForApplication(appInfo, configuration)
            } else {
                App.context.packageManager.getResourcesForApplication(appInfo).also {
                    it.updateConfiguration(configuration, App.context.resources.displayMetrics)
                }
            }

            _label = pkgRes.getString(appInfo.labelRes)
            _version = pkgInfo.versionName
        } catch (e: Exception) {
            return
        }

        labelCache.put(packageName, _label)
        versionCache.put(packageName, _version)
    }

    override fun toString(): String {
        return if (BuildConfig.DEBUG)
            super.toString()
        else
            "PackageName(packageName=redacted)"
    }

    companion object {
        private val labelCache = LruCache<String, String>(10)
        private val versionCache = LruCache<String, String>(10)
    }
}


