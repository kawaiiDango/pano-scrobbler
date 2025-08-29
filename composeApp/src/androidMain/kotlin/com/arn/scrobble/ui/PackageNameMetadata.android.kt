package com.arn.scrobble.ui

import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.collection.LruCache
import com.arn.scrobble.utils.AndroidStuff
import java.util.Locale

actual object PackageNameMetadata {
    actual val PackageName.englishLabel: String?
        get() {
            cacheIt()
            return labelCache[packageName]
        }

    actual val PackageName.version: String?
        get() {
            cacheIt()
            return versionCache[packageName]
        }

    @Synchronized
    private fun PackageName.cacheIt() {
        if (labelCache[packageName] != null)
            return

        val _label: String
        val _version: String

        try {
            val pkgInfo =
                AndroidStuff.applicationContext.packageManager.getPackageInfo(packageName, 0)
            val appInfo = pkgInfo.applicationInfo ?: return
            val configuration =
                Configuration(AndroidStuff.applicationContext.resources.configuration)
            configuration.setLocales(LocaleList(Locale.US))
            val pkgRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AndroidStuff.applicationContext.packageManager.getResourcesForApplication(
                    appInfo,
                    configuration
                )
            } else {
                AndroidStuff.applicationContext.packageManager.getResourcesForApplication(appInfo)
                    .also {
                        it.updateConfiguration(
                            configuration,
                            AndroidStuff.applicationContext.resources.displayMetrics
                        )
                    }
            }

            _label = pkgRes.getString(appInfo.labelRes)
            _version = pkgInfo.versionName!!
        } catch (e: Exception) {
            return
        }

        labelCache.put(packageName, _label)
        versionCache.put(packageName, _version)
    }

    override fun toString() = "PackageName(packageName=redacted)"

    private val labelCache = LruCache<String, String>(10)
    private val versionCache = LruCache<String, String>(10)
}