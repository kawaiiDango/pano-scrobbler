package com.arn.scrobble.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TonalToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.icons.Casino
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Lock
import com.arn.scrobble.icons.Palette
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.SliderPref
import com.arn.scrobble.themes.colors.ThemeVariants
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.appwidget_alpha
import pano_scrobbler.composeapp.generated.resources.auto
import pano_scrobbler.composeapp.generated.resources.blur
import pano_scrobbler.composeapp.generated.resources.blur_main_window
import pano_scrobbler.composeapp.generated.resources.blur_notice
import pano_scrobbler.composeapp.generated.resources.blur_sub_window
import pano_scrobbler.composeapp.generated.resources.contrast
import pano_scrobbler.composeapp.generated.resources.dark
import pano_scrobbler.composeapp.generated.resources.experimental
import pano_scrobbler.composeapp.generated.resources.high
import pano_scrobbler.composeapp.generated.resources.light
import pano_scrobbler.composeapp.generated.resources.low
import pano_scrobbler.composeapp.generated.resources.medium
import pano_scrobbler.composeapp.generated.resources.pref_themes
import pano_scrobbler.composeapp.generated.resources.random_text
import pano_scrobbler.composeapp.generated.resources.system_colors

@Composable
fun ThemeChooserScreen(
    onNavigateToBilling: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLicenseValid = LocalLicenseValidState.current
    val themeName by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeName }
    val dynamic by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeDynamic }
    val dayNightMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeDayNight }
    val random by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeRandom }
    val contrastMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeContrast }
    val alpha by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeAlpha }
    val alphaIntPercent by remember(alpha) { mutableIntStateOf((alpha * 100).toInt()) }
    val blurMainWindow by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeBlurMainWindow }
    val blurSubWindow by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.themeBlurSubWindow }
    val isAppInNightMode = LocalThemeAttributes.current.isDark
    val scope = rememberCoroutineScope()
    val enableAlpha = false // todo testing only

    fun save(block: MainPrefs.() -> MainPrefs) {
        scope.launch {
            PlatformStuff.mainPrefs.updateData(block)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {

        if (!isLicenseValid) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                )

                ButtonWithIcon(
                    onClick = onNavigateToBilling,
                    icon = Icons.Lock,
                    text = stringResource(Res.string.pref_themes),
                )

                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            ThemeUtils.themesMap.forEach { (_, themeObj) ->
                ThemeSwatch(
                    themeVariants = themeObj,
                    isDark = isAppInNightMode,
                    selected = themeName == themeObj.name && !dynamic && !random,
                    onClick = {
                        save {
                            copy(
                                themeName = themeObj.name,
                                themeDynamic = false,
                                themeRandom = false
                            )
                        }
                    },
                    enabled = isLicenseValid,
                )
            }

            if (PlatformStuff.supportsDynamicColors && !PlatformStuff.isTv) {
                ThemeSwatchLikeButton(
                    icon = Icons.Palette,
                    text = stringResource(Res.string.system_colors),
                    selected = dynamic,
                    onCheckedChange = {
                        save {
                            copy(
                                themeDynamic = it,
                                themeRandom = false
                            )
                        }
                    },
                    enabled = isLicenseValid,
                )
            }

            ThemeSwatchLikeButton(
                icon = Icons.Casino,
                text = stringResource(Res.string.random_text),
                selected = random,
                onCheckedChange = {
                    save {
                        copy(
                            themeRandom = it,
                            themeDynamic = false
                        )
                    }
                },
                enabled = isLicenseValid,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DayNightMode.entries.forEach {
                FilterChip(
                    label = { it.Label() },
                    selected = dayNightMode == it,
                    enabled = isLicenseValid,
                    shapes = FilterChipDefaults.shapes(),
                    onClick = {
                        save {
                            copy(themeDayNight = it)
                        }
                    }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (dynamic) 0.5f else 1f)
        ) {
            Text(
                text = stringResource(Res.string.contrast),
                style = MaterialTheme.typography.bodyLarge,
            )

            ContrastMode.entries.forEach {
                FilterChip(
                    label = { it.Label() },
                    enabled = !dynamic && isLicenseValid,
                    selected = contrastMode == it,
                    shapes = FilterChipDefaults.shapes(),
                    onClick = {
                        save {
                            copy(themeContrast = it)
                        }
                    }
                )
            }
        }

        if (enableAlpha && !PlatformStuff.isTv) {
            if (PlatformStuff.supportsBlur) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(Res.string.blur),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(end = 16.dp)
                        )

                        FilterChip(
                            label = { Text(stringResource(Res.string.blur_main_window)) },
                            selected = blurMainWindow,
                            enabled = isLicenseValid,
                            leadingIcon = if (blurMainWindow) {
                                {
                                    Icon(
                                        imageVector = Icons.Check,
                                        contentDescription = null
                                    )
                                }
                            } else null,
                            shapes = FilterChipDefaults.shapes(),
                            onClick = {
                                val newState = !blurMainWindow
                                save {
                                    copy(
                                        themeBlurMainWindow = newState,
                                        themeAlpha = if (themeAlpha == 1f && newState)
                                            MainPrefs.PREF_MID_ALPHA
                                        else
                                            themeAlpha
                                    )
                                }
                            },
                        )

                        FilterChip(
                            label = { Text(stringResource(Res.string.blur_sub_window)) },
                            selected = blurSubWindow,
                            enabled = isLicenseValid,
                            leadingIcon = if (blurSubWindow) {
                                {
                                    Icon(
                                        imageVector = Icons.Check,
                                        contentDescription = null
                                    )
                                }
                            } else null,
                            shapes = FilterChipDefaults.shapes(),
                            onClick = {
                                val newState = !blurSubWindow
                                save {
                                    copy(themeBlurSubWindow = newState)
                                }
                            },
                        )
                    }
                }

                Text(
                    text = "ⓘ " + stringResource(Res.string.experimental) + " " + stringResource(Res.string.blur_notice),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            SliderPref(
                text = stringResource(Res.string.appwidget_alpha) + " " + stringResource(Res.string.experimental),
                value = alphaIntPercent.toFloat(),
                copyToSave = {
                    val a = it / 100f
                    copy(
                        themeAlpha = a,
                        themeBlurMainWindow = if (a == 1f) false else themeBlurMainWindow,
                    )
                },
                default = null,
                min = (MainPrefs.PREF_MIN_ALPHA * 100).toInt(),
                max = (MainPrefs.PREF_MAX_ALPHA * 100).toInt(),
                increments = 5,
                stringRepresentation = { "$it%" },
                enabled = isLicenseValid
            )

        }
    }
}

