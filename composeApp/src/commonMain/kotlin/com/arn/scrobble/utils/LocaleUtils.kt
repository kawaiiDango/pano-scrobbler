package com.arn.scrobble.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.io.File
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
        "fa",
        "fi",
        "fr",
        "hi",
        "hr",
        "id",
        "it",
        "ja",
        "ko",
        "lt",
        "ms",
        "ne",
        "nl",
        "pl",
        "pt",
        "pt-BR",
        "ro",
        "ru",
        "sv",
        "tl",
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

    private val localeFile = File(PlatformStuff.filesDir, "locale.txt")
    val setLocaleFlow = MutableSharedFlow<String?>(extraBufferCapacity = 2)
    val locale = setLocaleFlow
        .distinctUntilChanged()
        .let {
            if (PlatformStuff.hasSystemLocaleStore)
                it.onStart {
                    emit(getCurrentLocale())
                }
            else
                it.onEach {
                    localeFile.writeText(it ?: "")
                }
        }
        .stateIn(
            GlobalScope,
            SharingStarted.Eagerly,
            if (!PlatformStuff.hasSystemLocaleStore && localeFile.exists())
                localeFile.readText().ifEmpty { null }
            else
                null
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

expect fun LocaleUtils.setAppLocale(lang: String?, activityContext: Any?)

expect fun LocaleUtils.getCurrentLocale(): String?

expect fun LocaleUtils.getSystemCountryCode(): String