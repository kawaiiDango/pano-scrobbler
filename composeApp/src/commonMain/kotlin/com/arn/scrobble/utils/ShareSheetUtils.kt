package com.arn.scrobble.utils

import androidx.compose.ui.graphics.ImageBitmap
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.UserCached

expect suspend fun showTrackShareSheet(track: Track, user: UserCached)

expect fun showCollageShareSheet(imageBitmap: ImageBitmap, text: String?)