package com.arn.scrobble.charts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.LocalPlatformContext
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.themes.LocalThemeAttributes
import com.arn.scrobble.ui.ButtonWithSpinner
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.placeholderImageVectorPainter
import com.arn.scrobble.utils.PlatformFile
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.collectAsStateWithInitialValue
import com.arn.scrobble.utils.showCollageShareSheet
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.as_text
import pano_scrobbler.composeapp.generated.resources.borders
import pano_scrobbler.composeapp.generated.resources.captions
import pano_scrobbler.composeapp.generated.resources.charts_num_text
import pano_scrobbler.composeapp.generated.resources.edit_all
import pano_scrobbler.composeapp.generated.resources.external_metadata
import pano_scrobbler.composeapp.generated.resources.random_text
import pano_scrobbler.composeapp.generated.resources.save
import pano_scrobbler.composeapp.generated.resources.saved_to_gallery
import pano_scrobbler.composeapp.generated.resources.share
import pano_scrobbler.composeapp.generated.resources.size
import pano_scrobbler.composeapp.generated.resources.skip_missing_images
import pano_scrobbler.composeapp.generated.resources.tracks
import pano_scrobbler.composeapp.generated.resources.username
import pano_scrobbler.composeapp.generated.resources.vd_launcher_fg_for_collage
import java.util.Calendar

