package com.arn.scrobble.billing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.icons.Apps
import com.arn.scrobble.icons.Automation
import com.arn.scrobble.icons.Block
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Keep
import com.arn.scrobble.icons.Palette
import com.arn.scrobble.icons.Share
import com.arn.scrobble.icons.SwipeLeftAlt
import com.arn.scrobble.icons.automirrored.Help
import com.arn.scrobble.main.MainViewModel
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.PanoOutlinedTextField
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.VariantStuff
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.automation
import pano_scrobbler.composeapp.generated.resources.billing_block
import pano_scrobbler.composeapp.generated.resources.billing_max_devices_reached
import pano_scrobbler.composeapp.generated.resources.billing_pin_friends
import pano_scrobbler.composeapp.generated.resources.billing_regex_extract
import pano_scrobbler.composeapp.generated.resources.billing_scrobble_source
import pano_scrobbler.composeapp.generated.resources.billing_sharing
import pano_scrobbler.composeapp.generated.resources.code_will_be_sent
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.get_pro
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.one_time_purchase
import pano_scrobbler.composeapp.generated.resources.pref_imexport_code
import pano_scrobbler.composeapp.generated.resources.pref_link_heart_button_rating
import pano_scrobbler.composeapp.generated.resources.pref_themes
import pano_scrobbler.composeapp.generated.resources.pro_also_get
import pano_scrobbler.composeapp.generated.resources.pro_support
import pano_scrobbler.composeapp.generated.resources.purchase_pending
import pano_scrobbler.composeapp.generated.resources.thank_you

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BillingScreen(
    viewModel: MainViewModel,
    onNavigateToTroubleshoot: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = getActivityOrNull()
    val formattedPrice by viewModel.formattedPrice.collectAsStateWithLifecycle(null)
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    val bulletStrings = listOfNotNull(
        Icons.Palette to stringResource(Res.string.pref_themes),
        Icons.Apps to stringResource(Res.string.billing_scrobble_source),
        Icons.Block to stringResource(Res.string.billing_block),
        Icons.Keep to stringResource(Res.string.billing_pin_friends, 10),
        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop)
            Icons.Favorite to stringResource(Res.string.pref_link_heart_button_rating)
        else null,
        if (!PlatformStuff.isTv)
            Icons.Automation to stringResource(Res.string.automation)
        else null,
        Icons.SwipeLeftAlt to stringResource(Res.string.billing_regex_extract),
        if (!PlatformStuff.isTv && !PlatformStuff.isDesktop)
            Icons.Share to stringResource(Res.string.billing_sharing)
        else null,
    )

    var purchaseMethodsExpanded by rememberSaveable { mutableStateOf(false) }
    var code by rememberSaveable { mutableStateOf("") }
    val purchaseMethods = remember { VariantStuff.billingRepository.purchaseMethods }
    val needsActivationCode = remember { VariantStuff.billingRepository.needsActivationCode }
    var purchaseMethodClicked by remember { mutableStateOf<PurchaseMethod?>(null) }

    fun verifyLicenseOnline() {
        code.trim().ifEmpty { null }?.let {
            viewModel.checkAndStoreLicense(it)
        }
    }

    Column(
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Favorite,
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

        ErrorText(errorText)

        if (purchaseMethodClicked != null && needsActivationCode) {
            Text(
                stringResource(
                    Res.string.code_will_be_sent,
                    purchaseMethodClicked!!.displayName
                ),
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        }

        if (code.isEmpty()) {
            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                MediumExtendedFloatingActionButton(
                    onClick = {
                        if (formattedPrice != null) {
                            if (purchaseMethods.size > 1) {
                                purchaseMethodsExpanded = true
                            } else if (activity != null && purchaseMethods.isNotEmpty()) {
                                viewModel.makePurchase(purchaseMethods.first(), activity)
                            }
                        } else {
                            val failSnackbarData = PanoSnackbarVisuals(
                                "...",
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
                                text = (formattedPrice ?: "") +
                                        ", " + stringResource(Res.string.one_time_purchase),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Favorite,
                            contentDescription = null
                        )
                    },
                )

                DropdownMenu(
                    expanded = purchaseMethodsExpanded,
                    onDismissRequest = { purchaseMethodsExpanded = false }
                ) {
                    purchaseMethods.forEach { purchaseMethod ->
                        DropdownMenuItem(
                            onClick = {
                                viewModel.makePurchase(purchaseMethod, activity)
                                purchaseMethodsExpanded = false
                                purchaseMethodClicked = purchaseMethod
                            },
                            text = {
                                Column {
                                    Text(
                                        purchaseMethod.displayName,
                                        style = MaterialTheme.typography.titleMediumEmphasized
                                    )
                                    Text(
                                        purchaseMethod.displayDesc,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        if (needsActivationCode) {
            PanoOutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    errorText = null
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
                            icon = Icons.Check,
                            contentDescription = stringResource(Res.string.done)
                        )
                    }
                } else
                    null,
                isError = errorText != null,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        ButtonWithIcon(
            onClick = onNavigateToTroubleshoot,
            icon = Icons.AutoMirrored.Help,
            text = stringResource(Res.string.help),
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally)
        )
    }

    LaunchedEffect(Unit) {
        VariantStuff.billingRepository.licenseState.collect {
            if (it == LicenseState.VALID) {
                val thankYouSnackbarData = PanoSnackbarVisuals(
                    getString(Res.string.thank_you),
                )
                Stuff.globalSnackbarFlow.emit(thankYouSnackbarData)
                onBack()
            }
        }
    }

    LaunchedEffect(Unit) {
        VariantStuff.billingRepository.licenseError.collect {
            when (it) {
                LicenseError.PENDING -> {
                    errorText = getString(Res.string.purchase_pending)
                }

                LicenseError.REJECTED -> {
                    errorText = getString(Res.string.not_found)
                }

                LicenseError.MAX_DEVICES_REACHED -> {
                    errorText = getString(Res.string.billing_max_devices_reached, 8)
                }

                LicenseError.NETWORK_ERROR -> {
                    errorText = getString(Res.string.network_error)
                }
            }
        }
    }
}