package com.arn.scrobble.edits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditFields
import com.arn.scrobble.icons.AlbumArtist
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.DraggableItem
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.PanoSnackbarVisuals
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.dragContainer
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.rememberDragDropState
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.edit_drag_handle
import pano_scrobbler.composeapp.generated.resources.edit_max_patterns
import pano_scrobbler.composeapp.generated.resources.edit_no_presets_available
import pano_scrobbler.composeapp.generated.resources.edit_preset_name
import pano_scrobbler.composeapp.generated.resources.edit_presets
import pano_scrobbler.composeapp.generated.resources.edit_presets_available
import pano_scrobbler.composeapp.generated.resources.edit_regex_test
import pano_scrobbler.composeapp.generated.resources.num_regex_edits
import pano_scrobbler.composeapp.generated.resources.reorder_dpad

@Composable
fun RegexEditsScreen(
    viewModel: RegexEditsVM = viewModel { RegexEditsVM() },
    onNavigateToTest: () -> Unit,
    onNavigateToEdit: (RegexEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val regexEdits by viewModel.regexes.collectAsStateWithLifecycle()
    var regexEditsReordered by remember { mutableStateOf<List<RegexEdit>?>(null) }
    val limitReached by viewModel.limitReached.collectAsStateWithLifecycle()

    val presetsAvailable by viewModel.presetsAvailable.collectAsStateWithLifecycle()
    var showPresetsDialog by remember { mutableStateOf(false) }
    val limitReachedMessage = if (limitReached) {
        stringResource(Res.string.edit_max_patterns, Stuff.MAX_PATTERNS)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(panoContentPadding(bottom = false)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AssistChip(
                onClick = onNavigateToTest,
                label = { Text(text = stringResource(Res.string.edit_regex_test)) })

            AssistChip(
                onClick = {
                    showPresetsDialog = true
                },
                label = { Text(text = stringResource(Res.string.edit_presets)) })
        }

        ErrorText(limitReachedMessage, modifier = Modifier.padding(vertical = 16.dp))

        EmptyText(
            visible = regexEditsReordered?.isEmpty() == true,
            text = pluralStringResource(Res.plurals.num_regex_edits, 0, 0)
        )


        RegexEditsList(
            regexEdits = regexEditsReordered,
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
    regexEdits: List<RegexEdit>?,
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

    PanoLazyColumn(
        state = listState,
        modifier = Modifier.dragContainer(dragDropState),
    ) {
        if (regexEdits == null) {
            val listForShimmer = List(10) {
                RegexEdit(_id = it)
            }
            items(listForShimmer, key = { it._id }) {
                RegexEditItem(
                    regexEdit = it,
                    forShimmer = true,
                    onKeypressMoveItem = { _, _ -> },
                    onItemClick = { },
                    modifier = Modifier.shimmerWindowBounds().animateItem(),
                )
            }
        } else {
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
}

@Composable
private fun RegexEditItem(
    regexEdit: RegexEdit,
    onKeypressMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemClick: (RegexEdit) -> Unit,
    modifier: Modifier = Modifier,
    forShimmer: Boolean = false,
) {
    val regexEdit = remember(regexEdit) { RegexPresets.getPossiblePreset(regexEdit) }
    val scope = rememberCoroutineScope()
    val modifierIcons = getModifierIcons(regexEdit)

    val name = if (regexEdit.preset == null) {
        regexEdit.name ?: regexEdit.pattern
    } else {
        stringResource(
            Res.string.edit_preset_name,
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
            contentDescription = stringResource(Res.string.edit_drag_handle),
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.medium)
                .then(
                    if (PlatformStuff.isTv) {
                        Modifier
                            .clickable(enabled = !forShimmer) {
                                scope.launch {
                                    Stuff.globalSnackbarFlow.emit(
                                        PanoSnackbarVisuals(
                                            getString(Res.string.reorder_dpad),
                                            isError = false
                                        )
                                    )
                                }
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
            RegexEditFields.TRACK -> m += Icons.Outlined.MusicNote
            RegexEditFields.ALBUM -> m += Icons.Outlined.Album
            RegexEditFields.ALBUM_ARTIST -> m += PanoIcons.AlbumArtist
            RegexEditFields.ARTIST -> m += Icons.Outlined.Mic
        }
    }

    if (regexEdit.extractionPatterns != null) m += Icons.Outlined.SwipeLeftAlt
    if (!regexEdit.packages.isNullOrEmpty()) m += Icons.Outlined.Apps
    if (regexEdit.caseSensitive) m += PanoIcons.AlbumArtist
    if (regexEdit.replaceAll) m += Icons.Outlined.Public
    if (regexEdit.continueMatching) m += Icons.Outlined.ChevronRight

    return m
}

@Composable
fun PresetsDialog(
    presetsAvailable: List<String>,
    onDismissRequest: () -> Unit,
    onPresetSelected: (String) -> Unit,
) {
    if (presetsAvailable.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = stringResource(Res.string.edit_presets_available))
            },
            text = {
                Column {
                    presetsAvailable.forEach { preset ->
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
                    Text(text = stringResource(Res.string.cancel))
                }
            }
        )
    } else {
        AlertDialogOk(
            onConfirmation = onDismissRequest,
            text = stringResource(Res.string.edit_no_presets_available)
        )

    }
}