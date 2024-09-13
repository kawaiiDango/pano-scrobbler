package com.arn.scrobble.charts

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.compose.LocalFragment
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.main.MainNotifierViewModel
import com.arn.scrobble.review.ReviewPrompter
import com.arn.scrobble.ui.BottomSheetDialogParent
import com.arn.scrobble.ui.ButtonWithDropdown
import com.arn.scrobble.ui.ErrorText
import com.arn.scrobble.ui.LabeledCheckbox
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.getSingle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.util.Calendar

@Composable
private fun CollageGeneratorContent(
    viewModel: CollageGeneratorVM = viewModel(),
    timePeriod: TimePeriod,
    user: UserCached,
    onAskForReview: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
//    val collageUri by viewModel.collageUri.collectAsState()

    var collageType by rememberSaveable { mutableIntStateOf(0) }
    var collageSize by rememberSaveable { mutableIntStateOf(3) }
    var includeCaptions by rememberSaveable { mutableStateOf(false) }
    var collageText by rememberSaveable { mutableStateOf(false) }
    var collageUsername by rememberSaveable { mutableStateOf(false) }
    var collageSkipMissing by rememberSaveable { mutableStateOf(false) }
    var shareCollageClicked by remember { mutableStateOf(false) }
    var saveCollageClicked by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }


    val errorText by viewModel.errorText.collectAsStateWithLifecycle(null)
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    var collagePreview by remember { mutableStateOf<ImageBitmap?>(null) }

    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val saveBySafRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        uri?.let {
            collagePreview ?: return@let
            context.contentResolver.openOutputStream(it)?.let {
                writeImage(collagePreview!!, it)
            }
        }
    }

    val collageTypes = mapOf(
        Stuff.TYPE_ALL to stringResource(id = R.string.edit_all),
        Stuff.TYPE_ARTISTS to stringResource(id = R.string.artists),
        Stuff.TYPE_ALBUMS to stringResource(id = R.string.albums),
        Stuff.TYPE_TRACKS to stringResource(id = R.string.tracks),
    )

    val collageSizes = remember {
        (3..6).associateWith { it.toString() }
    }

    fun generateCollage() {
        viewModel.generateCollage(
            type = collageType,
            size = collageSize,
            captions = includeCaptions,
            username = collageUsername,
            user = user,
            timePeriod = timePeriod,
            skipMissing = collageSkipMissing,
            textMeasurer = textMeasurer,
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
                shareCollage(context, image, text.takeIf { collageText && text.isNotBlank() })
                shareCollageClicked = false
            }
            if (saveCollageClicked) {
                saveCollage(image, saveBySafRequest)
                saveCollageClicked = false
                launchedShowSavedMessage()
            }

            collagePreview = image
        }
    }

    Column(
        modifier = modifier
    ) {

        collagePreview?.let {
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
            ButtonWithDropdown(
                prefixText = null,
                selected = collageType,
                itemToTexts = collageTypes,
                onItemSelected = { collageType = it },
                modifier = Modifier.weight(0.4f),
            )

            ButtonWithDropdown(
                prefixText = stringResource(id = R.string.size),
                selected = collageSize,
                itemToTexts = collageSizes,
                onItemSelected = { collageSize = it },
                modifier = Modifier.weight(0.4f),
            )
        }

        LabeledCheckbox(
            text = stringResource(id = R.string.captions),
            checked = includeCaptions,
            onCheckedChange = { includeCaptions = it },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledCheckbox(
            text = stringResource(id = R.string.skip_missing_images),
            enabled = includeCaptions,
            checked = collageSkipMissing || !includeCaptions,
            onCheckedChange = { collageSkipMissing = it },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledCheckbox(
            text = stringResource(id = R.string.username),
            checked = collageUsername,
            onCheckedChange = { collageUsername = it },
            modifier = Modifier.fillMaxWidth(),
        )

        LabeledCheckbox(
            text = stringResource(id = R.string.text),
            checked = collageText,
            onCheckedChange = { collageText = it },
            modifier = Modifier.fillMaxWidth(),
        )

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
                            id =
                            if (showSavedMessage)
                                R.string.saved_to_gallery
                            else
                                R.string.save
                        )
                    )
                }

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
                    Text(stringResource(id = R.string.share))
                }
            }
        }
    }
}

private fun saveCollage(
    imageBitmap: ImageBitmap,
    saveBySafRequest: ActivityResultLauncher<String>,
) {
    val mimeType = "image/jpeg"
    val cal = Calendar.getInstance()
    val fileName =
        "collage_" + cal[Calendar.YEAR] + "_" + cal[Calendar.MONTH] + "_" +
                cal[Calendar.DATE] + "_" + cal[Calendar.HOUR_OF_DAY] + "_" +
                cal[Calendar.MINUTE] + "_" + cal[Calendar.SECOND] + ".jpg"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PlatformStuff.savePictureQ(fileName, mimeType) {
            writeImage(imageBitmap, it)
        }
    } else {
        saveBySafRequest.launch(fileName)
    }
}

private fun writeImage(imageBitmap: ImageBitmap, ostream: OutputStream) {
    ostream.use {
        imageBitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, 95, it)
    }
}

private fun shareCollage(context: Context, imageBitmap: ImageBitmap, text: String?) {
    val collageFile = File(PlatformStuff.cacheDir, "share/collage.jpg")

    collageFile.parentFile!!.mkdirs()
    writeImage(imageBitmap, collageFile.outputStream())

    val uri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        collageFile
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        type = "image/jpeg"
    }
    context.startActivity(intent)
}

@Keep
@Composable
fun CollageGeneratorScreen() {
    val fragment = LocalFragment.current
    val activityViewModel by fragment.activityViewModels<MainNotifierViewModel>()
    val timePeriod = fragment.requireArguments().getSingle<TimePeriod>()!!
    val user = remember { activityViewModel.currentUser }

    BottomSheetDialogParent {
        CollageGeneratorContent(
            timePeriod = timePeriod,
            user = user,
            onAskForReview = {
                ReviewPrompter(
                    fragment.requireActivity(),
                    PlatformStuff.mainPrefs.data.first().lastReviewPromptTime
                ) { t ->
                    PlatformStuff.mainPrefs.updateData { it.copy(lastReviewPromptTime = t) }
                }.showIfNeeded()
            },
            modifier = it
        )
    }
}