package com.arn.scrobble.themes

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalRippleThemeConfiguration
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.RippleThemeConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.VariantStuff

@Composable
fun AppTheme(
    onInitDone: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val prefsVersion by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.version }
    val licenseState by VariantStuff.billingRepository.licenseState.collectAsStateWithLifecycle()
    val themeHue by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.themeHue.coerceIn(0f..360f)
    }
    val themeStyle by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { p ->
        PaletteStyle.entries.find { it.name == p.themeStyle } ?: ThemeUtils.defaultThemeStyle
    }
    val dynamic by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeDynamic }
    val random by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeRandom }
    val randomHue by remember { ThemeUtils.randomHueForProcess }
    val dayNightMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeDayNight }
    val contrastMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeContrast }
    val blurMainWindowPref by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.themeBlurMainWindow
    }
    var osWindowBlur by rememberSaveable { mutableStateOf(false) }
    val blurSubWindowPref by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        it.themeBlurSubWindow
    }
    val blurMainWindow by remember(blurMainWindowPref, osWindowBlur) {
        mutableStateOf(PlatformStuff.supportsBlur && blurMainWindowPref && osWindowBlur)
    }

    val blurSubWindow by remember(blurSubWindowPref, osWindowBlur) {
        mutableStateOf(PlatformStuff.supportsBlur && blurSubWindowPref && osWindowBlur)
    }

    val alpha by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        if (PlatformStuff.isTv)
            1f
        else
            it.themeAlpha.coerceIn(MainPrefs.PREF_MIN_ALPHA, 1f)
    }

    val isSystemInDarkTheme by isSystemInDarkThemeNative()
    val activity = getActivityOrNull()

    if (licenseState == LicenseState.UNKNOWN || prefsVersion == 0)
        return


    LaunchedEffect(Unit) {
        setupWindowBlurListener(activity) { osWindowBlur = it }
        onInitDone()
    }

    LaunchedEffect(licenseState) {
        if (licenseState != LicenseState.VALID) {
            PlatformStuff.mainPrefs.updateData {
                it.copy(
                    themeDayNight = DayNightMode.DARK,
                    themeAlpha = 1f,
                    themeDynamic = false,
                    themeRandom = false,
                    themeBlurMainWindow = false,
                    themeBlurSubWindow = false,
                )
            }
        }
    }

    val isDark by remember(dayNightMode, licenseState, isSystemInDarkTheme) {
        mutableStateOf(
            if (licenseState != LicenseState.VALID)
                true
            else
                dayNightMode == DayNightMode.DARK || (dayNightMode == DayNightMode.SYSTEM && isSystemInDarkTheme)
        )
    }

    val themeAttributes = remember(
        isDark,
        contrastMode,
        themeHue,
        themeStyle,
        alpha,
        blurMainWindow,
        blurSubWindow
    ) {
        val avatarColors = ThemeUtils.themeHues
            .filter { it != themeHue }
            .map {
                ThemeUtils.avatarColors(
                    seedColor = ThemeUtils.getThemeColor(it),
                    style = themeStyle,
                    isDark = isDark,
                    contrastMode = contrastMode
                )
            }

        ThemeAttributes(
            isDark = isDark,
            isTranslucent = alpha < 1f,
            blurMainWindow = blurMainWindow,
            blurSubWindow = blurSubWindow,
            contrastMode = contrastMode,
            style = themeStyle,
            avatarContainerColors = avatarColors.map { it.first },
            avatarColors = avatarColors.map { it.second },
        )
    }

    val colorScheme: ColorScheme = when {
        licenseState != LicenseState.VALID -> {
            remember(contrastMode) {
                ThemeUtils.materialColorScheme(
                    seedColor = ThemeUtils.getThemeColor(ThemeUtils.DEFAULT_HUE),
                    style = PaletteStyle.TonalSpot,
                    isDark = true,
                    contrastMode = contrastMode
                )
            }
        }

        dynamic && PlatformStuff.supportsDynamicColors -> {
            remember(isDark, alpha, blurSubWindow) {
                getDynamicColorScheme(activity, isDark)
                    .withAlpha(alpha, blurSubWindow)
            }
        }

        else -> {
            remember(
                contrastMode,
                themeHue,
                themeStyle,
                isDark,
                random,
                randomHue,
                alpha,
                blurSubWindow
            ) {
                val hue = if (random)
                    randomHue
                else
                    themeHue

                ThemeUtils.materialColorScheme(
                    seedColor = ThemeUtils.getThemeColor(hue),
                    isDark = isDark,
                    style = themeStyle,
                    contrastMode = contrastMode,
                ).withAlpha(alpha, blurSubWindow)
            }
        }
    }


    MaterialExpressiveTheme(
        colorScheme = colorScheme,
    ) {
        val rippleConfig = remember {
            RippleThemeConfiguration(
                focus = RippleThemeConfiguration.Focus.InsetRing(
                    outerStrokeInset = 0.dp,
                    outerStrokeWidth = 3.dp, // default is 2.dp — bumped for visibility on TV
                    innerStrokeInset = 1.dp,
                    innerStrokeWidth = 4.dp, // default is 3.dp
                )
            )
        }

        CompositionLocalProvider(
            LocalRippleThemeConfiguration provides rippleConfig,
            LocalThemeAttributes provides themeAttributes,
            LocalLicenseValidState provides (licenseState == LicenseState.VALID),
        ) {
            AddAdditionalProviders {
                content()
            }
        }
    }
}

private fun ColorScheme.withAlpha(alpha: Float, hasBlur: Boolean): ColorScheme {
    fun boostAlpha(alpha: Float, boost: Float) = alpha + (1f - alpha) * boost

    if (alpha == 1f && !hasBlur) return this

    val midAlpha = boostAlpha(alpha.coerceIn(0.5f, 1f), 0.6f)
    val highAlpha = boostAlpha(alpha.coerceIn(0.5f, 1f), 0.8f)

    return copy(
        background = background.copy(alpha = alpha),
        surface = surface.copy(alpha = alpha),
        surfaceContainerLow = surfaceContainerLow.copy(alpha = if (hasBlur) midAlpha else highAlpha),
        surfaceContainerHigh = surfaceContainerHigh.copy(alpha = if (hasBlur) midAlpha else highAlpha),
        surfaceContainerHighest = surfaceContainerHighest.copy(alpha = if (hasBlur) midAlpha else highAlpha),
        surfaceContainer = surfaceContainer.copy(alpha = if (hasBlur) midAlpha else highAlpha),
        outlineVariant = if (alpha < 1f)
            lerp(outlineVariant, outline, alpha.coerceAtLeast(0.65f)) // fix for bad contrast
        else
            outlineVariant,
//        secondaryContainer = secondaryContainer.copy(alpha = highAlpha),
//        tertiaryContainer = tertiaryContainer.copy(alpha = highAlpha),
//        inverseSurface = inverseSurface.copy(alpha = highAlpha),
    )
}

@Composable
fun AppPreviewTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme {
        content()
    }
}

@Composable
expect fun isSystemInDarkThemeNative(): State<Boolean>

expect fun getDynamicColorScheme(context: Any?, dark: Boolean): ColorScheme

@Composable
expect fun AddAdditionalProviders(content: @Composable () -> Unit)

expect fun setupWindowBlurListener(activity: Any?, onBlurChanged: (Boolean) -> Unit)