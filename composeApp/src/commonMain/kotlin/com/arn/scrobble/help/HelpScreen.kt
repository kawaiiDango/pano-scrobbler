package com.arn.scrobble.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.icons.BugReport
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.ui.ButtonWithIcon
import com.arn.scrobble.ui.EmptyText
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.SearchField
import com.arn.scrobble.utils.BugReportUtils
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.bug_report
import pano_scrobbler.composeapp.generated.resources.faq
import pano_scrobbler.composeapp.generated.resources.not_found
import pano_scrobbler.composeapp.generated.resources.search

@Composable
expect fun HelpSaveLogsButton(
    showFilePicker: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
    searchTerm: String,
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
    var searchTerm by rememberSaveable { mutableStateOf(searchTerm) }

    LaunchedEffect(searchTerm) {
        viewModel.setFilter(searchTerm)
    }

    Column(modifier = modifier) {

        if (!PlatformStuff.isTv)
            SearchField(
                searchTerm = searchTerm,
                onSearchTermChange = {
                    searchTerm = it
                },
                label = stringResource(Res.string.search) + ": " + stringResource(Res.string.faq),
                modifier = Modifier
            )

        EmptyText(
            visible = mdItems?.isEmpty() == true,
            text = stringResource(Res.string.not_found),
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
                            BugReportUtils.mail()
                        },
                        icon = Icons.BugReport,
                    )
                }
            }
        }
    }

    FilePicker(
        show = filePickerShown,
        mode = FilePickerMode.Save("pano_scrobbler_logs"),
        type = FileType.LOG,
        onDismiss = { filePickerShown = false },
    ) { file ->
        scope.launch {
            BugReportUtils.saveLogsToFile(file)
        }
    }
}