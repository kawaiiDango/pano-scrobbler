package com.arn.scrobble.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.BugReport
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Save
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.utils.BugReportUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.bug_report
import pano_scrobbler.composeapp.generated.resources.faq
import pano_scrobbler.composeapp.generated.resources.save_logs

@Composable
actual fun HelpScreenContents(
    showFilePicker: () -> Unit,
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = {
                PlatformStuff.openInBrowser(Stuff.FAQ_URL)
            }
        ) {
            Text(stringResource(Res.string.faq))
        }

        ButtonWithIcon(
            icon = Icons.Save,
            text = stringResource(Res.string.save_logs),
            onClick = {
                showFilePicker()
            }
        )

        ButtonWithIcon(
            text = stringResource(Res.string.bug_report),
            onClick = {
                BugReportUtils.mail()
            },
            icon = Icons.BugReport,
        )
    }
}