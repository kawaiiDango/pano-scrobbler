package com.arn.scrobble.themes

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import com.arn.scrobble.themes.ContrastMode.HIGH
import com.arn.scrobble.themes.ContrastMode.LOW
import com.arn.scrobble.themes.ContrastMode.MEDIUM
import dynamiccolor.DynamicColor
import dynamiccolor.DynamicScheme
import dynamiccolor.MaterialDynamicColors
import hct.Hct
import scheme.SchemeCmf
import scheme.SchemeExpressive
import scheme.SchemeTonalSpot
import scheme.SchemeVibrant

enum class DayNightMode {
    SYSTEM, LIGHT, DARK
}

enum class ContrastMode {
    LOW, MEDIUM, HIGH
}

private val ContrastMode.contrastLevel: Double
    get() = when (this) {
        LOW -> 0.0
        MEDIUM -> 0.5
        HIGH -> 1.0
    }

// Curated subset
enum class PaletteStyle {
    TonalSpot,
    Expressive,
    Vibrant,
    Cmf,
}

object ThemeUtils {

    fun materialColorScheme(
        seedColor: Int,
        isDark: Boolean,
        style: PaletteStyle,
        contrastMode: ContrastMode,
    ): ColorScheme {
        val scheme = getScheme(style, seedColor, isDark, contrastMode)

        val tokens = MaterialDynamicColors()
        fun DynamicColor.resolve() = Color(getArgb(scheme))

        return ColorScheme(
            primary = tokens.primary.resolve(),
            onPrimary = tokens.onPrimary.resolve(),
            primaryContainer = tokens.primaryContainer.resolve(),
            onPrimaryContainer = tokens.onPrimaryContainer.resolve(),
            inversePrimary = tokens.inversePrimary.resolve(),
            secondary = tokens.secondary.resolve(),
            onSecondary = tokens.onSecondary.resolve(),
            secondaryContainer = tokens.secondaryContainer.resolve(),
            onSecondaryContainer = tokens.onSecondaryContainer.resolve(),
            tertiary = tokens.tertiary.resolve(),
            onTertiary = tokens.onTertiary.resolve(),
            tertiaryContainer = tokens.tertiaryContainer.resolve(),
            onTertiaryContainer = tokens.onTertiaryContainer.resolve(),
            background = tokens.background.resolve(),
            onBackground = tokens.onBackground.resolve(),
            surface = tokens.surface.resolve(),
            onSurface = tokens.onSurface.resolve(),
            surfaceVariant = tokens.surfaceVariant.resolve(),
            onSurfaceVariant = tokens.onSurfaceVariant.resolve(),
            surfaceTint = tokens.surfaceTint.resolve(),
            inverseSurface = tokens.inverseSurface.resolve(),
            inverseOnSurface = tokens.inverseOnSurface.resolve(),
            error = tokens.error.resolve(),
            onError = tokens.onError.resolve(),
            errorContainer = tokens.errorContainer.resolve(),
            onErrorContainer = tokens.onErrorContainer.resolve(),
            outline = tokens.outline.resolve(),
            outlineVariant = tokens.outlineVariant.resolve(),
            scrim = tokens.scrim.resolve(),
            surfaceBright = tokens.surfaceBright.resolve(),
            surfaceDim = tokens.surfaceDim.resolve(),
            surfaceContainer = tokens.surfaceContainer.resolve(),
            surfaceContainerHigh = tokens.surfaceContainerHigh.resolve(),
            surfaceContainerHighest = tokens.surfaceContainerHighest.resolve(),
            surfaceContainerLow = tokens.surfaceContainerLow.resolve(),
            surfaceContainerLowest = tokens.surfaceContainerLowest.resolve(),
            primaryFixed = tokens.primaryFixed.resolve(),
            primaryFixedDim = tokens.primaryFixedDim.resolve(),
            onPrimaryFixed = tokens.onPrimaryFixed.resolve(),
            onPrimaryFixedVariant = tokens.onPrimaryFixedVariant.resolve(),
            secondaryFixed = tokens.secondaryFixed.resolve(),
            secondaryFixedDim = tokens.secondaryFixedDim.resolve(),
            onSecondaryFixed = tokens.onSecondaryFixed.resolve(),
            onSecondaryFixedVariant = tokens.onSecondaryFixedVariant.resolve(),
            tertiaryFixed = tokens.tertiaryFixed.resolve(),
            tertiaryFixedDim = tokens.tertiaryFixedDim.resolve(),
            onTertiaryFixed = tokens.onTertiaryFixed.resolve(),
            onTertiaryFixedVariant = tokens.onTertiaryFixedVariant.resolve(),
        )
    }

