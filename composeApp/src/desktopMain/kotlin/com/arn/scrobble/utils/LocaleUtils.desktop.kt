package com.arn.scrobble.utils

import java.util.Locale

// context is always null on desktop, lang = null indicates that the system locale should be set
actual fun setAppLocale(lang: String?, force: Boolean) {
    val locale = if (lang != null) {
        Locale.forLanguageTag(lang)
    } else {
        // todo: auto mode does not work on graalvm native image
        // the locale is hardcoded to build system's locale at build time
//        val country = System.getProperty("user.country")
//        val language = System.getProperty("user.language")
//
//        val locale = Locale.forLanguageTag("$language-$country")
        Locale.ENGLISH
    }

    Locale.setDefault(locale)
}

actual fun getCurrentLocale(localePref: String?): String {
    return localePref ?: "en"
}