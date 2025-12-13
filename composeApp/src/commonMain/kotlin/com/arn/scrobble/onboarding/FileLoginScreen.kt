package com.arn.scrobble.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arn.scrobble.api.file.FileScrobblable
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.OutlinedToggleButtons
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.RadioButtonGroup
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.create
import pano_scrobbler.composeapp.generated.resources.open_existing

private enum class FileOpenType {
    CREATE,
    OPEN
}

@Composable
fun FileLoginScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFileFormat by remember { mutableStateOf<FileScrobblable.FileFormat?>(null) }
    val createText = stringResource(Res.string.create)
    val openText = stringResource(Res.string.open_existing)
    val fileOpenTypesToTexts =
        remember { mapOf(FileOpenType.CREATE to createText, FileOpenType.OPEN to openText) }
    var filePickerMode by remember { mutableStateOf<FilePickerMode?>(null) }
    var filePickerType by remember { mutableStateOf<FileType?>(null) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {

        OutlinedToggleButtons(
            items = FileScrobblable.FileFormat.entries.map { "." + it.name },
            selectedIndex = selectedFileFormat?.ordinal ?: -1,
            onSelected = {
                selectedFileFormat = FileScrobblable.FileFormat.entries[it]
            },
        )

        AnimatedVisibility(visible = selectedFileFormat != null) {
            RadioButtonGroup(
                enumToTexts = fileOpenTypesToTexts,
                selected = null,
                onSelected = {
                    when (it) {
                        FileOpenType.CREATE -> {
                            if (selectedFileFormat == FileScrobblable.FileFormat.csv) {
                                filePickerMode =
                                    FilePickerMode.Save("scrobbles_log_" + Stuff.getFileNameDateSuffix())
                                filePickerType = FileType.CSV
                            } else if (selectedFileFormat == FileScrobblable.FileFormat.jsonl) {
                                filePickerMode =
                                    FilePickerMode.Save("scrobbles_log_" + Stuff.getFileNameDateSuffix())
                                filePickerType = FileType.JSONL
                            }
                        }

                        FileOpenType.OPEN -> {
                            filePickerMode = FilePickerMode.Open()
                            filePickerType =
                                if (selectedFileFormat == FileScrobblable.FileFormat.csv) {
                                    FileType.CSV
                                } else {
                                    FileType.JSONL
                                }
                        }
                    }
                }
            )
        }
    }

    if (filePickerMode != null && filePickerType != null) {
        FilePicker(
            show = true,
            mode = filePickerMode!!,
            type = filePickerType!!,
            onDismiss = {
                onBack()
//                filePickerMode = null
//                filePickerType = null
                // cannot reuse this screen on android after launching filePicker once, so always onBack()
            },
        ) { uri ->
            GlobalScope.launch {
                FileScrobblable.authAndGetSession(uri, selectedFileFormat!!)
                    .onFailure {
                        Stuff.globalSnackbarFlow.emit(
                            PanoSnackbarVisuals(
                                message = it.redactedMessage,
                                isError = true,
                                duration = SnackbarDuration.Long
                            )
                        )
                    }
            }
        }
    }
}