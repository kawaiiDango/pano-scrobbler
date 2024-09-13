package com.arn.scrobble.info

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arn.scrobble.R
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.valentinilk.shimmer.shimmer

@Composable
fun InfoWikiText(
    text: String,
    maxLinesWhenCollapsed: Int,
    modifier: Modifier = Modifier
) {
    var displayText = text
    val idx =
        displayText.indexOf("<a href=\"http://www.last.fm").takeIf { it != -1 }
            ?: displayText.indexOf("<a href=\"https://www.last.fm")
    if (idx != -1) {
        displayText = displayText.substring(0, idx).trim()
    }
    if (displayText.isNotBlank()) {
        displayText = displayText.replace("\n", "<br>")
        var expanded by rememberSaveable { mutableStateOf(false) }
        Row(
            modifier = modifier
        ) {
            Text(
                text = if (Stuff.isTv)
                    AnnotatedString(displayText)
                else
                    AnnotatedString.fromHtml(displayText),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else maxLinesWhenCollapsed,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { expanded = !expanded }
                    .padding(8.dp)
            )

            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(
                    id = if (expanded)
                        R.string.show_all
                    else
                        R.string.expand
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun InfoCounts(
    countPairs: List<Pair<String, Int?>>,
    onClickFirstItem: (() -> Unit)? = null,
    forShimmer: Boolean = false,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (forShimmer)
                    Modifier.shimmer()
                else
                    Modifier
            ),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        countPairs.forEachIndexed { index, (text, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (index == 0 && onClickFirstItem != null && !forShimmer)
                            Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    onClickFirstItem()
                                }
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.shapes.medium
                                )
                                .padding(16.dp)
                        else
                            Modifier
                    )

            ) {
                Text(
                    text = value?.format() ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (index == 0 && onClickFirstItem != null) FontWeight.Bold else null,
                    modifier = Modifier.backgroundForShimmer(forShimmer)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (index == 0 && onClickFirstItem != null) FontWeight.Bold else null
                )
            }
        }
    }
}

@Composable
fun InfoSimpleHeader(
    text: String,
    icon: ImageVector,
    url: String,
    onCopy: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !Stuff.isTv) {
                onCopy(text)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        if (!Stuff.isTv) {
            IconButton(onClick = {
                onOpenUrl(url)
            }) {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = stringResource(id = R.string.more_info),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}