package com.arn.scrobble.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.BugReport
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.KeyboardArrowDown
import com.arn.scrobble.onboarding.WebViewScreen
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.utils.BugReportUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.affect_performance
import pano_scrobbler.composeapp.generated.resources.bug_report
import pano_scrobbler.composeapp.generated.resources.log_to_file
import pano_scrobbler.composeapp.generated.resources.more
import pano_scrobbler.composeapp.generated.resources.save_logs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun HelpScreenContents(
    showFilePicker: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
    ) {
        WebViewScreen(
            initialUrl = Stuff.FAQ_URL,
            onSetTitle = {},
            onBack = { },
            modifier = Modifier.weight(1f)
        )

        if (!PlatformStuff.isTv) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                var menuShown by remember { mutableStateOf(false) }
                val logToFile by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.logToFileOnAndroid }

                var newCheckedState by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(newCheckedState) {
                    newCheckedState?.let { newCheckedState ->
                        PlatformStuff.mainPrefs.updateData { prefs ->
                            prefs.copy(
                                logToFileOnAndroidSince = if (newCheckedState)
                                    System.currentTimeMillis()
                                else
                                    -1
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = menuShown,
                    onDismissRequest = { menuShown = false },
                ) {
                    DropdownMenuItem(
                        checkedLeadingIcon = {
                            Icon(
                                imageVector = Icons.Check,
                                contentDescription = null,
                            )
                        },
                        text = {
                            Text(
                                stringResource(Res.string.log_to_file) + "\n" +
                                        "(" + stringResource(Res.string.affect_performance) + ")"

                            )
                        },
                        shapes = MenuDefaults.itemShapes(),
                        checked = logToFile,
                        onCheckedChange = {
                            newCheckedState = it
                        }
                    )
                }
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.OutlinedLeadingButton(
                            onClick = {
                                showFilePicker()
                            }
                        ) {
                            Text(stringResource(Res.string.save_logs))
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.OutlinedTrailingButton(
                            onCheckedChange = {
                                menuShown = it
                            },
                            checked = menuShown,
                        ) {
                            Icon(
                                imageVector = Icons.KeyboardArrowDown,
                                contentDescription = stringResource(Res.string.more),
                            )
                        }
                    },
                )

                ButtonWithIcon(
                    text = stringResource(Res.string.bug_report),
                    onClick = {
                        BugReportUtils.mail()
                    },
                    icon = Icons.BugReport,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                )
            }
        }
    }
}