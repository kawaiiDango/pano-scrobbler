package com.arn.scrobble.utils

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.FileProvider
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.recents_share
import pano_scrobbler.composeapp.generated.resources.recents_share_username
import pano_scrobbler.composeapp.generated.resources.share
import pano_scrobbler.composeapp.generated.resources.share_sig
import java.io.File
import java.io.FileOutputStream

actual suspend fun showTrackShareSheet(track: Track, user: UserCached) {
    val context = AndroidStuff.applicationContext
    val heart = when {
        track.userloved == true -> "♥️"
        track.userHated == true -> "💔"
        else -> ""
    }

    var shareText = if (user.isSelf)
        getString(
            Res.string.recents_share,
            heart + Stuff.formatBigHyphen(track.artist.name, track.name),
            PanoTimeFormatter.relative(track.date ?: 0, true)
        )
    else
        getString(
            Res.string.recents_share_username,
            heart + Stuff.formatBigHyphen(track.artist.name, track.name),
            PanoTimeFormatter.relative(track.date ?: 0, true),
            user.name
        )

    if (!VariantStuff.billingRepository.isLicenseValid)
        shareText += "\n\n" + getString(Res.string.share_sig)

    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, shareText)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    withContext(Dispatchers.IO) {
        val shareAlbumArt = if (track.album?.webp300 == null) {
            null
        } else {
            try {
                ImageLoader(context).execute(
                    ImageRequest.Builder(context)
                        .data(track.album.webp300)
                        .allowHardware(false)
                        .build()
                ).image?.toBitmap()
            } catch (e: Exception) {
                null
            }
        }

        if (shareAlbumArt != null) {
            val albumArtFile = File(context.cacheDir, "share/album_art.jpg")
            albumArtFile.parentFile!!.mkdirs()
            FileOutputStream(albumArtFile).use { fos ->
                shareAlbumArt.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }

            val albumArtUri = FileProvider.getUriForFile(
                context,
                "${BuildKonfig.APP_ID}.fileprovider",
                albumArtFile
            )

            i.putExtra(Intent.EXTRA_STREAM, albumArtUri)
            i.type = "image/jpeg"
        }
    }

    context.startActivity(
        Intent.createChooser(i, getString(Res.string.share)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

actual fun showCollageShareSheet(imageBitmap: ImageBitmap, text: String?) {
    val context = AndroidStuff.applicationContext

    val collageFile = File(PlatformStuff.cacheDir, "share/collage.jpg")

    collageFile.parentFile!!.mkdirs()
    PlatformStuff.writeBitmapToStream(imageBitmap, collageFile.outputStream())

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