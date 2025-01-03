package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.AwtWindow
import com.arn.scrobble.utils.PlatformFile
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.create
import pano_scrobbler.composeapp.generated.resources.open_existing
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
        is FilePickerMode.Open -> stringResource(Res.string.create)
        is FilePickerMode.Save -> stringResource(Res.string.open_existing)
    }

    if (show) {
        AwtWindow(
            create = {
                object : FileDialog(null as Frame?, title, fileMode) {
                    init {
                        if (mode is FilePickerMode.Save)
                            file = mode.title + extensions.first()
                    }

                    override fun setVisible(value: Boolean) {
                        super.setVisible(value)
                        onDismiss()
                        if (value &&
                            (file != null && directory != null)
                            && extensions.any { file.endsWith(it) }
                        ) {
                            // convert to file:/// URI
                            val uri = Path.of(directory, file).toUri().toString()
                            onFilePicked(PlatformFile(uri))
                        }
                    }

                    override fun getFilenameFilter(): FilenameFilter {
                        return FilenameFilter { dir, filename ->
                            extensions.any { filename.endsWith(it) }
                        }
                    }
                }
            },
            dispose = FileDialog::dispose
        )
    }
}