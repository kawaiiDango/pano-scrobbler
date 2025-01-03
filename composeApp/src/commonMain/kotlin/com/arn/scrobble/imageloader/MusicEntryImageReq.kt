package com.arn.scrobble.imageloader

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry

data class MusicEntryImageReq(
    val musicEntry: MusicEntry,
    val isHeroImage: Boolean = false,
    val fetchAlbumInfoIfMissing: Boolean = false,
    val accountType: AccountType? = Scrobblables.current.value?.userAccount?.type
)