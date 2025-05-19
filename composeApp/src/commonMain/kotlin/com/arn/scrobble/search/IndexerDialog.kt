package com.arn.scrobble.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.full_index
import pano_scrobbler.composeapp.generated.resources.full_index_desc
import pano_scrobbler.composeapp.generated.resources.take_long_time

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IndexerDialog(
    modifier: Modifier = Modifier,
    viewModel: IndexerVM = viewModel { IndexerVM() },
) {
    var progress by rememberSaveable { mutableFloatStateOf(1f) }
    var indexingMessage by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showOptions by rememberSaveable { mutableStateOf(true) }
    val longTimeMessage = stringResource(Res.string.take_long_time)
    val doneMessage = stringResource(Res.string.done)

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
                    text = { Text(stringResource(Res.string.full_index)) },
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
            CircularWavyProgressIndicator(
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
                title = { Text(stringResource(Res.string.full_index)) },
                text = { Text("⚠ " + stringResource(Res.string.full_index_desc)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false

                    }) {
                        Text(stringResource(Res.string.full_index))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(Res.string.cancel))
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.indexingProgress.collectLatest { indexingProgress ->

            indexingMessage =
                if (indexingProgress.progress == 1f || indexingProgress.state.isFinished) {
                    doneMessage
                } else if (indexingProgress.isError) {
                    "❗${indexingProgress.message}"
                } else {
                    longTimeMessage
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