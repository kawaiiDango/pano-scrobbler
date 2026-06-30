package com.arn.scrobble.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.io.File


object LocaleUtils {

    // localesSet start
    val localesMap = mapOf(
        "ca" to "català",
        "de" to "Deutsch",
        "en" to "English",
        "es" to "español",
        "fr" to "français",
        "hr" to "hrvatski",
        "id" to "Indonesia",
        "it" to "italiano",
        "lt" to "lietuvių",
        "ms" to "Melayu",
        "nl" to "Nederlands",
        "pl" to "polski",
        "pt" to "português",
        "pt-BR" to "português Brasil",
        "ro" to "română",
        "fi" to "suomi",
        "sv" to "svenska",
        "tl" to "Tagalog",
        "vi" to "Tiếng Việt",
        "tr" to "Türkçe",
        "cs" to "čeština",
        "el" to "Ελληνικά",
        "ru" to "русский",
        "uk" to "українська",
        "he" to "עברית",
        "ar" to "العربية",
        "fa" to "فارسی",
        "ne" to "नेपाली",
        "hi" to "हिन्दी",
        "zh-Hans" to "中文 简体",
        "ja" to "日本語",
        "ko" to "한국어"
    )
    // localesSet end

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
            Stuff.appScope,
            SharingStarted.Eagerly,
            if (!PlatformStuff.hasSystemLocaleStore && localeFile.exists())
                localeFile.readText().ifEmpty { null }
            else
                null
        )
}

expect fun LocaleUtils.setAppLocale(lang: String?, activityContext: Any?)

expect fun LocaleUtils.getCurrentLocale(): String?

expect fun LocaleUtils.getSystemCountryCode(): String