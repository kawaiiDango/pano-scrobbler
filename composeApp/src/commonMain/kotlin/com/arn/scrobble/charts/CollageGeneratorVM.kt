package com.arn.scrobble.charts

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import coil3.toBitmap
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.ui.colorSeed
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.charts_num_text
import pano_scrobbler.composeapp.generated.resources.share_sig
import pano_scrobbler.composeapp.generated.resources.top_albums
import pano_scrobbler.composeapp.generated.resources.top_artists
import pano_scrobbler.composeapp.generated.resources.top_tracks
import kotlin.math.abs
import kotlin.math.max

class CollageGeneratorVM : ViewModel() {

    private val _sharableCollage = MutableSharedFlow<Pair<ImageBitmap, String>>()
    val sharableCollage = _sharableCollage.asSharedFlow()
    private val _progress = MutableStateFlow(1f)
    val progress = _progress.asStateFlow()
    private var maxProgress = 0f
    private val _errorText = MutableStateFlow<String?>(null)
    val errorText = _errorText.asSharedFlow()
    private lateinit var context: PlatformContext

    private val paddingPx = 16
    private val isPro = PlatformStuff.billingRepository.isLicenseValid

    fun generateCollage(
        context: PlatformContext,
        type: Int,
        size: Int,
        timePeriod: TimePeriod,
        user: UserCached,
        captions: Boolean,
        skipMissing: Boolean,
        username: Boolean,
        borders: Boolean,
        textMeasurer: TextMeasurer,
        iconPainters: IconPaintersForCollage,
    ) {
        this.context = context
        viewModelScope.launch {
            _generateCollage(
                type = type,
                size = size,
                timePeriod = timePeriod,
                user = user,
                captions = captions,
                username = username,
                skipMissing = skipMissing,
                borders = borders,
                textMeasurer = textMeasurer,
                iconPainters = iconPainters,
            )
        }
    }

    private fun incrementProgress() {
        _progress.value += (1f / maxProgress)
    }

