package com.arn.scrobble.themes

import com.arn.scrobble.themes.colors.AquaBlue
import com.arn.scrobble.themes.colors.AvocadoBlue
import com.arn.scrobble.themes.colors.BluePurple
import com.arn.scrobble.themes.colors.DarkGreenBlue
import com.arn.scrobble.themes.colors.LightPinkOrange
import com.arn.scrobble.themes.colors.LimeBlue
import com.arn.scrobble.themes.colors.OrangeYellow
import com.arn.scrobble.themes.colors.PeachYellow
import com.arn.scrobble.themes.colors.PinkOrange
import com.arn.scrobble.themes.colors.RedOrange
import com.arn.scrobble.themes.colors.VioletMaroon
import com.arn.scrobble.themes.colors.YellowGreen
import com.arn.scrobble.themes.colors.YellowTurquoise

enum class DayNightMode {
    LIGHT, DARK, SYSTEM
}

enum class ContrastMode {
    LOW, MEDIUM, HIGH
}

object ThemeUtils {
    val themesMap = mapOf(
        AquaBlue.name to AquaBlue,
        AvocadoBlue.name to AvocadoBlue,
        BluePurple.name to BluePurple,
        DarkGreenBlue.name to DarkGreenBlue,
        LightPinkOrange.name to LightPinkOrange,
        LimeBlue.name to LimeBlue,
        OrangeYellow.name to OrangeYellow,
        PeachYellow.name to PeachYellow,
        PinkOrange.name to PinkOrange,
        RedOrange.name to RedOrange,
        VioletMaroon.name to VioletMaroon,
        YellowGreen.name to YellowGreen,
        YellowTurquoise.name to YellowTurquoise
    )

    fun themeNameToObject(name: String) = themesMap[name] ?: PinkOrange
}
