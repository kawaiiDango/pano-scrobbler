package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.AwtWindow
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PlatformFile
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.create
import pano_scrobbler.composeapp.generated.resources.fix_it_action
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import java.nio.file.Path


@Composable
actual fun FilePicker(
    show: Boolean,
    mode: FilePickerMode,
    type: FileType,
    onDismiss: () -> Unit,
    onFilePicked: (PlatformFile) -> Unit,
) {

    val fileMode by remember(mode) {
        mutableIntStateOf(
            when (mode) {
                is FilePickerMode.Open -> FileDialog.LOAD
                is FilePickerMode.Save -> FileDialog.SAVE
            }
        )
    }

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
        if (show && DesktopStuff.os == DesktopStuff.Os.Linux) {
            val requestId = (0..100000).random()
            PanoNativeComponents.xdgFileChooser(
                requestId = requestId,
                save = mode is FilePickerMode.Save,
                title = title,
                fileName = if (mode is FilePickerMode.Save) mode.title + extensions.first() else "",
                filters = extensions.map { "*$it" }.toTypedArray()
            )

            val (receivedRequestId, uri) = PanoNativeComponents.onFilePickedFlow.first()

            if (uri.isNotEmpty() && receivedRequestId == requestId) {
                onFilePicked(PlatformFile(uri))
            }
            onDismiss()
        }
    }

    if (show && DesktopStuff.os != DesktopStuff.Os.Linux) {
        // freezes or gives a segmentation fault on Linux
        AwtWindow(
            create = {
                object : FileDialog(null as Frame?, title, fileMode) {
                    init {
                        if (mode is FilePickerMode.Save)
                            file = mode.title + extensions.first()
                    }

                    override fun setVisible(value: Boolean) {
                        super.setVisible(value)
                        if (!value) {
                            onDismiss()
                        }
                        if (value &&
                            (file != null && directory != null) &&
                            extensions.any { file.endsWith(it) }
                        ) {
                            val uri = Path.of(directory, file).toUri().toString()
                            onFilePicked(PlatformFile(uri))
                        }
                    }

                    override fun getFilenameFilter(): FilenameFilter {
                        return FilenameFilter { _, filename ->
                            extensions.any { filename.endsWith(it) }
                        }
                    }
                }
            },
            dispose = FileDialog::dispose
        )
    } else {
        // open the custom file picker in a compose dialog
        // this is a workaround for the native file picker not working on Arch based distros
        /*
        BasicAlertDialog(
            onDismissRequest = { onDismiss() },
            content = {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    FilePickerScreen(
                        title = title,
                        mode = mode,
                        allowedExtensions = extensions,
                        onFilePicked = { file ->
                            onFilePicked(file)
                            onDismiss()
                        }
                    )
                }
            }
        )

         */

    }
}