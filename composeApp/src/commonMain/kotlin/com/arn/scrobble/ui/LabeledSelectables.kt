package com.arn.scrobble.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp


@Composable
fun LabeledCheckbox(
    text: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    textStyle: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .clip(MaterialTheme.shapes.medium)
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = {
                    onCheckedChange(it)
                },
                role = Role.Checkbox
            )
            .alpha(if (enabled) 1f else 0.5f)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null // null recommended for accessibility with screenreaders
        )
        Text(
            text = text,
            style = textStyle,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}


@Composable
fun LabeledSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .toggleable(
                value = checked,
                onValueChange = {
                    onCheckedChange(it)
                },
                role = Role.Switch
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = textStyle,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = null, // null recommended for accessibility with screenreaders
            modifier = Modifier.padding(start = 16.dp)
        )

    }
}

