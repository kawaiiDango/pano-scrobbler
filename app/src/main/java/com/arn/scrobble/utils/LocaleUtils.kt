package com.arn.scrobble.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.arn.scrobble.main.App
import java.lang.ref.WeakReference
import java.util.Locale


object LocaleUtils {

    val localesSet = arrayOf(
        "zh-hans",
        "de",
        "en",
        "es",
        "fr",
        "ja",
        "pt",
        "pt-BR",
        "ru",
        "tr",
        "pl",
        "it",
        "uk",
        "cs",
        "nl",
        "ar",
        "hr",
        "ca",
        "el",
        "id",
        "sv",
        "fi",
        "ko",
        "lt",
        "ne",
    ).toSortedSet()

    val showScriptSet = setOf(
        "zh",
    )

    private var deviceLocaleContext: WeakReference<Context> = WeakReference(null)

    fun Context.setLocaleCompat(force: Boolean = false): Context {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || force) {
            val prefs = App.prefs
            var lang = prefs.locale
            val configuration = Configuration(resources.configuration)

            if (lang != null && lang !in localesSet) {
                prefs.locale = null
                lang = null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && lang == null) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.wrap(LocaleList.getEmptyLocaleList()))
            } else {
                val locale = if (lang != null)
                    Locale.forLanguageTag(lang)
                else
                    deviceLocale
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))

                configuration.setLocale(locale)
//                Locale.setDefault(locale)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localeList = LocaleList(locale)
                    configuration.setLocales(localeList)
//                    LocaleList.setDefault(localeList)
                }
            }

            // for services
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
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
            ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)

    private val deviceLocaleLocaleList
        @RequiresApi(Build.VERSION_CODES.N)
        get() = ConfigurationCompat.getLocales(Resources.getSystem().configuration)

}