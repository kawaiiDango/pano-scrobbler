package com.arn.scrobble.utils

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.os.ConfigurationCompat
import com.arn.scrobble.utils.LocaleUtils.localesSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.Locale


private var deviceLocaleContext: WeakReference<Context> = WeakReference(null)

fun Context.applyAndroidLocaleLegacy(
    langp: String? = runBlocking { PlatformStuff.mainPrefs.data.map { it.locale }.first() },
): Context {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val configuration = Configuration(resources.configuration)

        val lang = if (langp != null && langp !in localesSet) {
            null
        } else
            langp

        val locale = if (lang != null)
            Locale.forLanguageTag(lang)
        else
            deviceLocale

        configuration.setLocale(locale)
        Locale.setDefault(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            configuration.setLocales(localeList)
            LocaleList.setDefault(localeList)
        }

        // for services
        return ContextWrapper(createConfigurationContext(configuration))
    }
    return this
}


fun Context.getStringInDeviceLocale(@StringRes res: Int): String {
    if (deviceLocaleContext.get() == null) {
        val config = Configuration(resources.configuration)
        config.setLocale(deviceLocale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(deviceLocaleLocaleList.unwrap() as LocaleList)
        }
        deviceLocaleContext = WeakReference(createConfigurationContext(config))
    }
    val resources = deviceLocaleContext.get()!!.resources

    return resources.getString(res)
}

private val deviceLocale
    get() =
        ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)!!

private val deviceLocaleLocaleList
    @RequiresApi(Build.VERSION_CODES.N)
    get() = ConfigurationCompat.getLocales(Resources.getSystem().configuration)

actual fun setAppLocale(lang: String?, force: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = AndroidStuff.application.getSystemService(LocaleManager::class.java)

        if (localeManager != null) {
            val newLang = if (lang != null && lang !in localesSet) {
                null
            } else
                lang

            val localeList = if (newLang != null) {
                LocaleList.forLanguageTags(newLang)
            } else {
                LocaleList.getEmptyLocaleList()
            }

            localeManager.applicationLocales = localeList
        }
    } else
        AndroidStuff.application.applyAndroidLocaleLegacy(lang)
}

actual fun getCurrentLocale(localePref: String?): String {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        localePref ?: "auto"
    } else {
        AndroidStuff.application.getSystemService(LocaleManager::class.java)
            .applicationLocales
            .takeIf { it.size() == 1 }
            ?.get(0)
            ?.let {
                if (it.toLanguageTag() in localesSet)
                    it.toLanguageTag()
                else if (it.language in localesSet)
                    it.language
                else
                    null
            } ?: "auto"
    }
}