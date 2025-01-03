package com.arn.scrobble.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arn.scrobble.pref.EditsMode


@Preview(showBackground = true)
@Composable
private fun OutlinedToggleButtonsPreview() {
    OutlinedToggleButtons(
        items = listOf("One", "Two", "Three"),
        selectedIndex = -1,
        onSelected = {}
    )
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


@Preview(showBackground = true)
@Composable
private fun SimpleHeaderItemPreview() {
    SimpleHeaderItem(
        text = "Text",
        icon = Icons.Outlined.Info
    )
}


@Preview(showBackground = true)
@Composable
private fun AvatarOrInitialsPreview() {
    MaterialTheme {
        AvatarOrInitials(
            avatarUrl = null,
            avatarInitialLetter = 'A',
            textStyle = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .padding(8.dp)
                .size(100.dp)
                .clip(CircleShape)
        )
    }
}