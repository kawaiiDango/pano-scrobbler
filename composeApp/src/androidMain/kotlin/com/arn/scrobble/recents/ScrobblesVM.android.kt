package com.arn.scrobble.recents

import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.imageloader.StarMapper
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.recents_share
import pano_scrobbler.composeapp.generated.resources.recents_share_username
import pano_scrobbler.composeapp.generated.resources.share
import java.io.File
import java.io.FileOutputStream

actual fun ScrobblesVM.shareTrack(track: Track, shareSig: String?) {
    viewModelScope.launch {
        val context = AndroidStuff.applicationContext
        val heart = when {
            track.userloved == true -> "♥️"
            track.userHated == true -> "💔"
            else -> ""
        }

        val artistTrack = Stuff.formatBigHyphen(track.artist.name, track.name)
        val dateString = PanoTimeFormatter.relative(track.date ?: 0, null, true)

        var shareText = if (user.isSelf)
            getString(
                Res.string.recents_share,
                heart + artistTrack,
                dateString
            )
        else
            getString(
                Res.string.recents_share_username,
                heart + artistTrack,
                dateString,
                user.name
            )

        if (shareSig != null)
            shareText += "\n\n" + shareSig

        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, shareText)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        withContext(Dispatchers.IO) {
            val shareAlbumArt = if (track.album?.webp300 == null ||
                track.album.webp300?.contains(StarMapper.STAR_PATTERN) == true
            ) {
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

}