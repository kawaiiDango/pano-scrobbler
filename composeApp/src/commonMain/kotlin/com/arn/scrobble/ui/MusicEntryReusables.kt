package com.arn.scrobble.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowRightAlt
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AllOut
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedToggleButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSupported
import androidx.compose.ui.graphics.vector.Group
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.icons.Nothing
import com.arn.scrobble.icons.PanoIcons
import com.arn.scrobble.icons.RectFilledTranslucent
import com.arn.scrobble.icons.StonksNew
import com.arn.scrobble.imageloader.MusicEntryImageReq
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    onMenuClick: (() -> Unit)? = null,
    menuContent: @Composable () -> Unit = {},
) {
    val hasOnlyOneClickable = onImageClick == null && onMenuClick == null
    var imageMemoryCacheKey by remember(entry) { mutableStateOf<MemoryCache.Key?>(null) }
    val context = LocalPlatformContext.current

    @Composable
    fun RowOrColumn(
        modifier: Modifier = Modifier,
        content: @Composable (Modifier) -> Unit,
    ) {
        if (isColumn) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier
            ) {
                content(
                    Modifier.weight(1f)
                )

                Spacer(
                    modifier = Modifier
                        .safeContentPadding()
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
            ) {
                content(Modifier)
            }
        }
    }

    NowPlayingSurface(
        nowPlaying = (entry as? Track)?.isNowPlaying == true,
    ) {
        RowOrColumn(
            modifier = modifier.then(
                if (hasOnlyOneClickable)
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(enabled = !forShimmer) { onEntryClick() }
                else
                    Modifier
            )
                .padding(8.dp)
        ) { weightModifier ->

            BoxWithConstraints(
                modifier = weightModifier
            ) {
                val boxSize = min(maxWidth, maxHeight)

                Box(
                    modifier = Modifier
                        .then(
                            if (fixedImageHeight)
                                Modifier
                                    .size(72.dp)
                            else
                                Modifier.size(boxSize)
                        )
                        .backgroundForShimmer(forShimmer)
                ) {
                    AsyncImage(
                        model = remember(forShimmer, entry, isColumn, fetchAlbumImageIfMissing) {
                            if (forShimmer)
                                null
                            else
                                ImageRequest.Builder(context)
                                    .data(
                                        imageUrlOverride
                                            ?: MusicEntryImageReq(
                                                entry,
                                                isHeroImage = !fixedImageHeight,
                                                fetchAlbumInfoIfMissing = fetchAlbumImageIfMissing
                                            )
                                    )
                                    .placeholderMemoryCacheKey(imageMemoryCacheKey)
                                    .listener(
                                        onSuccess = { req, res ->
                                            imageMemoryCacheKey = res.memoryCacheKey
                                        }
                                    )
                                    .build()
                        },
                        fallback = placeholderImageVectorPainter(null),
                        error = if (!isPending)
                            placeholderImageVectorPainter(entry)
                        else
                            placeholderImageVectorPainter(entry, Icons.Outlined.HourglassEmpty),
                        // this placeholder overrides the one set in model
                        placeholder = if (imageMemoryCacheKey != null)
                            null
                        else
                            placeholderPainter(),
                        contentDescription = stringResource(Res.string.album_art),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium)
                            .then(if (onImageClick != null) Modifier.clickable(enabled = !forShimmer) { onImageClick() } else Modifier)
                    )

                    if (entry is Track && (entry.userloved == true || entry.userHated == true)) {
                        val loveHateModifier = Modifier
                            .align(Alignment.TopEnd)
                            .rotate(11.25f)
                            .offset(x = 6.dp, y = (-6).dp)

                        Icon(
                            imageVector = if (entry.userloved == true) Icons.Outlined.Favorite else Icons.Outlined.HeartBroken,
                            contentDescription = stringResource(if (entry.userloved == true) Res.string.loved else Res.string.hate),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = loveHateModifier
                        )

                        Icon(
                            imageVector = Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = loveHateModifier
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .widthIn(200.dp)
                    .then(
                        if (isColumn)
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.medium
                            )
                        else
                            Modifier
                    )

            ) {
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
                    val topText = if (entry is Track && entry.date != null)
                        PanoTimeFormatter.relative(entry.date)
                    else
                        null

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

                    if (topText != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .align(Alignment.End)
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
                                text = if (forShimmer) "" else topText,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
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

                    if (thirdText != null)
                        Text(
                            text = if (forShimmer) "" else thirdText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                    if (progress != null) {
                        ScrobblesCountProgress(progress)
                    }
                }

                if (onMenuClick != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (entry is Track && entry.isNowPlaying) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(Res.string.time_just_now),
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (appItem != null) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
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
private fun NowPlayingSurface(
    nowPlaying: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (nowPlaying) MaterialTheme.colorScheme.secondaryContainer else Color.Unspecified,
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .then(
                if (nowPlaying && BlendMode.Overlay.isSupported()) {

                    val infiniteTransition = rememberInfiniteTransition()
                    val progress by infiniteTransition.animateFloat(
                        initialValue = -1f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(20000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    val spotlightColor = MaterialTheme.colorScheme.inverseSurface
                    var brushRadius by remember { mutableFloatStateOf(Float.POSITIVE_INFINITY) }

                    val brush = remember(spotlightColor, brushRadius) {
                        Brush.radialGradient(
                            listOf(spotlightColor, Color.Transparent),
                            radius = brushRadius
                        )
                    }

                    Modifier.drawWithContent {
                        drawContent()

                        brushRadius = size.maxDimension

                        translate(progress * size.width, size.height / 2) {
                            drawCircle(
                                brush = brush,
                                radius = size.maxDimension,
                                blendMode = BlendMode.Overlay,
                            )
                        }
                    }
                } else
                    Modifier
            )
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                    style = MaterialTheme.typography.bodyMedium,
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

            ScrobblesCountProgress(progress)
        }
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

@Composable
fun ExpandableHeaderItem(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
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
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
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
    fetchAlbumImageIfMissing: Boolean = false,
    minItems: Int = 3,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    stickyHeader(key = headerText) {
        Surface(
            shape = MaterialTheme.shapes.large,
            shadowElevation = 4.dp,
            tonalElevation = 4.dp,
        ) {
            ExpandableHeaderItem(
                title = headerText,
                icon = headerIcon,
                expanded = expanded || items.size <= minItems,
                enabled = items.size > minItems,
                onToggle = onToggle,
                modifier = modifier.animateItem(),
            )
        }
    }

    items(
        items.take(if (expanded) items.size else minItems),
        key = { it.hashCode() }
    ) { item ->
        val musicEntry = transformToMusicEntry(item)

        MusicEntryListItem(
            musicEntry,
            onEntryClick = { onItemClick(musicEntry) },
            fetchAlbumImageIfMissing = fetchAlbumImageIfMissing,
            modifier = modifier.animateItem(),
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
                            .width(minGridSize())
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
                        .width(minGridSize())
                        .animateItem()
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
                            modifier = Modifier.animateItem(),
                            throwable = error.error,
                            onRetry = { entries.retry() })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            OutlinedToggleButton(
                checked = gridMode == GridMode.HERO,
                onCheckedChange = {
                    if (it)
                        onGridModeChange(GridMode.HERO)
                },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
            ) {
                Icon(
                    Icons.Outlined.AllOut,
                    contentDescription = stringResource(Res.string.expand)
                )
            }

            OutlinedToggleButton(
                checked = gridMode == GridMode.GRID,
                onCheckedChange = {
                    if (it)
                        onGridModeChange(GridMode.GRID)
                },
                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
            ) {
                Icon(
                    Icons.Outlined.GridView,
                    contentDescription = stringResource(Res.string.grid)
                )
            }

            OutlinedToggleButton(
                checked = gridMode == GridMode.LIST,
                onCheckedChange = {
                    if (it)
                        onGridModeChange(GridMode.LIST)
                },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.List,
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
                icon = Icons.Outlined.Info,
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
                icon = Icons.Outlined.AutoAwesomeMosaic,
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
            columns = if (gridMode == GridMode.GRID)
                GridCells.Adaptive(minSize = minGridSize())
            else
                GridCells.Fixed(1),
            modifier = modifier.fillMaxSize()
                .then(if (shimmer) Modifier.shimmerWindowBounds() else Modifier)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {

                ButtonsBarForCharts(
                    gridMode = gridMode,
                    onCollageClick = onCollageClick,
                    onLegendClick = onLegendClick,
                    modifier = Modifier
                        .fillMaxWidth()
                )
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

fun getMusicEntryPlaceholderItem(type: Int, showScrobbleCount: Boolean = true): MusicEntry {
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

fun MusicEntry.generateKey(): String {
    val str = when (this) {
        is Track -> "" + date + "\n" + artist.name + "\n" + album?.name + "\n" + name
        is Album -> artist?.name + "\n" + name
        is Artist -> name
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
        is Artist -> Icons.Rounded.Mic
        is Album -> Icons.Outlined.Album
        is Track -> if (musicEntry.album != null)
            Icons.Outlined.Album
        else
            Icons.Rounded.GraphicEq

        else -> PanoIcons.Nothing
    },
    scaleFactor: Float = 0.6f,
): VectorPainter {

    val colors = LocalThemeAttributes.current.allSecondaryContainerColors
    val color = colors[abs(musicEntry.colorSeed()) % colors.size]

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
        RenderVectorGroup(group = PanoIcons.RectFilledTranslucent.root)
        Group(
            name = imageVector.root.name + "_scaled",
            scaleX = scaleFactor,
            scaleY = scaleFactor,
            translationX = (viewportWidth - imageVector.viewportWidth * scaleFactor) / 2,
            translationY = (viewportHeight - imageVector.viewportHeight * scaleFactor) / 2,
        ) {
            RenderVectorGroup(group = imageVector.root)
        }
    }
}