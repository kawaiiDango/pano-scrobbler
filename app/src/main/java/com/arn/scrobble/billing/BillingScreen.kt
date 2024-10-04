package com.arn.scrobble.billing

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwipeLeftAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.ExtrasConsts
import com.arn.scrobble.R
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.toast

@Composable
fun BillingScreen(
    viewModel: BillingViewModel = viewModel(),
    onNavigateToTroubleshoot: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val proProductDetails by viewModel.proProductDetails.collectAsStateWithLifecycle()
    val licenseState by viewModel.licenseState.collectAsStateWithLifecycle()
    var noticeText by rememberSaveable { mutableStateOf<String?>(null) }

    val thankYouText = stringResource(R.string.thank_you)
    val purchasePendingText = stringResource(R.string.purchase_pending)
    val maxDevicesReachedText = stringResource(R.string.billing_max_devices_reached, 4)
    val notFoundText = stringResource(R.string.not_found)

    val bulletStrings = listOfNotNull(
        Icons.Outlined.Palette to stringResource(R.string.pref_themes),
        Icons.Outlined.Apps to stringResource(R.string.billing_scrobble_source),
        Icons.Outlined.Block to stringResource(R.string.billing_block),
        Icons.Outlined.PushPin to stringResource(R.string.billing_pin_friends, 10),
        if (!Stuff.isTv) Icons.Outlined.FavoriteBorder to stringResource(R.string.pref_link_heart_button_rating) else null,
        Icons.Outlined.SwipeLeftAlt to stringResource(R.string.billing_regex_extract),
        if (!Stuff.isTv) Icons.Outlined.Share to stringResource(R.string.billing_sharing) else null,
    )

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
            contentDescription = stringResource(R.string.love),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )

        Text(
            text = stringResource(R.string.pro_support),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = stringResource(R.string.pro_also_get),
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

        ExtendedFloatingActionButton(
            onClick = {
                if (proProductDetails != null) {
                    makePurchase(context, viewModel)
                } else {
                    context.toast(R.string.loading)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.get_pro),
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
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        if (ExtrasConsts.isFossBuild) {
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    codeError = null
                },
                label = { Text(stringResource(R.string.pref_imexport_code)) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Ascii
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        verifyLicenseOnline()
                    }
                ),
                trailingIcon = if (!Stuff.isTv) {
                    {
                        IconButton(onClick = ::verifyLicenseOnline) {
                            Icon(
                                imageVector = Icons.Outlined.Done,
                                contentDescription = null
                            )
                        }
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
            contentDescription = stringResource(R.string.help),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    LaunchedEffect(licenseState) {
        when (licenseState) {
            LicenseState.VALID -> {
                context.toast(thankYouText)
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

private fun makePurchase(context: Context, viewModel: BillingViewModel) {
    if (ExtrasConsts.isFossBuild) {
        Stuff.openInBrowser(context.getString(R.string.ko_fi_link))
    } else {
        viewModel.makePlayPurchase(context as Activity)
    }
}