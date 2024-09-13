package com.arn.scrobble.info

import androidx.annotation.Keep
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.compose.LocalFragment
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Tag
import com.arn.scrobble.ui.BottomSheetDialogParent
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.copyToClipboard
import com.arn.scrobble.utils.Stuff.getData
import java.net.URLEncoder

@Composable
fun TagInfoContent(
    tag: Tag,
    onCopy: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    viewModel: TagInfoVM = viewModel(),
    modifier: Modifier = Modifier
) {
    val info by viewModel.info.collectAsState(initial = null)

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
            url = "https://www.last.fm/tag/" + URLEncoder.encode(tag.name, "UTF-8"),
            onCopy = onCopy,
            onOpenUrl = onOpenUrl
        )

        InfoCounts(
            countPairs = listOf(
                stringResource(R.string.taggers) to info?.reach,
                stringResource(R.string.taggings) to info?.count
            ),
            forShimmer = info == null
        )

        info?.wiki?.content?.let {
            InfoWikiText(
                text = it,
                maxLinesWhenCollapsed = 4,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Keep
@Composable
fun TagInfoScreen() {
    val fragment = LocalFragment.current

    val tag = fragment.requireArguments().getData<Tag>()!!

    BottomSheetDialogParent {
        TagInfoContent(
            tag = tag,
            onCopy = { fragment.requireContext().copyToClipboard(it) },
            onOpenUrl = { Stuff.openInBrowser(fragment.requireContext(), it) },
            modifier = it
        )
    }
}