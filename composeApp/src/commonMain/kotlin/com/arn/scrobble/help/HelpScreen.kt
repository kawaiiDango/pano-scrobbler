package com.arn.scrobble.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import com.arn.scrobble.ui.PanoLazyColumn
import com.arn.scrobble.ui.SearchField
import com.arn.scrobble.ui.panoContentPadding
import com.arn.scrobble.utils.BugReportUtils
import com.arn.scrobble.utils.PlatformStuff
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
    modifier: Modifier = Modifier,
    viewModel: HelpVM = viewModel { HelpVM() }
) {
    val scope = rememberCoroutineScope()
    var filePickerShown by remember { mutableStateOf(false) }
    val faqs by viewModel.faqs.collectAsStateWithLifecycle()
    var searchTerm by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchTerm) {
        viewModel.setFilter(searchTerm)
    }

    Column(modifier = modifier) {
        SearchField(
            searchTerm = searchTerm,
            onSearchTermChange = {
                searchTerm = it
            },
            modifier = Modifier
                .padding(panoContentPadding(bottom = false))
        )

        EmptyText(
            visible = faqs?.isEmpty() == true,
            text = stringResource(Res.string.not_found),
        )

        faqs?.let { faqs ->
            PanoLazyColumn(
                contentPadding = panoContentPadding(mayHaveBottomFab = true),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(faqs, key = { it.question }) {
                    FaqItemUi(it, searchTerm)
                }
            }

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FaqItemUi(
    faq: FaqItem,
    searchTerm: String,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
//        if (faq.platform != null) {
//            Text(
//                text = faq.platform.toString(),
//                style = MaterialTheme.typography.labelMediumEmphasized,
//                color = MaterialTheme.colorScheme.primary
//            )
//        }

        Text(
            text = faq.question.toAnnotatedString(
                searchTerm = searchTerm,
                highlightColor = MaterialTheme.colorScheme.tertiary
            ),
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.primary
        )

        SelectionContainer {
            Text(
                text = faq.answer.toAnnotatedString(
                    entities = faq.entities,
                    searchTerm = searchTerm,
                    highlightColor = MaterialTheme.colorScheme.tertiary
                ),
            )
        }
    }
}

private fun String.toAnnotatedString(
    searchTerm: String,
    highlightColor: Color,
    entities: List<FaqEntity> = emptyList(),
    boldStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
    codeStyle: SpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
    ),
    linkStyle: SpanStyle = SpanStyle(
        textDecoration = TextDecoration.Underline
    )
): AnnotatedString {
    val text = this

    return buildAnnotatedString {
        append(text)

        entities
            .sortedBy { it.start }
            .forEach { e ->
                val s = e.start.coerceIn(0, text.length)
                val en = e.end.coerceIn(0, text.length)
                if (s >= en) return@forEach

                when (e.type) {
                    FaqEntityType.bold -> addStyle(boldStyle, s, en)
                    FaqEntityType.code -> addStyle(codeStyle, s, en)
                    FaqEntityType.link -> {
                        addStyle(linkStyle, s, en)
                        e.url?.let { url ->
                            addLink(
                                LinkAnnotation.Url(url) {
                                    PlatformStuff.openInBrowser(url)
                                },
                                start = s,
                                end = en
                            )
                        }
                    }

                    null -> {}
                }
            }

        if (searchTerm.isNotBlank()) {
            var searchIndex = text.indexOf(searchTerm, ignoreCase = true)
            while (searchIndex >= 0) {
                val s = searchIndex.coerceIn(0, text.length)
                val en = (searchIndex + searchTerm.length).coerceIn(0, text.length)
                addStyle(SpanStyle(color = highlightColor), s, en)
                searchIndex =
                    text.indexOf(searchTerm, searchIndex + searchTerm.length, ignoreCase = true)
            }
        }
    }
}