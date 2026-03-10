package com.arn.scrobble.pref

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.navigation.enumSaver
import com.arn.scrobble.ui.ButtonWithSpinner
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.artist_splitting_exceptions
import pano_scrobbler.composeapp.generated.resources.import_lists_keep
import pano_scrobbler.composeapp.generated.resources.import_lists_replace_all
import pano_scrobbler.composeapp.generated.resources.import_lists_replace_existing
import pano_scrobbler.composeapp.generated.resources.import_options
import pano_scrobbler.composeapp.generated.resources.import_settings
import pano_scrobbler.composeapp.generated.resources.imported
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.pref_blocked_metadata
import pano_scrobbler.composeapp.generated.resources.pref_imexport_code
import pano_scrobbler.composeapp.generated.resources.pref_imexport_network_notice
import pano_scrobbler.composeapp.generated.resources.pref_import
import pano_scrobbler.composeapp.generated.resources.regex_rules
import pano_scrobbler.composeapp.generated.resources.scrobble_sources
import pano_scrobbler.composeapp.generated.resources.simple_edits

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportVM = viewModel { ImportVM() },
) {
    var codeText by rememberSaveable { mutableStateOf<String?>(null) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var toggleButtonSelectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    val networkMode =
        rememberSaveable(toggleButtonSelectedIndex) { toggleButtonSelectedIndex == 1 }

    val availableImportTypes by viewModel.availableImportTypes.collectAsStateWithLifecycle(null)
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

        ImExportModeSelector(
            toggleButtonSelectedIndex,
            supportsFile = !PlatformStuff.isTv,
            supportsNetwork = true,
        ) { index ->
            when (index) {
                0 -> {
                    filePickerShown = true
                }

                1 -> {
                    filePickerShown = false
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
                val serverAddress by viewModel.serverAddress.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    if (serverAddress == null) {
                        val firstIp = viewModel.localIps.firstOrNull()

                        if (firstIp != null) {
                            viewModel.setServerAddress(firstIp)
                        } else {
                            errorText = getString(Res.string.network_error)
                        }
                    }
                }

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

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                if (serverAddress != null) {
                    ButtonWithSpinner(
                        prefixText = "IP",
                        itemToTexts = remember {
                            viewModel.localIps.associateWith { it }
                        },
                        selected = serverAddress!!,
                        onItemSelected = {
                            viewModel.setServerAddress(it)
                        },
                    )
                }

                Text(
                    text = stringResource(Res.string.pref_imexport_network_notice),
                    textAlign = TextAlign.Center
                )
            }
        }

        AnimatedVisibility(visible = availableImportTypes != null) {
            var userImportTypes by remember(availableImportTypes) {
                mutableStateOf(availableImportTypes ?: emptySet())
            }
            var selectedWriteMode by rememberSaveable(saver = enumSaver()) {
                mutableStateOf(
                    ImExporter.WriteMode.keep_existing
                )
            }
            val editsReplaceAllText = stringResource(Res.string.import_lists_replace_all)
            val editsReplaceExistingText = stringResource(Res.string.import_lists_replace_existing)
            val editsKeepText = stringResource(Res.string.import_lists_keep)

            val writeModesMap = remember {
                mapOf(
                    ImExporter.WriteMode.replace_all to editsReplaceAllText,
                    ImExporter.WriteMode.replace_existing to editsReplaceExistingText,
                    ImExporter.WriteMode.keep_existing to editsKeepText
                )
            }

            val settingsText = stringResource(Res.string.import_settings)
            val simpleEditsText = stringResource(Res.string.simple_edits)
            val regexRulesText = stringResource(Res.string.regex_rules)
            val blockedMetadataText = stringResource(Res.string.pref_blocked_metadata)
            val artistsWithDelimitersText = stringResource(Res.string.artist_splitting_exceptions)
            val scrobbleSourcesText = stringResource(Res.string.scrobble_sources)

            val importTypesMap = remember {
                mapOf(
                    ImExporter.ImportTypes.settings to settingsText,
                    ImExporter.ImportTypes.simple_edits to simpleEditsText,
                    ImExporter.ImportTypes.regex_rules to regexRulesText,
                    ImExporter.ImportTypes.blocked_metadata to blockedMetadataText,
                    ImExporter.ImportTypes.artists_with_delimiters to artistsWithDelimitersText,
                    ImExporter.ImportTypes.scrobble_sources to scrobbleSourcesText,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.import_options),
                    style = MaterialTheme.typography.headlineSmall,
                )

                availableImportTypes?.forEach {
                    LabeledCheckbox(
                        checked = it in userImportTypes,
                        onCheckedChange = { checked ->
                            if (checked)
                                userImportTypes += it
                            else
                                userImportTypes -= it
                        },
                        text = importTypesMap[it] ?: it.name,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (
                    ImExporter.ImportTypes.simple_edits in userImportTypes ||
                    ImExporter.ImportTypes.regex_rules in userImportTypes ||
                    ImExporter.ImportTypes.blocked_metadata in userImportTypes ||
                    ImExporter.ImportTypes.artists_with_delimiters in userImportTypes
                ) {
                    ButtonWithSpinner(
                        prefixText = null,
                        itemToTexts = writeModesMap,
                        selected = selectedWriteMode,
                        onItemSelected = { selectedWriteMode = it },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.import(userImportTypes, selectedWriteMode)
                    },
                ) {
                    Text(text = stringResource(Res.string.pref_import))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.serverResult.filterNotNull().collectLatest { result ->
            result.onSuccess {
                codeText = it.chunkedSequence(4).joinToString(" ")
                errorText = null
            }.onFailure {
                errorText = it.redactedMessage
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.importResult.collectLatest {
            it.onSuccess {
                errorText = null
                Stuff.globalSnackbarFlow.emit(
                    PanoSnackbarVisuals(importSuccessText)
                )
                onBack()
            }.onFailure {
                errorText = it.redactedMessage
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