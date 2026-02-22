package com.arn.scrobble.pref

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.KeyboardArrowDown
import com.arn.scrobble.icons.automirrored.KeyboardArrowLeft
import com.arn.scrobble.icons.automirrored.KeyboardArrowRight
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.ui.AppIcon
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.move_left
import pano_scrobbler.composeapp.generated.resources.move_right
import pano_scrobbler.composeapp.generated.resources.no_apps_enabled
import pano_scrobbler.composeapp.generated.resources.pref_logout
import pano_scrobbler.composeapp.generated.resources.sure_tap_again
import kotlin.math.roundToInt

private val mainPrefs get() = PlatformStuff.mainPrefs

@Composable
fun SwitchPref(
    text: String,
    value: Boolean,
    copyToSave: MainPrefs.(Boolean) -> MainPrefs,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onNavigateToBilling: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val locked = onNavigateToBilling != null

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (enabled)
                    Modifier.toggleable(
                        value = value,
                        onValueChange = { newValue ->
                            scope.launch { mainPrefs.updateData { it.copyToSave(newValue) } }
                        },
                        role = Role.Switch
                    )
                else if (locked)
                    Modifier.clickable {
                        onNavigateToBilling()
                    }
                else
                    Modifier
            )
            .padding(vertical = 16.dp, horizontal = horizontalOverscanPadding())
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = (if (locked) "🔒 " else "") + text,
                style = MaterialTheme.typography.titleMedium,
            )

            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Switch(
            checked = value,
            onCheckedChange = null, // null recommended for accessibility with screenreaders
            enabled = enabled,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}


@Composable
fun TextPref(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    locked: Boolean = false,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick, enabled = enabled)
            .padding(vertical = 16.dp, horizontal = horizontalOverscanPadding())
            .alpha(if (locked || !enabled) 0.5f else 1f),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = (if (locked) "🔒 " else "") + text,
            style = MaterialTheme.typography.titleMedium,
        )

        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun <T> DropdownPref(
    text: String,
    selectedValue: T,
    values: Iterable<T>,
    toLabel: @Composable (T) -> String,
    copyToSave: MainPrefs.(T) -> MainPrefs,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedDisplayText = toLabel(selectedValue)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (enabled)
                    Modifier.clickable { expanded = true }
                else Modifier
            )
            .padding(vertical = 16.dp, horizontal = horizontalOverscanPadding())
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = selectedDisplayText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                values.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(text = toLabel(value)) },
                        onClick = {
                            scope.launch { mainPrefs.updateData { it.copyToSave(value) } }
                            expanded = false
                        },
                        enabled = selectedValue != value
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.KeyboardArrowDown,
            contentDescription = null,
        )
    }
}

@Composable
fun AppIconsPref(
    packageNames: Set<String>,
    modifier: Modifier = Modifier,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val maxIcons = 7
    var appItems by remember { mutableStateOf<List<AppItem>>(emptyList()) }

    LaunchedEffect(packageNames) {
        val items = mutableListOf<AppItem>()

        for (packageName in packageNames) {
            val label = PlatformStuff.loadApplicationLabel(packageName)
            if (label.isNotEmpty() || PlatformStuff.isDesktop) {
                items.add(AppItem(packageName, label))
            }
            if (items.size >= maxIcons) break
        }

        appItems = items
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick, enabled = enabled)
            .alpha(if (!enabled) 0.5f else 1f)
            .padding(vertical = 16.dp, horizontal = horizontalOverscanPadding())
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )

        if (packageNames.isEmpty()) {
            Text(
                stringResource(Res.string.no_apps_enabled),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(24.dp)
            ) {
                appItems
                    .forEach {
                        AppIcon(
                            appItem = it,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SliderPref(
    text: String,
    value: Float,
    copyToSave: MainPrefs.(Int) -> MainPrefs,
    min: Int,
    max: Int,
    increments: Int,
    stringRepresentation: (Int) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    var internalValue by remember(value) { mutableFloatStateOf(value) }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = horizontalOverscanPadding())
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Slider(
                value = internalValue.coerceIn(min.toFloat(), max.toFloat()),
                onValueChange = { internalValue = it },
                onValueChangeFinished = {
                    scope.launch { mainPrefs.updateData { it.copyToSave(internalValue.roundToInt()) } }
                },
                valueRange = min.toFloat()..max.toFloat(),
                steps = ((max - min) / increments) - 1,
                enabled = enabled && !PlatformStuff.isTv,
                modifier = Modifier
                    .weight(1f)
            )

            if (PlatformStuff.isTv) {
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.OutlinedLeadingButton(
                            onClick = {
                                internalValue -= increments
                                scope.launch { mainPrefs.updateData { it.copyToSave(internalValue.toInt()) } }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.KeyboardArrowLeft,
                                contentDescription = stringResource(Res.string.move_left),
                            )
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.OutlinedTrailingButton(
                            onCheckedChange = {
                                internalValue += increments
                                scope.launch { mainPrefs.updateData { it.copyToSave(internalValue.toInt()) } }
                            },
                            checked = false,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.KeyboardArrowRight,
                                contentDescription = stringResource(Res.string.move_right),
                            )
                        }
                    },
                )
            }

            Text(
                text = stringRepresentation(internalValue.roundToInt()),
                modifier = Modifier.padding(start = 16.dp)
            )

        }
    }
}

@Composable
fun AccountPref(
    type: AccountType,
    usernamesMap: Map<AccountType, String>,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = accountTypeLabel(type)
    val logoutString = stringResource(Res.string.pref_logout)
    val scope = rememberCoroutineScope()
    var canLogoutNow by remember { mutableStateOf(false) }
    val confirmTime = 3000L

    TextPref(
        text = label,
        summary = if (canLogoutNow) {
            stringResource(Res.string.sure_tap_again)
        } else {
            usernamesMap[type]?.let { "$logoutString [$it]" }
        },
        onClick = {
            if (usernamesMap[type] == null) {
                onNavigate(LoginDestinations.route(type))
            } else if (canLogoutNow) {
                scope.launch {
                    Scrobblables.deleteAllByType(type)
                    canLogoutNow = false
                }
            } else {
                scope.launch {
                    canLogoutNow = true
                    delay(confirmTime)
                    canLogoutNow = false
                }
            }
        },
        modifier = modifier
    )
}