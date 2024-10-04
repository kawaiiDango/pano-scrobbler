package com.arn.scrobble.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.arn.scrobble.R
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.themes.AppPreviewTheme
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.format
import com.valentinilk.shimmer.shimmer

@Composable
fun MusicEntryListItem(
    entry: MusicEntry,
    nowPlaying: Boolean = false,
    packageName: String? = null,
    showDateSeperator: Boolean = false,
    fetchAlbumImageIfMissing: Boolean = false,
    forShimmer: Boolean = false,
    imageUrlOverride: String? = null,
    onImageClick: (() -> Unit)? = null,
    onTrackClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val hasOnlyOneClickable = onImageClick == null && onMenuClick == null

    Column(
        modifier = modifier
            .then(
                if (hasOnlyOneClickable)
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(enabled = !forShimmer) { onTrackClick() }
                else
                    Modifier
            )
            .padding(8.dp)
    ) {
        if (showDateSeperator) {
            Icon(
                imageVector = Icons.Outlined.HorizontalRule,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterHorizontally)
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
                    else if (imageUrlOverride != null)
                        imageUrlOverride
                    else
                        ImageRequest.Builder(context)
                            .data(
                                MusicEntryImageReq(
                                    entry,
                                    fetchAlbumInfoIfMissing = fetchAlbumImageIfMissing
                                )
                            )
                            .placeholder(R.drawable.avd_loading)
                            .error(R.drawable.vd_wave_simple_filled)
                            .build(),
                    contentDescription = stringResource(R.string.album_art),
                    modifier = Modifier
                        .size(60.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .backgroundForShimmer(forShimmer)
                )

                if (entry is Track && (entry.userloved == true || entry.userHated == true)) {
                    Icon(
                        imageVector = if (entry.userloved == true) Icons.Outlined.Favorite else Icons.Outlined.HeartBroken,
                        contentDescription = stringResource(if (entry.userloved == true) R.string.loved else R.string.hate),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                            .rotate(11.25f)
                            .padding(vertical = 4.dp)
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
                                .clickable(enabled = !forShimmer) { onTrackClick() }
                        else
                            Modifier
                    )
                    .padding(8.dp)
                    .backgroundForShimmer(forShimmer)
            ) {
                val topText = when {
                    entry is Track && entry.date != null -> Stuff.myRelativeTime(millis = entry.date)
                    entry.listeners != null -> pluralStringResource(
                        R.plurals.num_listeners,
                        entry.listeners!!,
                        entry.listeners!!.format()
                    )

                    entry.playcount != null -> pluralStringResource(
                        R.plurals.num_scrobbles_noti,
                        entry.playcount!!,
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
                        text = topText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.End)
                    )

                Text(
                    text = firstText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (secondText != null)
                    Text(
                        text = secondText,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 8.dp)
                    )

                if (thirdText != null)
                    Text(
                        text = thirdText,
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
                    if (nowPlaying)
                        Icon(
                            painter = painterResource(id = R.drawable.avd_now_playing),
                            contentDescription = stringResource(R.string.time_just_now),
                            modifier = Modifier.size(19.dp)
                        )
                    else if (packageName != null) {
                        AsyncImage(
                            model = PackageName(packageName),
                            contentDescription = packageName,
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
                            contentDescription = stringResource(R.string.item_options)
                        )
                    }
                }
            }
        }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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
            else if (imageUrlOverride != null)
                imageUrlOverride
            else
                ImageRequest.Builder(context)
                    .data(
                        MusicEntryImageReq(
                            entry,
                            fetchAlbumInfoIfMissing = fetchAlbumImageIfMissing
                        )
                    )
                    .placeholder(R.drawable.avd_loading)
                    .error(R.drawable.vd_wave_simple_filled)
                    .build(),
            contentDescription = stringResource(R.string.album_art),
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
                is Track -> entry.name
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
            val thirdText = if (playCount != null)
                pluralStringResource(
                    R.plurals.num_scrobbles_noti,
                    playCount,
                    playCount.format()
                )
            else
                null

            Text(
                text = firstText,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .backgroundForShimmer(forShimmer)

            )

            if (secondText != null)
                Text(
                    text = secondText,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

            if (thirdText != null)
                Text(
                    text = thirdText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

            if (progress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
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
    modifier: Modifier = Modifier
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
                contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(
                    R.string.expand
                ),
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
    modifier: Modifier = Modifier
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
            contentDescription = stringResource(R.string.show_all),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ExpandableHeaderMenu(
    title: String,
    icon: ImageVector,
    menuItemText: String,
    onMenuClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
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
            style = MaterialTheme.typography.bodyMedium,
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
                        onMenuClick()
                        menuShown = false
                    }
                )
            }
        }
    }
}

