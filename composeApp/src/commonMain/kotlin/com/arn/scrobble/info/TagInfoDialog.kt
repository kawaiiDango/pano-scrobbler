package com.arn.scrobble.info

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.OpenInBrowser
import com.arn.scrobble.icons.Tag
import com.arn.scrobble.ui.IconButtonWithTooltip
import com.arn.scrobble.ui.TextWithIcon
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import io.ktor.http.encodeURLPathPart
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.more_info
import pano_scrobbler.composeapp.generated.resources.taggers
import pano_scrobbler.composeapp.generated.resources.taggings

@Composable
fun TagInfoDialog(
    tag: Tag,
    scrollState: ScrollState,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagInfoVM = viewModel { TagInfoVM(tag) },
) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    var wikiExpanded by rememberSaveable(isExpanded) { mutableStateOf(isExpanded) }
    val wikiLangs by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.wikiLangs }
    var selectedLang by rememberSaveable { mutableStateOf(wikiLangs.firstOrNull() ?: "en") }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (!isExpanded) {
            TextWithIcon(
                text = tag.name,
                icon = Icons.Tag,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            InfoCounts(
                countPairs = listOf(
                    stringResource(Res.string.taggers) to info?.reach,
                    stringResource(Res.string.taggings) to info?.count
                ),
                avatarUrl = null,
                avatarName = null,
                forShimmer = info == null,
                modifier = Modifier.weight(1f)
            )

            if (!PlatformStuff.isTv) {
                IconButtonWithTooltip(
                    icon = Icons.OpenInBrowser,
                    contentDescription = stringResource(Res.string.more_info),
                    onClick = {
                        val url =
                            "https://www.last.fm/tag/" + tag.name.encodeURLPathPart()
                        PlatformStuff.openInBrowser(url)
                    }
                )
            }
        }

        if (info != null) {
            InfoWikiText(
                text = info?.wiki?.content.orEmpty(),
                maxLinesWhenCollapsed = 10,
                expanded = wikiExpanded,
                onExpandToggle = {
                    wikiExpanded = !wikiExpanded
                    onExpand()
                },
                wikiLangs = wikiLangs,
                selectedLang = selectedLang,
                onSelectedLang = {
                    selectedLang = it
                    viewModel.setLang(it)
                },
                scrollState = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}