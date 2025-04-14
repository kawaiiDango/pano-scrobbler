package com.arn.scrobble.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.icons.AlbumFilled
import com.arn.scrobble.icons.EqualizerFilled
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.icons.StonksNew
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.collapse
import pano_scrobbler.composeapp.generated.resources.expand
import pano_scrobbler.composeapp.generated.resources.hate
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.num_listeners
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_noti
import pano_scrobbler.composeapp.generated.resources.show_all
import pano_scrobbler.composeapp.generated.resources.time_just_now
import kotlin.math.abs

@Composable
fun MusicEntryListItem(
    entry: MusicEntry,
    appId: String? = null,
    showDateSeperator: Boolean = false,
    fixedImageHeight: Boolean = true,
    fetchAlbumImageIfMissing: Boolean = false,
    forShimmer: Boolean = false,
    imageUrlOverride: String? = null,
    onImageClick: (() -> Unit)? = null,
    onEntryClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    menuContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hasOnlyOneClickable = onImageClick == null && onMenuClick == null

    NowPlayingSurface(
        nowPlaying = (entry as? Track)?.isNowPlaying == true,
    ) {
        Column(
            modifier = modifier
                .then(
                    if (hasOnlyOneClickable)
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(enabled = !forShimmer) { onEntryClick() }
                    else
                        Modifier
                )
                .padding(8.dp)
        ) {
            if (showDateSeperator) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = if (onImageClick != null) Modifier.clickable(enabled = !forShimmer) { onImageClick() } else Modifier
                ) {
                    AsyncImage(
                        model = if (forShimmer)
                            null
                        else imageUrlOverride
                            ?: MusicEntryImageReq(
                                entry,
                                fetchAlbumInfoIfMissing = fetchAlbumImageIfMissing
                            ),
                        fallback = rememberTintedVectorPainter(PanoIcons.EqualizerFilled, entry),
                        error = rememberTintedVectorPainter(PanoIcons.AlbumFilled, entry),
                        placeholder = placeholderPainter(),
                        contentDescription = stringResource(Res.string.album_art),
                        modifier = Modifier
                            .then(
                                if (fixedImageHeight)
                                    Modifier
                                        .size(60.dp)
                                else
                                    Modifier
                                        .heightIn(min = 60.dp, max = 200.dp)
                                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            )
                            .clip(MaterialTheme.shapes.medium)
                            .backgroundForShimmer(forShimmer)
                    )

                    if (entry is Track && (entry.userloved == true || entry.userHated == true)) {
                        Icon(
                            imageVector = if (entry.userloved == true) Icons.Outlined.Favorite else Icons.Outlined.HeartBroken,
                            contentDescription = stringResource(if (entry.userloved == true) Res.string.loved else Res.string.hate),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .rotate(11.25f)
                                .offset(x = 6.dp, y = (-6).dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (!hasOnlyOneClickable)
                                Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable(enabled = !forShimmer) { onEntryClick() }
                            else
                                Modifier
                        )
                        .padding(8.dp)
                        .backgroundForShimmer(forShimmer)
                ) {
                    val topText = when {
                        entry is Track && entry.date != null -> PanoTimeFormatter.relative(entry.date)
                        entry.listeners != null -> pluralStringResource(
                            Res.plurals.num_listeners,
                            entry.listeners!!.toInt(),
                            entry.listeners!!.format()
                        )

                        entry.playcount != null -> pluralStringResource(
                            Res.plurals.num_scrobbles_noti,
                            entry.playcount!!.toInt(),
                            entry.playcount!!.format()
                        )

                        else -> null
                    }

                    val firstText = when (entry) {
                        is Album -> entry.name
                        is Track -> entry.name
                        is Artist -> entry.name
                    }

                    val secondText = when (entry) {
                        is Album -> entry.artist?.name
                        is Track -> entry.artist.name
                        else -> null
                    }

                    val thirdText = when (entry) {
                        is Track -> entry.album?.name?.ifEmpty { null }
                        else -> null
                    }

                    if (topText != null)
                        Text(
                            text = if (forShimmer) "" else topText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.End)
                        )

                    Text(
                        text = if (forShimmer) "" else firstText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (secondText != null)
                        Text(
                            text = if (forShimmer) "" else secondText,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 8.dp)
                        )

                    if (thirdText != null)
                        Text(
                            text = if (forShimmer) "" else thirdText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 8.dp)
                        )
                }

                if (onMenuClick != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (entry is Track && entry.isNowPlaying == true) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(Res.string.time_just_now),
                                modifier = Modifier.size(19.dp)
                            )
                        } else if (appId != null) {
                            AppIcon(
                                appItem = AppItem(appId, ""),
                                modifier = Modifier
                                    .size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = onMenuClick,
                            enabled = !forShimmer
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(Res.string.item_options)
                            )
                        }

                        menuContent()
                    }
                }
            }
        }
    }
}