    fun avatarColors(
        seedColor: Int,
        isDark: Boolean,
        style: PaletteStyle,
        contrastMode: ContrastMode,
    ): Pair<Color, Color> {
        val scheme = getScheme(style, seedColor, isDark, contrastMode)
        val tokens = MaterialDynamicColors()
        fun DynamicColor.resolve() = Color(getArgb(scheme))

        return Pair(
            tokens.secondaryContainer.resolve(),
            tokens.onSecondaryContainer.resolve()
        )
    }

    fun themePreviewColors(
        seedColor: Int,
        isDark: Boolean,
        style: PaletteStyle,
        contrastMode: ContrastMode,
    ): Triple<Color, Color, Color> {
        val scheme = getScheme(style, seedColor, isDark, contrastMode)
        val tokens = MaterialDynamicColors()
        fun DynamicColor.resolve() = Color(getArgb(scheme))

        return Triple(
            tokens.primary.resolve(),
            tokens.secondary.resolve(),
            tokens.tertiary.resolve()
        )
    }

    fun getScheme(
        paletteStyle: PaletteStyle,
        seedColor: Int,
        isDark: Boolean,
        contrastMode: ContrastMode
    ): DynamicScheme {
        val seedHct = listOf(Hct.fromInt(seedColor))
//        val spec = SpecVersion.SPEC_2026
        // the SPEC_2026 (SPEC_2025) makes some texts illegible
        // just use the defaults instead
        val contrastLevel = contrastMode.contrastLevel

        return when (paletteStyle) {
            PaletteStyle.TonalSpot -> SchemeTonalSpot(seedHct, isDark, contrastLevel)
            PaletteStyle.Vibrant -> SchemeVibrant(seedHct, isDark, contrastLevel)
            PaletteStyle.Expressive -> SchemeExpressive(seedHct, isDark, contrastLevel)
            PaletteStyle.Cmf -> SchemeCmf(seedHct, isDark, contrastLevel)
        }
    }

    const val DEFAULT_HUE = 8f
    private const val DEFAULT_CHROMA = 45.0
    private const val DEFAULT_TONE = 60.0

    val defaultThemeStyle = PaletteStyle.TonalSpot

    private val _randomHueForProcess = mutableFloatStateOf((0..360).random().toFloat())
    val randomHueForProcess get() = _randomHueForProcess.asFloatState()

    private val defaultThemeColor = Color(0xFFEA1E63) // HCT(8, 93, 51)
    private val themeColors = arrayOf(
        Color(0xFF4385F6),
        Color(0xFF03A9F5),
        Color(0xFF01BDD6),
        Color(0xFF3F51B5),
        Color(0xFF683AB7),
        Color(0xFF9E28B2),
        Color(0xFFB19BDB),
        Color(0xFF009788),
        Color(0xFF109D58),
        Color(0xFF8CC24A),
        Color(0xFFCDDC39),
        Color(0xFFFFEB3C),
        Color(0xFFF6B300),
        Color(0xFFFBBC6F),
        Color(0xFFFF9803),
        Color(0xFFFE5722),
        Color(0xFFDD4337),
        Color(0xFFEA1E63),
        Color(0xFFFF748F),
        Color(0xFFFDC5C6),
        Color(0xFFE8D1A8),
    )

    val themeHues = floatArrayOf(
        266f,
        242f,
        213f,
        278f,
        327f,
        302f,
        184f,
        155f,
        131f,
        116f,
        103f,
        82f,
        70f,
        64f,
        34f,
        25f,
        8f,
        9f,
        15f,
        84f,
    )

    fun randomizeHue() {
        _randomHueForProcess.floatValue = (0..360).random().toFloat()
    }

    // Hct calculation is expensive
    fun getThemeColor(hue: Float, chroma: Double = DEFAULT_CHROMA, tone: Double = DEFAULT_TONE) =
        Hct.from(hue.toDouble(), chroma = chroma, tone = tone).toInt()

//    init {
//        Logger.d {
//            themeColors.joinToString("\n") { c ->
//                c.toArgb().toHexString(HexFormat.UpperCase) + " -> " + Hct.fromInt(c.toArgb())
//                    .toString()
//            }
//        }
//    }
}
