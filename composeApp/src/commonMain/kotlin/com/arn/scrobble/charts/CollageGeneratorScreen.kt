package com.arn.scrobble.charts

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
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
import com.arn.scrobble.ui.BottomSheetDialogParent
import com.arn.scrobble.ui.ButtonWithSpinner
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.FilePicker
import com.arn.scrobble.ui.FilePickerMode
import com.arn.scrobble.ui.FileType
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.ui.getActivityOrNull
import com.arn.scrobble.ui.placeholderImageVectorPainter
import com.arn.scrobble.utils.PlatformFile
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.showCollageShareSheet
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.albums
import pano_scrobbler.composeapp.generated.resources.artists
import pano_scrobbler.composeapp.generated.resources.captions
import pano_scrobbler.composeapp.generated.resources.edit_all
import pano_scrobbler.composeapp.generated.resources.save
import pano_scrobbler.composeapp.generated.resources.saved_to_gallery
import pano_scrobbler.composeapp.generated.resources.share
import pano_scrobbler.composeapp.generated.resources.size
import pano_scrobbler.composeapp.generated.resources.skip_missing_images
import pano_scrobbler.composeapp.generated.resources.text
import pano_scrobbler.composeapp.generated.resources.tracks
import pano_scrobbler.composeapp.generated.resources.username
import pano_scrobbler.composeapp.generated.resources.vd_launcher_fg
import java.util.Calendar

data class IconPaintersForCollage(
    val app: Painter,
    val user: Painter,
    val artist: Painter,
    val album: Painter,
    val track: Painter,
    val colors: List<Color>
)

@Composable
private fun CollageGeneratorContent(
    collageType: Int,
    timePeriod: TimePeriod,
    user: UserCached,
    onAskForReview: suspend () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollageGeneratorVM = viewModel { CollageGeneratorVM() },
) {
    var collageType by rememberSaveable { mutableIntStateOf(collageType) }
    var collageSize by rememberSaveable { mutableIntStateOf(3) }
    var includeCaptions by rememberSaveable { mutableStateOf(false) }
    var collageText by rememberSaveable { mutableStateOf(false) }
    var collageUsername by rememberSaveable { mutableStateOf(false) }
    var collageSkipMissing by rememberSaveable { mutableStateOf(false) }
    var shareCollageClicked by remember { mutableStateOf(false) }
    var saveCollageClicked by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    val iconPainters = IconPaintersForCollage(
        app = painterResource(Res.drawable.vd_launcher_fg),
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
        viewModel.generateCollage(
            context = context,
            type = collageType,
            size = collageSize,
            captions = includeCaptions,
            username = collageUsername,
            user = user,
            timePeriod = timePeriod,
            skipMissing = collageSkipMissing,
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
        val prefs = PlatformStuff.mainPrefs.data.first()
        includeCaptions = prefs.collageCaptions
        collageSize = prefs.collageSize
        collageUsername = prefs.collageUsername
        collageText = prefs.collageText
        collageSkipMissing = prefs.collageSkipMissing
    }

    LaunchedEffect(collageSkipMissing, collageSize, includeCaptions, collageUsername, collageText) {
        PlatformStuff.mainPrefs.updateData {
            it.copy(
                collageSize = collageSize,
                collageCaptions = includeCaptions,
                collageUsername = collageUsername,
                collageText = collageText,
                collageSkipMissing = collageSkipMissing,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.sharableCollage.collectLatest { (image, text) ->
            if (shareCollageClicked) {
                showCollageShareSheet(image, text.takeIf { collageText && text.isNotBlank() })
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
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {

        collageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .align(Alignment.CenterHorizontally),
            )
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
                onItemSelected = { collageSize = it },
                modifier = Modifier.weight(0.4f),
            )
        }

        LabeledCheckbox(
            text = stringResource(Res.string.captions),
            checked = includeCaptions,
            onCheckedChange = { includeCaptions = it },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledCheckbox(
            text = stringResource(Res.string.skip_missing_images),
            enabled = includeCaptions,
            checked = collageSkipMissing || !includeCaptions,
            onCheckedChange = { collageSkipMissing = it },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledCheckbox(
            text = stringResource(Res.string.username),
            checked = collageUsername,
            onCheckedChange = { collageUsername = it },
            modifier = Modifier.fillMaxWidth(),
        )

        if (shareEnabled) {
            LabeledCheckbox(
                text = stringResource(Res.string.text),
                checked = collageText,
                onCheckedChange = { collageText = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
        ) {
            if (progress != 1f) {
                CircularProgressIndicator(
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
            title = remember {
                val cal = Calendar.getInstance()
                "collage_" + cal[Calendar.YEAR] + "_" + cal[Calendar.MONTH] + "_" +
                        cal[Calendar.DATE] + "_" + cal[Calendar.HOUR_OF_DAY] + "_" +
                        cal[Calendar.MINUTE] + "_" + cal[Calendar.SECOND]
            }
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

@Composable
fun CollageGeneratorScreen(
    collageType: Int,
    timePeriod: TimePeriod,
    user: UserCached,
    onDismiss: () -> Unit,
) {
    val activity = getActivityOrNull()

    BottomSheetDialogParent(
        onDismiss = onDismiss,
    ) {
        CollageGeneratorContent(
            collageType = collageType,
            timePeriod = timePeriod,
            user = user,
            onAskForReview = {
                PlatformStuff.promptForReview(activity)
            },
            modifier = it
        )
    }
}