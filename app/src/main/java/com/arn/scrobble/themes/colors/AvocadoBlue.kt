package com.arn.scrobble.themes.colors

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object AvocadoBlue : ThemeVariants {
    override val name = "Avocado Blue"

    override val light = lightColorScheme(
        primary = Color(0xFF576421),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDBEA98),
        onPrimaryContainer = Color(0xFF181E00),
        secondary = Color(0xFF5C6146),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE1E6C3),
        onSecondaryContainer = Color(0xFF191D08),
        tertiary = Color(0xFF3A665D),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFBDECE0),
        onTertiaryContainer = Color(0xFF00201B),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBFAED),
        onBackground = Color(0xFF1B1C15),
        surface = Color(0xFFFBFAED),
        onSurface = Color(0xFF1B1C15),
        surfaceVariant = Color(0xFFE3E4D3),
        onSurfaceVariant = Color(0xFF46483C),
        outline = Color(0xFF77786A),
        outlineVariant = Color(0xFFC7C8B7),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF303129),
        inverseOnSurface = Color(0xFFF2F1E5),
        inversePrimary = Color(0xFFBFCE7F),
        surfaceDim = Color(0xFFDBDBCE),
        surfaceBright = Color(0xFFFBFAED),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5F4E7),
        surfaceContainer = Color(0xFFEFEEE2),
        surfaceContainerHigh = Color(0xFFEAE9DC),
        surfaceContainerHighest = Color(0xFFE4E3D7)
    )

    override val dark = darkColorScheme(
        primary = Color(0xFFBFCE7F),
        onPrimary = Color(0xFF2B3400),
        primaryContainer = Color(0xFF404C09),
        onPrimaryContainer = Color(0xFFDBEA98),
        secondary = Color(0xFFC5CAA8),
        onSecondary = Color(0xFF2E331B),
        secondaryContainer = Color(0xFF444930),
        onSecondaryContainer = Color(0xFFE1E6C3),
        tertiary = Color(0xFFA1D0C4),
        onTertiary = Color(0xFF04372F),
        tertiaryContainer = Color(0xFF214E45),
        onTertiaryContainer = Color(0xFFBDECE0),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF13140D),
        onBackground = Color(0xFFE4E3D7),
        surface = Color(0xFF13140D),
        onSurface = Color(0xFFE4E3D7),
        surfaceVariant = Color(0xFF46483C),
        onSurfaceVariant = Color(0xFFC7C8B7),
        outline = Color(0xFF919283),
        outlineVariant = Color(0xFF46483C),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE4E3D7),
        inverseOnSurface = Color(0xFF303129),
        inversePrimary = Color(0xFF576421),
        surfaceDim = Color(0xFF13140D),
        surfaceBright = Color(0xFF393A31),
        surfaceContainerLowest = Color(0xFF0E0F08),
        surfaceContainerLow = Color(0xFF1B1C15),
        surfaceContainer = Color(0xFF1F2019),
        surfaceContainerHigh = Color(0xFF2A2B23),
        surfaceContainerHighest = Color(0xFF34352D)
    )

    override val lightHighContrast = lightColorScheme(
        primary = Color(0xFF1E2500),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF3C4805),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFF20240E),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF40452C),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFF002821),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF1D4A41),
        onTertiaryContainer = Color(0xFFFFFFFF),
        error = Color(0xFF4E0002),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF8C0009),
        onErrorContainer = Color(0xFFFFFFFF),
        background = Color(0xFFFBFAED),
        onBackground = Color(0xFF1B1C15),
        surface = Color(0xFFFBFAED),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFE3E4D3),
        onSurfaceVariant = Color(0xFF23251A),
        outline = Color(0xFF424438),
        outlineVariant = Color(0xFF424438),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF303129),
        inverseOnSurface = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFE4F4A1),
        surfaceDim = Color(0xFFDBDBCE),
        surfaceBright = Color(0xFFFBFAED),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5F4E7),
        surfaceContainer = Color(0xFFEFEEE2),
        surfaceContainerHigh = Color(0xFFEAE9DC),
        surfaceContainerHighest = Color(0xFFE4E3D7)
    )

    override val darkHighContrast = darkColorScheme(
        primary = Color(0xFFF8FFD1),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFFC3D283),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFF9FEDA),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFFC9CEAC),
        onSecondaryContainer = Color(0xFF000000),
        tertiary = Color(0xFFECFFF8),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFFA5D4C8),
        onTertiaryContainer = Color(0xFF000000),
        error = Color(0xFFFFF9F9),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFFFFBAB1),
        onErrorContainer = Color(0xFF000000),
        background = Color(0xFF13140D),
        onBackground = Color(0xFFE4E3D7),
        surface = Color(0xFF13140D),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF46483C),
        onSurfaceVariant = Color(0xFFFCFCEB),
        outline = Color(0xFFCBCCBC),
        outlineVariant = Color(0xFFCBCCBC),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE4E3D7),
        inverseOnSurface = Color(0xFF000000),
        inversePrimary = Color(0xFF252D00),
        surfaceDim = Color(0xFF13140D),
        surfaceBright = Color(0xFF393A31),
        surfaceContainerLowest = Color(0xFF0E0F08),
        surfaceContainerLow = Color(0xFF1B1C15),
        surfaceContainer = Color(0xFF1F2019),
        surfaceContainerHigh = Color(0xFF2A2B23),
        surfaceContainerHighest = Color(0xFF34352D)
    )

    override val lightMediumContrast = lightColorScheme(
        primary = Color(0xFF3C4805),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF6D7B35),
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFF40452C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFF72775A),
        onSecondaryContainer = Color(0xFFFFFFFF),
        tertiary = Color(0xFF1D4A41),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFF517D73),
        onTertiaryContainer = Color(0xFFFFFFFF),
        error = Color(0xFF8C0009),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFDA342E),
        onErrorContainer = Color(0xFFFFFFFF),
        background = Color(0xFFFBFAED),
        onBackground = Color(0xFF1B1C15),
        surface = Color(0xFFFBFAED),
        onSurface = Color(0xFF1B1C15),
        surfaceVariant = Color(0xFFE3E4D3),
        onSurfaceVariant = Color(0xFF424438),
        outline = Color(0xFF5F6053),
        outlineVariant = Color(0xFF7B7C6E),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF303129),
        inverseOnSurface = Color(0xFFF2F1E5),
        inversePrimary = Color(0xFFBFCE7F),
        surfaceDim = Color(0xFFDBDBCE),
        surfaceBright = Color(0xFFFBFAED),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF5F4E7),
        surfaceContainer = Color(0xFFEFEEE2),
        surfaceContainerHigh = Color(0xFFEAE9DC),
        surfaceContainerHighest = Color(0xFFE4E3D7)
    )

    override val darkMediumContrast = darkColorScheme(
        primary = Color(0xFFC3D283),
        onPrimary = Color(0xFF131900),
        primaryContainer = Color(0xFF89974E),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFFC9CEAC),
        onSecondary = Color(0xFF141804),
        secondaryContainer = Color(0xFF8F9475),
        onSecondaryContainer = Color(0xFF000000),
        tertiary = Color(0xFFA5D4C8),
        onTertiary = Color(0xFF001A16),
        tertiaryContainer = Color(0xFF6C998F),
        onTertiaryContainer = Color(0xFF000000),
        error = Color(0xFFFFBAB1),
        onError = Color(0xFF370001),
        errorContainer = Color(0xFFFF5449),
        onErrorContainer = Color(0xFF000000),
        background = Color(0xFF13140D),
        onBackground = Color(0xFFE4E3D7),
        surface = Color(0xFF13140D),
        onSurface = Color(0xFFFCFBEE),
        surfaceVariant = Color(0xFF46483C),
        onSurfaceVariant = Color(0xFFCBCCBC),
        outline = Color(0xFFA3A495),
        outlineVariant = Color(0xFF838476),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE4E3D7),
        inverseOnSurface = Color(0xFF2A2B23),
        inversePrimary = Color(0xFF414D0B),
        surfaceDim = Color(0xFF13140D),
        surfaceBright = Color(0xFF393A31),
        surfaceContainerLowest = Color(0xFF0E0F08),
        surfaceContainerLow = Color(0xFF1B1C15),
        surfaceContainer = Color(0xFF1F2019),
        surfaceContainerHigh = Color(0xFF2A2B23),
        surfaceContainerHighest = Color(0xFF34352D)
    )
}