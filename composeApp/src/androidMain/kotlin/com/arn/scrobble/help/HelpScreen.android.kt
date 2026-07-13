package com.arn.scrobble.help

import android.os.Build
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.KeyboardArrowDown
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.affect_performance
import pano_scrobbler.composeapp.generated.resources.copy
import pano_scrobbler.composeapp.generated.resources.exit_reasons
import pano_scrobbler.composeapp.generated.resources.log_to_file
import pano_scrobbler.composeapp.generated.resources.more
import pano_scrobbler.composeapp.generated.resources.save_logs

@Composable
actual fun HelpSaveLogsButton(
    showFilePicker: () -> Unit,
    modifier: Modifier
) {
    var menuShown by remember { mutableStateOf(false) }
    val logToFile by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.logToFileOnAndroid }

    var newCheckedState by remember { mutableStateOf<Boolean?>(null) }
    var exitReasonsShown by remember { mutableStateOf(false) }

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

    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.OutlinedLeadingButton(
                onClick = showFilePicker
            ) {
                Text(stringResource(Res.string.save_logs))

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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(Res.string.exit_reasons))
                            },
                            onClick = {
                                menuShown = false
                                exitReasonsShown = true
                            }
                        )
                    }
                }
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

    if (exitReasonsShown) {
        val exitReasonsText = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                AndroidStuff.getScrobblerExitReasons()
                    .take(5)
                    .mapIndexed { index, info -> "${index + 1}. $info" }
                    .joinToString(separator = "\n\n")
            else null
        }

        if (exitReasonsText != null) {
            AlertDialog(
                text = {
                    Text(
                        text = exitReasonsText,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                },
                onDismissRequest = {
                    exitReasonsShown = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            PlatformStuff.copyToClipboard(exitReasonsText)
                        }
                    ) {
                        Text(text = stringResource(Res.string.copy))
                    }
                }
            )
        }
    }
}