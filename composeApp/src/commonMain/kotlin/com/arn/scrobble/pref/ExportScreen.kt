package com.arn.scrobble.pref

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.OutlinedToggleButtons
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.export_file_name
import pano_scrobbler.composeapp.generated.resources.pref_imexport_code
import pano_scrobbler.composeapp.generated.resources.pref_imexport_network
import pano_scrobbler.composeapp.generated.resources.pref_imexport_network_notice
import pano_scrobbler.composeapp.generated.resources.scrobble_to_file

@Composable
fun ExportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportVM = viewModel { ExportVM() },
) {
    var codeText by rememberSaveable { mutableStateOf("") }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var toggleButtonSelectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    val networkMode =
        remember(toggleButtonSelectedIndex) { toggleButtonSelectedIndex == 1 }
    val exportFileName = stringResource(Res.string.export_file_name, Stuff.getFileNameDateSuffix())
    val exportFileNamePrivate =
        stringResource(Res.string.export_file_name, "private_" + Stuff.getFileNameDateSuffix())
    // todo implement private data export
    var filePickerShown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ImExportModeSelector(toggleButtonSelectedIndex) { index ->
            if (index == 0) {
                filePickerShown = true
            }
            toggleButtonSelectedIndex = index
            errorText = null
        }

        Spacer(modifier = Modifier.height(16.dp))

        ErrorText(
            errorText = errorText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AnimatedVisibility(visible = networkMode) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it.uppercase() },
                    label = { Text(text = stringResource(Res.string.pref_imexport_code)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Ascii,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.exportToServer(codeText)
                        }
                    ),
                    trailingIcon = if (!PlatformStuff.isTv) {
                        {
                            IconButton(onClick = {
                                viewModel.exportToServer(codeText)
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = null
                                )
                            }
                        }
                    } else
                        null,
                    modifier = Modifier.defaultMinSize(minWidth = 200.dp)
                )

                Text(
                    text = stringResource(Res.string.pref_imexport_network_notice),
                    textAlign = TextAlign.Center
                )
            }
        }

    }

    FilePicker(
        show = filePickerShown,
        mode = FilePickerMode.Save(exportFileName),
        type = FileType.JSON,
        onDismiss = { filePickerShown = false },
    ) {
        viewModel.exportToFile(it, privateData = false)
    }

    LaunchedEffect(Unit) {
        viewModel.result.filterNotNull().collectLatest {
            it.onSuccess {
                errorText = null
                onBack()
            }.onFailure {
                errorText = it.redactedMessage
            }
        }
    }
}

@Composable
fun ImExportModeSelector(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    if (PlatformStuff.isTv) {
        LaunchedEffect(Unit) {
            onSelected(1)
        }
        Text(
            text = stringResource(Res.string.pref_imexport_network),
            style = MaterialTheme.typography.titleLarge,
        )
    } else if (PlatformStuff.isDesktop) {
        LaunchedEffect(Unit) {
            onSelected(0)
        }
        Text(
            text = stringResource(Res.string.scrobble_to_file),
            style = MaterialTheme.typography.titleLarge,
        )
    } else {
        OutlinedToggleButtons(
            items = listOf(
                stringResource(Res.string.scrobble_to_file),
                stringResource(Res.string.pref_imexport_network),
            ),
            onSelected = onSelected,
            selectedIndex = selectedIndex,
        )
    }
}