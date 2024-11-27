package com.arn.scrobble.help

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arn.scrobble.R
import com.arn.scrobble.onboarding.WebViewScreen
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.utils.BugReportUtils
import com.arn.scrobble.utils.Stuff

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier
) {
    WebViewScreen(
        initialUrl = stringResource(id = R.string.faq_link),
        onTitleChange = {},
        onBack = { },
        bottomContent = {
            if (!Stuff.isTv)
                ButtonWithIcon(
                    text = stringResource(id = R.string.bug_report),
                    onClick = { BugReportUtils.mailLogs() },
                    icon = Icons.Outlined.BugReport,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .align(Alignment.CenterHorizontally)
                )
        },
        modifier = modifier
    )
}