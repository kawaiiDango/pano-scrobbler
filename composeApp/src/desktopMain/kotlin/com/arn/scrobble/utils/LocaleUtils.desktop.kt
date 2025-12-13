package com.arn.scrobble.utils

import java.util.Locale

private var systemDefaultLocale: Locale? = null

// lang = null indicates that the system locale should be set
actual fun setAppLocale(lang: String?, force: Boolean) {
    if (systemDefaultLocale == null) {
        systemDefaultLocale = Locale.getDefault()
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

actual fun getSystemCountryCode(): String {
    return systemDefaultLocale?.country?.ifEmpty {
        // Fallback to the system default locale
        Locale.getDefault().country.ifEmpty { null }
    } ?: "US"
}