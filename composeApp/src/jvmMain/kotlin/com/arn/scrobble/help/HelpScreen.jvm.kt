package com.arn.scrobble.help

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Save
import com.arn.scrobble.ui.ButtonWithIcon
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.save_logs

@Composable
actual fun HelpSaveLogsButton(
    showFilePicker: () -> Unit,
    modifier: Modifier
) {
    ButtonWithIcon(
        icon = Icons.Save,
        text = stringResource(Res.string.save_logs),
        onClick = showFilePicker
    )
}