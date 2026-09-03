package com.arn.scrobble.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.icons.BugReport
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.main.ScrobblerState
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.SearchEffect
import com.arn.scrobble.utils.BugReportUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.bug_report
import pano_scrobbler.composeapp.generated.resources.not_found

@Composable
expect fun HelpSaveLogsButton(
    showFilePicker: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun HelpScreen(
    searchFieldState: TextFieldState,
    searchTerm: String,
    modifier: Modifier = Modifier,
    scrobblerStateFlow: StateFlow<ScrobblerState>,
    viewModel: MdViewerVM = viewModel {
        MdViewerVM(
            "https://kawaiidango.github.io/pano-scrobbler/faq.md",
            "files/faq.md"
        )
    }
) {
    val scope = rememberCoroutineScope()
    var filePickerShown by remember { mutableStateOf(false) }
    val mdItems by viewModel.mdBlocks.collectAsStateWithLifecycle()

    SearchEffect(
        searchFieldState,
        initialText = searchTerm
    ) {
        viewModel.setFilter(it)
    }

    Column(modifier = modifier) {
        if (mdItems?.isEmpty() == true)
            Text(
                text = stringResource(Res.string.not_found),
                style = MaterialTheme.typography.titleLarge
            )

        mdItems?.let { mdItems ->
            MdText(
                mdItems,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )

            if (!PlatformStuff.isTv) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        8.dp,
                        Alignment.CenterHorizontally
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HelpSaveLogsButton({
                        filePickerShown = true
                    })

                    ButtonWithIcon(
                        text = stringResource(Res.string.bug_report),
                        onClick = {
                            BugReportUtils.mail(scrobblerStateFlow.value)
                        },
                        icon = Icons.BugReport,
                    )
                }
            }
        }
    }

    FilePicker(
        show = filePickerShown,
        mode = FilePickerMode.Save("pano_scrobbler_logs_" + Stuff.getFileNameDateSuffix()),
        type = FileType.LOG,
        onDismiss = { filePickerShown = false },
    ) { file ->
        scope.launch {
            BugReportUtils.saveLogsToFile(file)
        }
    }
}