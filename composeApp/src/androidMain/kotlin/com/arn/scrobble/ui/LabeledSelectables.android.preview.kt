package com.arn.scrobble.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
private fun LabelledCheckboxPreview() {
    LabeledCheckbox(
        text = "LabelledCheckbox",
        checked = false,
        onCheckedChange = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun LabelledSwitchPreview() {
    LabeledSwitch(
        text = "LabelledSwitch",
        checked = false,
        onCheckedChange = {}
    )
}