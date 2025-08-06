package com.arn.scrobble.info

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.utils.PlatformStuff
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.more_info
import pano_scrobbler.composeapp.generated.resources.taggers
import pano_scrobbler.composeapp.generated.resources.taggings
import java.net.URLEncoder

@Composable
fun TagInfoDialog(
    tag: Tag,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    viewModel: TagInfoVM = viewModel { TagInfoVM() },
) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    var wikiExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(tag) {
        viewModel.loadInfoIfNeeded(tag)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        InfoSimpleHeader(
            text = tag.name,
            icon = Icons.Outlined.Tag,
            trailingContent = {
                if (!PlatformStuff.isTv) {
                    IconButtonWithTooltip(
                        icon = Icons.Outlined.OpenInBrowser,
                        contentDescription = stringResource(Res.string.more_info),
                        onClick = {
                            val url =
                                "https://www.last.fm/tag/" + URLEncoder.encode(tag.name, "UTF-8")
                            PlatformStuff.openInBrowser(url)
                        }
                    )
                }
            },
            onClick = null,
        )

        InfoCounts(
            countPairs = listOf(
                stringResource(Res.string.taggers) to info?.reach,
                stringResource(Res.string.taggings) to info?.count
            ),
            firstItemIsUsers = false,
            avatarUrl = null,
            avatarName = null,
            forShimmer = info == null
        )

        info?.wiki?.content?.let {
            InfoWikiText(
                text = it,
                maxLinesWhenCollapsed = 4,
                expanded = wikiExpanded,
                onExpandToggle = { wikiExpanded = !wikiExpanded },
                scrollState = scrollState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}