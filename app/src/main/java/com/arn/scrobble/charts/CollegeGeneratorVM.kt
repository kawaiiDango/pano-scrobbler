package com.arn.scrobble.charts

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import coil3.toBitmap
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.ui.MusicEntryImageReq
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollageGeneratorVM : ViewModel() {

    private val _sharableCollage = MutableSharedFlow<Pair<ImageBitmap, String>>()
    val sharableCollage = _sharableCollage.asSharedFlow()
    private val _progress = MutableStateFlow(1f)
    val progress = _progress.asStateFlow()
    private var maxProgress = 0f
    private val _errorText = MutableStateFlow<String?>(null)
    val errorText = _errorText.asSharedFlow()
    private val captionStyle = TextStyle.Default.copy(
        color = Color.White,
        fontSize = 18.sp,
        shadow = Shadow(
            color = Color.Black,
            offset = Offset(2f, 2f),
            blurRadius = 2f
        )
    )

    private val paddingPx = 16
    private val context = PlatformStuff.application
    private val isPro = false // Stuff.billingRepository.isLicenseValid

    fun generateCollage(
        type: Int,
        size: Int,
        timePeriod: TimePeriod,
        user: UserCached,
        captions: Boolean,
        skipMissing: Boolean,
        username: Boolean,
        textMeasurer: TextMeasurer
    ) {
        viewModelScope.launch {
            _generateCollage(
                type,
                size,
                timePeriod,
                user,
                captions,
                username,
                skipMissing,
                textMeasurer
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
        textMeasurer: TextMeasurer
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
                entries.getOrThrow(),
                cols,
                rows,
                skipMissing,
                captions,
                textMeasurer
            )
        }

        val image = addHeaderAndFooter(
            collages,
            type,
            timePeriod,
            user.name.takeIf { username },
            textMeasurer
        )
        _progress.value = 1f

        var shareTitle = timePeriod.name
        if (username)
            shareTitle += " • ${user.name}"

        if (type != Stuff.TYPE_ALL) {
            val topType = when (type) {
                Stuff.TYPE_ARTISTS -> context.getString(R.string.top_artists)
                Stuff.TYPE_ALBUMS -> context.getString(R.string.top_albums)
                Stuff.TYPE_TRACKS -> context.getString(R.string.top_tracks)
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
            shareTitle = "$topType • $shareTitle"
        }

        shareTitle += "\n\n"

        val shareSig = if (!isPro) "\n\n" + context.getString(R.string.share_sig) else ""

        val shareBody = if (type == Stuff.TYPE_ALL)
            createDigestShareText(results.map { it.getOrThrow() })
        else
            createSpecificCollageShareText(results.first().getOrThrow())

        _sharableCollage.emit(image to (shareTitle + shareBody + shareSig))
    }

    private suspend fun fetchProfilePic(user: UserCached, sizePx: Int): ImageBitmap {
        // load profile pic
        val profilePicUrl = if (user.isSelf)
            PlatformStuff.mainPrefs.data.map { it.drawerData[Scrobblables.current.value!!.userAccount.type]?.profilePicUrl?.ifEmpty { null } }
                .first()
        else
            user.largeImage

        val profilePicRequest = ImageRequest.Builder(context).apply {
            data(profilePicUrl ?: R.drawable.vd_user)
            crossfade(false)
            placeholder(null)
        }.build()

        return withContext(Dispatchers.IO) {
            context.imageLoader.execute(profilePicRequest).image!!
                .toBitmap(width = sizePx, height = sizePx)
                .asImageBitmap()
        }
    }

    private suspend fun fetchAndDrawImages(
        musicEntries: List<MusicEntry>,
        cols: Int,
        rows: Int,
        skipMissing: Boolean,
        captions: Boolean,
        textMeasurer: TextMeasurer
    ): ImageBitmap {
        val limit = cols * rows
        val cellSize = 300
        val textSize =
            Size(cellSize.toFloat() - 2 * paddingPx, 100f) // height is no-op in this case
        val bitmap = ImageBitmap(cols * cellSize, rows * cellSize, hasAlpha = false)
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
                }.build()
                var scaledWidth = cellSize
                var scaledHeight = cellSize
                var imgOffsetX = 0
                var imgOffsetY = 0

                val artwork = withContext(Dispatchers.IO) {
                    val img = context.imageLoader.execute(request).image
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

                val x = (entriesPlaced.size % cols) * cellSize.toFloat()
                val y = (entriesPlaced.size / cols) * cellSize.toFloat()

                incrementProgress()

                // draw image

                if ((skipMissing || !captions) && artwork == null)
                    continue

                entriesPlaced += entry

                val cellImage = artwork ?: ContextCompat.getDrawable(
                    context,
                    R.drawable.vd_wave_simple_filled
                )!!.toBitmap(width = cellSize, height = cellSize)
                    .asImageBitmap()

                drawImage(
                    image = cellImage,
                    dstOffset = IntOffset(x.toInt() + imgOffsetX, y.toInt() + imgOffsetY),
                    dstSize = IntSize(scaledWidth, scaledHeight)
                )

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

                if (artist != null) {
                    style = captionStyle.copy(fontSize = 17.sp)
                    textY -= measureText(
                        textMeasurer,
                        scrobbleCount,
                        style
                    ).size.height
                    drawText(
                        textMeasurer,
                        text = artist,
                        topLeft = Offset(x + paddingPx, textY),
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        size = textSize,
                        style = style
                    )
                }

                style = captionStyle.copy(fontSize = 18.sp, fontWeight = FontWeight(700))
                textY -= measureText(
                    textMeasurer,
                    name,
                    style
                ).size.height
                drawText(
                    textMeasurer,
                    text = name,
                    topLeft = Offset(x + paddingPx, textY),
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    size = textSize,
                    style = style
                )

                style = captionStyle.copy(fontSize = 16.sp)
                textY -= measureText(
                    textMeasurer,
                    scrobbleCount,
                    style
                ).size.height
                drawText(
                    textMeasurer,
                    text = scrobbleCount,
                    topLeft = Offset(x + paddingPx, textY),
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    size = textSize,
                    style = style
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
            val measurement = measureText(
                textMeasurer,
                text,
                TextStyle.Default.copy(
                    fontSize = fontSize.sp,
                ),
                constraints = Constraints(maxWidth = width.toInt())
            )
            overflowed = measurement.didOverflowWidth
            if (overflowed)
                fontSize -= step
        }

        return fontSize
    }

    private fun addHeaderAndFooter(
        mosaics: List<ImageBitmap>,
        type: Int,
        timePeriod: TimePeriod,
        username: String?,
        textMeasurer: TextMeasurer
    ): ImageBitmap {

        val artistsStr = "🎙️ " + context.getString(R.string.top_artists)
        val albumsStr = "💿️ " + context.getString(R.string.top_albums)
        val tracksStr = "🎵️ " + context.getString(R.string.top_tracks)

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
            captionStyle.copy(fontSize = 21.sp),
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

        brandTextWidth = if (isPro) 0 else measureText(
            textMeasurer,
            brandText,
            titleStyle.copy(
                color = Color.White,
                fontSize = (footerTextSize / 2).sp,
            ),
            2
        ).size.width

        val extraHeights =
            headerTexts.map {
                measureText(
                    textMeasurer,
                    it,
                    titleStyle
                ).size.height
            } + measureText(
                textMeasurer,
                footerText,
                titleStyle,
            ).size.height

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

            if (!isPro) {
                val appIconScaledSize = (footerTextSize * 1.5).toInt()
                val appIcon = ContextCompat.getDrawable(
                    context,
                    R.drawable.vd_launcher_fg
                )!!
                    .toBitmap(width = appIconScaledSize, height = appIconScaledSize)
                    .asImageBitmap()

                // Calculate offsets for center cropping
                val cropOffsetX = (appIconScaledSize - footerTextSize) / 2
                val cropOffsetY = (appIconScaledSize - footerTextSize) / 2

                // Draw the cropped image
                drawImage(
                    image = appIcon,
                    srcOffset = IntOffset(cropOffsetX, cropOffsetY),
                    srcSize = IntSize(footerTextSize, footerTextSize),
                    dstOffset = IntOffset(
                        (totalWidth - brandTextWidth - footerTextSize - brandTextPadding / 2 - paddingPx),
                        offsetY.toInt()
                    ),
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = brandText,
                    topLeft = Offset(totalWidth - brandTextWidth - paddingPx.toFloat(), offsetY),
                    style = titleStyle.copy(fontSize = (footerTextSize / 2.25).sp),
                    maxLines = 2,
                    size = Size(brandTextWidth.toFloat(), extraHeights.last().toFloat())
                )
            }
        }

        return bitmap
    }

    private suspend fun fetchCharts(
        type: Int,
        timePeriod: TimePeriod,
        username: String
    ): Result<PageResult<out MusicEntry>> {
        return Scrobblables.current.value!!.getCharts(
            type,
            timePeriod,
            1,
            username
        ).onFailure {
            _errorText.emit(it.message ?: "Unknown error")
        }
    }

    private fun createSpecificCollageShareText(musicEntries: List<MusicEntry>): String {
        if (musicEntries.isEmpty()) return ""
        var pos = 1
        val list = musicEntries.take(10).joinToString(separator = "\n") {
            when (it) {
                is Track -> context.getString(
                    R.string.charts_num_text,
                    pos++,
                    context.getString(R.string.artist_title, it.artist.name, it.name)
                )

                is Album -> context.getString(
                    R.string.charts_num_text,
                    pos++,
                    context.getString(R.string.artist_title, it.artist!!.name, it.name)
                )

                else -> context.getString(R.string.charts_num_text, pos++, it.name)
            }
        }

        return list
    }

    private fun createDigestShareText(musicEntriesAll: List<List<MusicEntry>>): String {
        val shareTextList = mutableListOf<String>()
        val resultsMap = mapOf(
            R.string.top_artists to musicEntriesAll[0],
            R.string.top_albums to musicEntriesAll[1],
            R.string.top_tracks to musicEntriesAll[2],
        )
        resultsMap.forEach { (titleRes, musicEntries) ->

            val title = context.getString(titleRes)
            val text = musicEntries.take(3).joinToString { it.name }
            shareTextList += "$title:\n$text"
        }

        return shareTextList.joinToString("\n\n")
    }
}