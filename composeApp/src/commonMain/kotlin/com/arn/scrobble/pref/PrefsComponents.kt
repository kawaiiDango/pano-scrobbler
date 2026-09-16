package com.arn.scrobble.pref

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.icons.ArrowDropDown
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.KeyboardArrowLeftAutoMirrored
import com.arn.scrobble.icons.KeyboardArrowRightAutoMirrored
import com.arn.scrobble.icons.Lock
import com.arn.scrobble.icons.ResetSettings
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.ui.AppIcon
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.PanoDropdownMenu
import com.arn.scrobble.ui.myCheckableItemColors
import com.arn.scrobble.ui.myTransparentCheckableItemColors
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.move_left
import pano_scrobbler.composeapp.generated.resources.move_right
import pano_scrobbler.composeapp.generated.resources.no_apps_enabled
import pano_scrobbler.composeapp.generated.resources.pref_logout
import pano_scrobbler.composeapp.generated.resources.reset
import pano_scrobbler.composeapp.generated.resources.sure_tap_again
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

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
    val locked = onNavigateToBilling != null

    ListItem(
        modifier = modifier.alpha(if (!locked) 1f else 0.5f),
        enabled = enabled,
        checked = value,
        colors = ListItemDefaults.myTransparentCheckableItemColors(),
        verticalAlignment = Alignment.CenterVertically,
        onCheckedChange = { newValue ->
            if (locked)
                onNavigateToBilling()
            else
                Stuff.appScope.launch { mainPrefs.updateData { it.copyToSave(newValue) } }
        },
        leadingContent = if (locked) {
            {
                Icon(
                    imageVector = Icons.Lock,
                    contentDescription = null,
                )
            }
        } else null,
        supportingContent = if (summary != null) {
            {
                Text(summary)
            }
        } else null,
        trailingContent = {
            Switch(
                checked = value,
                onCheckedChange = null,
                enabled = enabled,
            )
        }
    ) {
        Text(text)
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
    ListItem(
        modifier = modifier.alpha(if (!locked) 1f else 0.5f),
        enabled = enabled,
        onClick = onClick,
        colors = ListItemDefaults.myCheckableItemColors(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = if (locked) {
            {
                Icon(
                    imageVector = Icons.Lock,
                    contentDescription = null,
                )
            }
        } else null,
        supportingContent = if (summary != null) {
            {
                Text(summary)
            }
        } else null,
    ) {
        Text(text)
    }
}

@Composable
fun <T> DropdownPref(
    text: String,
    selectedValue: T?,
    values: Iterable<T>,
    toLabel: @Composable (T) -> String,
    copyToSave: MainPrefs.(T) -> MainPrefs,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        modifier = modifier,
        enabled = enabled,
        checked = expanded,
        colors = ListItemDefaults.myCheckableItemColors(),
        verticalAlignment = Alignment.CenterVertically,
        onCheckedChange = { expanded = it },
        supportingContent = if (selectedValue != null) {
            {
                Text(
                    text = toLabel(selectedValue),
                )
            }
        } else null,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text)
            Icon(
                imageVector = Icons.ArrowDropDown,
                contentDescription = null,
            )
            Box {
                PanoDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    values.forEach { value ->
                        item(
                            text = { Text(text = toLabel(value)) },
                            onClick = {
                                Stuff.appScope.launch { mainPrefs.updateData { it.copyToSave(value) } }
                                expanded = false
                            },
                            enabled = selectedValue != value
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MultiSelectDropdownPref(
    text: String,
    checkedValues: Set<String>,
    values: Set<String>,
    toLabel: (String) -> String,
    copyToSave: MainPrefs.(Set<String>) -> MainPrefs,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var checkedSet by remember { mutableStateOf(checkedValues) }
    val orderedValues = remember(checkedValues) { checkedSet + (values - checkedSet) }

    ListItem(
        modifier = modifier,
        enabled = enabled,
        checked = expanded,
        colors = ListItemDefaults.myCheckableItemColors(),
        verticalAlignment = Alignment.CenterVertically,
        onCheckedChange = { expanded = it },
        supportingContent = {
            Text(
                text = checkedSet.joinToString(),
            )
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text)
            Icon(
                imageVector = Icons.ArrowDropDown,
                contentDescription = null,
            )
            Box {
                PanoDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        Stuff.appScope.launch { mainPrefs.updateData { it.copyToSave(checkedSet) } }
                        expanded = false
                    }
                ) {
                    orderedValues.forEachIndexed { index, value ->
                        val checked = value in checkedSet

                        checkableItem(
                            text = { Text(text = toLabel(value)) },
                            checked = checked,
                            onCheckedChange = {
                                if (it) {
                                    checkedSet += value
                                } else {
                                    checkedSet -= value
                                }
                            },
                            trailingContent = if (checked) {
                                {
                                    val pos = remember(checkedSet) {
                                        (checkedSet.indexOf(value) + 1).format()
                                    }
                                    Text(text = pos)
                                }
                            } else null
                        )

                        if (index < values.size - 1) {
                            custom {
                                Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))
                            }
                        }
                    }
                }
            }
        }
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

    ListItem(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        colors = ListItemDefaults.myCheckableItemColors(),
        supportingContent = {
            if (packageNames.isEmpty()) {
                Text(stringResource(Res.string.no_apps_enabled))
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .height(24.dp)
                ) {
                    appItems.forEach {
                        AppIcon(
                            appItem = it,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            }
        }
    ) {
        Text(title)
    }
}

@Composable
fun SliderPref(
    text: String,
    value: Float,
    copyToSave: MainPrefs.(Int) -> MainPrefs,
    default: Int?,
    min: Int,
    max: Int,
    increments: Int,
    stringRepresentation: (Int) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var internalValue by remember(value) { mutableFloatStateOf(value) }

    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.myCheckableItemColors(),
        supportingContent = if (!PlatformStuff.isTv) {
            {
                Slider(
                    value = internalValue.coerceIn(min.toFloat(), max.toFloat()),
                    onValueChange = { internalValue = it },
                    onValueChangeFinished = {
                        Stuff.appScope.launch { mainPrefs.updateData { it.copyToSave(internalValue.roundToInt()) } }
                    },
                    valueRange = min.toFloat()..max.toFloat(),
                    steps = ((max - min) / increments) - 1,
                    enabled = enabled,
                )
            }
        } else null,
        trailingContent = if (default != null || PlatformStuff.isTv) {
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (PlatformStuff.isTv) {
                        SplitButtonLayout(
                            leadingButton = {
                                SplitButtonDefaults.OutlinedLeadingButton(
                                    onClick = {
                                        internalValue -= increments
                                        Stuff.appScope.launch {
                                            mainPrefs.updateData {
                                                it.copyToSave(
                                                    internalValue.toInt()
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.KeyboardArrowLeftAutoMirrored,
                                        contentDescription = stringResource(Res.string.move_left),
                                    )
                                }
                            },
                            trailingButton = {
                                SplitButtonDefaults.OutlinedTrailingButton(
                                    onCheckedChange = {
                                        internalValue += increments
                                        Stuff.appScope.launch {
                                            mainPrefs.updateData {
                                                it.copyToSave(
                                                    internalValue.toInt()
                                                )
                                            }
                                        }
                                    },
                                    checked = false,
                                ) {
                                    Icon(
                                        imageVector = Icons.KeyboardArrowRightAutoMirrored,
                                        contentDescription = stringResource(Res.string.move_right),
                                    )
                                }
                            },
                        )
                    }

                    if (default != null) {
                        IconButtonWithTooltip(
                            enabled = enabled && internalValue.roundToInt() != default,
                            icon = Icons.ResetSettings,
                            contentDescription = stringResource(Res.string.reset),
                            onClick = {
                                Stuff.appScope.launch { mainPrefs.updateData { it.copyToSave(default) } }
                            },
                        )
                    }
                }
            }
        } else null,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringRepresentation(internalValue.roundToInt()),
            )
        }
    }
}

@Composable
fun AccountPref(
    title: String,
    type: AccountType,
    usernamesMap: Map<AccountType, String>,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val logoutString = stringResource(Res.string.pref_logout)
    var canLogoutNow by remember { mutableStateOf(false) }

    TextPref(
        text = title,
        summary = if (canLogoutNow) {
            stringResource(Res.string.sure_tap_again)
        } else {
            usernamesMap[type]?.let { "$logoutString [$it]" }
        },
        onClick = {
            if (usernamesMap[type] == null) {
                onNavigate(LoginDestinations.route(type))
            } else if (canLogoutNow) {
                Stuff.appScope.launch {
                    Scrobblables.deleteAllByType(type)
                    canLogoutNow = false
                }
            } else {
                Stuff.appScope.launch {
                    canLogoutNow = true
                    delay(3.seconds)
                    canLogoutNow = false
                }
            }
        },
        modifier = modifier
    )
}