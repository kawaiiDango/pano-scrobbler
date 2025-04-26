package com.arn.scrobble.pref

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.RadioButtonGroup
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.import_hey_wtf
import pano_scrobbler.composeapp.generated.resources.import_lists_keep
import pano_scrobbler.composeapp.generated.resources.import_lists_nope
import pano_scrobbler.composeapp.generated.resources.import_lists_replace_all
import pano_scrobbler.composeapp.generated.resources.import_lists_replace_existing
import pano_scrobbler.composeapp.generated.resources.import_options
import pano_scrobbler.composeapp.generated.resources.import_settings
import pano_scrobbler.composeapp.generated.resources.imported
import pano_scrobbler.composeapp.generated.resources.ok
import pano_scrobbler.composeapp.generated.resources.pref_imexport_code
import pano_scrobbler.composeapp.generated.resources.pref_imexport_network_notice

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportVM = viewModel { ImportVM() },
) {
    var codeText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var toggleButtonSelectedIndex by remember { mutableIntStateOf(-1) }
    val networkMode =
        remember(toggleButtonSelectedIndex) { toggleButtonSelectedIndex == 1 }
    var selectedEditsMode by remember { mutableStateOf(EditsMode.EDITS_NOPE) }
    var settingsMode by remember { mutableStateOf(false) }
    var importDialogShown by remember { mutableStateOf(false) }
    val importErrorText = stringResource(Res.string.import_hey_wtf)
    val importSuccessText = stringResource(Res.string.imported)
    var filePickerShown by remember { mutableStateOf(false) }


    // On Android 11 TV:
    // Permission Denial: opening provider com.android.externalstorage.ExternalStorageProvider
    // from ProcessRecord{a608cee 5039:com.google.android.documentsui/u0a21}
    // (pid=5039, uid=10021) requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        ImExportModeSelector(toggleButtonSelectedIndex) { index ->
            when (index) {
                0 -> {
                    filePickerShown = true
                }

                1 -> {
                    viewModel.startServer()
                }
            }
            toggleButtonSelectedIndex = index
            errorText = null
        }

        ErrorText(
            errorText = errorText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AnimatedVisibility(visible = networkMode) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.pref_imexport_code),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = codeText ?: "",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(Res.string.pref_imexport_network_notice),
                    textAlign = TextAlign.Center
                )
            }
        }

    }

    if (importDialogShown) {
        ImportDialog(
            editsMode = selectedEditsMode,
            onEditsModeChange = { selectedEditsMode = it },
            settingsMode = settingsMode,
            onSettingsModeChange = { settingsMode = it },
            onDismissRequest = { importDialogShown = false },
            onImport = {
                importDialogShown = false
                viewModel.import(selectedEditsMode, settingsMode)
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.serverAddress.filterNotNull().collectLatest { result ->
            result.onSuccess {
                codeText = it
                errorText = null
            }.onFailure {
                errorText = it.redactedMessage
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.jsonText.collectLatest {
            importDialogShown = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.importResult.collectLatest {
            if (it) {
                errorText = null
                Stuff.globalSnackbarFlow.emit(
                    PanoSnackbarVisuals(
                        message = importSuccessText,
                        isError = false
                    )
                )
                onBack()
            } else {
                errorText = importErrorText
            }
        }
    }

    FilePicker(
        show = filePickerShown,
        mode = FilePickerMode.Open(),
        type = FileType.JSON,
        onDismiss = { filePickerShown = false },
    ) {
        viewModel.setPlatformFile(it)
    }
}

@Composable
private fun ImportDialog(
    editsMode: EditsMode,
    onEditsModeChange: (EditsMode) -> Unit,
    settingsMode: Boolean,
    onSettingsModeChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onImport: () -> Unit,
) {
    val editsNopeText = stringResource(Res.string.import_lists_nope)
    val editsReplaceAllText = stringResource(Res.string.import_lists_replace_all)
    val editsReplaceExistingText = stringResource(Res.string.import_lists_replace_existing)
    val editsKeepText = stringResource(Res.string.import_lists_keep)

    val radioOptions = remember {
        mapOf(
            EditsMode.EDITS_NOPE to editsNopeText,
            EditsMode.EDITS_REPLACE_ALL to editsReplaceAllText,
            EditsMode.EDITS_REPLACE_EXISTING to editsReplaceExistingText,
            EditsMode.EDITS_KEEP_EXISTING to editsKeepText
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(Res.string.import_options))
        },
        text = {
            Column {
                RadioButtonGroup(
                    selected = editsMode,
                    onSelected = { onEditsModeChange(it) },
                    enumToTexts = radioOptions,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                LabeledCheckbox(
                    checked = settingsMode,
                    onCheckedChange = { onSettingsModeChange(it) },
                    text = stringResource(Res.string.import_settings)
                )
            }
        },
        confirmButton = {
            OutlinedButton(onClick = {
                if (editsMode == EditsMode.EDITS_NOPE && !settingsMode) {
                    onDismissRequest()
                } else {
                    onImport()
                }
            }) {
                Text(text = stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(Res.string.cancel))
            }
        }
    )
}