@Composable
fun NowPlayingSurface(
    nowPlaying: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {

    if (nowPlaying) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = modifier
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun MusicEntryGridItem(
    entry: MusicEntry,
    progress: Float? = null,
    showArtist: Boolean,
    fetchAlbumImageIfMissing: Boolean = false,
    forShimmer: Boolean = false,
    imageUrlOverride: String? = null,
    index: Int?,
    stonksDelta: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = !forShimmer, onClick = onClick)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = if (forShimmer)
                null
            else imageUrlOverride
                ?: MusicEntryImageReq(
                    entry,
                    fetchAlbumInfoIfMissing = fetchAlbumImageIfMissing
                ),
            fallback = rememberTintedVectorPainter(PanoIcons.EqualizerFilled, entry),
            error = rememberTintedVectorPainter(PanoIcons.AlbumFilled, entry),
            placeholder = placeholderPainter(),
            contentDescription = stringResource(Res.string.album_art),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    MaterialTheme.shapes.medium.copy(
                        bottomEnd = ZeroCornerSize,
                        bottomStart = ZeroCornerSize
                    )
                )
                .backgroundForShimmer(forShimmer)
        )
        Column(
            modifier = Modifier
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    MaterialTheme.shapes.medium.copy(
                        topEnd = ZeroCornerSize,
                        topStart = ZeroCornerSize
                    )
                )
                .padding(8.dp)
        ) {

            val firstText = when (entry) {
                is Album -> entry.name
                is Track -> (if (entry.userloved == true) "❤️ " else "") + entry.name
                is Artist -> entry.name
            }

            val secondText = if (showArtist) {
                when (entry) {
                    is Album -> entry.artist?.name
                    is Track -> entry.artist.name
                    else -> null
                }
            } else null

            val playCount = entry.userplaycount ?: entry.playcount
            val scrobbleDateText = if (entry is Track && entry.date != null)
                " | " + PanoTimeFormatter.relative(entry.date)
            else
                ""
            val thirdText = if (playCount != null) {
                pluralStringResource(
                    Res.plurals.num_scrobbles_noti,
                    playCount.toInt(),
                    playCount.format()
                ) + scrobbleDateText
            } else
                null

            Text(
                text = if (forShimmer)
                    ""
                else if (index != null)
                    "${index + 1}. $firstText"
                else
                    firstText,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .backgroundForShimmer(forShimmer)

            )

            if (secondText != null)
                Text(
                    text = if (forShimmer) "" else secondText,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .backgroundForShimmer(forShimmer)
                )

            if (thirdText != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .backgroundForShimmer(forShimmer)
                ) {
                    if (stonksDelta != null) {
                        stonksIconForDelta(stonksDelta)?.let { (icon, color) ->
                            Icon(
                                imageVector = icon,
                                tint = color,
                                contentDescription = stonksDelta.toString(),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = if (forShimmer) "" else thirdText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,

                        )
                }
            }

            // always reserve space for the progress bar
            Box(
                modifier = Modifier
                    .then(
                        if (progress == null)
                            Modifier
                        else
                            Modifier
                                .fillMaxWidth(progress)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ExpandableHeaderItem(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .toggleable(enabled = enabled, value = expanded, onValueChange = onToggle)
            .padding(16.dp)
    ) {
        if (enabled)
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowDown else Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = if (expanded)
                    stringResource(Res.string.collapse)
                else
                    stringResource(Res.string.expand),
                tint = MaterialTheme.colorScheme.primary
            )

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun GoToDetailsHeaderItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowRightAlt,
            contentDescription = stringResource(Res.string.show_all),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun TextHeaderItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .indication(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            )
            .focusable(interactionSource = interactionSource)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
fun ExpandableHeaderMenu(
    title: String,
    icon: ImageVector,
    menuItemText: String,
    onMenuItemClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var menuShown by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = {
                menuShown = true
            })
            .padding(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )

        Box {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            DropdownMenu(
                expanded = menuShown,
                onDismissRequest = { menuShown = false },
            ) {
                DropdownMenuItem(
                    text = { Text(menuItemText) },
                    onClick = {
                        onMenuItemClick()
                        menuShown = false
                    }
                )
            }
        }
    }
}

fun <T> LazyListScope.expandableSublist(
    headerText: String,
    headerIcon: ImageVector,
    items: List<T>,
    transformToMusicEntry: (T) -> MusicEntry = { it as MusicEntry },
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onItemClick: (MusicEntry) -> Unit,
    onMenuClick: (() -> Unit)? = null,
    menuContent: @Composable (T) -> Unit = {},
    fetchAlbumImageIfMissing: Boolean = false,
    minItems: Int = 3,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    item(key = headerText) {
        ExpandableHeaderItem(
            title = headerText,
            icon = headerIcon,
            expanded = expanded || items.size <= minItems,
            enabled = items.size > minItems,
            onToggle = onToggle,
            modifier = modifier.animateItem(),
        )
    }

    items(
        items.take(if (expanded) items.size else minItems),
        key = { it.hashCode() }
    ) { item ->
        val musicEntry = transformToMusicEntry(item)
        MusicEntryListItem(
            musicEntry,
            onEntryClick = { onItemClick(musicEntry) },
            onMenuClick = onMenuClick,
            menuContent = { menuContent(item) },
            fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
            modifier = modifier.animateItem(),
        )
    }
}

@Composable
fun EntriesHorizontal(
    title: String,
    entries: LazyPagingItems<MusicEntry>,
    headerIcon: ImageVector,
    maxCountEvaluater: () -> Float = {
        if (entries.itemCount > 0)
            entries.peek(0)?.playcount?.toFloat() ?: 0f
        else
            0f
//        (0 until entries.itemCount)
//            .maxOfOrNull { i -> entries.peek(i)?.playcount?.toFloat() ?: 0f }
//            ?: 0f
    },
    placeholderItem: MusicEntry,
    fetchAlbumImageIfMissing: Boolean,
    showArtists: Boolean,
    emptyStringRes: StringResource,
    onHeaderClick: () -> Unit,
    onItemClick: (MusicEntry) -> Unit,
) {
    val maxCount by remember(entries.loadState) { mutableFloatStateOf(maxCountEvaluater()) }
    val shimmer by remember(entries.loadState.refresh) { mutableStateOf(entries.loadState.refresh is LoadState.Loading) }

    GoToDetailsHeaderItem(
        icon = headerIcon,
        title = title,
        enabled = !shimmer,
        onClick = onHeaderClick,
    )

    if (entries.itemCount == 0 && !shimmer) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            EmptyText(
                text = stringResource(emptyStringRes),
                visible = true,
            )
        }
    } else {
        PanoLazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = if (shimmer) Modifier.shimmerWindowBounds() else Modifier
        ) {
            if (shimmer) {
                items(4) { idx ->
                    MusicEntryGridItem(
                        placeholderItem,
                        forShimmer = true,
                        onClick = {},
                        progress = 0f,
                        showArtist = showArtists,
                        stonksDelta = null,
                        index = null,
                        modifier = Modifier
                            .width(150.dp)
                            .animateItem()
                    )
                }
            }

            items(entries.itemCount, key = entries.itemKey()) { idx ->
                val entryNullable = entries[idx]
                val entry = entryNullable ?: Artist(" ", listeners = idx.toLong())

                MusicEntryGridItem(
                    entry,
                    forShimmer = entryNullable == null,
                    onClick = {
                        onItemClick(entry)
                    },
                    progress = if (maxCount > 0) {
                        entry.playcount?.toFloat()?.div(maxCount) ?: 0f
                    } else
                        entry.match,
                    stonksDelta = entry.stonksDelta,
                    fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
                    showArtist = showArtists,
                    index = idx,
                    modifier = Modifier
                        .width(150.dp)
                        .animateItem()
                )
            }
        }
    }
}