//@Composable
fun LazyListScope.ExpandableSublist(
    headerRes: Int,
    headerIcon: ImageVector,
    items: List<MusicEntry>,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onItemClick: (MusicEntry) -> Unit,
    fetchAlbumImageIfMissing: Boolean = false,
    minItems: Int = 3,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    item(key = headerRes) {
        ExpandableHeaderItem(
            title = stringResource(headerRes),
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
        MusicEntryListItem(
            item,
            onTrackClick = { onItemClick(item) },
            modifier = modifier.animateItem(),
            fetchAlbumImageIfMissing = fetchAlbumImageIfMissing
        )
    }
}

@Composable
fun ColumnScope.EntriesHorizontal(
    title: String,
    entries: List<MusicEntry>,
    headerIcon: ImageVector,
    maxCountEvaluater: () -> Float = {
        entries.maxOfOrNull { it.playcount?.toFloat() ?: 0f } ?: 0f
    },
    showArtists: Boolean,
    emptyStringRes: Int,
    shimmer: Boolean = false,
    onHeaderClick: () -> Unit,
    onItemClick: (MusicEntry) -> Unit,
) {
    val maxCount by remember(entries) { mutableFloatStateOf(maxCountEvaluater()) }

    GoToDetailsHeaderItem(
        icon = headerIcon,
        title = title,
        enabled = !shimmer,
        onClick = onHeaderClick,
    )

    if (entries.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(150.dp)
                .align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = stringResource(emptyStringRes),
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = if (shimmer) Modifier.shimmer() else Modifier
        ) {
            items(entries, key = { it }) { entry ->
                MusicEntryGridItem(
                    entry,
                    forShimmer = shimmer,
                    onClick = {
                        onItemClick(entry)
                    },
                    progress = if (maxCount > 0) {
                        entry.playcount?.toFloat()?.div(maxCount) ?: 0f
                    } else
                        entry.match,
                    showArtist = showArtists,
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
    entries: List<MusicEntry>,
    maxCountEvaluater: () -> Float = {
        entries.maxOfOrNull { it.playcount?.toFloat() ?: 0f } ?: 0f
    },
    showArtists: Boolean,
    emptyStringRes: Int,
    shimmer: Boolean = false,
    onItemClick: (MusicEntry) -> Unit,
) {
    val maxCount by remember(entries) { mutableFloatStateOf(maxCountEvaluater()) }

    if (entries.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(emptyStringRes),
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyVerticalGrid(
            contentPadding = PaddingValues(horizontal = 24.dp),
            columns = GridCells.Adaptive(minSize = 220.dp),
            modifier = if (shimmer) Modifier.shimmer() else Modifier
        ) {
            items(entries, key = { it }) { entry ->
                MusicEntryGridItem(
                    entry,
                    forShimmer = shimmer,
                    onClick = {
                        onItemClick(entry)
                    },
                    progress = if (maxCount > 0) {
                        entry.playcount?.toFloat()?.div(maxCount) ?: 0f
                    } else
                        entry.match,
                    showArtist = showArtists,
                    modifier = Modifier
                        .width(150.dp)
                        .animateItem()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableHeaderItemPreview() {
    ExpandableHeaderItem(
        icon = Icons.Outlined.Info,
        title = "Title",
        expanded = false,
        onToggle = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ExpandableHeaderMenuPreview() {
    ExpandableHeaderMenu(
        icon = Icons.Outlined.Info,
        title = "Title",
        menuItemText = "Menu",
        onMenuClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun GoToDetailsHeaderItemPreview() {
    GoToDetailsHeaderItem(
        icon = Icons.Outlined.Info,
        title = "Title",
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun RecentsListItemPreview() {
    AppPreviewTheme {
        MusicEntryListItem(
            entry = Track(
                name = "Track Name",
                artist = Artist(name = "Artist Name"),
                album = Album(name = "Album Name"),
                userloved = true,
            ),
            imageUrlOverride = "",
            onMenuClick = {},
            onTrackClick = {},
//            showDateSeperator = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MusicEntryGridItemPreview() {
    AppPreviewTheme {
        MusicEntryGridItem(
            entry = Track(
                name = "Track Name",
                artist = Artist(name = "Artist Name"),
                album = Album(name = "Album Name"),
                userloved = true,
            ),
            showArtist = true,
            imageUrlOverride = "",
            progress = 0.5f,
            onClick = {},
        )
    }
}