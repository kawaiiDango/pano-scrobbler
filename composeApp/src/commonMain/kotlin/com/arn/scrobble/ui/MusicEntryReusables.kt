package com.arn.scrobble.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.OutlinedToggleButtonDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.size.SizeResolver
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.ChartsCount
import com.arn.scrobble.icons.Album
import com.arn.scrobble.icons.AllOut
import com.arn.scrobble.icons.ArrowRightAltAutoMirrored
import com.arn.scrobble.icons.ArrowRightAutoMirrored
import com.arn.scrobble.icons.AutoAwesomeMosaic
import com.arn.scrobble.icons.Close
import com.arn.scrobble.icons.Favorite
import com.arn.scrobble.icons.FavoriteFilled
import com.arn.scrobble.icons.FiberManualRecord
import com.arn.scrobble.icons.GraphicEq
import com.arn.scrobble.icons.GridView
import com.arn.scrobble.icons.HeartBroken
import com.arn.scrobble.icons.HourglassEmpty
import com.arn.scrobble.icons.Icons
import com.arn.scrobble.icons.Info
import com.arn.scrobble.icons.KeyboardArrowDown
import com.arn.scrobble.icons.KeyboardArrowUp
import com.arn.scrobble.icons.KeyboardDoubleArrowDown
import com.arn.scrobble.icons.KeyboardDoubleArrowUp
import com.arn.scrobble.icons.ListAutoMirrored
import com.arn.scrobble.icons.Mic
import com.arn.scrobble.icons.MoreVert
import com.arn.scrobble.icons.PlayArrow
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.panoicons.Nothing
import com.arn.scrobble.panoicons.PanoIcons
import com.arn.scrobble.panoicons.RectFilled
import com.arn.scrobble.panoicons.StonksNew
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.Stuff.format
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.album_art
import pano_scrobbler.composeapp.generated.resources.close
import pano_scrobbler.composeapp.generated.resources.collapse
import pano_scrobbler.composeapp.generated.resources.create_collage
import pano_scrobbler.composeapp.generated.resources.expand
import pano_scrobbler.composeapp.generated.resources.grid
import pano_scrobbler.composeapp.generated.resources.hate
import pano_scrobbler.composeapp.generated.resources.item_options
import pano_scrobbler.composeapp.generated.resources.legend
import pano_scrobbler.composeapp.generated.resources.list
import pano_scrobbler.composeapp.generated.resources.loved
import pano_scrobbler.composeapp.generated.resources.num_listeners
import pano_scrobbler.composeapp.generated.resources.num_scrobbles_noti
import pano_scrobbler.composeapp.generated.resources.show_all
import pano_scrobbler.composeapp.generated.resources.time_just_now
import kotlin.math.abs

enum class GridMode {
    HERO, LIST, GRID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicEntryListItem(
    entry: MusicEntry,
    onEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPending: Boolean = false,
    appItem: AppItem? = null,
    fixedImageHeight: Boolean = true,
    fetchAlbumImageIfMissing: Boolean = false,
    index: Int? = null,
    stonksDelta: Int? = null,
    progress: Float? = null,
    isColumn: Boolean = false,
    forShimmer: Boolean = false,
    imageUrlOverride: String? = null,
    onImageClick: (() -> Unit)? = null,
    menuShown: Boolean = false,
    onMenuToggle: ((Boolean) -> Unit)? = null,
    menuContent: @Composable () -> Unit = {},
) {
    val hasOnlyOneClickable = onImageClick == null && onMenuToggle == null
    var imageMemoryCacheKey by remember(entry) { mutableStateOf<MemoryCache.Key?>(null) }
    val context = LocalPlatformContext.current
    val isNowPlaying = (entry as? Track)?.isNowPlaying == true
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }

    val topText = if (entry is Track && entry.date != null)
        PanoTimeFormatter.relative(entry.date, stringResource(Res.string.time_just_now))
    else
        null