@Composable
fun EntriesGrid(
    entries: LazyPagingItems<MusicEntry>,
    maxCountEvaluater: () -> Float = {
        if (entries.itemCount > 0)
            entries.peek(0)?.playcount?.toFloat() ?: 0f
        else
            0f
//        (0 until entries.itemCount)
//            .maxOfOrNull { i -> entries.peek(i)?.playcount?.toFloat() ?: 0f }
//            ?: 0f
    },
    onItemClick: (MusicEntry) -> Unit,
    placeholderItem: MusicEntry,
    fetchAlbumImageIfMissing: Boolean,
    showArtists: Boolean,
    emptyStringRes: StringResource,
    modifier: Modifier = Modifier,
) {
    val maxCount by remember(entries.loadState) { mutableFloatStateOf(maxCountEvaluater()) }
    val shimmer by remember(entries.loadState.refresh) { mutableStateOf(entries.loadState.refresh is LoadState.Loading) }

    if (entries.itemCount == 0 && !shimmer) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(emptyStringRes),
                textAlign = TextAlign.Center,
            )
        }
    } else {
        PanoLazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = Stuff.GRID_MIN_SIZE.dp),
            modifier = modifier
                .fillMaxSize()
                .then(if (shimmer) Modifier.shimmerWindowBounds() else Modifier)
        ) {
            if (shimmer) {
                items(8) { idx ->
                    MusicEntryGridItem(
                        placeholderItem,
                        forShimmer = true,
                        onClick = {},
                        progress = 0f,
                        stonksDelta = null,
                        index = null,
                        showArtist = showArtists,
                        modifier = Modifier
                            .animateItem()
                    )
                }
            }

            items(entries.itemCount, key = entries.itemKey()) { idx ->
                val entryNullable = entries[idx]
                val entry = entryNullable ?: Artist(" ", listeners = idx.toLong())

                MusicEntryGridItem(
                    entry,
                    forShimmer = entryNullable == null,
                    onClick = {
                        onItemClick(entry)
                    },
                    progress = if (maxCount > 0) {
                        entry.playcount?.toFloat()?.div(maxCount) ?: 0f
                    } else
                        entry.match,
                    stonksDelta = entry.stonksDelta,
                    fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
                    showArtist = showArtists,
                    index = idx,
                    modifier = Modifier
                        .animateItem()
                )
            }


            if (entries.loadState.refresh is LoadState.Error ||
                entries.loadState.append is LoadState.Error
            ) {
                val error = entries.loadState.refresh as LoadState.Error
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ListLoadError(
                        modifier = Modifier.animateItem(),
                        throwable = error.error,
                        onRetry = { entries.retry() })
                }
            }
        }
    }
}