data class IconPaintersForCollage(
    val app: Painter,
    val user: Painter,
    val artist: Painter,
    val album: Painter,
    val track: Painter,
    val colors: List<Color>
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollageGeneratorDialog(
    collageType: Int,
    timePeriod: TimePeriod,
    user: UserCached,
    onAskForReview: suspend () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollageGeneratorVM = viewModel { CollageGeneratorVM() },
) {
    var collageType by rememberSaveable(collageType) { mutableIntStateOf(collageType) }
    val collageSize by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { it.collageSize }
    val includeCaptions by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { it.collageCaptions }
    val collageUsername by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { it.collageUsername }
    val collageSkipMissing by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { it.collageSkipMissing }
    val collageBorders by PlatformStuff.mainPrefs.data
        .collectAsStateWithInitialValue { it.collageBorders }
    var shareCollageClicked by remember { mutableStateOf(false) }
    var saveCollageClicked by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    var shareTextToCopy by remember { mutableStateOf<String?>(null) }
    val iconPainters = IconPaintersForCollage(
        app = painterResource(Res.drawable.vd_launcher_fg_for_collage),
        user = placeholderImageVectorPainter(null, Icons.Outlined.Person),
        artist = placeholderImageVectorPainter(null, Icons.Outlined.Mic),
        album = placeholderImageVectorPainter(null, Icons.Outlined.Album),
        track = placeholderImageVectorPainter(null, Icons.Outlined.MusicNote),
        colors = LocalThemeAttributes.current.allSecondaryContainerColors
    )
    val context = LocalPlatformContext.current
    val shareEnabled = !PlatformStuff.isTv && !PlatformStuff.isDesktop

    val errorText by viewModel.errorText.collectAsStateWithLifecycle(null)
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    var collageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()

    var filePickerShown by remember { mutableStateOf(false) }

    val collageTypes = mapOf(
        Stuff.TYPE_ALL to stringResource(Res.string.edit_all),
        Stuff.TYPE_ARTISTS to stringResource(Res.string.artists),
        Stuff.TYPE_ALBUMS to stringResource(Res.string.albums),
        Stuff.TYPE_TRACKS to stringResource(Res.string.tracks),
    )

    val collageSizes = remember {
        (3..6).associateWith { it.toString() }
    }

    fun generateCollage() {
        shareTextToCopy = null
        collageBitmap = null

        viewModel.generateCollage(
            context = context,
            type = collageType,
            size = collageSize,
            captions = includeCaptions,
            username = collageUsername,
            user = user,
            timePeriod = timePeriod,
            skipMissing = collageSkipMissing,
            borders = collageBorders,
            textMeasurer = textMeasurer,
            iconPainters = iconPainters,
        )
    }

    fun launchedShowSavedMessage() {
        showSavedMessage = true
        scope.launch {
            delay(1000)

            onAskForReview()

            delay(2000)
            showSavedMessage = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sharableCollage.collectLatest { (image, text) ->
            shareTextToCopy = text.ifBlank { null }

            if (shareCollageClicked) {
                showCollageShareSheet(image, shareTextToCopy)
                shareCollageClicked = false
            }
            if (saveCollageClicked) {
                filePickerShown = true
                saveCollageClicked = false
                launchedShowSavedMessage()
            }

            collageBitmap = image
        }
    }

    Column(
        modifier = modifier,
    ) {

        collageBitmap?.let { collageBitmap ->
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Image(
                    bitmap = collageBitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.25f),
                )

                OutlinedButton(
                    onClick = {
                        shareTextToCopy?.let { PlatformStuff.copyToClipboard(it) }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy, contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(Res.string.as_text))
                }
            }
        }

        ErrorText(errorText)

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ButtonWithSpinner(
                prefixText = null,
                selected = collageType,
                itemToTexts = collageTypes,
                onItemSelected = { collageType = it },
                modifier = Modifier.weight(0.4f),
            )

            ButtonWithSpinner(
                prefixText = stringResource(Res.string.size),
                selected = collageSize,
                itemToTexts = collageSizes,
                onItemSelected = { value ->
                    scope.launch {
                        PlatformStuff.mainPrefs.updateData { it.copy(collageSize = value) }
                    }
                },
                modifier = Modifier.weight(0.4f),
            )
        }

        LabeledCheckbox(
            text = stringResource(Res.string.captions),
            checked = includeCaptions,
            onCheckedChange = { value ->
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(collageCaptions = value) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledCheckbox(
            text = stringResource(Res.string.skip_missing_images),
            enabled = includeCaptions,
            checked = collageSkipMissing || !includeCaptions,
            onCheckedChange = { value ->
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(collageSkipMissing = value) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledCheckbox(
            text = stringResource(Res.string.borders),
            checked = collageBorders,
            onCheckedChange = { value ->
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(collageBorders = value) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledCheckbox(
            text = stringResource(Res.string.username),
            checked = collageUsername,
            onCheckedChange = { value ->
                scope.launch {
                    PlatformStuff.mainPrefs.updateData { it.copy(collageUsername = value) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

//        if (shareEnabled) {
//            LabeledCheckbox(
//                text = stringResource(Res.string.text),
//                checked = collageText,
//                onCheckedChange = { collageText = it },
//                modifier = Modifier.fillMaxWidth(),
//            )
//        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
        ) {
            if (progress != 1f) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                )
            } else {
                TextButton(
                    enabled = !showSavedMessage,
                    onClick = {
                        generateCollage()
                        saveCollageClicked = true
                    },
                    modifier = if (showSavedMessage) Modifier.alpha(0.5f) else Modifier,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download, contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        stringResource(
                            if (showSavedMessage)
                                Res.string.saved_to_gallery
                            else
                                Res.string.save
                        )
                    )
                }

                if (shareEnabled) {
                    OutlinedButton(
                        onClick = {
                            generateCollage()
                            shareCollageClicked = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share, contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(Res.string.share))
                    }
                }
            }
        }
    }

    FilePicker(
        show = filePickerShown,
        mode = FilePickerMode.Save(
            title = remember { "collage_" + Stuff.getFileNameDateSuffix() }
        ),
        type = FileType.PHOTO,
        onDismiss = { filePickerShown = false },
    ) { platformFile ->
        collageBitmap?.let {
            writeImage(it, platformFile)
        }
    }
}

private fun writeImage(imageBitmap: ImageBitmap, platformFile: PlatformFile) {
    GlobalScope.launch {
        platformFile.overwrite { stream ->
            PlatformStuff.writeBitmapToStream(imageBitmap, stream)
        }
    }
}