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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.ui.AppIcon
import com.arn.scrobble.ui.accountTypeLabel
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.move_left
import pano_scrobbler.composeapp.generated.resources.move_right
import pano_scrobbler.composeapp.generated.resources.no_apps_enabled
import pano_scrobbler.composeapp.generated.resources.pref_logout
import pano_scrobbler.composeapp.generated.resources.save
import pano_scrobbler.composeapp.generated.resources.sure_tap_again
import kotlin.math.roundToInt

private val mainPrefs = PlatformStuff.mainPrefs

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
    val enabled =
        if (onNavigateToBilling != null) PlatformStuff.billingRepository.isLicenseValid else enabled
    val locked = onNavigateToBilling != null && !PlatformStuff.billingRepository.isLicenseValid

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
    locked: Boolean = false,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = horizontalOverscanPadding())
            .alpha(if (locked) 0.5f else 1f),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldDialogPref(
    text: String,
    hint: String,
    value: String,
    validate: (String) -> Boolean,
    copyToSave: MainPrefs.(String) -> MainPrefs,
    modifier: Modifier = Modifier,
) {
    var dialogShown by rememberSaveable { mutableStateOf(false) }
    val textFieldValue by rememberSaveable(value) { mutableStateOf(value) }
    val scope = rememberCoroutineScope()
    var isError by rememberSaveable { mutableStateOf(false) }

    TextPref(
        text = text,
        summary = textFieldValue,
        onClick = { dialogShown = true },
        modifier = modifier
    )

    if (dialogShown) {
        BasicAlertDialog(
            onDismissRequest = { dialogShown = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                var textFieldValueInDialog by rememberSaveable { mutableStateOf(textFieldValue) }

                Column {
                    OutlinedTextField(
                        value = textFieldValueInDialog,
                        onValueChange = { textFieldValueInDialog = it },
                        label = { Text(hint) },
                        isError = isError,
                        modifier = Modifier.padding(16.dp)
                    )

                    TextButton(
                        onClick = {
                            if (textFieldValueInDialog.isEmpty()) {
                                textFieldValueInDialog = Stuff.DEFAULT_SEARCH_URL
                            }

                            if (validate(textFieldValueInDialog)) {
                                scope.launch {
                                    PlatformStuff.mainPrefs.updateData {
                                        it.copyToSave(textFieldValueInDialog)
                                    }
                                }
                                isError = false
                                dialogShown = false
                            } else
                                isError = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = stringResource(Res.string.save))
                    }
                }
            }
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
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = null,
        )
    }
}

@Composable
fun AppIconsPref(
    packageNames: Set<String>,
    seenAppsMap: Map<String, String>,
    modifier: Modifier = Modifier,
    summary: String,
    onClick: () -> Unit,
) {
    val maxIcons = 7
    val packageNamesFiltered = remember(packageNames) { mutableStateListOf<String>() }

    LaunchedEffect(packageNames) {
        withContext(Dispatchers.IO) {
            packageNamesFiltered.addAll(
                filterAppList(packageNames, seenAppsMap).take(maxIcons)
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = horizontalOverscanPadding())
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(24.dp)
        ) {
            if (packageNamesFiltered.isEmpty()) {
                Text(stringResource(Res.string.no_apps_enabled))
            } else {
                packageNamesFiltered.forEach {
                    AppIcon(
                        appItem = AppItem(it, seenAppsMap[it] ?: ""),
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        }

        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
        )
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
                enabled = !PlatformStuff.isTv,
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
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
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
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
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

expect fun filterAppList(
    packageNames: Set<String>,
    seenAppsMap: Map<String, String>,
): List<String>