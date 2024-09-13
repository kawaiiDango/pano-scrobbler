package com.arn.scrobble.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.R
import com.arn.scrobble.api.file.FileScrobblable
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.OutlinedToggleButtons
import com.arn.scrobble.ui.RadioButtonGroup
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.launch

private enum class FileOpenType {
    CREATE,
    OPEN
}

@Composable
private fun FileLoginContent(
    modifier: Modifier = Modifier,
) {
    var selectedFileFormat by remember { mutableStateOf<FileScrobblable.FileFormat?>(null) }
    val createText = stringResource(id = R.string.create)
    val openText = stringResource(id = R.string.open_existing)
    var errorText by remember { mutableStateOf<String?>(null) }
    val fileOpenTypesToTexts =
        remember { mapOf(FileOpenType.CREATE to createText, FileOpenType.OPEN to openText) }
    val scope = rememberCoroutineScope()
    val fragment = LocalFragment.current

    val fileScrobblableCreate =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
            if (uri != null && selectedFileFormat != null) {
                scope.launch {
                    onFilePicked(
                        uri,
                        selectedFileFormat!!,
                        onDone = {
                            fragment.findNavController().popBackStack()
                        },
                        onError = { errorText = it })
                }
            }
        }

    val fileScrobblableOpen =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null && selectedFileFormat != null) {
                scope.launch {
                    onFilePicked(
                        uri,
                        selectedFileFormat!!,
                        onDone = {
                            fragment.findNavController().popBackStack()
                        },
                        onError = { errorText = it }
                    )
                }
            }
        }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        OutlinedToggleButtons(
            items = FileScrobblable.FileFormat.entries.map { "." + it.name },
            selectedIndex = selectedFileFormat?.ordinal ?: -1,
            onSelected = {
                selectedFileFormat = FileScrobblable.FileFormat.entries[it]
                errorText = null
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
                                fileScrobblableCreate.launch("scrobbles_log_" + Stuff.getFileNameDateSuffix() + ".csv")
                            } else if (selectedFileFormat == FileScrobblable.FileFormat.jsonl) {
                                fileScrobblableCreate.launch("scrobbles_log_" + Stuff.getFileNameDateSuffix() + ".jsonl")
                            }
                        }

                        FileOpenType.OPEN -> {
                            fileScrobblableOpen.launch(arrayOf("*/*"))
                        }
                    }

                    errorText = null
                }
            )
        }

        ErrorText(errorText = errorText)
    }
}

private suspend fun onFilePicked(
    uri: Uri,
    fileFormat: FileScrobblable.FileFormat,
    onDone: () -> Unit,
    onError: (String) -> Unit
) {
    FileScrobblable.authAndGetSession(uri, fileFormat)
        .onSuccess { onDone() }
        .onFailure {
            onError(it.message.toString())
        }
}

@Keep
@Composable
fun FileLoginScreen() {
    ScreenParent { FileLoginContent(it) }
}