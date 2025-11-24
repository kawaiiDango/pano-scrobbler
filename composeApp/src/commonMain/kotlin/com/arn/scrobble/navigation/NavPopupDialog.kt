package com.arn.scrobble.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arn.scrobble.api.DrawerData
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.VariantStuff
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.get_pro
import pano_scrobbler.composeapp.generated.resources.help
import pano_scrobbler.composeapp.generated.resources.search
import pano_scrobbler.composeapp.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavPopupDialog(
    otherUser: UserCached?,
    drawerDataFlow: StateFlow<DrawerData>,
    drawSnowfall: Boolean,
    loadOtherUserDrawerData: (UserCached?) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerData by drawerDataFlow.collectAsStateWithLifecycle()

    LaunchedEffect(otherUser) {
        loadOtherUserDrawerData(otherUser)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileHeader(
            otherUser = otherUser,
            drawerData = drawerData,
            compact = false,
            onNavigate = onNavigate,
            drawSnowfall = drawSnowfall,
        )

        if (!VariantStuff.billingRepository.isLicenseValid) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(1f)
            ) {
                FilledTonalButton(
                    onClick = {
                        onNavigate(PanoRoute.Billing)
                    },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        Icons.Outlined.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(Res.string.get_pro), maxLines = 1)
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonWithIcon(
                text = stringResource(Res.string.settings),
                icon = Icons.Outlined.Settings,
                onClick = {
                    onNavigate(PanoRoute.Prefs)
                },
            )

            if (!PlatformStuff.isTv) {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Above
                    ),
                    tooltip = { PlainTooltip { Text(stringResource(Res.string.search)) } },
                    state = rememberTooltipState(),
                ) {
                    OutlinedIconButton(
                        onClick = {
                            onNavigate(PanoRoute.Search)
                        },
                        border = ButtonDefaults.outlinedButtonBorder(true),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(Res.string.search),
                        )
                    }
                }

                ButtonWithIcon(
                    text = stringResource(Res.string.help),
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    onClick = {
                        onNavigate(PanoRoute.Help)
                    },
                )
            } else {
                ButtonWithIcon(
                    text = stringResource(Res.string.search),
                    icon = Icons.Outlined.Search,
                    onClick = {
                        onNavigate(PanoRoute.Search)
                    },
                )
            }
        }
    }
}