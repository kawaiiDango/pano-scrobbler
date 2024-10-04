package com.arn.scrobble.edits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SwipeLeftAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.NLService
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.DraggableItem
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.dragContainer
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.rememberDragDropState
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils.toast
import com.valentinilk.shimmer.shimmer

@Composable
fun RegexEditsScreen(
    viewModel: RegexEditsVM = viewModel(),
    onNavigateToTest: () -> Unit,
    onNavigateToEdit: (RegexEdit) -> Unit,
    modifier: Modifier = Modifier
) {
    val regexEdits by viewModel.regexes.collectAsStateWithLifecycle()
    var regexEditsReordered by remember { mutableStateOf<List<RegexEdit>?>(null) }
    val limitReached by viewModel.limitReached.collectAsStateWithLifecycle()

    val presetsAvailable by viewModel.presetsAvailable.collectAsStateWithLifecycle()
    var showPresetsDialog by remember { mutableStateOf(false) }
    val limitReachedMessage = if (limitReached) {
        stringResource(R.string.edit_max_patterns, Stuff.MAX_PATTERNS)
    } else {
        null
    }

    LaunchedEffect(regexEdits) {
        regexEditsReordered = regexEdits
    }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(panoContentPadding(bottom = false)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AssistChip(onClick = onNavigateToTest,
                label = { Text(text = stringResource(R.string.edit_regex_test)) })

            AssistChip(onClick = {
                showPresetsDialog = true
            },
                label = { Text(text = stringResource(R.string.edit_presets)) })
        }

        ErrorText(limitReachedMessage, modifier = Modifier.padding(vertical = 16.dp))

        AnimatedVisibility(
            visible = regexEdits == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RegexEditsListShimmer()
        }

        EmptyText(
            visible = regexEditsReordered?.isEmpty() == true,
            text = pluralStringResource(R.plurals.num_regex_edits, 0, 0)
        )

        AnimatedVisibility(
            visible = regexEditsReordered?.isNotEmpty() == true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RegexEditsList(
                regexEdits = regexEditsReordered ?: emptyList(),
                onItemClick = {
                    onNavigateToEdit(RegexPresets.getPossiblePreset(it))
                },
                onMoveItem = { fromIndex, toIndex ->
                    val _regexEditsReordered = regexEditsReordered ?: return@RegexEditsList

                    if (fromIndex == toIndex) return@RegexEditsList
                    if (fromIndex < 0 || toIndex < 0) return@RegexEditsList
                    if (fromIndex >= _regexEditsReordered.size || toIndex >= _regexEditsReordered.size) return@RegexEditsList

                    regexEditsReordered = _regexEditsReordered.toMutableList()
                        .apply { add(toIndex, removeAt(fromIndex)) }
                },
                onDragEnd = {
                    regexEditsReordered?.mapIndexed { index, regex ->
                        regex.copy(order = index)
                    }?.let { viewModel.upsertAll(it) }
                },
            )
        }
    }

    if (showPresetsDialog) {
        PresetsDialog(
            presetsAvailable = presetsAvailable,
            onDismissRequest = { showPresetsDialog = false },
            onPresetSelected = { preset ->
                viewModel.insertPreset(preset)
                showPresetsDialog = false
            }
        )
    }
}


@Composable
private fun RegexEditsList(
    regexEdits: List<RegexEdit>,
    onItemClick: (RegexEdit) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit,
) {
    val listState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(
            listState,
            onDragEnd = onDragEnd
        ) { fromIndex, toIndex ->
            onMoveItem(fromIndex, toIndex)
        }

    LazyColumn(
        state = listState,
        contentPadding = panoContentPadding(),
        modifier = Modifier.dragContainer(dragDropState),
    ) {
        itemsIndexed(regexEdits, key = { _, item -> item._id }) { index, item ->
            DraggableItem(dragDropState, index) { isDragging ->
                RegexEditItem(
                    regexEdit = item,
                    onItemClick = { onItemClick(it) },
                    onKeypressMoveItem = { f, t ->
                        onMoveItem(f, t)
                        onDragEnd()
                    },
                    modifier = Modifier.alpha(if (isDragging) 0.5f else 1f)
                )
            }
        }
    }
}

