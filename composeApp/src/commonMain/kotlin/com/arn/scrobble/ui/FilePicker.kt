package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import com.arn.scrobble.utils.PlatformFile

sealed interface FilePickerMode {
    sealed interface HasInitialPath {
        val initialPath: String?
    }

    data class Open(
        override val initialPath: String? = null,
    ) : FilePickerMode, HasInitialPath

    data class Save(
        val title: String,
        override val initialPath: String? = null,
    ) : FilePickerMode, HasInitialPath
}

enum class FileType {
    LOG, JSON, JSONL, CSV, PHOTO
}

fun getExtensionsForFilePicker(type: FileType) =
    when (type) {
        FileType.LOG -> setOf(".log")
        FileType.JSON -> setOf(".json")
        FileType.JSONL -> setOf(".jsonl")
        FileType.CSV -> setOf(".csv")
        FileType.PHOTO -> setOf(".jpg", ".jpeg", ".png", ".gif")
    }

@Composable
expect fun FilePicker(
    show: Boolean,
    mode: FilePickerMode,
    type: FileType,
    onDismiss: () -> Unit,
    onFilePicked: (PlatformFile) -> Unit,
)