package com.arn.scrobble.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.file.FileScrobblable
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.OutlinedToggleButtons
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.PlatformFile
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.convert_file
import pano_scrobbler.composeapp.generated.resources.create
import pano_scrobbler.composeapp.generated.resources.open_existing

@Composable
fun FileLoginScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel { LoginViewModel() },
) {
    var selectedFileFormat by remember { mutableStateOf<FileScrobblable.FileFormat?>(null) }
    var filePickerMode by remember { mutableStateOf<FilePickerMode?>(null) }
    var filePickerType by remember { mutableStateOf<FileType?>(null) }
    var convert by remember { mutableStateOf(false) }
    var convertFromPlatformFile by remember { mutableStateOf<PlatformFile?>(null) }
    var filePickerShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.result.collect {
            it.onFailure { e ->
                e.printStackTrace()
                Stuff.globalSnackbarFlow.emit(
                    PanoSnackbarVisuals(
                        e.localizedMessage,
                        isError = true,
                        longDuration = true
                    )
                )
            }
            onBack()
        }
    }

    LaunchedEffect(convertFromPlatformFile) {
        val fromFile = convertFromPlatformFile
        if (fromFile != null) {
            val type = selectedFileFormat!!.toFileType()

            Stuff.globalSnackbarFlow.emit(
                PanoSnackbarVisuals(
                    getString(Res.string.create) + " ." + type.name
                )
            )
            delay(1000)

            val fileNameWithoutExtension =
                fromFile.name().substringBeforeLast('.')
            filePickerMode = FilePickerMode.Save(fileNameWithoutExtension)
            filePickerType = type
            filePickerShown = true
        }
    }

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

        AnimatedVisibility(visible = selectedFileFormat != null && convertFromPlatformFile == null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                OutlinedButton(
                    onClick = {
                        convert = false
                        filePickerMode =
                            FilePickerMode.Save("scrobbles_log_" + Stuff.getFileNameDateSuffix())
                        filePickerType = selectedFileFormat!!.toFileType()
                        filePickerShown = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(Res.string.create)) }

                OutlinedButton(
                    onClick = {
                        convert = false
                        filePickerMode = FilePickerMode.Open()
                        filePickerType = selectedFileFormat!!.toFileType()
                        filePickerShown = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(Res.string.open_existing)) }

                val otherFileFormat =
                    if (selectedFileFormat == FileScrobblable.FileFormat.csv) {
                        FileScrobblable.FileFormat.jsonl
                    } else {
                        FileScrobblable.FileFormat.csv
                    }

                OutlinedButton(
                    onClick = {
                        convert = true
                        filePickerMode = FilePickerMode.Open()
                        filePickerType = otherFileFormat.toFileType()
                        filePickerShown = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            Res.string.convert_file,
                            "." + otherFileFormat.toFileType().name
                        )
                    )
                }
            }
        }
    }

    if (filePickerMode != null && filePickerType != null) {
        FilePicker(
            show = filePickerShown,
            mode = filePickerMode!!,
            type = filePickerType!!,
            onDismiss = {
                filePickerShown = false
//                onBack()
//                filePickerMode = null
//                filePickerType = null
                // cannot reuse this screen on android after launching filePicker once, so always onBack()
            },
        ) { file ->
            if (!convert)
                viewModel.fileLogin(file, selectedFileFormat!!)
            else {
                if (convertFromPlatformFile == null)
                    convertFromPlatformFile = file
                else
                    viewModel.fileConvertLogin(
                        convertFromPlatformFile!!,
                        if (selectedFileFormat == FileScrobblable.FileFormat.csv)
                            FileScrobblable.FileFormat.jsonl
                        else
                            FileScrobblable.FileFormat.csv,
                        file,
                        selectedFileFormat!!
                    )
            }
        }
    }
}

private fun FileScrobblable.FileFormat.toFileType(): FileType =
    when (this) {
        FileScrobblable.FileFormat.csv -> FileType.CSV
        FileScrobblable.FileFormat.jsonl -> FileType.JSONL
    }