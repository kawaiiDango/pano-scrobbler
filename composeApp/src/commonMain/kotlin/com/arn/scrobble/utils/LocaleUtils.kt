package com.arn.scrobble.utils

import java.util.Locale


object LocaleUtils {

    // localesSet start
    val localesSet = arrayOf(
        "ar",
        "ca",
        "cs",
        "de",
        "el",
        "en",
        "es",
        "fi",
        "fr",
        "hi",
        "hr",
        "id",
        "it",
        "ja",
        "ko",
        "lt",
        "ne",
        "nl",
        "pl",
        "pt",
        "pt-BR",
        "ro",
        "ru",
        "sv",
        "tr",
        "uk",
        "vi",
        "zh-Hans",
    )
    // localesSet end

    val showScriptSet = setOf(
        "zh",
    )

    val showCountrySet = setOf(
        "pt",
    )

    fun localesMap(): Map<String, String> {
        return localesSet.associateWith {
            val localeObj = Locale.forLanguageTag(it)
            val displayLanguage = localeObj.displayLanguage

            val suffix = when (localeObj.language) {
                in showScriptSet -> " (${localeObj.displayScript})"
                in showCountrySet -> localeObj.displayCountry
                    .ifEmpty { null }
                    ?.let { " ($it)" } ?: ""

                else -> ""
            }

            displayLanguage + suffix
        }
    }
}

expect fun setAppLocale(lang: String?, force: Boolean)

expect fun getCurrentLocale(localePref: String?): String?