    private suspend fun _generateCollage(
        type: Int,
        size: Int,
        timePeriod: TimePeriod,
        user: UserCached,
        captions: Boolean,
        username: Boolean,
        skipMissing: Boolean,
        borders: Boolean,
        textMeasurer: TextMeasurer,
        iconPainters: IconPaintersForCollage,
    ) {

        _progress.value = 0f
        _errorText.value = null

        val cols = size
        val rows = if (type == Stuff.TYPE_ALL) 1 else size

        val results =
            if (type == Stuff.TYPE_ALL) { // digest
                maxProgress = 3f * size + 3f

                incrementProgress()
                listOf(
                    Stuff.TYPE_ARTISTS,
                    Stuff.TYPE_ALBUMS,
                    Stuff.TYPE_TRACKS
                ).mapConcurrently(3) {
                    if (_errorText.value != null)
                        return@mapConcurrently Result.success(emptyList())

                    fetchCharts(it, timePeriod, user.name).map { it.entries }
                }

            } else {
                maxProgress = (size * size).toFloat() + 1f

                incrementProgress()
                listOf(
                    fetchCharts(type, timePeriod, user.name).map { it.entries }
                )
            }

        if (_errorText.value != null)
            return


        val collages = results.map { entries ->
            fetchAndDrawImages(
                musicEntries = entries.getOrThrow(),
                cols = cols,
                rows = rows,
                skipMissing = skipMissing,
                captions = captions,
                borders = borders,
                textMeasurer = textMeasurer,
                artistPlaceholder = iconPainters.artist,
                albumPlaceholder = iconPainters.album,
                trackPlaceholder = iconPainters.track,
                placeholderColors = iconPainters.colors
            )
        }

        val image = addHeaderAndFooter(
            collages,
            type,
            timePeriod,
            user.name.takeIf { username },
            textMeasurer,
            iconPainters.app,
            iconPainters.user,
        )
        _progress.value = 1f

        var shareTitle = timePeriod.name
        if (username)
            shareTitle += " • ${user.name}"

        if (type != Stuff.TYPE_ALL) {
            val topType = when (type) {
                Stuff.TYPE_ARTISTS -> getString(Res.string.top_artists)
                Stuff.TYPE_ALBUMS -> getString(Res.string.top_albums)
                Stuff.TYPE_TRACKS -> getString(Res.string.top_tracks)
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
            shareTitle = "$topType • $shareTitle"
        }

        shareTitle += "\n\n"

        val shareSig = if (!isPro) "\n\n" + getString(Res.string.share_sig) else ""

        val shareBody = if (type == Stuff.TYPE_ALL)
            createDigestShareText(results.map { it.getOrThrow() })
        else
            createSpecificCollageShareText(results.first().getOrThrow())

        _sharableCollage.emit(image to (shareTitle + shareBody + shareSig))
    }

    private suspend fun fetchProfilePic(user: UserCached, sizePx: Int): ImageBitmap? {
        // load profile pic
        val profilePicUrl = if (user.isSelf)
            PlatformStuff.mainPrefs.data.map { it.drawerData[Scrobblables.currentAccount.value?.type]?.profilePicUrl?.ifEmpty { null } }
                .first()
        else
            user.largeImage

        val profilePicRequest = ImageRequest.Builder(context).apply {
            data(profilePicUrl)
            crossfade(false)
            placeholder(null)
        }.build()

        return withContext(Dispatchers.IO) {
            SingletonImageLoader.get(context).execute(profilePicRequest).image
                ?.toBitmap(width = sizePx, height = sizePx)
                ?.asImageBitmap()
//                ?: Icons.Default.Person.asComposeImageBitmap()
        }
    }

    private fun DrawScope.drawTextFillAndStroke(
        textLayoutResult: TextLayoutResult,
        topLeft: Offset = Offset.Zero
    ) {
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = topLeft,
            color = Color.Black,
            drawStyle = Stroke(2f),
            shadow = Shadow(
                color = Color.Black,
                offset = Offset(0f, 0f),
                blurRadius = 5f
            )
        )
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = topLeft,
            color = Color.White,
            drawStyle = Fill,
            shadow = Shadow(
                color = Color.Black,
                offset = Offset(0f, 0f),
                blurRadius = 5f
            )
        )
    }


    private suspend fun fetchAndDrawImages(
        musicEntries: List<MusicEntry>,
        cols: Int,
        rows: Int,
        skipMissing: Boolean,
        captions: Boolean,
        borders: Boolean,
        textMeasurer: TextMeasurer,
        artistPlaceholder: Painter,
        albumPlaceholder: Painter,
        trackPlaceholder: Painter,
        placeholderColors: List<Color>,
    ): ImageBitmap {
        val limit = cols * rows
        val cellSize = 300
        val cellPadding = if (borders) paddingPx else 0
        val cornerRadius = if (borders) paddingPx.toFloat() else 0f
        val bitmap = ImageBitmap(
            cols * cellSize + cellPadding * (cols + 1),
            rows * cellSize + cellPadding * (rows + 1),
            hasAlpha = false
        )
        val canvas = Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        val entriesPlaced = mutableListOf<MusicEntry>()

        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
        ) {
            for (entry in musicEntries) {
                if (entriesPlaced.size >= limit)
                    break

                val request = ImageRequest.Builder(context).apply {
                    data(
                        MusicEntryImageReq(
                            entry,
                            fetchAlbumInfoIfMissing = (entry is Album && entry.webp300 == null) || (entry is Track && entry.album == null)
                        )
                    )

                    crossfade(false)
                    size(cellSize, cellSize)
                    scale(Scale.FIT)
                    placeholder(null)
                    listener(
                        onError = { req, err ->
                            Logger.e("coil", err.throwable)
                        }
                    )
                }.build()
                var scaledWidth = cellSize
                var scaledHeight = cellSize
                var imgOffsetX = 0
                var imgOffsetY = 0

                val artwork = withContext(Dispatchers.IO) {
                    val img = SingletonImageLoader.get(context).execute(request).image
                        ?: return@withContext null

                    // scale the image to fit, maintaining aspect ratio
                    // scale it up to cellSize if it's smaller but do not crop
                    // also center it in the cell

                    val aspectRatio = img.width.toFloat() / img.height
                    if (aspectRatio > 1) {
                        scaledWidth = cellSize
                        scaledHeight = (cellSize / aspectRatio).toInt()
                    } else {
                        scaledWidth = (cellSize * aspectRatio).toInt()
                        scaledHeight = cellSize
                    }

                    imgOffsetX = (cellSize - scaledWidth) / 2
                    imgOffsetY = (cellSize - scaledHeight) / 2

                    img.toBitmap().asImageBitmap()
                }

                val x =
                    (entriesPlaced.size % cols) * cellSize.toFloat() + cellPadding * (entriesPlaced.size % cols + 1)
                val y =
                    (entriesPlaced.size / cols) * cellSize.toFloat() + cellPadding * (entriesPlaced.size / cols + 1)

                incrementProgress()

                // draw image

                if ((skipMissing || !captions) && artwork == null)
                    continue

                entriesPlaced += entry

//                val cellImage = artwork ?: ContextCompat.getDrawable(
//                    context,
//                    Res.drawable.vd_wave_simple_filled
//                )!!.toBitmap(width = cellSize, height = cellSize)
//                    .asImageBitmap()

                clipPath(Path().apply {
                    addRoundRect(
                        RoundRect(
                            Rect(
                                offset = Offset(x + imgOffsetX, y + imgOffsetY),
                                size = Size(cellSize.toFloat(), cellSize.toFloat())
                            ),
                            CornerRadius(cornerRadius, cornerRadius)
                        )
                    )
                }) {
                    if (artwork != null) {
                        drawImage(
                            image = artwork,
                            dstOffset = IntOffset(x.toInt() + imgOffsetX, y.toInt() + imgOffsetY),
                            dstSize = IntSize(scaledWidth, scaledHeight)
                        )
                    } else {
                        val placeholderIcon = when (entry) {
                            is Track -> trackPlaceholder
                            is Album -> albumPlaceholder
                            is Artist -> artistPlaceholder
                        }

                        val color =
                            placeholderColors[abs(entry.colorSeed()) % placeholderColors.size]

                        with(placeholderIcon) {
                            translate(
                                left = x + imgOffsetX,
                                top = y + imgOffsetY,
                            ) {
                                draw(
                                    size = Size(cellSize.toFloat(), cellSize.toFloat()),
                                    colorFilter = ColorFilter.tint(color)
                                )
                            }
                        }
                    }
                }

                if (!captions)
                    continue


                // draw text
                val name = entry.name
                val artist = when (entry) {
                    is Track -> entry.artist.name
                    is Album -> entry.artist!!.name
                    else -> null
                }
                val scrobbleCount = "${entry.playcount} ▶"

                var textY = y + cellSize - paddingPx

                var style: TextStyle

                var tlr: TextLayoutResult
                if (artist != null) {
                    style = TextStyle.Default.copy(fontSize = 17.sp)
                    tlr = measureText(
                        textMeasurer,
                        artist,
                        style,
                        constraints = Constraints(maxWidth = cellSize - 2 * paddingPx)
                    )
                    textY -= tlr.size.height
                    drawTextFillAndStroke(
                        textLayoutResult = tlr,
                        topLeft = Offset(x + paddingPx, textY),
                    )
                }

                style = TextStyle.Default.copy(fontSize = 18.sp, fontWeight = FontWeight(700))
                tlr = measureText(
                    textMeasurer,
                    name,
                    style,
                    constraints = Constraints(maxWidth = cellSize - 2 * paddingPx)
                )
                textY -= tlr.size.height
                drawTextFillAndStroke(
                    tlr,
                    topLeft = Offset(x + paddingPx, textY),
                )

                style = TextStyle.Default.copy(fontSize = 16.sp)
                tlr = measureText(
                    textMeasurer,
                    scrobbleCount,
                    style,
                    constraints = Constraints(maxWidth = cellSize - 2 * paddingPx)
                )
                textY -= tlr.size.height
                drawTextFillAndStroke(
                    tlr,
                    topLeft = Offset(x + paddingPx, textY),
                )

            }
        }

        return bitmap
    }

    private fun measureText(
        textMeasurer: TextMeasurer,
        text: String,
        style: TextStyle,
        maxLines: Int = 1,
        constraints: Constraints = Constraints(),
    ) = textMeasurer.measure(
        text = text,
        style = style,
        maxLines = maxLines,
        density = Density(1f),
        softWrap = maxLines != 1,
        layoutDirection = LayoutDirection.Ltr,
        constraints = constraints
    )

    private fun findFooterTextSize(text: String, width: Float, textMeasurer: TextMeasurer): Int {
        val step = 4
        var fontSize = 42
        var overflowed = true
        while (overflowed) {
            val tlr = measureText(
                textMeasurer,
                text,
                TextStyle.Default.copy(
                    fontSize = fontSize.sp,
                ),
                constraints = Constraints(maxWidth = width.toInt())
            )
            overflowed = tlr.didOverflowWidth
            if (overflowed)
                fontSize -= step
        }

        return fontSize
    }

    private suspend fun addHeaderAndFooter(
        mosaics: List<ImageBitmap>,
        type: Int,
        timePeriod: TimePeriod,
        username: String?,
        textMeasurer: TextMeasurer,
        appIcon: Painter,
        userIcon: Painter,
    ): ImageBitmap {

        val artistsStr = getString(Res.string.top_artists)
        val albumsStr = getString(Res.string.top_albums)
        val tracksStr = getString(Res.string.top_tracks)

        val collageTypeString = when (type) {
            Stuff.TYPE_ARTISTS -> artistsStr
            Stuff.TYPE_ALBUMS -> albumsStr
            Stuff.TYPE_TRACKS -> tracksStr
            else -> null
        }

        val footerText = collageTypeString?.let { "$it • " }.orEmpty() +
                timePeriod.name +
                username?.let { " • $it" }.orEmpty()

        val headerTexts = if (type == Stuff.TYPE_ALL) {
            arrayOf(
                artistsStr,
                albumsStr,
                tracksStr
            )
        } else {
            arrayOf()
        }

        val totalWidth = mosaics.first().width

        val brandText = "Pano\nScrobbler"

        val brandTextPadding = 32


        var brandTextWidth = if (isPro) 0 else measureText(
            textMeasurer,
            brandText,
            TextStyle.Default.copy(fontSize = 21.sp),
            2
        ).size.width

        val footerTextSize =
            findFooterTextSize(
                footerText,
                totalWidth - brandTextWidth - brandTextPadding - 2 * paddingPx.toFloat(),
                textMeasurer
            )


        val titleStyle = TextStyle.Default.copy(
            color = Color.White,
            fontSize = footerTextSize.sp,
            fontWeight = FontWeight.Medium
        )

        val brandTlr = measureText(
            textMeasurer,
            brandText,
            style = titleStyle.copy(fontSize = titleStyle.fontSize / 2.25),
            maxLines = 2
        )

        brandTextWidth = if (isPro) 0 else brandTlr.size.width

        val extraHeights =
            headerTexts.map {
                measureText(textMeasurer, it, titleStyle).size.height
            } + max(
                measureText(textMeasurer, footerText, titleStyle).size.height,
                brandTlr.size.height
            )

        val totalHeight = mosaics.size * mosaics.first().height +
                extraHeights.sum() + extraHeights.size * 2 * paddingPx
        var offsetY = 0f

        val bitmap = ImageBitmap(totalWidth, totalHeight, hasAlpha = false)
        val canvas = Canvas(bitmap)

        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(totalWidth.toFloat(), totalHeight.toFloat()),
        ) {
            // digest collage
            if (type == Stuff.TYPE_ALL) {
                if (mosaics.size != 3)
                    return@draw

                offsetY = paddingPx.toFloat()

                for (i in mosaics.indices) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = headerTexts[i],
                        topLeft = Offset(paddingPx.toFloat(), offsetY),
                        style = titleStyle,
                        maxLines = 1,
                    )

                    offsetY += measureText(
                        textMeasurer,
                        headerTexts[i],
                        titleStyle
                    ).size.height + paddingPx

                    drawImage(mosaics[i], Offset(0f, offsetY))

                    offsetY += mosaics[i].height + paddingPx
                }


            } else { // specific collage
                if (mosaics.size != 1)
                    return@draw

                drawImage(mosaics.first(), Offset(0f, offsetY))

                offsetY += mosaics.first().height + paddingPx
            }

            drawText(
                textMeasurer = textMeasurer,
                text = footerText,
                topLeft = Offset(paddingPx.toFloat(), offsetY),
                style = titleStyle,
                maxLines = 1,
                size = Size(totalWidth.toFloat() - 2 * paddingPx - brandTextWidth, 100f)
            )

            if (!isPro || PlatformStuff.isDebug) {

                val appIconScaledSize = brandTlr.size.height

                with(appIcon) {
                    translate(
                        left = (totalWidth - brandTlr.size.width - appIconScaledSize - brandTextPadding / 2 - paddingPx).toFloat(),
                        top = offsetY
                    ) {
                        draw(
                            size = Size(
                                appIconScaledSize.toFloat(),
                                appIconScaledSize.toFloat()
                            ),
                            colorFilter = ColorFilter.tint(
                                color = Color(0xFFFFB1C7) // pinkPrimary
                            )
                        )
                    }
                }

                drawText(
                    brandTlr,
                    topLeft = Offset(
                        totalWidth - brandTlr.size.width - paddingPx.toFloat(),
                        offsetY
                    ),
                )
            }
        }

        return bitmap
    }

    private suspend fun fetchCharts(
        type: Int,
        timePeriod: TimePeriod,
        username: String,
    ): Result<PageResult<out MusicEntry>> {
        return Scrobblables.current!!.getCharts(
            type,
            timePeriod,
            1,
            username
        ).onFailure {
            _errorText.emit(it.redactedMessage)
        }
    }

    private suspend fun createSpecificCollageShareText(musicEntries: List<MusicEntry>): String {
        if (musicEntries.isEmpty()) return ""
        var pos = 1
        val list = musicEntries.take(10).map {
            when (it) {
                is Track -> getString(
                    Res.string.charts_num_text,
                    pos++,
                    Stuff.formatBigHyphen(it.artist.name, it.name)
                )

                is Album -> getString(
                    Res.string.charts_num_text,
                    pos++,
                    Stuff.formatBigHyphen(it.artist!!.name, it.name)
                )

                else -> getString(Res.string.charts_num_text, pos++, it.name)
            }
        }.joinToString(separator = "\n")

        return list
    }

    private suspend fun createDigestShareText(musicEntriesAll: List<List<MusicEntry>>): String {
        val shareTextList = mutableListOf<String>()
        val resultsMap = mapOf(
            Res.string.top_artists to musicEntriesAll[0],
            Res.string.top_albums to musicEntriesAll[1],
            Res.string.top_tracks to musicEntriesAll[2],
        )
        resultsMap.forEach { (titleRes, musicEntries) ->

            val title = getString(titleRes)
            val text = musicEntries.take(3).joinToString { it.name }
            shareTextList += "$title:\n$text"
        }

        return shareTextList.joinToString("\n\n")
    }

}