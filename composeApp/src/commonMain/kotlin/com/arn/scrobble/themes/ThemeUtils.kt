package com.arn.scrobble.themes

import com.arn.scrobble.themes.colors.Theme009788
import com.arn.scrobble.themes.colors.Theme01bdd6
import com.arn.scrobble.themes.colors.Theme03a9f5
import com.arn.scrobble.themes.colors.Theme109d58
import com.arn.scrobble.themes.colors.Theme3f51b5
import com.arn.scrobble.themes.colors.Theme4385f6
import com.arn.scrobble.themes.colors.Theme683ab7
import com.arn.scrobble.themes.colors.Theme8cc24a
import com.arn.scrobble.themes.colors.Theme9e28b2
import com.arn.scrobble.themes.colors.Themeb19bdb
import com.arn.scrobble.themes.colors.Themecddc39
import com.arn.scrobble.themes.colors.Themedd4337
import com.arn.scrobble.themes.colors.Themee8d1a8
import com.arn.scrobble.themes.colors.Themeea1e63
import com.arn.scrobble.themes.colors.Themef6b300
import com.arn.scrobble.themes.colors.Themefbbc6f
import com.arn.scrobble.themes.colors.Themefdc5c6
import com.arn.scrobble.themes.colors.Themefe5722
import com.arn.scrobble.themes.colors.Themeff748f
import com.arn.scrobble.themes.colors.Themeff9803
import com.arn.scrobble.themes.colors.Themeffeb3c

enum class DayNightMode {
    LIGHT, DARK, SYSTEM
}

enum class ContrastMode {
    LOW, MEDIUM, HIGH
}

object ThemeUtils {
    val themesMap = arrayOf(
        Theme4385f6,
        Theme03a9f5,
        Theme01bdd6,
        Theme3f51b5,
        Theme683ab7,
        Theme9e28b2,
        Themeb19bdb,
        Theme009788,
        Theme109d58,
        Theme8cc24a,
        Themecddc39,
        Themeffeb3c,
        Themef6b300,
        Themefbbc6f,
        Themeff9803,
        Themefe5722,
        Themedd4337,
        Themeea1e63,
        Themeff748f,
        Themefdc5c6,
        Themee8d1a8
    ).associateBy { it.name }

    val defaultTheme = Themeea1e63

    fun themeNameToObject(name: String) = themesMap[name] ?: defaultTheme
}