@Composable
private fun RegexEditsListShimmer() {
    val listForShimmer = List(10) {
        RegexEdit(_id = it)
    }

    LazyColumn(
        contentPadding = panoContentPadding(),
        modifier = Modifier.shimmer(),
    ) {
        items(listForShimmer, key = { it._id }) {
            RegexEditItem(
                regexEdit = it,
                forShimmer = true,
                onKeypressMoveItem = { _, _ -> },
                onItemClick = { },
            )
        }
    }
}

@Composable
private fun RegexEditItem(
    regexEdit: RegexEdit,
    onKeypressMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemClick: (RegexEdit) -> Unit,
    forShimmer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val regexEdit = remember(regexEdit) { RegexPresets.getPossiblePreset(regexEdit) }

    val modifierIcons = getModifierIcons(regexEdit)

    val name = if (regexEdit.preset == null) {
        regexEdit.name ?: regexEdit.pattern
    } else {
        stringResource(
            R.string.edit_preset_name,
            RegexPresets.getString(regexEdit.preset)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = horizontalOverscanPadding())
    ) {
        Icon(
            imageVector = Icons.Outlined.DragHandle,
            contentDescription = stringResource(R.string.edit_drag_handle),
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.medium)
                .then(
                    if (Stuff.isTv) {
                        Modifier
                            .clickable(enabled = !forShimmer) {
                                PlatformStuff.application.toast(R.string.reorder_dpad)
                            }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionUp -> {
                                            onKeypressMoveItem(regexEdit.order, regexEdit.order - 1)
                                            true
                                        }

                                        Key.DirectionDown -> {
                                            onKeypressMoveItem(regexEdit.order, regexEdit.order + 1)
                                            true
                                        }

                                        else -> {
                                            false
                                        }
                                    }
                                } else {
                                    false
                                }
                            }
                    } else {
                        Modifier
                    }
                )
                .padding(8.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !forShimmer) { onItemClick(regexEdit) }
                .padding(8.dp)
                .backgroundForShimmer(forShimmer)
        ) {
            Text(
                text = name ?: "",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (regexEdit.replacement.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FindReplace,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = regexEdit.replacement,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(24.dp),
            ) {
                modifierIcons.forEach { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun getModifierIcons(regexEdit: RegexEdit): List<ImageVector> {
    val m = mutableListOf<ImageVector>()

    regexEdit.fields?.forEach {
        when (it) {
            NLService.B_TRACK -> m += Icons.Outlined.MusicNote
            NLService.B_ALBUM -> m += Icons.Outlined.Album
            NLService.B_ALBUM_ARTIST -> m += ImageVector.vectorResource(R.drawable.vd_album_artist)
            NLService.B_ARTIST -> m += Icons.Outlined.Mic
        }
    }

    if (regexEdit.extractionPatterns != null) m += Icons.Outlined.SwipeLeftAlt
    if (!regexEdit.packages.isNullOrEmpty()) m += Icons.Outlined.Apps
    if (regexEdit.caseSensitive) m += ImageVector.vectorResource(R.drawable.vd_match_case)
    if (regexEdit.replaceAll) m += Icons.Outlined.Public
    if (regexEdit.continueMatching) m += Icons.Outlined.ChevronRight

    return m
}

@Composable
fun PresetsDialog(
    presetsAvailable: List<String>,
    onDismissRequest: () -> Unit,
    onPresetSelected: (String) -> Unit
) {
    if (presetsAvailable.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(R.string.edit_presets_available))
            },
            text = {
                LazyColumn {
                    items(presetsAvailable) { preset ->
                        Text(
                            text = RegexPresets.getString(preset),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onPresetSelected(preset) }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    } else {
        AlertDialogOk(
            onConfirmation = onDismissRequest,
            text = stringResource(R.string.edit_no_presets_available)
        )

    }
}