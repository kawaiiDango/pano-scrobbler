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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwipeLeftAlt
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.arn.scrobble.pref.AppListSaveType
import com.arn.scrobble.ui.DraggableItem
import com.arn.scrobble.ui.EmptyTextWithImportButtonOnTv
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.dragContainer
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.ui.rememberDragDropState
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.charts_custom
import pano_scrobbler.composeapp.generated.resources.delete
import pano_scrobbler.composeapp.generated.resources.disable
import pano_scrobbler.composeapp.generated.resources.edit_drag_handle
import pano_scrobbler.composeapp.generated.resources.edit_max_patterns
import pano_scrobbler.composeapp.generated.resources.edit_presets
import pano_scrobbler.composeapp.generated.resources.edit_regex_test
import pano_scrobbler.composeapp.generated.resources.enable
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.move_down
import pano_scrobbler.composeapp.generated.resources.move_up
import pano_scrobbler.composeapp.generated.resources.num_regex_edits
import pano_scrobbler.composeapp.generated.resources.settings

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
        onItemDelete = {
            viewModel.delete(it)
        },
        onItemToggle = { regexEdit, isEnabled ->
            viewModel.upsertAll(
                listOf(regexEdit.copy(enabled = isEnabled))
            )
        },
        onPresetToggled = viewModel::updatePreset,
        onNavigate = onNavigate,
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
    onItemDelete: (RegexEdit) -> Unit,
    onItemToggle: (RegexEdit, Boolean) -> Unit,
    onMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragEnd: () -> Unit,
    onPresetToggled: (RegexPreset, Boolean) -> Unit,
    onNavigate: (PanoRoute) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
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
        contentPadding = panoContentPadding(mayHaveBottomFab = true),
        modifier = modifier.dragContainer(dragDropState),
    ) {
        if (!PlatformStuff.isTv) {
            item(key = "test_button") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = {
                            onNavigate(PanoRoute.RegexEditsTest)
                        },
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
                    .animateItem()
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        items(presetsWithState, key = { it.first }) { (preset, checked) ->
            PresetItem(
                preset = preset,
                isChecked = checked,
                onToggle = { isChecked ->
                    onPresetToggled(preset, isChecked)
                },
                onNavigateSettings = if (preset in RegexPresets.hasSettings) {
                    {
                        scope.launch {
                            val preSelectedPackages = PlatformStuff.mainPrefs.data.map {
                                it.getRegexPresetApps(preset)
                            }.first()

                            onNavigate(
                                PanoRoute.AppList(
                                    isSingleSelect = false,
                                    saveType = AppListSaveType.RegexPresetApps(preset),
                                    preSelectedPackages = preSelectedPackages.toList()
                                )
                            )
                        }
                    }
                } else null,
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
            )
        }

        item(key = "custom_header") {
            if (regexEdits.size >= Stuff.MAX_PATTERNS) {
                Text(
                    text = stringResource(Res.string.edit_max_patterns, Stuff.MAX_PATTERNS),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            } else {
                Text(
                    text = stringResource(Res.string.charts_custom),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }

        if (regexEdits.isEmpty()) {
            item(key = "no_custom_regexes") {
                EmptyTextWithImportButtonOnTv(
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
                    onItemDelete = onItemDelete,
                    onItemToggle = onItemToggle,
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
    onNavigateSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabeledCheckbox(
            checked = isChecked,
            onCheckedChange = onToggle,
            text = RegexPresets.getString(preset),
            modifier = Modifier.weight(1f)
        )

        if (onNavigateSettings != null) {
            IconButton(
                onClick = onNavigateSettings
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(Res.string.settings),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RegexEditItem(
    regexEdit: RegexEdit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onKeypressMoveItem: (fromIndex: Int, toIndex: Int) -> Unit,
    onItemClick: (RegexEdit) -> Unit,
    onItemDelete: (RegexEdit) -> Unit,
    onItemToggle: (RegexEdit, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    forShimmer: Boolean = false,
) {
    val modifierIcons = getModifierIcons(regexEdit)
    var dropdownShown by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .alpha(
                if (regexEdit.enabled) 1f else 0.75f
            )
            .padding(vertical = 4.dp)
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
                text = regexEdit.name,
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

        IconButton(
            onClick = { dropdownShown = true },
            enabled = !forShimmer,
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(Res.string.item_options),
            )

            DropdownMenu(
                expanded = dropdownShown,
                onDismissRequest = { dropdownShown = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (regexEdit.enabled)
                                    Res.string.disable
                                else
                                    Res.string.enable,
                            ),
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (!regexEdit.enabled)
                                Icons.Outlined.ToggleOff
                            else
                                Icons.Outlined.ToggleOn,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        dropdownShown = false
                        onItemToggle(regexEdit, !regexEdit.enabled)
                    },
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(Res.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        dropdownShown = false
                        onItemDelete(regexEdit)
                    },
                )
            }
        }
    }
}

@Composable
private fun getModifierIcons(regexEdit: RegexEdit): List<ImageVector> {
    val m = mutableListOf<ImageVector>()

    if (!regexEdit.enabled) m += Icons.Outlined.ToggleOff

    if (regexEdit.blockPlayerAction != null) m += Icons.Outlined.Block
    else if (regexEdit.replacement == null) m += Icons.Outlined.SwipeLeftAlt
    else m += Icons.Outlined.FindReplace

    if (regexEdit.appIds.isNotEmpty()) m += Icons.Outlined.Apps
    if (regexEdit.caseSensitive) m += PanoIcons.MatchCase
    if (regexEdit.replacement?.replaceAll == true) m += Icons.Outlined.Public

    return m
}