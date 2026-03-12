package com.arn.scrobble.charts

import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import coil3.Image
import coil3.toBitmap
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.launch
import java.io.File

actual fun CollageGeneratorVM.shareCollage(
    image: ImageBitmap,
    text: String?
) {
    viewModelScope.launch {
        val collageFile = File(PlatformStuff.cacheDir, "share/collage.jpg")


        collageFile.parentFile!!.mkdirs()
        PlatformStuff.writeBitmapToStream(image, collageFile.outputStream())

        val context = AndroidStuff.applicationContext
        val uri = FileProvider.getUriForFile(
            context,
            "${BuildKonfig.APP_ID}.fileprovider",
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
}

actual fun CollageGeneratorVM.drawCoilImage(
    drawScope: DrawScope,
    image: Image,
    dstOffset: IntOffset,
    dstSize: IntSize
) {
    drawScope.drawImage(
        image = image.toBitmap().asImageBitmap(),
        dstOffset = dstOffset,
        dstSize = dstSize
    )
}