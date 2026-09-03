package com.arn.scrobble.info

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.arn.scrobble.ui.shapedClickable
import com.arn.scrobble.ui.shimmerWindowBounds
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.collapse
import pano_scrobbler.composeapp.generated.resources.expand

@Composable
fun InfoWikiText(
    text: String,
    maxLinesWhenCollapsed: Int,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    scrollState: ScrollState, // from vertically scrollable column
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var overflows by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scrollStepPx = with(density) { 96.dp.toPx() }
    var minYInColumn by remember { mutableFloatStateOf(0f) }
    var maxYInColumn by remember { mutableFloatStateOf(0f) }

    val containerColor =
        MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current + 1.dp)

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
                .background(
                    color = containerColor,
                    shape = MaterialTheme.shapes.medium
                )
                .animateContentSize()
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInParent()

                    minYInColumn = bounds.top
                    maxYInColumn = bounds.bottom
                }
                .then(
                    if (overflows)
                        Modifier.shapedClickable(
                            onClick = onExpandToggle,
                            clickableAdded = !expanded || PlatformStuff.isTv
                        )
                            .onKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                val canScrollUp = scrollState.value > minYInColumn
                                val canScrollDown =
                                    scrollState.value + scrollState.viewportSize < maxYInColumn

                                when (event.key) {
                                    Key.DirectionDown -> {
                                        if (canScrollDown) {
                                            scope.launch {
                                                scrollState.animateScrollBy(scrollStepPx)
                                            }
                                            true
                                        } else false
                                    }

                                    Key.DirectionUp -> {
                                        if (canScrollUp) {
                                            scope.launch { scrollState.animateScrollBy(-scrollStepPx) }
                                            true
                                        } else false
                                    }

                                    else -> false
                                }
                            }
                    else Modifier
                )
                .padding(4.dp),
        ) {
            val bodyMedium = MaterialTheme.typography.bodyMedium
            val bodyLarge = MaterialTheme.typography.bodyLarge
            // average the two
            val textStyle = bodyMedium.copy(
                fontSize = ((bodyMedium.fontSize.value + bodyLarge.fontSize.value) / 2).sp,
                lineHeight = ((bodyMedium.lineHeight.value + bodyLarge.lineHeight.value) / 2).sp,
                letterSpacing = ((bodyMedium.letterSpacing.value + bodyLarge.letterSpacing.value) / 2).sp
            )

            Text(
                text = MinimalHtmlParser.parseLinksToAnnotatedString(
                    text = displayText,
                    onLinkClick = if (PlatformStuff.isTv || !expanded)
                        null
                    else {
                        { url -> PlatformStuff.openInBrowser(url) }
                    }
                ),
                color = MaterialTheme.colorScheme.contentColorFor(containerColor),
                style = textStyle,
                maxLines = if (expanded) Int.MAX_VALUE else maxLinesWhenCollapsed,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = {
                    if (!expanded) {
                        overflows = it.hasVisualOverflow
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = if (expanded)
                            IconButtonDefaults.smallContainerSize().height
                        else if (overflows)
                            12.dp
                        else
                            0.dp
                    )
                    .padding(8.dp)
            )

            if (overflows) {
                if (expanded && !PlatformStuff.isTv) {
                    IconButton(
                        onClick = { onExpandToggle() },
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier
                            .size(IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                            .align(Alignment.BottomCenter)
                    ) {
                        Icon(
                            imageVector = Icons.KeyboardArrowUp,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = stringResource(Res.string.collapse),
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }
                } else if (!expanded) {
                    Icon(
                        imageVector = Icons.KeyboardArrowDown,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(Res.string.expand),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCounts(
    countPairs: List<Pair<String, Number?>>,
    avatarUrl: String?,
    avatarName: String?,
    modifier: Modifier = Modifier,
    onClickFirstItem: (() -> Unit)? = null,
    forShimmer: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shimmerWindowBounds(forShimmer),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        countPairs.forEachIndexed { index, (text, value) ->
            if (index == 0 && avatarName != null) {
                if (onClickFirstItem != null) {
                    OutlinedButton(
                        onClick = onClickFirstItem,
                        enabled = !forShimmer,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                text = value?.format() ?: "",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.backgroundForShimmer(forShimmer)
                            )

                            AvatarOrInitials(
                                avatarUrl = avatarUrl,
                                avatarName = avatarName,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape),
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
                            text = value?.format() ?: "",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.backgroundForShimmer(forShimmer)
                        )

                        AvatarOrInitials(
                            avatarUrl = avatarUrl,
                            avatarName = avatarName,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(24.dp)
                                .clip(CircleShape),
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = value?.format() ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.backgroundForShimmer(forShimmer)
                    )

                    Text(
                        text = text.takeIf { !forShimmer } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.backgroundForShimmer(forShimmer)
                    )
                }
            }
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