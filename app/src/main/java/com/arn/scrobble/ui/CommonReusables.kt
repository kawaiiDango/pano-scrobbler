package com.arn.scrobble.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arn.scrobble.R
import com.arn.scrobble.navigation.LocalNavigationType
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.pref.EditsMode
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
    icon: ImageVector = Icons.Outlined.Info,
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
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
        modifier = modifier,
    ) {
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
fun TextWithIcon(
    text: String,
    icon: ImageVector,
    style: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp)
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
fun <T> ButtonWithSpinner(
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

@Composable
fun <T> ButtonWithDropdown(
    text: String,
    itemToTexts: Map<T, String>,
    onMainButtonClick: () -> Unit,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var dropDownShown by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy((-5).dp),
        modifier = modifier
    ) {
        OutlinedButton(
            onClick = onMainButtonClick,
            shape = CircleShape.copy(
                topEnd = ZeroCornerSize,
                bottomEnd = ZeroCornerSize
            ),
        ) {
            Text(text)
        }

        OutlinedIconButton(
            onClick = { dropDownShown = true },
            border = ButtonDefaults.outlinedButtonBorder(true),
            shape = CircleShape.copy(
                topStart = ZeroCornerSize,
                bottomStart = ZeroCornerSize
            ),
        ) {
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)

            DropdownMenu(
                expanded = dropDownShown,
                onDismissRequest = { dropDownShown = false }
            ) {
                itemToTexts.forEach { (item, text) ->
                    DropdownMenuItem(
                        onClick = {
                            onItemClick(item)
                            dropDownShown = false
                        },
                        text = {
                            Text(text)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconButtonWithTooltip(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.secondary,
    modifier: Modifier = Modifier
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(contentDescription) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}

@Composable
fun ButtonWithIcon(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        modifier = modifier
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text)
    }
}

@Preview(showBackground = true)
@Composable
private fun IconButtonWithTooltipPreview() {
    IconButtonWithTooltip(
        icon = Icons.Outlined.Info,
        onClick = {},
        contentDescription = "Info"
    )
}

@Preview(showBackground = true)
@Composable
private fun ButtonWithDropdownPreview() {
    ButtonWithDropdown(
        text = "Text",
        itemToTexts = EditsMode.entries.associateWith { it.name },
        onItemClick = {},
        onMainButtonClick = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun ButttonWithSpinnerPreview() {
    ButtonWithSpinner(
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
    content(
        Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.overscan_padding_horiz))
            .padding(horizontal = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialogParent(
    padding: Boolean = true,
    content: @Composable (modifier: Modifier) -> Unit
) {
//    val fragment = LocalFragment.current as DialogFragment
    val sheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = Stuff.isTv || UiUtils.isTabletUi)
    var showSheet by remember { mutableStateOf(true) }
//    val nestedScrollConnection = rememberNestedScrollInteropConnection()

    if (showSheet) {
//        AppTheme {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
//                    fragment.dismiss()
            },
//                dragHandle = null,
            sheetState = sheetState,
        ) {
            content(
                Modifier
                    .fillMaxWidth()
//                        .nestedScroll(nestedScrollConnection)
                    .then(
                        if (padding)
                            Modifier.padding(horizontal = 24.dp)
                        else
                            Modifier
                    )
            )
        }
//        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogParent(
    content: @Composable (modifier: Modifier) -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
//        AppTheme {
        BasicAlertDialog(
            onDismissRequest = {
                showDialog = false
            },
            content = {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    content(Modifier.padding(24.dp))
                }
            }
        )
//        }
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


@Composable
fun SimpleHeaderItem(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium
            )
            .padding(vertical = 8.dp, horizontal = horizontalOverscanPadding()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(end = 32.dp)
                .size(24.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SimpleHeaderItemPreview() {
    SimpleHeaderItem(
        text = "Text",
        icon = Icons.Outlined.Info
    )
}

@Composable
fun VerifyButton(
    doStuff: () -> Unit,
    result: Result<*>?,
    onDone: () -> Unit,
    buttonText: String = stringResource(R.string.login_submit),
    modifier: Modifier = Modifier,
    extraContent: @Composable RowScope.() -> Unit = {},
) {
    var verifying by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val networkErrorStr = stringResource(R.string.network_error)

    LaunchedEffect(result) {
        result?.onFailure {
            verifying = false
            errorText = it.message ?: networkErrorStr
        }?.onSuccess {
            verifying = false
            errorText = null
            onDone()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.align(Alignment.End)
        ) {
            extraContent()
            Box(
                modifier = Modifier
                    .height(60.dp)
            ) {
                if (verifying) {
                    CircularProgressIndicator()
                } else {
                    OutlinedButton(
                        onClick = {
                            verifying = true
                            errorText = null
                            doStuff()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(buttonText, maxLines = 1)
                    }
                }
            }
        }

        ErrorText(errorText)
    }
}

@Composable
fun Modifier.backgroundForShimmer(isShimmer: Boolean): Modifier {
    if (!isShimmer) return this

    return clip(MaterialTheme.shapes.medium)
        .background(Color.Gray.copy(alpha = 0.3f))
}


@Composable
fun horizontalOverscanPadding(): Dp {
    val navigationType = LocalNavigationType.current
    return when (navigationType) {
        PanoNavigationType.BOTTOM_NAVIGATION -> 16.dp
        PanoNavigationType.NAVIGATION_RAIL -> 24.dp
        PanoNavigationType.PERMANENT_NAVIGATION_DRAWER -> 48.dp
    }
}

@Composable
fun verticalOverscanPadding(): Dp {
    val navigationType = LocalNavigationType.current
    return when (navigationType) {
        PanoNavigationType.BOTTOM_NAVIGATION,
        PanoNavigationType.NAVIGATION_RAIL -> 0.dp

        PanoNavigationType.PERMANENT_NAVIGATION_DRAWER -> 27.dp
    }
}