package com.arn.scrobble.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemShapes
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

class PanoMenuScope {
    val entries = mutableListOf<@Composable ColumnScope.(shape: MenuItemShapes) -> Unit>()

    fun item(
        text: @Composable () -> Unit,
        onClick: () -> Unit,
        enabled: Boolean = true,
        selected: Boolean? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingContent: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
    ) {
        entries += { shapes ->
            if (selected == null)
                DropdownMenuItem(
                    onClick = onClick,
                    enabled = enabled,
                    text = text,
                    shape = shapes.shape,
                    leadingIcon = leadingIcon,
                    trailingContent = trailingContent,
                    supportingText = supportingText,
                )
            else
                SelectableDropdownMenuItem(
                    onClick = onClick,
                    enabled = enabled,
                    text = text,
                    shapes = shapes,
                    leadingIcon = leadingIcon,
                    trailingContent = trailingContent,
                    supportingText = supportingText,
                    selected = selected
                )
        }
    }

    fun custom(content: @Composable ColumnScope.() -> Unit) {
        entries += { content() }
    }
}

@Composable
fun PanoDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    position: MenuAnchorPosition = MenuAnchorPosition.Below,
    headerContent: @Composable (ColumnScope.() -> Unit)? = null,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    content: (PanoMenuScope.() -> Unit)
) {
    // the outer offset param does nothing on android
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        popupPositionProvider =
            MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                position,
                offset
            ),
    ) {
        if (headerContent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                // allows the header to shrink if no space
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShape(0, 2),
                    tonalElevation = 2.dp,
                    shadowElevation = shadowElevation,
                    content = headerContent,
                    containerColor = MenuDefaults.myGroupStandardContainerColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(MenuDefaults.GroupSpacing))
        }

        val scope = PanoMenuScope().apply(content)

        DropdownMenuGroup(
            shapes = if (headerContent != null)
                MenuDefaults.groupShape(1, 2)
            else
                MenuDefaults.groupShapes(),
            tonalElevation = 2.dp,
            shadowElevation = shadowElevation,
            containerColor = MenuDefaults.myGroupStandardContainerColor,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            val headerOffset = if (headerContent != null) 1 else 0
            scope.entries.forEachIndexed { index, entry ->
                val shapes = MenuDefaults.itemShape(
                    index + headerOffset,
                    scope.entries.size + headerOffset
                )
                entry(shapes)
            }
        }
    }
}
