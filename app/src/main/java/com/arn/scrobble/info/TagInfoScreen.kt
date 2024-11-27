package com.arn.scrobble.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.ui.BottomSheetDialogParent
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.utils.Stuff
import java.net.URLEncoder

@Composable
fun TagInfoContent(
    tag: Tag,
    onOpenUrl: (String) -> Unit,
    viewModel: TagInfoVM = viewModel(),
    modifier: Modifier = Modifier,
) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    var wikiExpanded by rememberSaveable { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        viewModel.loadInfoIfNeeded(tag)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        InfoSimpleHeader(
            text = tag.name,
            icon = Icons.Outlined.Tag,
            trailingContent = {
                if (!Stuff.isTv) {
                    IconButtonWithTooltip(
                        icon = Icons.Outlined.OpenInBrowser,
                        contentDescription = stringResource(id = R.string.more_info),
                        onClick = {
                            val url =
                                "https://www.last.fm/tag/" + URLEncoder.encode(tag.name, "UTF-8")
                            onOpenUrl(url)
                        }
                    )
                }
            },
            onClick = null,
        )

        InfoCounts(
            countPairs = listOf(
                stringResource(R.string.taggers) to info?.reach,
                stringResource(R.string.taggings) to info?.count
            ),
            firstItemIsUsers = false,
            forShimmer = info == null
        )

        AnimatedVisibility(
            info?.wiki?.content != null,
        ) {
            InfoWikiText(
                text = info?.wiki?.content ?: "",
                maxLinesWhenCollapsed = 4,
                expanded = wikiExpanded,
                onExpandToggle = { wikiExpanded = !wikiExpanded },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TagInfoScreen(
    tag: Tag,
    onDismiss: () -> Unit,
) {
    BottomSheetDialogParent(
        onDismiss = onDismiss
    ) {
        TagInfoContent(
            tag = tag,
            onOpenUrl = { Stuff.openInBrowser(it) },
            modifier = it
        )
    }
}