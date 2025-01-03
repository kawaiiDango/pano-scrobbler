package com.arn.scrobble.info

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.AlbumArtist
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.PlatformStuff.toHtmlAnnotatedString
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.collapse
import pano_scrobbler.composeapp.generated.resources.show_all

@Composable
fun InfoWikiText(
    text: String,
    maxLinesWhenCollapsed: Int,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayText = text
    val idx =
        displayText.indexOf("<a href=\"http://www.last.fm").takeIf { it != -1 }
            ?: displayText.indexOf("<a href=\"https://www.last.fm")
    if (idx != -1) {
        displayText = displayText.substring(0, idx).trim()
    }
    if (displayText.isNotBlank()) {
        if (!PlatformStuff.isTv) {
            displayText = displayText.replace("<br>", "\n")
        }
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
                text = if (PlatformStuff.isTv || PlatformStuff.isDesktop)
                    AnnotatedString(displayText)
                else
                    displayText.toHtmlAnnotatedString(),
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
                    if (expanded)
                        Res.string.collapse
                    else
                        Res.string.show_all
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
    avatarInitialLetter: Char? = null,
    firstItemIsUsers: Boolean,
    onClickFirstItem: (() -> Unit)? = null,
    forShimmer: Boolean = false,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (forShimmer)
                    Modifier.shimmerWindowBounds()
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
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    if (index == 0 && firstItemIsUsers) {
                        AvatarOrInitials(
                            avatarUrl = avatarUrl,
                            avatarInitialLetter = avatarInitialLetter,
                        )
                    } else {
                        Text(
                            text = text.takeIf { !forShimmer } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (onClick != null)
                    Modifier.clickable(onClick = onClick)
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

@Composable
fun getMusicEntryIcon(type: Int) = when (type) {
    Stuff.TYPE_TRACKS -> Icons.Outlined.MusicNote
    Stuff.TYPE_ALBUMS -> Icons.Outlined.Album
    Stuff.TYPE_ARTISTS -> Icons.Outlined.Mic
    Stuff.TYPE_ALBUM_ARTISTS -> PanoIcons.AlbumArtist
    Stuff.TYPE_LOVES -> Icons.Outlined.FavoriteBorder
    else -> throw IllegalArgumentException("Unknown type: $type")
}

@Composable
private fun InfoCountsPreview() {
    InfoCounts(
        countPairs = listOf(
            "Tracks" to 123,
            "Albums" to 456,
            "Artists" to 789
        ),
        onClickFirstItem = {},
        avatarUrl = null,
        firstItemIsUsers = true,
        avatarInitialLetter = 'L'
    )
}
