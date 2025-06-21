package com.arn.scrobble.db

import com.arn.scrobble.api.lastfm.ScrobbleData

data class RegexResults(
    val matches: Set<RegexEdit>,
    val scrobbleData: ScrobbleData?,
    val blockPlayerAction: BlockPlayerAction?,
)
