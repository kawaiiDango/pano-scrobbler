package com.arn.scrobble

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import com.arn.scrobble.pref.MainPrefs
import java.util.*

object LocaleUtils {

    val localesSet = arrayOf(
        "zh-hans",
        "de",
        "en",
        "ja",
        "pt",
        "pt-BR",
        "ru",
        "tr",
    ).toSortedSet()

    val showScriptSet = setOf(
        "zh",
    )

    lateinit var systemDefaultLocale: Locale
    lateinit var systemDefaultLocaleList: LocaleList

    fun Context.getLocaleContextWrapper(): ContextWrapper {
        val prefs = MainPrefs(this)
        var lang: String? = prefs.locale ?: return ContextWrapper(this)

        if (lang !in localesSet) {
            prefs.locale = null
            lang = null
        }

        val configuration = resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val newLocaleList = if (lang == null)
                systemDefaultLocaleList
            else
                LocaleList(Locale.forLanguageTag(lang))

            LocaleList.setDefault(newLocaleList)
            configuration.setLocales(newLocaleList)
        } else {
            val newLocale = if (lang == null)
                systemDefaultLocale
            else
                Locale.forLanguageTag(lang)

            Locale.setDefault(newLocale)
            configuration.setLocale(newLocale)
        }

        val context = createConfigurationContext(configuration)
        return ContextWrapper(context)
    }
}