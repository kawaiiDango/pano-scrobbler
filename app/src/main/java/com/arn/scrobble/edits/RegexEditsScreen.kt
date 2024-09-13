package com.arn.scrobble.edits

import android.os.Bundle
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.compose.LocalFragment
import androidx.navigation.fragment.findNavController
import com.arn.scrobble.NLService
import com.arn.scrobble.R
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.main.FabData
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.ui.AlertDialogOk
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.ExtraBottomSpace
import com.arn.scrobble.ui.ScreenParent
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.putSingle
import com.valentinilk.shimmer.shimmer
import kotlin.math.roundToInt

@Composable
private fun RegexEditsContent(
    viewModel: RegexEditsVM = viewModel(),
    onNavigateToTest: () -> Unit,
    onNavigateToEdit: (RegexEdit) -> Unit,
    modifier: Modifier = Modifier
) {
    val regexEdits by viewModel.regexes.collectAsStateWithLifecycle()
    val limitReached by viewModel.limitReached.collectAsStateWithLifecycle()

    val presetsAvailable by viewModel.presetsAvailable.collectAsStateWithLifecycle()
    var showPresetsDialog by remember { mutableStateOf(false) }
    val limitReachedMessage = if (limitReached) {
        stringResource(R.string.edit_max_patterns, Stuff.MAX_PATTERNS)
    } else {
        null
    }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AssistChip(onClick = onNavigateToTest,
                label = { Text(text = stringResource(R.string.edit_regex_test)) })

            AssistChip(onClick = {
                showPresetsDialog = true
            },
                label = { Text(text = stringResource(R.string.edit_presets)) })
        }
        Spacer(modifier = Modifier.height(16.dp))

        ErrorText(limitReachedMessage, modifier = Modifier.padding(bottom = 16.dp))

        AnimatedVisibility(
            visible = regexEdits == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RegexEditsListShimmer()
        }

        EmptyText(
            visible = regexEdits?.isEmpty() == true,
            text = pluralStringResource(R.plurals.num_regex_edits, 0, 0)
        )

        AnimatedVisibility(
            visible = regexEdits?.isNotEmpty() == true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RegexEditsList(
                regexEdits = regexEdits ?: emptyList(),
                onItemClick = {
                    onNavigateToEdit(RegexPresets.getPossiblePreset(it))
                },
                onMoveItem = { fromIndex, toIndex ->
                    val regexEdits = regexEdits ?: return@RegexEditsList

                    if (fromIndex == toIndex) return@RegexEditsList
                    if (fromIndex < 0 || toIndex < 0) return@RegexEditsList
                    if (fromIndex >= regexEdits.size || toIndex >= regexEdits.size) return@RegexEditsList

                    val updatedList = regexEdits.toMutableList().apply {
                        val movedItem = removeAt(fromIndex)
                        add(toIndex, movedItem)
                    }.mapIndexed { index, regex ->
                        regex.copy(order = index)
                    }

                    viewModel.upsertAll(updatedList)
                }
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
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(regexEdits, key = { it._id }) { item ->
            var offsetY by remember { mutableFloatStateOf(0f) }
            val dragState = rememberDraggableState { delta ->
                offsetY = delta
            }

            RegexEditItem(
                regexEdit = item,
                dragState = dragState,
                onItemClick = { onItemClick(it) },
                onMoveItem = { f, t ->
                    onMoveItem(f, t)
                    // todo auto scroll if out of bounds
                },
                modifier = Modifier
                    .animateItem()
                    .offset { IntOffset(0, offsetY.roundToInt()) }
            )
        }

        item("extra_space") {
            ExtraBottomSpace()
        }
    }
}

@Composable
private fun RegexEditsListShimmer(
) {
    val listForShimmer = List(10) {
        RegexEdit(_id = it)
    }

    LazyColumn(
        modifier = Modifier.shimmer(),
    ) {
        items(listForShimmer, key = { it._id }) {
            RegexEditItem(
                regexEdit = it,
                forShimmer = true,
                onItemClick = { },
                dragState = null,
                onMoveItem = { _, _ -> }
            )
        }
    }
}

@Composable
private fun RegexEditItem(
    regexEdit: RegexEdit,
    dragState: DraggableState?,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemClick: (RegexEdit) -> Unit,
    forShimmer: Boolean = false,
    modifier: Modifier = Modifier
) {
    val regexEdit = remember(regexEdit) { RegexPresets.getPossiblePreset(regexEdit) }
    var isDragging by remember { mutableStateOf(false) }
    val itemHeight = remember { mutableIntStateOf(0) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

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
            .padding(8.dp)
            .alpha(if (isDragging) 0.5f else 1f)
            .onGloballyPositioned { coordinates ->
                itemHeight.value = coordinates.size.height
            }
    ) {
        Icon(
            imageVector = Icons.Outlined.DragHandle,
            contentDescription = stringResource(R.string.edit_drag_handle),
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !forShimmer) {
                    // todo toast that this is for reordering
                }
                .padding(end = 12.dp)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionUp -> {
                                onMoveItem(regexEdit.order, regexEdit.order - 1)
                                true
                            }

                            Key.DirectionDown -> {
                                onMoveItem(regexEdit.order, regexEdit.order + 1)
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
                .then(if (forShimmer || dragState == null) Modifier
                else
                    Modifier
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStarted = {
                                isDragging = true
                            },
                            onDragStopped = {
                                isDragging = false
                                dragOffset = 0f
                                val newPosition =
                                    (regexEdit.order + (it / itemHeight.value).toInt())
                                if (newPosition != regexEdit.order) {
                                    onMoveItem(regexEdit.order, newPosition)
                                }
                            }
                        )
                )
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

@Keep
@Composable
fun RegexEditsScreen() {
    val fragment = LocalFragment.current
    LaunchedEffect(Unit) {
        val fabData = FabData(
            fragment.viewLifecycleOwner,
            R.string.add,
            R.drawable.vd_add_borderless,
            {
                // todo do not navigate if limit reached
                fragment.findNavController().navigate(R.id.regexEditsAddFragment)
            }
        )

        val mainNotifierViewModel by fragment.activityViewModels<MainNotifierViewModel>()

        mainNotifierViewModel.setFabData(fabData)
    }

    ScreenParent {
        RegexEditsContent(
            onNavigateToTest = {
                fragment.findNavController().navigate(R.id.regexEditsTestFragment)
            },
            onNavigateToEdit = {
                val args = Bundle().apply {
                    putSingle(it)
                }
                fragment.findNavController().navigate(R.id.regexEditsAddFragment, args)
            },
            modifier = it
        )
    }
}