@Composable
private fun DayNightMode.Label() {
    when (this) {
        DayNightMode.LIGHT -> Text(stringResource(Res.string.light))
        DayNightMode.DARK -> Text(stringResource(Res.string.dark))
        DayNightMode.SYSTEM -> Text(stringResource(Res.string.auto))
    }
}

@Composable
private fun ContrastMode.Label() {
    when (this) {
        ContrastMode.LOW -> Text(stringResource(Res.string.low))
        ContrastMode.MEDIUM -> Text(stringResource(Res.string.medium))
        ContrastMode.HIGH -> Text(stringResource(Res.string.high))
    }
}

@Composable
private fun ThemeSwatch(
    themeVariants: ThemeVariants,
    isDark: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = remember(isDark) {
        if (isDark)
            themeVariants.dark.primary
        else
            themeVariants.light.primary
    }

    val secondaryColor = remember(isDark) {
        if (isDark)
            themeVariants.dark.secondary
        else
            themeVariants.light.secondary
    }

    val tertiaryColor = remember(isDark) {
        if (isDark)
            themeVariants.dark.tertiary
        else
            themeVariants.light.tertiary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val toggleButtonShapes = ToggleButtonDefaults.shapes()

    FilledTonalIconToggleButton(
        checked = selected,
        shapes = IconButtonDefaults.toggleableShapes(),
        onCheckedChange = { onClick() },
        interactionSource = interactionSource,
        enabled = enabled,
        modifier = modifier
            .size(72.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(
                    if (isFocused) toggleButtonShapes.pressedShape
                    else if (selected) toggleButtonShapes.checkedShape
                    else toggleButtonShapes.shape
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(Alignment.TopStart)
                    .background(primaryColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth(0.5f)
                    .align(Alignment.TopEnd)
                    .background(secondaryColor)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth(0.5f)
                    .align(Alignment.BottomEnd)
                    .background(tertiaryColor)
            )

            if (selected) {
                Icon(
                    imageVector = Icons.Check,
                    contentDescription = null,
//                    tint = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatchLikeButton(
    icon: ImageVector,
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    TonalToggleButton(
        checked = selected,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.widthIn(max = 96.dp)
            )
        }
    }
}

@Composable
private fun ThemeSwatchPreview() {
    ThemeSwatch(
        themeVariants = ThemeUtils.defaultTheme,
        selected = true,
        onClick = {},
        isDark = false,
        enabled = true,
        modifier = Modifier
    )
}