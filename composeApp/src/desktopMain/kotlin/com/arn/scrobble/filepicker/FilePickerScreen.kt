package com.arn.scrobble.filepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.ArrowUpward
import com.arn.scrobble.icons.Check
import com.arn.scrobble.icons.Description
import com.arn.scrobble.icons.Folder
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Menu
import com.arn.scrobble.icons.Visibility
import com.arn.scrobble.icons.VisibilityOff
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.utils.PlatformFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.back
import pano_scrobbler.composeapp.generated.resources.cannot_access
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.file_exists_confirm_overwrite
import pano_scrobbler.composeapp.generated.resources.show_all
import java.io.File

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilePickerScreen(
    title: String,
    mode: FilePickerMode,
    allowedExtensions: Set<String>,
    onFilePicked: (PlatformFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentDir by remember {
        mutableStateOf(
            File(
                (mode as FilePickerMode.HasInitialPath).initialPath
                    ?: System.getProperty("user.home")!!
            )
        )
    }
    var files by remember(currentDir) { mutableStateOf<List<File>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canOverwrite by remember { mutableStateOf(false) }
    var showHiddenFiles by remember { mutableStateOf(false) }
    val roots = remember { File.listRoots() }
    val scope = rememberCoroutineScope()
    var fileName by remember {
        mutableStateOf(
            if (mode is FilePickerMode.Save)
                mode.title + allowedExtensions.first()
            else
                null
        )
    }

    fun errorCannotAccess(file: File) {
        scope.launch {
            errorMessage = getString(Res.string.cannot_access, file.name)
        }
    }

    LaunchedEffect(canOverwrite) {
        if (canOverwrite) {
            errorMessage = getString(Res.string.file_exists_confirm_overwrite)
            delay(2000)
            canOverwrite = false
            errorMessage = null
        }
    }

    LaunchedEffect(currentDir, showHiddenFiles) {
        try {
            files = currentDir.listFiles {
                var condition = it.isDirectory

                if (mode is FilePickerMode.Open)
                    condition = condition || it.isFile && allowedExtensions.any { ext ->
                        it.name.lowercase().endsWith(ext)
                    }

                if (!showHiddenFiles)
                    condition = condition && !it.isHidden

                condition
            }.sortedWith { f1, f2 ->
                when {
                    f1.isDirectory && !f2.isDirectory -> -1
                    !f1.isDirectory && f2.isDirectory -> 1
                    else -> f1.name.compareTo(f2.name, ignoreCase = true)
                }
            }
            errorMessage = null
        } catch (e: Exception) {
            errorCannotAccess(currentDir)
            errorMessage = getString(Res.string.cannot_access, currentDir.name)
            files = emptyList()
        }
    }


    PanoLazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(0.8f)
    ) {
        // Sticky header with current directory, back button and done button.
        stickyHeader("header_title") {
            Surface(
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )

                    if (currentDir.parentFile == null && roots.size > 1) {
                        ButtonGroup(
                            overflowIndicator = {
                                Icon(
                                    imageVector = Icons.Menu,
                                    contentDescription = stringResource(Res.string.show_all),
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            roots.forEach { root ->
                                clickableItem(
                                    onClick = {
                                        currentDir = root
                                    },
                                    weight = 1f,
                                    label = root.path
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (currentDir.parentFile != null) {
                            IconButton(
                                onClick = {
                                    // Navigate to parent directory.
                                    currentDir.parentFile?.let { currentDir = it }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.ArrowUpward,
                                    contentDescription = stringResource(Res.string.back),
                                )
                            }
                        }
                        Text(
                            text = currentDir.name.ifEmpty { currentDir.path },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp).weight(1f)
                        )

                        IconButtonWithTooltip(
                            onClick = {
                                showHiddenFiles = !showHiddenFiles
                            },
                            icon = if (showHiddenFiles)
                                Icons.Visibility
                            else
                                Icons.VisibilityOff,
                            contentDescription = stringResource(Res.string.show_all),
                        )
                    }

                    if (mode is FilePickerMode.Save) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = fileName ?: "",
                                onValueChange = { newFileName ->
                                    fileName = newFileName
                                    errorMessage = null
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )

                            FilledTonalIconButton(
                                onClick = {
                                    if (fileName.isNullOrBlank()) {
                                        errorMessage = "File name cannot be empty."
                                        return@FilledTonalIconButton
                                    }

                                    val newFile = File(currentDir, fileName)

                                    if (newFile.exists() && !canOverwrite) {
                                        canOverwrite = true
                                    } else {
                                        onFilePicked(
                                            PlatformFile(
                                                newFile.toPath().toUri().toString()
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Check,
                                    contentDescription = stringResource(Res.string.done)
                                )
                            }
                        }
                    }

                    // display error message if any
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // List directories and files.
        items(
            files,
            key = { it.absolutePath }
        ) { file ->
            FileItem(
                file = file,
                onClick = {
                    if (file.isDirectory) {
                        // Navigate into directory.
                        try {
                            if (file.canRead()) {
                                currentDir = file
                            } else {
                                errorCannotAccess(file)
                            }
                        } catch (e: Exception) {
                            errorCannotAccess(file)
                        }
                    } else {
                        // Only allow file selection.
                        val fileNameLower = file.name.lowercase()
                        if (!allowedExtensions.any { fileNameLower.endsWith(it) }) {
                            errorMessage = "File type not allowed."
                        } else if (file.canRead()) {
                            onFilePicked(PlatformFile(file.toPath().toUri().toString()))
                        } else {
                            errorCannotAccess(file)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FileItem(
    file: File,
    onClick: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick(file) }
    ) {
        Icon(
            imageVector = if (file.isDirectory) {
                Icons.Folder
            } else {
                Icons.Description
            },
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )

        Text(
            text = file.name,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            style = if (file.isDirectory) {
                MaterialTheme.typography.bodyLargeEmphasized
            } else {
                MaterialTheme.typography.bodyLarge
            },
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        )

    }
}