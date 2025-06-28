package com.arn.scrobble.utils

import com.arn.scrobble.PanoNativeComponents
import java.util.Locale

private var systemDefaultLocale: Locale? = null

// lang = null indicates that the system locale should be set
actual fun setAppLocale(lang: String?, force: Boolean) {
    if (systemDefaultLocale == null) {
        if (Runtime.version().version().first() >= 24) {
            systemDefaultLocale = Locale.getDefault()
        } else {
            // for older graalvm, we need to get the system locale manually
            val (lang, country) = PanoNativeComponents.getSystemLocale().split("-")
            try {
                systemDefaultLocale = Locale.of(lang, country)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val locale = if (lang != null) {
        Locale.forLanguageTag(lang)
    } else {
        systemDefaultLocale ?: Locale.ENGLISH
    }

    Locale.setDefault(locale)
}

actual fun getCurrentLocale(localePref: String?): String? {
    return localePref
}