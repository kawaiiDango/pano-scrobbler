package com.arn.scrobble.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.BugReport
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.onboarding.WebViewScreen
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.utils.BugReportUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.bug_report
import pano_scrobbler.composeapp.generated.resources.faq
import pano_scrobbler.composeapp.generated.resources.save_logs
import java.io.File

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var filePickerShown by remember { mutableStateOf(false) }

    if (PlatformStuff.isDesktop) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            OutlinedButton(
                onClick = {
                    PlatformStuff.openInBrowser(Stuff.LINK_FAQ)
                }
            ) {
                Text(stringResource(Res.string.faq))
            }

            BugReportButtons(
                onBugReportClick = {
                    scope.launch {
                        BugReportUtils.mail()
                    }
                },
                onLogsClick = {
                    scope.launch {
                        filePickerShown = true
                    }
                }
            )
        }
    } else {
        WebViewScreen(
            initialUrl = Stuff.LINK_FAQ,
            onSetTitle = {},
            onBack = { },
            bottomContent = {
                if (!PlatformStuff.isTv) {
                    BugReportButtons(
                        onBugReportClick = {
                            scope.launch {
                                BugReportUtils.mail()
                            }
                        },
                        onLogsClick = {
                            scope.launch {
                                filePickerShown = true
                            }
                        }
                    )
                }
            },
            modifier = modifier
        )
    }

    FilePicker(
        show = filePickerShown,
        mode = FilePickerMode.Save("pano_scrobbler_logs"),
        type = FileType.LOG,
        onDismiss = { filePickerShown = false },
    ) { platformFile ->
        scope.launch {
            val sourcePath = BugReportUtils.saveLogsToFile()
            if (sourcePath != null) {
                platformFile.overwrite {
                    File(sourcePath).inputStream().use { input ->
                        input.copyTo(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun BugReportButtons(
    onBugReportClick: () -> Unit,
    onLogsClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(
            onClick = onLogsClick,
        ) {
            Text(stringResource(Res.string.save_logs))
        }

        ButtonWithIcon(
            text = stringResource(Res.string.bug_report),
            onClick = onBugReportClick,
            icon = Icons.BugReport,
            modifier = Modifier
                .padding(horizontal = 24.dp)
        )
    }

}