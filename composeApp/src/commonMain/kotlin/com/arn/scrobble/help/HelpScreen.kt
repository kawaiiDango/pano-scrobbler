package com.arn.scrobble.help

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.utils.BugReportUtils
import kotlinx.coroutines.launch

@Composable
expect fun HelpScreenContents(
    showFilePicker: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var filePickerShown by remember { mutableStateOf(false) }

    HelpScreenContents(
        showFilePicker = { filePickerShown = true },
        modifier
    )

    FilePicker(
        show = filePickerShown,
        mode = FilePickerMode.Save("pano_scrobbler_logs"),
        type = FileType.LOG,
        onDismiss = { filePickerShown = false },
    ) { file ->
        scope.launch {
            BugReportUtils.saveLogsToFile(file)
        }
    }
}