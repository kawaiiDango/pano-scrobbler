package com.arn.scrobble.themes.colors

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object Theme01bdd6 : ThemeVariants {
    override val name = this::class.simpleName!!

    override val light
        get() = lightColorScheme(
            primary = Color(0xFF006877),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFA2EEFF),
            onPrimaryContainer = Color(0xFF001F25),
            secondary = Color(0xFF4A6268),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFCDE7ED),
            onSecondaryContainer = Color(0xFF051F24),
            tertiary = Color(0xFF545D7E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFDBE1FF),
            onTertiaryContainer = Color(0xFF101A37),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = Color(0xFFF5FAFC),
            onBackground = Color(0xFF171D1E),
            surface = Color(0xFFF5FAFC),
            onSurface = Color(0xFF171D1E),
            surfaceVariant = Color(0xFFDBE4E7),
            onSurfaceVariant = Color(0xFF3F484A),
            outline = Color(0xFF6F797B),
            outlineVariant = Color(0xFFBFC8CB),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF2B3133),
            inverseOnSurface = Color(0xFFECF2F3),
            inversePrimary = Color(0xFF83D2E3),
            surfaceDim = Color(0xFFD5DBDD),
            surfaceBright = Color(0xFFF5FAFC),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEFF5F6),
            surfaceContainer = Color(0xFFE9EFF0),
            surfaceContainerHigh = Color(0xFFE3E9EB),
            surfaceContainerHighest = Color(0xFFDEE3E5)
        )

    override val dark
        get() = darkColorScheme(
            primary = Color(0xFF83D2E3),
            onPrimary = Color(0xFF00363E),
            primaryContainer = Color(0xFF004E5A),
            onPrimaryContainer = Color(0xFFA2EEFF),
            secondary = Color(0xFFB1CBD1),
            onSecondary = Color(0xFF1C3439),
            secondaryContainer = Color(0xFF334A50),
            onSecondaryContainer = Color(0xFFCDE7ED),
            tertiary = Color(0xFFBCC5EB),
            onTertiary = Color(0xFF262F4D),
            tertiaryContainer = Color(0xFF3D4665),
            onTertiaryContainer = Color(0xFFDBE1FF),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = Color(0xFF0E1416),
            onBackground = Color(0xFFDEE3E5),
            surface = Color(0xFF0E1416),
            onSurface = Color(0xFFDEE3E5),
            surfaceVariant = Color(0xFF3F484A),
            onSurfaceVariant = Color(0xFFBFC8CB),
            outline = Color(0xFF899295),
            outlineVariant = Color(0xFF3F484A),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFDEE3E5),
            inverseOnSurface = Color(0xFF2B3133),
            inversePrimary = Color(0xFF006877),
            surfaceDim = Color(0xFF0E1416),
            surfaceBright = Color(0xFF343A3C),
            surfaceContainerLowest = Color(0xFF090F10),
            surfaceContainerLow = Color(0xFF171D1E),
            surfaceContainer = Color(0xFF1B2122),
            surfaceContainerHigh = Color(0xFF252B2C),
            surfaceContainerHighest = Color(0xFF303637)
        )

    override val lightHighContrast
        get() = lightColorScheme(
            primary = Color(0xFF00272D),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF004A55),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF0D262A),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF2F464C),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFF17213E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF394260),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFF4E0002),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFF8C0009),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFFF5FAFC),
            onBackground = Color(0xFF171D1E),
            surface = Color(0xFFF5FAFC),
            onSurface = Color(0xFF000000),
            surfaceVariant = Color(0xFFDBE4E7),
            onSurfaceVariant = Color(0xFF1D2527),
            outline = Color(0xFF3B4447),
            outlineVariant = Color(0xFF3B4447),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF2B3133),
            inverseOnSurface = Color(0xFFFFFFFF),
            inversePrimary = Color(0xFFC5F4FF),
            surfaceDim = Color(0xFFD5DBDD),
            surfaceBright = Color(0xFFF5FAFC),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEFF5F6),
            surfaceContainer = Color(0xFFE9EFF0),
            surfaceContainerHigh = Color(0xFFE3E9EB),
            surfaceContainerHighest = Color(0xFFDEE3E5)
        )

    override val darkHighContrast
        get() = darkColorScheme(
            primary = Color(0xFFF3FCFF),
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFF87D7E8),
            onPrimaryContainer = Color(0xFF000000),
            secondary = Color(0xFFF3FCFF),
            onSecondary = Color(0xFF000000),
            secondaryContainer = Color(0xFFB6CFD5),
            onSecondaryContainer = Color(0xFF000000),
            tertiary = Color(0xFFFCFAFF),
            onTertiary = Color(0xFF000000),
            tertiaryContainer = Color(0xFFC0C9EF),
            onTertiaryContainer = Color(0xFF000000),
            error = Color(0xFFFFF9F9),
            onError = Color(0xFF000000),
            errorContainer = Color(0xFFFFBAB1),
            onErrorContainer = Color(0xFF000000),
            background = Color(0xFF0E1416),
            onBackground = Color(0xFFDEE3E5),
            surface = Color(0xFF0E1416),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF3F484A),
            onSurfaceVariant = Color(0xFFF3FCFF),
            outline = Color(0xFFC3CCCF),
            outlineVariant = Color(0xFFC3CCCF),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFDEE3E5),
            inverseOnSurface = Color(0xFF000000),
            inversePrimary = Color(0xFF002F37),
            surfaceDim = Color(0xFF0E1416),
            surfaceBright = Color(0xFF343A3C),
            surfaceContainerLowest = Color(0xFF090F10),
            surfaceContainerLow = Color(0xFF171D1E),
            surfaceContainer = Color(0xFF1B2122),
            surfaceContainerHigh = Color(0xFF252B2C),
            surfaceContainerHighest = Color(0xFF303637)
        )

    override val lightMediumContrast
        get() = lightColorScheme(
            primary = Color(0xFF004A55),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF277F8F),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFF2F464C),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFF60797E),
            onSecondaryContainer = Color(0xFFFFFFFF),
            tertiary = Color(0xFF394260),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF6A7395),
            onTertiaryContainer = Color(0xFFFFFFFF),
            error = Color(0xFF8C0009),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFDA342E),
            onErrorContainer = Color(0xFFFFFFFF),
            background = Color(0xFFF5FAFC),
            onBackground = Color(0xFF171D1E),
            surface = Color(0xFFF5FAFC),
            onSurface = Color(0xFF171D1E),
            surfaceVariant = Color(0xFFDBE4E7),
            onSurfaceVariant = Color(0xFF3B4447),
            outline = Color(0xFF576163),
            outlineVariant = Color(0xFF737C7F),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF2B3133),
            inverseOnSurface = Color(0xFFECF2F3),
            inversePrimary = Color(0xFF83D2E3),
            surfaceDim = Color(0xFFD5DBDD),
            surfaceBright = Color(0xFFF5FAFC),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFEFF5F6),
            surfaceContainer = Color(0xFFE9EFF0),
            surfaceContainerHigh = Color(0xFFE3E9EB),
            surfaceContainerHighest = Color(0xFFDEE3E5)
        )

    override val darkMediumContrast
        get() = darkColorScheme(
            primary = Color(0xFF87D7E8),
            onPrimary = Color(0xFF001A1E),
            primaryContainer = Color(0xFF4A9CAC),
            onPrimaryContainer = Color(0xFF000000),
            secondary = Color(0xFFB6CFD5),
            onSecondary = Color(0xFF01191E),
            secondaryContainer = Color(0xFF7C959B),
            onSecondaryContainer = Color(0xFF000000),
            tertiary = Color(0xFFC0C9EF),
            onTertiary = Color(0xFF0B1531),
            tertiaryContainer = Color(0xFF868FB3),
            onTertiaryContainer = Color(0xFF000000),
            error = Color(0xFFFFBAB1),
            onError = Color(0xFF370001),
            errorContainer = Color(0xFFFF5449),
            onErrorContainer = Color(0xFF000000),
            background = Color(0xFF0E1416),
            onBackground = Color(0xFFDEE3E5),
            surface = Color(0xFF0E1416),
            onSurface = Color(0xFFF6FCFD),
            surfaceVariant = Color(0xFF3F484A),
            onSurfaceVariant = Color(0xFFC3CCCF),
            outline = Color(0xFF9BA4A7),
            outlineVariant = Color(0xFF7B8587),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFDEE3E5),
            inverseOnSurface = Color(0xFF252B2C),
            inversePrimary = Color(0xFF00505B),
            surfaceDim = Color(0xFF0E1416),
            surfaceBright = Color(0xFF343A3C),
            surfaceContainerLowest = Color(0xFF090F10),
            surfaceContainerLow = Color(0xFF171D1E),
            surfaceContainer = Color(0xFF1B2122),
            surfaceContainerHigh = Color(0xFF252B2C),
            surfaceContainerHighest = Color(0xFF303637)
        )
}