    val smallerCornerSize = if (isColumn) ZeroCornerSize else MaterialTheme.shapes.large.bottomEnd
    val contentColor = if (isNowPlaying)
        MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.primaryContainer)
    else
        LocalContentColor.current

    val artShape = if (isColumn)
        MaterialTheme.shapes.large.copy(
            bottomEnd = smallerCornerSize,
            bottomStart = smallerCornerSize
        )
    else
        MaterialTheme.shapes.medium

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

    val thirdText = when {
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

        entry is Track && entry.album?.name?.isEmpty() == false -> entry.album.name
        else -> null
    }

    RowOrColumnLayout(
        isColumnMode = isColumn,
        modifier = modifier
            .clip(
                if (isColumn)
                    MaterialTheme.shapes.large
                else
                    MaterialTheme.shapes.medium
            )
            .nowPlayingAnim(
                nowPlaying = isNowPlaying,
                colorA = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                colorB = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            )
            .then(
                if (hasOnlyOneClickable)
                    Modifier
                        .shapedClickable(
                            shape = artShape,
                            clickableAdded = !forShimmer,
                            onClick = onEntryClick
                        )
                else
                    Modifier
            )
            .padding(
                horizontal = 4.dp,
                vertical = if (!fixedImageHeight ||
                    listOfNotNull(topText, secondText, thirdText, progress).size <= 3
                )
                    4.dp // add extra space
                else
                    0.dp // the inner row is high enough
            )
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = Modifier
                    .then(
                        if (fixedImageHeight)
                            Modifier
                                .size(72.dp)
                        else if (!isColumn) // height is bounded in my use case
                            Modifier.aspectRatio(1f, true)
                        else // unbounded height, let the custom layout handle it
                            Modifier.aspectRatio(1f, false)
                    )
                    .backgroundForShimmer(forShimmer)
            ) {
                AsyncImage(
                    model = remember(
                        forShimmer,
                        entry,
                        isColumn,
                        fetchAlbumImageIfMissing
                    ) {
                        if (forShimmer)
                            null
                        else
                            ImageRequest.Builder(context)
                                .data(
                                    imageUrlOverride
                                        ?: MusicEntryImageReq(
                                            entry,
                                            accountType = accountType,
                                            isHeroImage = !fixedImageHeight,
                                            fetchAlbumInfoIfMissing = fetchAlbumImageIfMissing
                                        )
                                )
                                .placeholderMemoryCacheKey(imageMemoryCacheKey)
                                .size(SizeResolver.ORIGINAL)
                                .build()
                    },
                    fallback = placeholderImageVectorPainter(null),
                    error = if (!isPending)
                        placeholderImageVectorPainter(entry)
                    else
                        placeholderImageVectorPainter(entry, Icons.HourglassEmpty),
                    // this placeholder overrides the one set in model
                    placeholder = if (imageMemoryCacheKey != null)
                        null
                    else
                        placeholderPainter(),
                    onSuccess = {
                        imageMemoryCacheKey = it.result.memoryCacheKey
                    },
                    contentDescription = stringResource(Res.string.album_art),
                    modifier = Modifier
                        .clip(artShape)
                        .fillMaxSize()
                        .then(
                            if (onImageClick != null)
                                Modifier.shapedClickable(
                                    shape = artShape,
                                    clickableAdded = !forShimmer,
                                    onClick = onImageClick,
                                )
                            else
                                Modifier
                        )
                )

                if (entry is Track && (entry.userloved == true || entry.userHated == true)) {
                    val loveHateModifier = Modifier
                        .align(Alignment.TopEnd)
                        .rotate(11.25f)
                        .offset(x = 6.dp, y = (-6).dp)

                    Icon(
                        imageVector = if (entry.userloved == true) Icons.FavoriteFilled else Icons.HeartBroken,
                        contentDescription = stringResource(if (entry.userloved == true) Res.string.loved else Res.string.hate),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = loveHateModifier
                    )

                    Icon(
                        imageVector = Icons.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = loveHateModifier
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .padding(
                        start = if (isColumn || fixedImageHeight) 0.dp else 8.dp,
                    )
                    .then(
                        if (!fixedImageHeight)
                            Modifier.background(
                                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.07f),
                                shape = MaterialTheme.shapes.large.copy(
                                    topStart = smallerCornerSize,
                                    topEnd = smallerCornerSize
                                )
                            )
                        else
                            Modifier
                    )
                    .padding(
                        vertical = if (isColumn) 4.dp else 0.dp,
                        horizontal = if (isColumn) 4.dp else 0.dp
                    )
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 2.dp)
                        .then(
                            if (!hasOnlyOneClickable)
                                Modifier
                                    .shapedClickable(
                                        clickableAdded = !forShimmer,
                                        onClick = onEntryClick
                                    )
                            else
                                Modifier
                        )
                        .padding(vertical = 2.dp, horizontal = 6.dp)
                        .backgroundForShimmer(forShimmer)
                ) {

                    if (topText != null) {
                        Text(
                            text = if (forShimmer) "" else topText,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.End)
                                .backgroundForShimmer(forShimmer)
                        )
                    }

                    Text(
                        text = if (forShimmer)
                            ""
                        else if (index != null)
                            "${index + 1}. $firstText"
                        else
                            firstText,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (secondText != null)
                        Text(
                            text = if (forShimmer) "" else secondText,
                            style = MaterialTheme.typography.bodyLargeEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                    if (thirdText != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (stonksDelta != null) {
                                stonksIconForDelta(stonksDelta)?.let { (icon, color) ->
                                    Icon(
                                        imageVector = icon,
                                        tint = color,
                                        contentDescription = stonksDelta.toString(),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .offset((-2).dp)
                                    )
                                }
                            }

                            Text(
                                text = if (forShimmer) "" else thirdText,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (progress != null) {
                        ScrobblesCountProgress(progress)
                    }
                }

                if (onMenuToggle != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        if (entry is Track && entry.isNowPlaying) {
                            Icon(
                                imageVector = Icons.PlayArrow,
                                contentDescription = stringResource(Res.string.time_just_now),
                                modifier = Modifier
                                    .size(22.dp)
                            )
                        } else if (appItem != null) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above
                                ),
                                tooltip = { PlainTooltip { Text(appItem.friendlyLabel) } },
                                state = rememberTooltipState(),
                            ) {
                                AppIcon(
                                    appItem = appItem,
                                    modifier = Modifier
                                        .size(22.dp)
                                )
                            }
                        }

                        OutlinedIconToggleButton(
                            checked = menuShown,
                            shapes = IconButtonDefaults.toggleableShapes(),
                            onCheckedChange = onMenuToggle,
                            enabled = !forShimmer,
                            border = null,
                            colors = IconButtonDefaults.outlinedIconToggleButtonVibrantColors()
                                .let {
                                    if (isNowPlaying)
                                        it.copy(
                                            contentColor = LocalContentColor.current
                                        )
                                    else
                                        it
                                }
                        ) {
                            Icon(
                                imageVector = Icons.MoreVert,
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
fun MusicEntryGridItem(
    entry: MusicEntry,
    showArtist: Boolean,
    index: Int?,
    stonksDelta: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrlOverride: String? = null,
    fetchAlbumImageIfMissing: Boolean = false,
    isHero: Boolean = false,
    progress: Float? = null,
    forShimmer: Boolean = false,
) {
    val accountType by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.currentAccountType }
    val colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.07f)
    )

    ListItem(
        modifier = modifier.padding(8.dp),
        enabled = !forShimmer,
        onClick = onClick,
        contentPadding = PaddingValues.Zero,
        colors = colors,
        shapes = ListItemDefaults.myBigImageShapes(),
        supportingContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
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
                    " | " + PanoTimeFormatter.relative(
                        entry.date,
                        stringResource(Res.string.time_just_now)
                    )
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
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .backgroundForShimmer(forShimmer)

                )

                if (secondText != null)
                    Text(
                        text = if (forShimmer) "" else secondText,
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
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                ScrobblesCountProgress(progress)
            }
        }
    ) {
        AsyncImage(
            model = if (forShimmer)
                null
            else imageUrlOverride
                ?: MusicEntryImageReq(
                    entry,
                    accountType = accountType,
                    isHeroImage = isHero,
                    fetchAlbumInfoIfMissing = fetchAlbumImageIfMissing
                ),
            fallback = placeholderImageVectorPainter(null),
            error = placeholderImageVectorPainter(entry),
            placeholder = placeholderPainter(),
            contentDescription = stringResource(Res.string.album_art),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .backgroundForShimmer(forShimmer)
        )
    }
}

