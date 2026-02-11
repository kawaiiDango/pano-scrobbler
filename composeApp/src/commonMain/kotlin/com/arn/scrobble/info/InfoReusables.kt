package com.arn.scrobble.info

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.KeyboardArrowDown
import com.arn.scrobble.icons.KeyboardArrowUp
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MusicNote
import com.arn.scrobble.panoicons.AlbumArtist
import com.arn.scrobble.panoicons.PanoIcons
import com.arn.scrobble.ui.AvatarOrInitials
import com.arn.scrobble.ui.MinimalHtmlParser
import com.arn.scrobble.ui.backgroundForShimmer
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.launch
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
    scrollState: ScrollState, // from vertically scrollable column
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var overflows by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scrollStepPx = with(density) { 96.dp.toPx() }
    var minYInColumn by remember { mutableFloatStateOf(0f) }
    var maxYInColumn by remember { mutableFloatStateOf(0f) }


    val displayText by remember(text) {
        mutableStateOf(
            text
                .replaceFirst(
                    """<a href="https?://[^"]+">Read more on Last\.fm</a>""".toRegex(),
                    "\n\n$0"
                )
        )
    }

    if (displayText.isNotBlank()) {
        Box(
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(4.dp)
                .animateContentSize()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInParent()

                    minYInColumn = bounds.top
                    maxYInColumn = bounds.bottom
                },
        ) {
            Text(
                text = MinimalHtmlParser.parseLinksToAnnotatedString(
                    text = displayText,
                    onLinkClick = if (PlatformStuff.isTv || !expanded)
                        null
                    else {
                        { url -> PlatformStuff.openInBrowser(url) }
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else maxLinesWhenCollapsed,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = {
                    if (!expanded) {
                        overflows = it.hasVisualOverflow
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (overflows)
                            Modifier.clip(MaterialTheme.shapes.medium)
                                .clickable(onClick = onExpandToggle)
                                .padding(end = 24.dp)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (!expanded || keyEvent.type != KeyEventType.KeyDown)
                                        return@onPreviewKeyEvent false

                                    val canScrollUp = scrollState.value > minYInColumn
                                    val canScrollDown =
                                        scrollState.value + scrollState.viewportSize < maxYInColumn

                                    when (keyEvent.key) {
                                        Key.DirectionDown -> {
                                            if (canScrollDown) {
                                                coroutineScope.launch {
                                                    scrollState.animateScrollBy(scrollStepPx)
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        Key.DirectionUp -> {
                                            if (canScrollUp) {
                                                coroutineScope.launch {
                                                    scrollState.animateScrollBy(-scrollStepPx)
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        else -> false
                                    }
                                }
                        else Modifier
                    )
                    .padding(8.dp)
            )

            if (overflows) {
                Icon(
                    imageVector = if (expanded) Icons.KeyboardArrowUp else Icons.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (expanded)
                            Res.string.collapse
                        else
                            Res.string.show_all
                    ),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
fun InfoCounts(
    countPairs: List<Pair<String, Number?>>,
    avatarUrl: String?,
    avatarName: String?,
    firstItemIsUsers: Boolean,
    modifier: Modifier = Modifier,
    onClickFirstItem: (() -> Unit)? = null,
    forShimmer: Boolean = false,
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
                            avatarName = avatarName,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(CircleShape),
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
    trailingContent: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit)? = null,
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
    Stuff.TYPE_TRACKS -> Icons.MusicNote
    Stuff.TYPE_ALBUMS -> Icons.Album
    Stuff.TYPE_ARTISTS -> Icons.Mic
    Stuff.TYPE_ALBUM_ARTISTS -> PanoIcons.AlbumArtist
    Stuff.TYPE_LOVES -> Icons.Favorite
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
        avatarName = "LA"
    )
}