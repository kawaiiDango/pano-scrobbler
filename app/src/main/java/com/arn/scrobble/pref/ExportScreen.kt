package com.arn.scrobble.pref

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.R
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.OutlinedToggleButtons
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun ExportScreen(
    viewModel: ExportVM = viewModel(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var codeText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var toggleButtonSelectedIndex by remember { mutableIntStateOf(-1) }
    val networkMode =
        remember(toggleButtonSelectedIndex) { toggleButtonSelectedIndex == 1 }
    val exportFileName = stringResource(R.string.export_file_name, Stuff.getFileNameDateSuffix())
    val exportFileNamePrivate =
        stringResource(R.string.export_file_name, "private_" + Stuff.getFileNameDateSuffix())

    val exportRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Stuff.MIME_TYPE_JSON)) { uri ->
            uri?.let {
                viewModel.exportToFile(
                    context.contentResolver.openOutputStream(it),
                    privateData = false
                )
            }
        }

    val exportPrivateDataRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Stuff.MIME_TYPE_JSON)) { uri ->
            uri?.let {
                viewModel.exportToFile(
                    context.contentResolver.openOutputStream(it),
                    privateData = true
                )
            }
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ImExportModeSelector(toggleButtonSelectedIndex) { index ->
            if (index == 0) {
                exportRequest.launch(exportFileName)
                exportPrivateDataRequest.launch(exportFileNamePrivate)
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
                    label = { Text(text = stringResource(id = R.string.pref_imexport_code)) },
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
                    trailingIcon = if (!Stuff.isTv) {
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
                    text = stringResource(id = R.string.pref_imexport_network_notice),
                    textAlign = TextAlign.Center
                )
            }
        }

    }

    LaunchedEffect(Unit) {
        viewModel.result.filterNotNull().collectLatest {
            it.onSuccess {
                errorText = null
                onBack()
            }.onFailure {
                errorText = it.message
            }
        }
    }
}

@Composable
fun ImExportModeSelector(
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    if (Stuff.isTv) {
        LaunchedEffect(Unit) {
            onSelected(1)
        }
        Text(
            text = stringResource(id = R.string.pref_imexport_network),
            style = MaterialTheme.typography.titleLarge,
        )
    } else {
        OutlinedToggleButtons(
            items = listOf(
                stringResource(id = R.string.scrobble_to_file),
                stringResource(id = R.string.pref_imexport_network),
            ),
            onSelected = onSelected,
            selectedIndex = selectedIndex,
        )
    }
}