package com.arn.scrobble.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.MySplitButtonLayout
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.fix_it_action
import pano_scrobbler.composeapp.generated.resources.lastfm
import pano_scrobbler.composeapp.generated.resources.skip


enum class OnboardingStepType {
    LOGIN,
    NOTIFICATION_LISTENER,
    DKMA,
    CHOOSE_APPS,
    SEND_NOTIFICATIONS,
}

@Composable
fun ButtonsStepper(
    onOpenClick: () -> Unit,
    onSkipClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.End)
    ) {
        if (onSkipClick != null) {
            TextButton(onClick = onSkipClick) {
                Text(text = stringResource(Res.string.skip))
            }
        }
        OutlinedButton(onClick = onOpenClick) {
            Text(text = stringResource(Res.string.fix_it_action))
        }
    }
}

@Composable
fun ButtonStepperForLogin(navigate: (PanoRoute) -> Unit) {
    val accountTypesToStringRes = remember {
        AccountType.entries.filterNot {
            it == AccountType.LASTFM || PlatformStuff.isTv && it == AccountType.FILE
        }.associateWith {
            Scrobblables.getStringRes(it)
        }
    }

    var dropDownShown by remember { mutableStateOf(false) }

    MySplitButtonLayout(
        leadingButtonContent = {
            Text(stringResource(Res.string.lastfm))
        },
        leadingButtonOnClick = {
            navigate(LoginDestinations.route(AccountType.LASTFM))
        },
        trailingButtonContent = {
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)

            DropdownMenu(
                expanded = dropDownShown,
                onDismissRequest = { dropDownShown = false }
            ) {
                accountTypesToStringRes.forEach { (accType, stringRes) ->
                    DropdownMenuItem(
                        onClick = {
                            navigate(LoginDestinations.route(accType))
                            dropDownShown = false
                        },
                        text = {
                            Text(stringResource(stringRes))
                        }
                    )
                }
            }
        },
        trailingButtonOnCheckedChange = {
            dropDownShown = it
        },
        trailingButtonChecked = dropDownShown,
    )
}

@Composable
fun VerticalStepperItem(
    titleRes: StringResource,
    descriptionRes: StringResource?,
    openAction: () -> Unit,
    isDone: Boolean,
    isExpanded: Boolean,
    onSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    buttonsContent: @Composable () -> Unit = {
        ButtonsStepper(
            onOpenClick = openAction,
            onSkipClick = onSkip
        )
    },
) {

    val icon = if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.Circle

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = horizontalOverscanPadding())
            .then(
                if (isExpanded)
                    Modifier.alpha(1f)
                else
                    Modifier.alpha(0.5f)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )


            AnimatedVisibility(isExpanded) {
                Column(
                    modifier = modifier.fillMaxWidth()
                ) {
                    if (descriptionRes != null) {
                        Text(
                            text = stringResource(descriptionRes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    buttonsContent()
                }
            }
        }
    }
}

@Composable
expect fun OnboardingScreen(
    onNavigate: (PanoRoute) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
)