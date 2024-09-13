package com.arn.scrobble.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.compose.LocalFragment
import com.arn.scrobble.R
import com.arn.scrobble.pref.EditsMode
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils

@Composable
fun ExtraBottomSpace(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
    )
}

@Composable
fun AlertDialogOk(
    text: String,
    onConfirmation: () -> Unit = {},
    onDismissRequest: () -> Unit = onConfirmation,
    title: String? = null,
    confirmText: String = stringResource(id = android.R.string.ok),
) {
    AlertDialog(
        title = if (title != null) {
            {
                Text(text = title)
            }
        } else
            null,
        text = {
            Text(text = text)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text(text = confirmText)
            }
        }
    )
}

@Composable
fun OutlinedToggleButtons(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, item ->
            OutlinedButton(
                onClick = {
                    if (selectedIndex != index) {
                        onSelected(index)
                    }
                },
                shape = when (index) {
                    items.size - 1 -> {
                        CircleShape.copy(
                            topStart = ZeroCornerSize,
                            bottomStart = ZeroCornerSize
                        )
                    }

                    0 -> {
                        CircleShape.copy(
                            topEnd = ZeroCornerSize,
                            bottomEnd = ZeroCornerSize
                        )
                    }

                    else -> {
                        CircleShape.copy(ZeroCornerSize)
                    }
                },
                colors = if (selectedIndex == index) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(text = item)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutlinedToggleButtonsPreview() {
    OutlinedToggleButtons(
        items = listOf("One", "Two", "Three"),
        selectedIndex = -1,
        onSelected = {}
    )
}

@Composable
fun <E : Enum<*>> RadioButtonGroup(
    enumToTexts: Map<E, String>,
    selected: E?,
    onSelected: (E) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        enumToTexts.forEach { (enu, text) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        if (enu != selected)
                            onSelected(enu)
                    }
                    .padding(horizontal = 8.dp, vertical = 16.dp)
            ) {
                RadioButton(
                    selected = selected == enu,
                    onClick = null,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(text)
            }
        }
    }
}

@Composable
fun ErrorText(
    errorText: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(errorText != null) {
        Row(
            modifier = modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = errorText ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Composable
fun InfoText(
    text: String,
    icon: ImageVector = Icons.Outlined.Info,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = style
        )
    }
}


@Composable
fun SearchBox(
    searchTerm: String,
    label: String = stringResource(R.string.search),
    onSearchTermChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchTerm,
        onValueChange = {
            onSearchTermChange(it)
        },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (searchTerm.isNotEmpty()) {
                IconButton(onClick = {
                    onSearchTermChange("")
                }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.delete))
                }
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        singleLine = true,
    )
}

@Composable
fun <T> ButtonWithDropdown(
    prefixText: String?,
    itemToTexts: Map<T, String>,
    selected: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropDownShown by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { dropDownShown = true },
        modifier = modifier.padding(end = 8.dp)
    ) {
        Text(
            if (prefixText == null) {
                itemToTexts[selected] ?: ""
            } else {
                "$prefixText: ${itemToTexts[selected] ?: ""}"
            }
        )
        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)

        DropdownMenu(
            expanded = dropDownShown,
            onDismissRequest = { dropDownShown = false }
        ) {
            itemToTexts.forEach { (item, text) ->
                DropdownMenuItem(
                    onClick = {
                        onItemSelected(item)
                        dropDownShown = false
                    },
                    enabled = item != selected,
                    text = {
                        Text(text)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ButttonWithDropdownPreview() {
    ButtonWithDropdown(
        prefixText = "Prefix",
        itemToTexts = EditsMode.entries.associateWith { it.name },
        selected = EditsMode.EDITS_NOPE,
        onItemSelected = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun ErrorTextPreview() {
    MaterialTheme {
        ErrorText(errorText = "Error")
    }
}

@Preview(showBackground = true)
@Composable
private fun RadioButtonGroupPreview() {
    RadioButtonGroup(
        enumToTexts = EditsMode.entries.associateWith { it.name },
        selected = EditsMode.EDITS_NOPE,
        onSelected = {}
    )
}

@Composable
fun ScreenParent(
    content: @Composable (modifier: Modifier) -> Unit
) {
    val nestedScrollConnection = rememberNestedScrollInteropConnection()
    AppTheme {
        Surface {
            content(
                Modifier
                    .nestedScroll(nestedScrollConnection)
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(R.dimen.overscan_padding_horiz))
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialogParent(
    content: @Composable (modifier: Modifier) -> Unit
) {
    val fragment = LocalFragment.current as DialogFragment
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = Stuff.isTv || UiUtils.isTabletUi)
    var showSheet by remember { mutableStateOf(true) }

    if (showSheet) {
        AppTheme {
            ModalBottomSheet(
                onDismissRequest = {
                    showSheet = false
                    fragment.dismiss()
                },
                sheetState = sheetState,
            ) {
                content(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogParent(
    content: @Composable (modifier: Modifier) -> Unit
) {
    val fragment = LocalFragment.current as DialogFragment
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AppTheme {
            BasicAlertDialog(
                onDismissRequest = {
                    showDialog = false
                    fragment.dismiss()
                },
                content = {
                    Surface {
                        content(Modifier.padding(24.dp))
                    }
                }
            )
        }
    }
}

@Composable
fun EmptyText(
    visible: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        }
    }
}


fun Modifier.backgroundForShimmer(isShimmer: Boolean): Modifier = composed {
    if (!isShimmer) return@composed this

    clip(MaterialTheme.shapes.medium)
        .background(Color.Gray.copy(alpha = 0.3f))
}