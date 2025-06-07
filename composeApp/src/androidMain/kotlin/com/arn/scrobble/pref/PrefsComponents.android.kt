package com.arn.scrobble.pref

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arn.scrobble.themes.AppPreviewTheme
import com.arn.scrobble.utils.AndroidStuff

actual fun filterAppList(
    packageNames: Set<String>,
    seenAppsMap: Map<String, String>,
): List<String> {
    return packageNames.filter { AndroidStuff.isPackageInstalled(it) }
}

@Preview(showBackground = true)
@Composable
fun SwitchPrefPreview() {
    AppPreviewTheme {
        SwitchPref(
            text = "Master switch",
            value = true,
            copyToSave = { copy(scrobblerEnabled = it) },
            onNavigateToBilling = { },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TextPrefPreview() {
    AppPreviewTheme {
        TextPref(
            text = "Add to Quick Settings",
            onClick = { },
            locked = true
        )
    }
}