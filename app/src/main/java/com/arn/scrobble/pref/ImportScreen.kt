package com.arn.scrobble.pref

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.R
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.RadioButtonGroup
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.toast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun ImportScreen(
    viewModel: ImportVM = viewModel(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var codeText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var toggleButtonSelectedIndex by remember { mutableIntStateOf(-1) }
    val networkMode =
        remember(toggleButtonSelectedIndex) { toggleButtonSelectedIndex == 1 }
    var selectedEditsMode by remember { mutableStateOf(EditsMode.EDITS_NOPE) }
    var settingsMode by remember { mutableStateOf(false) }
    var importDialogShown by remember { mutableStateOf(false) }
    val importErrorText = stringResource(id = R.string.import_hey_wtf)
    val importSuccessText = stringResource(id = R.string.imported)

    // On Android 11 TV:
    // Permission Denial: opening provider com.android.externalstorage.ExternalStorageProvider
    // from ProcessRecord{a608cee 5039:com.google.android.documentsui/u0a21}
    // (pid=5039, uid=10021) requires that you obtain access using ACTION_OPEN_DOCUMENT or related APIs
    val importRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(uri)
                viewModel.setInputStream(inputStream)
            }
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        ImExportModeSelector(toggleButtonSelectedIndex) { index ->
            when (index) {
                0 -> {
                    importRequest.launch(arrayOf(Stuff.MIME_TYPE_JSON))
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
                    text = stringResource(id = R.string.pref_imexport_code),
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
                    text = stringResource(id = R.string.pref_imexport_network_notice),
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
                errorText = it.message
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.inputStream.collectLatest {
            importDialogShown = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.importResult.collectLatest {
            if (it) {
                errorText = null
                context.toast(importSuccessText)
                onBack()
            } else {
                errorText = importErrorText
            }
        }
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
    val editsNopeText = stringResource(id = R.string.import_lists_nope)
    val editsReplaceAllText = stringResource(id = R.string.import_lists_replace_all)
    val editsReplaceExistingText = stringResource(id = R.string.import_lists_replace_existing)
    val editsKeepText = stringResource(id = R.string.import_lists_keep)

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
            Text(text = stringResource(id = R.string.import_options))
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
                    text = stringResource(id = R.string.import_settings)
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
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}