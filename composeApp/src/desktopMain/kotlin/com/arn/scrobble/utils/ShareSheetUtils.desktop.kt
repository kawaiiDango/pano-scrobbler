package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.UserCached

actual suspend fun showTrackShareSheet(track: Track, user: UserCached) {
    // no-op
}

actual fun showCollageShareSheet(imageBitmap: ImageBitmap, text: String?) {
    // no-op
}