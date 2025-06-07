package com.arn.scrobble.ui

import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.AwtWindow
import com.arn.scrobble.filepicker.FilePickerScreen
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PlatformFile
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.create
import pano_scrobbler.composeapp.generated.resources.fix_it_action
import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import java.nio.file.Path


@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun FilePicker(
    show: Boolean,
    mode: FilePickerMode,
    type: FileType,
    onDismiss: () -> Unit,
    onFilePicked: (PlatformFile) -> Unit,
) {
    // freezes or gives a segmentation fault on Linux
    val useNativeFilePicker = DesktopStuff.os == DesktopStuff.Os.Windows

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

    if (show) {
        if (useNativeFilePicker) {
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
        }
    }
}
