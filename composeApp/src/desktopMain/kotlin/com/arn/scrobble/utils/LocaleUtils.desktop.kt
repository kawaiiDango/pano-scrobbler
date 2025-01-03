package com.arn.scrobble.utils

import java.util.Locale

// context is always null on desktop, lang = null idicates that the system locale should be set
actual fun setAppLocale(context: Any?, lang: String?, force: Boolean) {
    if (lang != null) {
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
    } else if (force) {
        val country = System.getProperty("user.country")
        val language = System.getProperty("user.language")

        val locale = Locale.forLanguageTag("$language-$country")
        Locale.setDefault(locale)
    }
}

actual fun getCurrentLocale(localePref: String?): String {
    return localePref ?: "auto"
}