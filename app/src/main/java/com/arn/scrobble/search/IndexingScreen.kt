package com.arn.scrobble.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.ui.BottomSheetDialogParent
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

@Composable
private fun IndexingContent(
    modifier: Modifier = Modifier,
    viewModel: IndexingVM = viewModel(),
) {
    var progress by remember { mutableFloatStateOf(1f) }
    var indexingMessage by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(true) }
    val longTimeMessage = stringResource(R.string.take_long_time)
    val doneMessage = stringResource(R.string.done)

    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.align(Alignment.End),
        ) {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.full_index)) },
                    onClick = {
                        showMenu = false
                        showDialog = true
                    })
            }
        }

        AnimatedVisibility(
            visible = progress > 0f && progress < 1f,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            CircularProgressIndicator(
                progress = { progress },
            )
        }

        Text(
            text = indexingMessage,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.full_index)) },
                text = { Text("⚠ " + stringResource(R.string.full_index_desc)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        viewModel.fullIndex()
                    }) {
                        Text(stringResource(R.string.full_index))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.indexingProgress.collectLatest { progressList ->
            if (progressList.isNotEmpty()) {
                val workInfo = progressList.first()

                progress = workInfo.progress.getDouble(IndexingWorker.PROGRESS_KEY, 0.0).toFloat()
                val errorMsg = workInfo.progress.getString(IndexingWorker.ERROR_KEY)
                val finished =
                    workInfo.state == WorkInfo.State.SUCCEEDED || workInfo.state == WorkInfo.State.FAILED

                indexingMessage = if (progress == 1f || finished) {
                    doneMessage
                } else if (errorMsg != null) {
                    "❗$errorMsg"
                } else {
                    longTimeMessage
                }

            }
        }
    }

    LaunchedEffect(Unit) {
        PlatformStuff.mainPrefs.data.map { it.lastFullIndexTime }.collectLatest {
            if (it != null && System.currentTimeMillis() - it < Stuff.FULL_INDEX_ALLOWED_INTERVAL) {
                showOptions = false
            }
        }
    }
}

@Composable
fun IndexingScreen(
    onDismiss: () -> Unit,
) {
    BottomSheetDialogParent(
        onDismiss = onDismiss
    ) {
        IndexingContent(it)
    }
}
