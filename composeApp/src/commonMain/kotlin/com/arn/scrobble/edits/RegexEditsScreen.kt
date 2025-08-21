package com.arn.scrobble.edits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SwipeLeftAlt
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.icons.MatchCase
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.navigation.PanoRoute
import com.arn.scrobble.ui.DraggableItem
import com.arn.scrobble.ui.EmptyTextWithButtonOnTv
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.dragContainer
import com.arn.scrobble.ui.horizontalOverscanPadding
import com.arn.scrobble.ui.rememberDragDropState
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.edit_drag_handle
import pano_scrobbler.composeapp.generated.resources.edit_max_patterns
import pano_scrobbler.composeapp.generated.resources.edit_presets
import pano_scrobbler.composeapp.generated.resources.edit_regex_test
import pano_scrobbler.composeapp.generated.resources.move_down
import pano_scrobbler.composeapp.generated.resources.move_up
import pano_scrobbler.composeapp.generated.resources.num_regex_edits

@Composable
fun RegexEditsScreen(
    onNavigate: (PanoRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegexEditsVM = viewModel { RegexEditsVM() },
) {
    val regexEdits by viewModel.regexes.collectAsStateWithLifecycle()
    var regexEditsReordered by remember { mutableStateOf(regexEdits) }

    val presetsWithState by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue {
        val presetsEnabled = it.regexPresets

        RegexPresets.filteredPresets.map {
            it to (it.name in presetsEnabled)
        }
    }

    LaunchedEffect(regexEdits) {
        regexEditsReordered = regexEdits
    }

    RegexEditsList(
        regexEdits = regexEditsReordered,
        presetsWithState = presetsWithState,
        onItemClick = {
            onNavigate(PanoRoute.RegexEditsAdd(it))
        },
        onMoveItem = { fromIndex, toIndex ->
            if (fromIndex == toIndex) return@RegexEditsList
            if (fromIndex < 0 || toIndex < 0) return@RegexEditsList
            if (fromIndex >= regexEditsReordered.size || toIndex >= regexEditsReordered.size) return@RegexEditsList

            regexEditsReordered = regexEditsReordered.toMutableList()
                .apply { add(toIndex, removeAt(fromIndex)) }
        },
        onDragEnd = {
            regexEditsReordered.mapIndexed { index, regex ->
                regex.copy(order = index)
            }.let { viewModel.upsertAll(it) }
        },
        onPresetToggled = viewModel::updatePreset,
        onNavigateToTest = {
            onNavigate(PanoRoute.RegexEditsTest)
        },
        onImport = {
            onNavigate(PanoRoute.Import)
        },
        modifier = modifier
    )
}


@Composable
private fun RegexEditsList(
    presetsWithState: List<Pair<RegexPreset, Boolean>>,
    regexEdits: List<RegexEdit>,
    onItemClick: (RegexEdit) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit,
    onPresetToggled: (RegexPreset, Boolean) -> Unit,
    onNavigateToTest: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nonRegexItemsCount = remember {
        RegexPresets.filteredPresets.size + listOfNotNull(
            if (!PlatformStuff.isTv) 1 else null, // Test button
            1, // presets_header
            1, // custom_header
        ).sum()
    }

    val listState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(
            listState,
            onDragEnd = onDragEnd
        ) { fromIndex, toIndex ->
            onMoveItem(fromIndex - nonRegexItemsCount, toIndex - nonRegexItemsCount)
        }


    PanoLazyColumn(
        state = listState,
        modifier = modifier.dragContainer(dragDropState),
    ) {
        if (!PlatformStuff.isTv) {
            item(key = "test_button") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                ) {
                    OutlinedButton(
                        onClick = onNavigateToTest,
                    ) {
                        Text(text = stringResource(Res.string.edit_regex_test))
                    }
                }
            }
        }

        item(key = "presets_header") {
            Text(
                text = stringResource(Res.string.edit_presets),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalOverscanPadding(), vertical = 4.dp)
                    .animateItem()
            )
        }

        items(presetsWithState, key = { it.first }) { (preset, checked) ->
            PresetItem(
                preset = preset,
                isChecked = checked,
                onToggle = { isChecked ->
                    onPresetToggled(preset, isChecked)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalOverscanPadding())
                    .animateItem()
            )
        }

        item(key = "custom_header") {
            if (regexEdits.size >= Stuff.MAX_PATTERNS) {
                Text(
                    text = stringResource(Res.string.edit_max_patterns),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalOverscanPadding(), vertical = 4.dp)
                        .animateItem()
                )
            } else {
                Text(
                    text = stringResource(Res.string.charts_custom),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalOverscanPadding(), vertical = 4.dp)
                        .animateItem()
                )
            }
        }

        if (regexEdits.isEmpty()) {
            item(key = "no_custom_regexes") {
                EmptyTextWithButtonOnTv(
                    visible = true,
                    text = pluralStringResource(Res.plurals.num_regex_edits, 0, 0),
                    onButtonClick = onImport
                )
            }
        }

        itemsIndexed(regexEdits, key = { _, item -> item._id }) { index, item ->
            DraggableItem(dragDropState, index + nonRegexItemsCount) { isDragging ->
                RegexEditItem(
                    regexEdit = item,
                    onItemClick = onItemClick,
                    canMoveUp = (index + nonRegexItemsCount) > 0,
                    canMoveDown = index < regexEdits.size - 1,
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
private fun PresetItem(
    preset: RegexPreset,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LabeledCheckbox(
        checked = isChecked,
        onCheckedChange = onToggle,
        text = RegexPresets.getString(preset),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RegexEditItem(
    regexEdit: RegexEdit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onKeypressMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemClick: (RegexEdit) -> Unit,
    modifier: Modifier = Modifier,
    forShimmer: Boolean = false,
) {
    val modifierIcons = getModifierIcons(regexEdit)

    val name = regexEdit.name

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = horizontalOverscanPadding())
    ) {

        if (!PlatformStuff.isTv) {
            Icon(
                imageVector = Icons.Outlined.DragHandle,
                contentDescription = stringResource(Res.string.edit_drag_handle),
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .padding(8.dp)
            )
        } else {
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.OutlinedLeadingButton(
                        enabled = canMoveUp,
                        onClick = {
                            onKeypressMoveItem(regexEdit.order, regexEdit.order - 1)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = stringResource(Res.string.move_up),
                        )
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.OutlinedTrailingButton(
                        enabled = canMoveDown,
                        onCheckedChange = {
                            onKeypressMoveItem(regexEdit.order, regexEdit.order + 1)
                        },
                        checked = false,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = stringResource(Res.string.move_down),
                        )
                    }
                },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !forShimmer) { onItemClick(regexEdit) }
                .padding(8.dp)
                .backgroundForShimmer(forShimmer)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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
private fun getModifierIcons(regexEdit: RegexEdit): List<ImageVector> {
    val m = mutableListOf<ImageVector>()

    if (regexEdit.blockPlayerAction != null) m += Icons.Outlined.Block
    else if (regexEdit.replacement == null) m += Icons.Outlined.SwipeLeftAlt
    else m += Icons.Outlined.FindReplace

    if (regexEdit.appIds.isNotEmpty()) m += Icons.Outlined.Apps
    if (regexEdit.caseSensitive) m += PanoIcons.MatchCase
    if (regexEdit.replacement?.replaceAll == true) m += Icons.Outlined.Public

    return m
}