fun getMusicEntryPlaceholderItem(type: Int, showScrobbleCount: Boolean = true): MusicEntry {
    val count = if (showScrobbleCount) 10L else 0L

    return when (type) {
        Stuff.TYPE_TRACKS -> Track(
            name = "Track",
            artist = Artist(
                name = "Artist",
            ),
            playcount = count,
            album = null,
        )

        Stuff.TYPE_ALBUMS -> Album(
            name = "Album",
            artist = Artist(
                name = "Artist",
            ),
            playcount = count,
        )

        Stuff.TYPE_ARTISTS,
        Stuff.TYPE_ALBUM_ARTISTS,
            -> Artist(
            name = "Artist",
            playcount = count,
        )

        else -> throw IllegalArgumentException("Unknown type $type")
    }
}

@Composable
fun stonksIconForDelta(delta: Int?) = when {
    delta == null -> null
    delta == Int.MAX_VALUE -> PanoIcons.StonksNew to MaterialTheme.colorScheme.primary
    delta in 1..5 -> Icons.Outlined.KeyboardArrowUp to MaterialTheme.colorScheme.tertiary
    delta > 5 -> Icons.Outlined.KeyboardDoubleArrowUp to MaterialTheme.colorScheme.tertiary
    delta in -1 downTo -5 -> Icons.Outlined.KeyboardArrowDown to MaterialTheme.colorScheme.secondary
    delta < -5 -> Icons.Outlined.KeyboardDoubleArrowDown to MaterialTheme.colorScheme.secondary
    delta == 0 -> Icons.Outlined.FiberManualRecord to MaterialTheme.colorScheme.outline
    else -> null
}

@Composable
fun rememberTintedVectorPainter(
    imageVector: ImageVector,
    musicEntry: MusicEntry?,
): VectorPainter {

    val colors = LocalThemeAttributes.current.allSecondaryContainerColors

    val colorSeed = when (musicEntry) {
        is Track -> musicEntry.artist.name + musicEntry.album?.name + musicEntry.name
        is Album -> musicEntry.artist?.name + musicEntry.name
        is Artist -> musicEntry.name
        else -> null
    }

    val color = colors[abs(colorSeed.hashCode()) % colors.size]

    return rememberVectorPainter(
        defaultWidth = imageVector.defaultWidth,
        defaultHeight = imageVector.defaultHeight,
        viewportWidth = imageVector.viewportWidth,
        viewportHeight = imageVector.viewportHeight,
        name = imageVector.name,
        tintColor = color,
        tintBlendMode = imageVector.tintBlendMode,
        autoMirror = imageVector.autoMirror
    ) { viewportWidth, viewportHeight ->

        RenderVectorGroup(group = imageVector.root)
    }
}