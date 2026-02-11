package com.arn.scrobble.pref

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.billing.LocalLicenseValidState
import com.arn.scrobble.icons.ContentCopy
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.automation_cli_info
import pano_scrobbler.composeapp.generated.resources.automation_cp_info
import pano_scrobbler.composeapp.generated.resources.automation_replace_app_id
import pano_scrobbler.composeapp.generated.resources.choose_apps

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AutomationInfoScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier
) {
    val allowedPackages by
    PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.allowedAutomationPackages }

    val commandsList = remember {
        listOf(
            Automation.ENABLE,
            Automation.DISABLE,
            Automation.LOVE,
            Automation.UNLOVE,
            Automation.CANCEL,
            Automation.ALLOWLIST,
            Automation.BLOCKLIST
        )
    }

    val isLicenseValid = LocalLicenseValidState.current

    val appIdPlaceholder = if (PlatformStuff.isDesktop)
        "<APP_ID/MPRIS_ID>"
    else
        "<package_name>"

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {

        if (!PlatformStuff.isDesktop) {
            AppIconsPref(
                packageNames = allowedPackages,
                title = stringResource(Res.string.choose_apps),
                enabled = isLicenseValid,
                onClick = {
                    if (!isLicenseValid)
                        onNavigate(PanoRoute.Billing)
                    else
                        onNavigate(
                            PanoRoute.AppList(
                                isSingleSelect = false,
                                saveType = AppListSaveType.Automation,
                                preSelectedPackages = allowedPackages.toList()
                            )
                        )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium
                    )
            )
        }

        Text(
            text = if (PlatformStuff.isDesktop)
                stringResource(Res.string.automation_cli_info)
            else
                stringResource(Res.string.automation_cp_info),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        commandsList.forEach { command ->
            val commandText = if (PlatformStuff.isDesktop) {
                if (command == Automation.BLOCKLIST || command == Automation.ALLOWLIST) {
                    "--$command $appIdPlaceholder"
                } else {
                    "--$command"
                }
            } else {
                if (command == Automation.BLOCKLIST || command == Automation.ALLOWLIST) {
                    "content://${Automation.PREFIX}/${command}/$appIdPlaceholder"
                } else {
                    "content://${Automation.PREFIX}/$command"
                }
            }

            val commandSubtext =
                if (command == Automation.BLOCKLIST || command == Automation.ALLOWLIST) {
                    stringResource(Res.string.automation_replace_app_id, appIdPlaceholder)
                } else {
                    null
                }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        if (!isLicenseValid)
                            onNavigate(PanoRoute.Billing)
                        else
                            PlatformStuff.copyToClipboard(commandText)
                    }
                    .alpha(if (!isLicenseValid) 0.5f else 1f),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                ) {
                    Text(
                        text = (if (!isLicenseValid) "🔒 " else "") + commandText,
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                    )

                    if (commandSubtext != null) {
                        Text(
                            text = commandSubtext,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                Icon(
                    imageVector = Icons.ContentCopy,
                    contentDescription = null,
                )
            }
        }
    }
}