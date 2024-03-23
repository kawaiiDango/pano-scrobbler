package com.arn.scrobble.ui

import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.collection.LruCache
import com.arn.scrobble.main.App
import java.util.Locale


data class PackageName(val packageName: String) {
    override fun toString(): String {
        return "PackageName(packageName=redacted)"
    }
}

object PackageNameMetadata {
    val PackageName.englishLabel: String?
        get() {
            cacheIt()
            return labelCache[packageName]
        }

    val PackageName.version: String?
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
            val pkgInfo = App.context.packageManager.getPackageInfo(packageName, 0)
            val appInfo = pkgInfo.applicationInfo ?: return
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
