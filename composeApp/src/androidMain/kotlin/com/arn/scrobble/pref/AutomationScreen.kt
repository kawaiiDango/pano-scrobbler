package com.arn.scrobble.pref

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.providers.AutomationProvider
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.choose_apps
import pano_scrobbler.composeapp.generated.resources.pref_automation_desc
import pano_scrobbler.composeapp.generated.resources.pref_automation_tasker

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AutomationScreen(
    allowedPackages: List<String>,
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        val prefix = "content://com.arn.scrobble.automation/"
        val uris = remember {
            arrayOf(
                prefix + AutomationProvider.ENABLE,
                prefix + AutomationProvider.DISABLE,
                prefix + AutomationProvider.LOVE,
                prefix + AutomationProvider.UNLOVE,
                prefix + AutomationProvider.CANCEL,
                prefix + AutomationProvider.ALLOWLIST + "/<packageName>",
                prefix + AutomationProvider.BLOCKLIST + "/<packageName>",
            )
        }
        Text(
            text = stringResource(Res.string.pref_automation_desc),
            style = MaterialTheme.typography.titleMediumEmphasized,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        Text(
            text = stringResource(Res.string.pref_automation_tasker),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        OutlinedButton(
            onClick = {
                onNavigate(
                    PanoRoute.AppList(
                        saveType = AppListSaveType.Automation,
                        isSingleSelect = false,
                        preSelectedPackages = allowedPackages.toList()
                    )
                )
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(Res.string.choose_apps))
        }

        uris.forEach { uri ->
            Text(
                text = uri,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        PlatformStuff.copyToClipboard(uri)
                    }
                    .padding(16.dp)
            )
        }
    }
}