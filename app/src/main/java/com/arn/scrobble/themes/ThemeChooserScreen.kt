package com.arn.scrobble.themes

import android.os.Build
import androidx.annotation.Keep
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.themes.colors.OrangeYellow
import com.arn.scrobble.themes.colors.ThemeVariants
import com.arn.scrobble.ui.ExtraBottomSpace
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private data class ThemeChooserState(
    val themeName: String,
    val dynamic: Boolean,
    val dayNightMode: DayNightMode,
    val contrastMode: ContrastMode,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeChooserContent(
    onThemeChooserStateChange: (ThemeChooserState) -> Unit,
    modifier: Modifier = Modifier
) {
    var themeName: String? by remember { mutableStateOf(null) }
    var dynamic: Boolean? by remember { mutableStateOf(null) }
    var dayNightMode: DayNightMode? by remember { mutableStateOf(null) }
    var contrastMode: ContrastMode? by remember { mutableStateOf(null) }
    val config = LocalConfiguration.current
    val isAppInNightMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        config.isNightModeActive
    } else {
        false
    }

    LaunchedEffect(themeName, dynamic, dayNightMode, contrastMode) {
        if (themeName != null && dynamic != null && dayNightMode != null && contrastMode != null) {
            onThemeChooserStateChange(
                ThemeChooserState(
                    themeName = themeName!!,
                    dynamic = dynamic!!,
                    dayNightMode = dayNightMode!!,
                    contrastMode = contrastMode!!
                )
            )
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

    if (dynamic == null || dayNightMode == null || contrastMode == null || themeName == null)
        return

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
            text = stringResource(R.string.contrast),
            style = MaterialTheme.typography.bodyLarge,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (dayNightMode == DayNightMode.SYSTEM) 0.5f else 1f)
        ) {
            ContrastMode.entries.forEach {
                FilterChip(
                    label = { it.label() },
                    enabled = dayNightMode != DayNightMode.SYSTEM,
                    selected = contrastMode == it,
                    onClick = {
                        contrastMode = it
                    }
                )
            }
        }

        LabeledCheckbox(
            text = stringResource(R.string.system_colors),
            checked = dynamic == true,
            onCheckedChange = { dynamic = it }
        )

        ExtraBottomSpace()

    }
}

@Composable
private fun DayNightMode.label() {
    when (this) {
        DayNightMode.LIGHT -> Text(stringResource(R.string.light))
        DayNightMode.DARK -> Text(stringResource(R.string.dark))
        DayNightMode.SYSTEM -> Text(stringResource(R.string.auto))
    }
}

@Composable
private fun ContrastMode.label() {
    when (this) {
        ContrastMode.LOW -> Text(stringResource(R.string.low))
        ContrastMode.MEDIUM -> Text(stringResource(R.string.medium))
        ContrastMode.HIGH -> Text(stringResource(R.string.high))
    }
}

@Composable
private fun ThemeSwatch(
    themeVariants: ThemeVariants,
    isDark: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClickLabel = themeVariants.name, enabled = enabled) { onClick() }
            .border(
                width = 3.dp,
                color = if (selected) {
                    if (isDark) Color.White else Color.Black
                } else Color.Transparent,
                shape = MaterialTheme.shapes.extraLarge
            )
            .alpha(if (enabled) 1f else 0.5f)
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
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeSwatchPreview() {
    ThemeSwatch(
        themeVariants = OrangeYellow,
        selected = true,
        onClick = {},
        isDark = false,
        enabled = true,
        modifier = Modifier
    )
}

@Keep
@Composable
fun ThemeChooserScreen() {
    val licenseState by Stuff.billingRepository.licenseState.collectAsStateWithLifecycle()
    var themeChooserState: ThemeChooserState? by remember { mutableStateOf(null) }

    val scope = rememberCoroutineScope()
    val fragment = LocalFragment.current

    LaunchedEffect(Unit) {
        val fabData = FabData(
            fragment.viewLifecycleOwner,
            R.string.done,
            R.drawable.vd_check_simple,
            {
                if (licenseState == LicenseState.VALID && themeChooserState != null) {
                    scope.launch {
                        PlatformStuff.mainPrefs.updateData {
                            it.copy(
                                themeName = themeChooserState!!.themeName,
                                themeDynamic = themeChooserState!!.dynamic,
                                themeDayNight = themeChooserState!!.dayNightMode,
                                themeContrast = themeChooserState!!.contrastMode
                            )
                        }
                        fragment.findNavController().popBackStack()
                    }
                } else
                    fragment.findNavController().navigate(R.id.billingFragment)
            }
        )

        val mainNotifierViewModel by fragment.activityViewModels<MainNotifierViewModel>()

        mainNotifierViewModel.setFabData(fabData)
    }

    ScreenParent {
        ThemeChooserContent(
            onThemeChooserStateChange = { themeChooserState = it },
            modifier = it
        )
    }
}