@Composable
private fun ScrobblesCountProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
) {
    // always reserve space for the progress bar
    Box(
        modifier = modifier
            .height(3.dp)
            .then(
                if (progress == null)
                    Modifier
                else
                    Modifier
                        .fillMaxWidth(progress)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            MaterialTheme.shapes.medium,
                        )
            )
    )
}

@Composable
fun ExpandableHeaderItem(
    text: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    collapsedImage: @Composable (() -> Unit)? = null,
    canExpand: Boolean = true,
) {
    if (!canExpand) {
        SimpleHeaderItem(
            text = text,
            icon = icon,
            modifier = modifier
        )

        return
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val rotationState by animateFloatAsState(
        targetValue = when {
            expanded && isLtr -> 90f
            expanded && !isLtr -> -90f
            else -> 0f
        },
    )

    val colors = ListItemDefaults.myTogglableHeaderItemColors()

    ListItem(
        enabled = canExpand,
        checked = expanded,
        onCheckedChange = onToggle,
        colors = colors,
        contentPadding = ListItemDefaults.ContentPadding.let {
            // make some room for the collapsedImage while still maintaining min interactive height
            PaddingValues(
                start = it.calculateStartPadding(LayoutDirection.Ltr),
                top = 0.dp,
                end = it.calculateEndPadding(LayoutDirection.Ltr),
                bottom = 0.dp,
            )
        },
        leadingContent = {
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Icon(
                    imageVector = Icons.ArrowRightAutoMirrored,
                    contentDescription = if (expanded && canExpand)
                        stringResource(Res.string.collapse)
                    else
                        stringResource(Res.string.expand),
                    modifier = Modifier
                        .rotate(rotationState)
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        },
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (collapsedImage != null) {
                AnimatedVisibility(
                    visible = !expanded
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                    ) {
                        collapsedImage()
                    }
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun HeaderItemWithAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector = Icons.ArrowRightAltAutoMirrored,
    trailingIconContentDescription: String = stringResource(Res.string.show_all),
    enabled: Boolean = true,
) {
    ListItem(
        enabled = enabled,
        onClick = onClick,
        colors = ListItemDefaults.myTogglableHeaderItemColors(),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        trailingContent = {
            Icon(
                imageVector = trailingIcon,
                contentDescription = trailingIconContentDescription,
                modifier = Modifier
                    .padding(end = horizontalOverscanPadding())
            )
        },
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun DismissableNotice(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
    ) {

        if (onDismiss != null) {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = onDismiss,
                modifier = Modifier
            ) {
                Icon(
                    imageVector = Icons.Close,
                    contentDescription = stringResource(Res.string.close),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .shapedClickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.ArrowRightAltAutoMirrored,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun LazyListScope.expandableSublist(
    headerText: String,
    headerIcon: ImageVector,
    items: List<MusicEntry>,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onItemClick: (MusicEntry) -> Unit,
    fetchAlbumImageIfMissing: Boolean = false,
    minItems: Int = 3,
) {
    if (items.isEmpty()) return

    stickyHeader(key = headerText) {
        ExpandableHeaderItem(
            text = headerText,
            icon = headerIcon,
            expanded = expanded || items.size <= minItems,
            canExpand = items.size > minItems,
            onToggle = onToggle,
            modifier = Modifier.animateItem()
        )
    }

    items(
        items.take(if (expanded) items.size else minItems),
        key = { it.generateKey() }
    ) { item ->

        MusicEntryListItem(
            item,
            onEntryClick = { onItemClick(item) },
            fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
            modifier = Modifier.animateItem()
        )
    }
}

@Composable
fun EntriesRow(
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

    HeaderItemWithAction(
        icon = headerIcon,
        title = title,
        enabled = !shimmer && entries.itemCount > 0,
        onClick = onHeaderClick,
    )

    if (!entries.loadState.hasError && entries.itemCount == 0 && !shimmer) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Text(
                text = stringResource(emptyStringRes),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    } else {
        PanoLazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = if (shimmer) Modifier.shimmerWindowBounds() else Modifier
        ) {
            if (shimmer) {
                items(
                    4,
                    key = { "shimmer_$it" }
                ) { idx ->
                    MusicEntryGridItem(
                        placeholderItem,
                        forShimmer = true,
                        onClick = {},
                        progress = 0f,
                        showArtist = showArtists,
                        stonksDelta = null,
                        index = null,
                        modifier = Modifier
                            .animateItem()
                            .width(minGridSize())
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
                        .width(minGridSize())
                )
            }

            if (entries.loadState.hasError) {
                val error = when {
                    entries.loadState.refresh is LoadState.Error -> entries.loadState.refresh as LoadState.Error
                    entries.loadState.append is LoadState.Error -> entries.loadState.append as LoadState.Error
                    else -> null
                }

                if (error != null) {
                    item {
                        ListLoadError(
                            modifier = Modifier
                                .animateItem()
                                .height(150.dp)
                                .fillParentMaxWidth(),
                            throwable = error.error,
                            onRetry = { entries.retry() })
                    }
                }
            }
        }
    }
}

@Composable
fun GridOrListSelector(
    gridMode: GridMode,
    onGridModeChange: (GridMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {

            if (!PlatformStuff.isTv) {
                OutlinedToggleButton(
                    checked = gridMode == GridMode.HERO,
                    onCheckedChange = {
                        if (it)
                            onGridModeChange(GridMode.HERO)
                    },
                    colors = OutlinedToggleButtonDefaults.myColors(),
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                ) {
                    Icon(
                        Icons.AllOut,
                        contentDescription = stringResource(Res.string.expand)
                    )
                }
            }

            OutlinedToggleButton(
                checked = gridMode == GridMode.GRID,
                onCheckedChange = {
                    if (it)
                        onGridModeChange(GridMode.GRID)
                },
                colors = OutlinedToggleButtonDefaults.myColors(),
                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
            ) {
                Icon(
                    Icons.GridView,
                    contentDescription = stringResource(Res.string.grid)
                )
            }

            OutlinedToggleButton(
                checked = gridMode == GridMode.LIST,
                onCheckedChange = {
                    if (it)
                        onGridModeChange(GridMode.LIST)
                },
                colors = OutlinedToggleButtonDefaults.myColors(),
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
            ) {
                Icon(
                    Icons.ListAutoMirrored,
                    contentDescription = stringResource(Res.string.list)
                )
            }
        }
    }
}

@Composable
private fun ButtonsBarForCharts(
    gridMode: GridMode,
    onCollageClick: (() -> Unit)?,
    onLegendClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        if (onLegendClick != null)
            IconButtonWithTooltip(
                onClick = onLegendClick,
                icon = Icons.Info,
                contentDescription = stringResource(Res.string.legend)
            )


        GridOrListSelector(
            gridMode = gridMode,
            onGridModeChange = { gridMode ->
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(gridMode = gridMode) }
                }
            },
            modifier = Modifier
                .weight(1f)
        )

        if (onCollageClick != null)
            IconButtonWithTooltip(
                onClick = onCollageClick,
                icon = Icons.AutoAwesomeMosaic,
                contentDescription = stringResource(Res.string.create_collage)
            )
    }
}

@Composable
fun EntriesGridOrList(
    entries: LazyPagingItems<MusicEntry>,
    onItemClick: (MusicEntry) -> Unit,

    placeholderItem: MusicEntry,
    fetchAlbumImageIfMissing: Boolean,
    showArtists: Boolean,
    emptyStringRes: StringResource,
    modifier: Modifier = Modifier,
    onCollageClick: (() -> Unit)? = null,
    onLegendClick: (() -> Unit)? = null,
    titleText: String? = null,
    maxCountEvaluater: () -> Float = {
        if (entries.itemCount > 0)
            entries.peek(0)?.playcount?.toFloat() ?: 0f
        else
            0f
//        (0 until entries.itemCount)
//            .maxOfOrNull { i -> entries.peek(i)?.playcount?.toFloat() ?: 0f }
//            ?: 0f
    },
) {
    val maxCount by remember(entries.loadState) { mutableFloatStateOf(maxCountEvaluater()) }
    val shimmer by remember(entries.loadState.refresh) { mutableStateOf(entries.loadState.refresh is LoadState.Loading) }
    val gridMode by PlatformStuff.mainPrefs.data.collectAsStateWithInitialValue { it.gridMode }

    if (!entries.loadState.hasError && entries.itemCount == 0 && !shimmer) {
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
            columns = if (gridMode == GridMode.GRID)
                GridCells.Adaptive(minSize = minGridSize())
            else
                GridCells.Fixed(1),
            modifier = modifier.fillMaxSize()
                .then(if (shimmer) Modifier.shimmerWindowBounds() else Modifier)
        ) {
            item(
                key = "buttons_bar",
                span = { GridItemSpan(maxLineSpan) }
            ) {

                ButtonsBarForCharts(
                    gridMode = gridMode,
                    onCollageClick = onCollageClick?.takeIf { !PlatformStuff.isTv },
                    onLegendClick = onLegendClick,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

            if (titleText != null) {
                item(
                    key = "charts_count",
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        ChartsCount(titleText)
                    }
                }
            }

            if (shimmer) {
                items(8) { idx ->

                    if (gridMode == GridMode.LIST) {
                        MusicEntryListItem(
                            placeholderItem,
                            forShimmer = true,
                            onEntryClick = {},
                            modifier = Modifier
                                .animateItem()
                        )
                    } else {
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
            }

            items(entries.itemCount, key = entries.itemKey()) { idx ->
                val entryNullable = entries[idx]
                val entry = entryNullable ?: Artist(" ", listeners = idx.toLong())

                if (gridMode == GridMode.LIST) {
                    MusicEntryListItem(
                        entry,
                        forShimmer = entryNullable == null,
                        onEntryClick = {
                            onItemClick(entry)
                        },
                        progress = if (maxCount > 0) {
                            entry.playcount?.toFloat()?.div(maxCount) ?: 0f
                        } else
                            entry.match,
                        stonksDelta = entry.stonksDelta,
                        fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
                        //                    showArtist = showArtists,
                        index = idx,
                        modifier = Modifier
                            .animateItem()
                    )
                } else {
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
                        isHero = gridMode == GridMode.HERO,
                        modifier = Modifier
                            .animateItem()
                    )
                }
            }

            if (entries.loadState.hasError) {
                val error = when {
                    entries.loadState.refresh is LoadState.Error -> entries.loadState.refresh as LoadState.Error
                    entries.loadState.append is LoadState.Error -> entries.loadState.append as LoadState.Error
                    else -> null
                }

                if (error != null) {
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
}

fun getMusicEntryPlaceholderItem(
    type: Int,
    showScrobbleCount: Boolean = true,
    showDate: Boolean = false,
): MusicEntry {
    val count = if (showScrobbleCount) 10L else 0L

    return when (type) {
        Stuff.TYPE_TRACKS -> Track(
            name = "Track",
            album = Album(
                name = "Album",
            ),
            artist = Artist(
                name = "Artist",
            ),
            playcount = count,
            date = if (showDate) Stuff.TIME_2002 else null
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
    delta in 1..5 -> Icons.KeyboardArrowUp to MaterialTheme.colorScheme.tertiary
    delta > 5 -> Icons.KeyboardDoubleArrowUp to MaterialTheme.colorScheme.tertiary
    delta in -1 downTo -5 -> Icons.KeyboardArrowDown to MaterialTheme.colorScheme.secondary
    delta < -5 -> Icons.KeyboardDoubleArrowDown to MaterialTheme.colorScheme.secondary
    delta == 0 -> Icons.FiberManualRecord to MaterialTheme.colorScheme.outline
    else -> null
}

fun MusicEntry.generateKey(): String {
    val sep = "\u001F"

    val str = when (this) {
        is Track -> "Track" + sep + date + sep + artist.name + sep + album?.name + sep + name
        is Album -> "Album" + sep + artist?.name + sep + name
        is Artist -> "Artist" + sep + name
    }

    return str
}

fun MusicEntry?.colorSeed(): Int {
    val str = this?.generateKey()
    return str.hashCode()
}

@Composable
fun placeholderImageVectorPainter(
    musicEntry: MusicEntry?,
    imageVector: ImageVector = when (musicEntry) {
        is Artist -> Icons.Mic
        is Album -> Icons.Album
        is Track -> if (musicEntry.album != null)
            Icons.Album
        else
            Icons.GraphicEq

        else -> PanoIcons.Nothing
    },
    scaleFactor: Float = 0.6f,
): Painter {
    val containerColors = LocalThemeAttributes.current.avatarContainerColors
    val containerColor = containerColors[abs(musicEntry.colorSeed()) % containerColors.size]
        .copy(alpha = 0.6f)

    val colors = LocalThemeAttributes.current.avatarColors
    val color = colors[abs(musicEntry.colorSeed()) % colors.size].copy(alpha = 0.5f)

    val bg = rememberVectorPainter(PanoIcons.RectFilled)
    val fg = rememberVectorPainter(imageVector)

    return remember(bg, fg, containerColor, color, scaleFactor) {
        CombinedVectorPainter(
            background = bg,
            backgroundTint = containerColor,
            foreground = fg,
            foregroundTint = color,
            foregroundScale = scaleFactor,
        )
    }
}

private class CombinedVectorPainter(
    private val background: VectorPainter,
    private val backgroundTint: Color,
    private val foreground: VectorPainter,
    private val foregroundTint: Color,
    private val foregroundScale: Float,
) : Painter() {

    override val intrinsicSize: Size
        get() = background.intrinsicSize

    override fun DrawScope.onDraw() {
        with(background) {
            draw(size, colorFilter = ColorFilter.tint(backgroundTint))
        }

        val fgSize = size * foregroundScale
        val offset = Offset(
            x = (size.width - fgSize.width) / 2f,
            y = (size.height - fgSize.height) / 2f
        )

        translate(left = offset.x, top = offset.y) {
            with(foreground) {
                draw(fgSize, colorFilter = ColorFilter.tint(foregroundTint))
            }
        }
    }
}