package com.arn.scrobble.billing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwipeLeftAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.billing_block
import pano_scrobbler.composeapp.generated.resources.billing_max_devices_reached
import pano_scrobbler.composeapp.generated.resources.billing_pin_friends
import pano_scrobbler.composeapp.generated.resources.billing_regex_extract
import pano_scrobbler.composeapp.generated.resources.billing_scrobble_source
import pano_scrobbler.composeapp.generated.resources.billing_sharing
import pano_scrobbler.composeapp.generated.resources.bmc
import pano_scrobbler.composeapp.generated.resources.bmc_link
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.get_pro
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.ko_fi
import pano_scrobbler.composeapp.generated.resources.ko_fi_link
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.pref_automation
import pano_scrobbler.composeapp.generated.resources.pref_imexport_code
import pano_scrobbler.composeapp.generated.resources.pref_link_heart_button_rating
import pano_scrobbler.composeapp.generated.resources.pref_themes
import pano_scrobbler.composeapp.generated.resources.pro_also_get
import pano_scrobbler.composeapp.generated.resources.pro_support
import pano_scrobbler.composeapp.generated.resources.purchase_pending
import pano_scrobbler.composeapp.generated.resources.thank_you

@Composable
fun BillingScreen(
    viewModel: MainViewModel,
    onNavigateToTroubleshoot: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = getActivityOrNull()
    val proProductDetails by viewModel.proProductDetails.collectAsStateWithLifecycle()
    val licenseState by PlatformStuff.billingRepository.licenseState.collectAsStateWithLifecycle()
    var noticeText by rememberSaveable { mutableStateOf<String?>(null) }

    val thankYouText = stringResource(Res.string.thank_you)
    val purchasePendingText = stringResource(Res.string.purchase_pending)
    val maxDevicesReachedText = stringResource(Res.string.billing_max_devices_reached, 8)
    val notFoundText = stringResource(Res.string.not_found)
    val koFiLink = stringResource(Res.string.ko_fi_link)
    val bmcLink = stringResource(Res.string.bmc_link)

    val bulletStrings = listOfNotNull(
        Icons.Outlined.Palette to stringResource(Res.string.pref_themes),
        Icons.Outlined.Apps to stringResource(Res.string.billing_scrobble_source),
        Icons.Outlined.Block to stringResource(Res.string.billing_block),
        Icons.Outlined.PushPin to stringResource(Res.string.billing_pin_friends, 10),
        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop)
            Icons.Outlined.FavoriteBorder to stringResource(Res.string.pref_link_heart_button_rating)
        else null,
        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop)
            Icons.AutoMirrored.Outlined.Rule to stringResource(Res.string.pref_automation)
        else null,
        Icons.Outlined.SwipeLeftAlt to stringResource(Res.string.billing_regex_extract),
        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop)
            Icons.Outlined.Share to stringResource(Res.string.billing_sharing)
        else null,
    )

    var purchaseOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    var codeError by rememberSaveable { mutableStateOf<String?>(null) }

    fun verifyLicenseOnline() {
        code.trim().ifEmpty { null }?.let {
            viewModel.checkAndStoreLicense(it)
        }
    }

    Column(
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(Res.string.love),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )

        Text(
            text = stringResource(Res.string.pro_support),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = stringResource(Res.string.pro_also_get),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        bulletStrings.forEach { (icon, string) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = string,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    if (proProductDetails != null) {
                        if (PlatformStuff.isNonPlayBuild) {
                            purchaseOptionsExpanded = true
                        } else if (activity != null) {
                            viewModel.makePlayPurchase(activity)
                        }
                    } else {
                        val failSnackbarData = PanoSnackbarVisuals(
                            message = "...",
                            isError = false,
                        )
                        Stuff.globalSnackbarFlow.tryEmit(failSnackbarData)
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.get_pro),
                        )

                        Text(
                            text = proProductDetails?.formattedPrice ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                },
            )

            DropdownMenu(
                expanded = purchaseOptionsExpanded,
                onDismissRequest = { purchaseOptionsExpanded = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        PlatformStuff.openInBrowser(koFiLink)
                        purchaseOptionsExpanded = false
                    },
                    text = {
                        Text(stringResource(Res.string.ko_fi))
                    }
                )

                DropdownMenuItem(
                    onClick = {
                        PlatformStuff.openInBrowser(bmcLink)
                        purchaseOptionsExpanded = false
                    },
                    text = {
                        Text(stringResource(Res.string.bmc))
                    }
                )
            }
        }

        if (PlatformStuff.isNonPlayBuild) {
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    codeError = null
                },
                label = { Text(stringResource(Res.string.pref_imexport_code)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Ascii
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        verifyLicenseOnline()
                    }
                ),
                trailingIcon = if (!PlatformStuff.isTv) {
                    {
                        IconButtonWithTooltip(
                            onClick = ::verifyLicenseOnline,
                            icon = Icons.Outlined.Done,
                            contentDescription = stringResource(Res.string.done)
                        )
                    }
                } else
                    null,
                isError = codeError != null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            ErrorText(
                errorText = codeError,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        IconButtonWithTooltip(
            onClick = onNavigateToTroubleshoot,
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = stringResource(Res.string.help),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    LaunchedEffect(licenseState) {
        when (licenseState) {
            LicenseState.VALID -> {
                val thankYouSnackbarData = PanoSnackbarVisuals(
                    message = thankYouText,
                    isError = false,
                )
                Stuff.globalSnackbarFlow.emit(thankYouSnackbarData)
                onBack()
            }

            LicenseState.PENDING -> {
                noticeText = purchasePendingText
            }

            LicenseState.REJECTED -> {
                noticeText = notFoundText
            }

            LicenseState.MAX_DEVICES_REACHED -> {
                noticeText = maxDevicesReachedText
            }

            else -> {}
        }
    }

    if (noticeText != null) {
        AlertDialogOk(
            text = noticeText!!,
            onConfirmation = { noticeText = null },
        )
    }
}