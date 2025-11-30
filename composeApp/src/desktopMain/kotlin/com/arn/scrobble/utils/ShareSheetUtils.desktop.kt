package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.Track

actual suspend fun showTrackShareSheet(track: Track, user: UserCached) {
    // no-op
}

actual fun showCollageShareSheet(imageBitmap: ImageBitmap, text: String?) {
    // no-op
}