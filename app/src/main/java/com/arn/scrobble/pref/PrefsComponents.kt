package com.arn.scrobble.pref

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import coil3.compose.AsyncImage
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.onboarding.LoginDestinations
import com.arn.scrobble.themes.AppPreviewTheme
import com.arn.scrobble.ui.PackageName
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val mainPrefs = PlatformStuff.mainPrefs

@Composable
fun SwitchPref(
    text: String,
    summary: String? = null,
    enabled: Boolean = true,
    needsPremium: Boolean = false,
    value: Boolean,
    copyToSave: MainPrefs.(Boolean) -> MainPrefs,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val enabled = if (needsPremium) Stuff.billingRepository.isLicenseValid else enabled
    val locked = needsPremium && !Stuff.billingRepository.isLicenseValid
    val fragment = LocalFragment.current

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
                        fragment
                            .findNavController()
                            .navigate(R.id.billingFragment)
                    }
                else
                    Modifier
            )
            .padding(16.dp)
//            .padding(start = 56.dp)
            .alpha(if (enabled) 1f else UiUtils.DISABLED_ALPHA),
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
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (enabled)
                    Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp)
//            .padding(start = 56.dp)
            .alpha(if (enabled) 1f else UiUtils.DISABLED_ALPHA),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
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
    toLabel: (T) -> String,
    enabled: Boolean = true,
    copyToSave: MainPrefs.(T) -> MainPrefs,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedDisplayText = toLabel(selectedValue)

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (enabled)
                    Modifier.clickable { expanded = true }
                else Modifier
            )
            .padding(16.dp)
//            .padding(start = 56.dp)
            .alpha(if (enabled) 1f else UiUtils.DISABLED_ALPHA),
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
}

@Composable
fun HeaderPref(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .padding(end = 32.dp)
                .size(24.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun AppIconsPref(
    packageNames: Set<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val packageNamesFiltered = remember(packageNames) { mutableStateListOf<String>() }

    LaunchedEffect(packageNames) {
        withContext(Dispatchers.IO) {
            packageNamesFiltered.addAll(
                packageNames.filter { Stuff.isPackageInstalled(it) }
            )
        }
    }

    val maxIcons = 7

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(16.dp),
//            .padding(start = 56.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(24.dp)
        ) {
            packageNamesFiltered.take(maxIcons).forEach {
                AsyncImage(
                    model = PackageName(it),
                    contentDescription = it,
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }

        Text(
            text = stringResource(R.string.pref_enabled_apps_summary),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun SliderPref(
    text: String,
    value: Float,
    copyToSave: MainPrefs.(Int) -> MainPrefs,
    min: Int,
    max: Int,
    increments: Int,
    stringRepresentation: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var internalValue by remember(value) { mutableStateOf(value) }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .fillMaxWidth()
            .padding(16.dp)
//            .padding(start = 56.dp)
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
                    scope.launch { mainPrefs.updateData { it.copyToSave(internalValue.toInt()) } }
                },
                valueRange = min.toFloat()..max.toFloat(),
                steps = (max - min) / increments,
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val incrementValue = when (keyEvent.key) {
                                Key.DirectionLeft -> -increments
                                Key.DirectionRight -> increments
                                else -> return@onKeyEvent false
                            }
                            internalValue += incrementValue
                            scope.launch {
                                mainPrefs.updateData { it.copyToSave(internalValue.toInt()) }
                            }
                            true
                        } else false
                    })
            Text(
                text = stringRepresentation(internalValue.toInt()),
                modifier = Modifier.padding(start = 16.dp)
            )

        }
    }
}

@Composable
fun AccountPref(
    type: AccountType,
    usernamesMap: Map<AccountType, String>,
    modifier: Modifier = Modifier
) {
    val fragment = LocalFragment.current
    val label = accountTypeLabel(type)
    val logoutString = stringResource(R.string.pref_logout)
    val scope = rememberCoroutineScope()
    var canLogoutNow by remember { mutableStateOf(false) }
    val confirmTime = 3000L

    TextPref(
        text = label,
        summary = if (canLogoutNow) {
            stringResource(R.string.sure_tap_again)
        } else {
            usernamesMap[type]?.let { "$logoutString [$it]" }
        },
        onClick = {
            if (usernamesMap[type] == null) {
                LoginDestinations(fragment.findNavController()).go(type)
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

fun accountTypeStringRes(accountType: AccountType) = when (accountType) {
    AccountType.LASTFM -> R.string.lastfm
    AccountType.LIBREFM -> R.string.librefm
    AccountType.GNUFM -> R.string.gnufm
    AccountType.LISTENBRAINZ -> R.string.listenbrainz
    AccountType.CUSTOM_LISTENBRAINZ -> R.string.custom_listenbrainz
    AccountType.MALOJA -> R.string.maloja
    AccountType.PLEROMA -> R.string.pleroma
    AccountType.FILE -> R.string.scrobble_to_file
}

@Composable
fun accountTypeLabel(accountType: AccountType) = stringResource(accountTypeStringRes(accountType))

@Preview(showBackground = true)
@Composable
fun SwitchPrefPreview() {
    AppPreviewTheme {
        SwitchPref(
            text = "Master switch",
            value = true,
            copyToSave = { copy(scrobblerEnabled = it) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TextPrefPreview() {
    AppPreviewTheme {
        TextPref(
            text = "Add to Quick Settings",
            onClick = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeaderPrefPreview() {
    AppPreviewTheme {
        HeaderPref(
            text = "Header",
            icon = Icons.Outlined.Apps
        )
    }
}