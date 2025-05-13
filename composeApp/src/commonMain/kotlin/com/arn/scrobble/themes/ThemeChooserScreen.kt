package com.arn.scrobble.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.themes.colors.ThemeVariants
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.auto
import pano_scrobbler.composeapp.generated.resources.contrast
import pano_scrobbler.composeapp.generated.resources.dark
import pano_scrobbler.composeapp.generated.resources.high
import pano_scrobbler.composeapp.generated.resources.light
import pano_scrobbler.composeapp.generated.resources.low
import pano_scrobbler.composeapp.generated.resources.medium
import pano_scrobbler.composeapp.generated.resources.system_colors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeChooserScreen(
    onNavigateToBilling: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val licenseState by PlatformStuff.billingRepository.licenseState.collectAsStateWithLifecycle()
    var themeName: String? by rememberSaveable { mutableStateOf(null) }
    var dynamic: Boolean? by rememberSaveable { mutableStateOf(null) }
    var dayNightMode: DayNightMode? by rememberSaveable { mutableStateOf(null) }
    var contrastMode: ContrastMode? by rememberSaveable { mutableStateOf(null) }
    val isAppInNightMode = LocalThemeAttributes.current.isDark

    DisposableEffect(Unit) {
        onDispose {
            if (themeName != null && dynamic != null && dayNightMode != null && contrastMode != null) {
                if (licenseState == LicenseState.VALID) {
                    GlobalScope.launch {
                        PlatformStuff.mainPrefs.updateData {
                            it.copy(
                                themeName = themeName!!,
                                themeDynamic = dynamic!!,
                                themeDayNight = dayNightMode!!,
                                themeContrast = contrastMode!!
                            )
                        }.also {
                            Stuff.mainPrefsInitialValue = it
                        }
                    }
                } else
                    onNavigateToBilling()
            }
        }
    }

    LaunchedEffect(Unit) {
        PlatformStuff.mainPrefs.data
            .collectLatest {
                themeName = it.themeName
                dynamic = it.themeDynamic
                dayNightMode = it.themeDayNight
                contrastMode = it.themeContrast
            }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeUtils.themesMap.forEach { (_, themeObj) ->
                ThemeSwatch(
                    themeVariants = themeObj,
                    isDark = isAppInNightMode,
                    selected = themeName == themeObj.name,
                    onClick = {
                        themeName = themeObj.name
                    },
                    enabled = dynamic != true,
                    modifier = Modifier
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DayNightMode.entries.forEach {
                FilterChip(
                    label = { it.label() },
                    selected = dayNightMode == it,
                    onClick = {
                        dayNightMode = it
                    }
                )
            }
        }

        Text(
            text = stringResource(Res.string.contrast),
            style = MaterialTheme.typography.bodyLarge,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (dynamic == true) 0.5f else 1f)
        ) {
            ContrastMode.entries.forEach {
                FilterChip(
                    label = { it.label() },
                    enabled = dynamic != true,
                    selected = contrastMode == it,
                    onClick = {
                        contrastMode = it
                    }
                )
            }
        }

        if (!PlatformStuff.isDesktop && !PlatformStuff.isTv) {
            LabeledCheckbox(
                text = stringResource(Res.string.system_colors),
                checked = dynamic == true,
                onCheckedChange = { dynamic = it }
            )
        }
    }
}

@Composable
private fun DayNightMode.label() {
    when (this) {
        DayNightMode.LIGHT -> Text(stringResource(Res.string.light))
        DayNightMode.DARK -> Text(stringResource(Res.string.dark))
        DayNightMode.SYSTEM -> Text(stringResource(Res.string.auto))
    }
}

@Composable
private fun ContrastMode.label() {
    when (this) {
        ContrastMode.LOW -> Text(stringResource(Res.string.low))
        ContrastMode.MEDIUM -> Text(stringResource(Res.string.medium))
        ContrastMode.HIGH -> Text(stringResource(Res.string.high))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        onCheckedChange = { onClick() },
        interactionSource = interactionSource,
        shapes = IconButtonDefaults.shapes(
            toggleButtonShapes.shape,
            toggleButtonShapes.pressedShape,
            toggleButtonShapes.checkedShape
        ),
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
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
//                    tint = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
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