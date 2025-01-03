package com.arn.scrobble.utils


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
        "zh-Hans",
    )
    // localesSet end

    val showScriptSet = setOf(
        "zh",
    )

    val showCountrySet = setOf(
        "pt",
    )
}

expect fun setAppLocale(context: Any?, lang: String?, force: Boolean)

expect fun getCurrentLocale(localePref: String?): String