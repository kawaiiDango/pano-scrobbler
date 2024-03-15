package com.arn.scrobble.ui

import com.arn.scrobble.api.lastfm.MusicEntry

class MusicEntryImageReq(
    val musicEntry: MusicEntry,
    val isHeroImage: Boolean = false,
    val fetchAlbumInfoIfMissing: Boolean = false
)