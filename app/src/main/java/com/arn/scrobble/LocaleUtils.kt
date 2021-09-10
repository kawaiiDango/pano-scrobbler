package com.arn.scrobble

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.LocaleList
import com.arn.scrobble.pref.MultiPreferences
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
        val pref = MultiPreferences(this)
        var lang: String? = pref.getString(Stuff.PREF_LOCALE, null) ?:
            return ContextWrapper(this)

        if (lang !in localesSet) {
            pref.remove(Stuff.PREF_LOCALE)
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