package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.utils.PlatformFile
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.create
import pano_scrobbler.composeapp.generated.resources.fix_it_action


@Composable
actual fun FilePicker(
    show: Boolean,
    mode: FilePickerMode,
    type: FileType,
    onDismiss: () -> Unit,
    onFilePicked: (PlatformFile) -> Unit,
) {
    val extensions by remember(type) {
        mutableStateOf(
            getExtensionsForFilePicker(type)
        )
    }

    val title = when (mode) {
        is FilePickerMode.Open -> stringResource(Res.string.fix_it_action)
        is FilePickerMode.Save -> stringResource(Res.string.create)
    }

    LaunchedEffect(show) {
        if (show) {
            val requestId = (0..100000).random()
            PanoNativeComponents.fileChooser(
                requestId = requestId,
                save = mode is FilePickerMode.Save,
                title = title,
                fileName = if (mode is FilePickerMode.Save) mode.title + extensions.first() else "",
                extensions = extensions.toTypedArray()
            )

            val (receivedRequestId, uri) = PanoNativeComponents.onFilePickedFlow.first()

            if (receivedRequestId == requestId) {
                if (uri.isNotEmpty())
                    onFilePicked(PlatformFile(uri))
                onDismiss()
            }
        }
    }
}