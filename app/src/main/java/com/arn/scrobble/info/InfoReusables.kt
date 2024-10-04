package com.arn.scrobble.info

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.valentinilk.shimmer.shimmer

@Composable
fun InfoWikiText(
    text: String,
    maxLinesWhenCollapsed: Int,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
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
        Row(
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(4.dp)
                .animateContentSize(),
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
                    .clickable(onClick = onExpandToggle)
                    .padding(8.dp)
            )

            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(
                    id = if (expanded)
                        R.string.collapse
                    else
                        R.string.show_all
                ),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun InfoCounts(
    countPairs: List<Pair<String, Int?>>,
    avatarUrl: String? = null,
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
                                    MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.medium
                                )
                                .padding(8.dp)
                        else
                            Modifier
                    )

            ) {
                Text(
                    text = value?.format() ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = if (index == 0 && onClickFirstItem != null) FontWeight.Bold else null,
                    modifier = Modifier.backgroundForShimmer(forShimmer)
                )
                Row {
                    if (avatarUrl != null && index == 0) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = stringResource(id = R.string.profile_pic),
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                        )
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = if (index == 0 && onClickFirstItem != null) FontWeight.Bold else null
                    )
                }
            }
        }
    }
}

@Composable
fun InfoSimpleHeader(
    text: String,
    icon: ImageVector,
    onClick: (() -> Unit)?,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (onClick != null)
                    Modifier.clickable(enabled = !Stuff.isTv) {
                        onClick()
                    }
                else
                    Modifier
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingContent != null) {
            leadingContent()
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InfoCountsPreview() {
    InfoCounts(
        countPairs = listOf(
            "Tracks" to 123,
            "Albums" to 456,
            "Artists" to 789
        ),
        onClickFirstItem = {},
        avatarUrl = "https://lastfm.freetls.fastly.net/i/u/64s/2a96cbd8b46e442fc41c2b86b821562f.png"
    )
}

fun getMusicEntryIcon(entry: MusicEntry) = when (entry) {
    is Track -> Icons.Outlined.MusicNote
    is Album -> Icons.Outlined.Album
    is Artist -> Icons.Outlined.Mic
}

@Composable
fun getMusicEntryIcon(type: Int) = when (type) {
    Stuff.TYPE_TRACKS -> Icons.Outlined.MusicNote
    Stuff.TYPE_ALBUMS -> Icons.Outlined.Album
    Stuff.TYPE_ARTISTS -> Icons.Outlined.Mic
    Stuff.TYPE_ALBUM_ARTISTS -> ImageVector.vectorResource(id = R.drawable.vd_album_artist)
    Stuff.TYPE_LOVES -> Icons.Outlined.FavoriteBorder
    else -> throw IllegalArgumentException("Unknown type: $type")
}