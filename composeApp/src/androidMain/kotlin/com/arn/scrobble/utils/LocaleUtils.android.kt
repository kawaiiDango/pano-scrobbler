package com.arn.scrobble.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import com.arn.scrobble.utils.LocaleUtils.localesSet
import java.lang.ref.WeakReference
import java.util.Locale


private var deviceLocaleContext: WeakReference<Context> = WeakReference(null)


private val deviceLocaleLocaleList
    get() = Resources.getSystem().configuration.locales

fun Context.applyAndroidLocaleLegacy(): Context {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val langPref = LocaleUtils.locale.value

        val lang = if (langPref != null && langPref !in localesSet) {
            null
        } else
            langPref

        val locales = if (lang != null)
            LocaleList(Locale.forLanguageTag(lang))
        else
            deviceLocaleLocaleList

        LocaleList.setDefault(locales)
        resources.configuration.setLocales(locales)
    }

    return this
}


fun Context.getStringInDeviceLocale(@StringRes res: Int): String {
    if (deviceLocaleContext.get() == null) {
        val config = Configuration(resources.configuration)
        config.setLocales(deviceLocaleLocaleList)
        deviceLocaleContext = WeakReference(createConfigurationContext(config))
    }
    val resources = deviceLocaleContext.get()!!.resources

    return resources.getString(res)
}

actual fun LocaleUtils.setAppLocale(lang: String?, activityContext: Any?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager =
            AndroidStuff.applicationContext.getSystemService(LocaleManager::class.java)

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
    } else if (activityContext is Context) {
        val locales = if (lang != null)
            LocaleList(Locale.forLanguageTag(lang))
        else
            deviceLocaleLocaleList
        LocaleList.setDefault(locales)

        val configuration = Configuration(activityContext.resources.configuration)
        configuration.setLocales(locales)

        activityContext.resources.updateConfiguration(
            configuration,
            activityContext.resources.displayMetrics
        )
    }

    setLocaleFlow.tryEmit(lang)
}

actual fun LocaleUtils.getCurrentLocale(): String? {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        null
    } else {
        AndroidStuff.applicationContext.getSystemService(LocaleManager::class.java)
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
            }
    }
}

actual fun LocaleUtils.getSystemCountryCode(): String {
    return deviceLocaleLocaleList.get(0)?.let { locale ->
        locale.country.ifEmpty {
            // Fallback to the system default locale
            Locale.getDefault().country.ifEmpty { null }
        }
    } ?: "US"
}