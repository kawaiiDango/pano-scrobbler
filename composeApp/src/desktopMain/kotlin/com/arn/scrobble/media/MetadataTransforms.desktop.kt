package com.arn.scrobble.media

import com.arn.scrobble.utils.Stuff

actual typealias PlatformMediaMetadata = MetadataInfo

actual fun transformMediaMetadata(
    trackInfo: PlayingTrackInfo,
    metadata: PlatformMediaMetadata,
): Pair<MetadataInfo, Boolean> {
    val canDoFallbackScrobble = trackInfo.ignoreOrigArtist && (
            trackInfo.appId in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK
            )

    val metadataInfo = metadata.copy()

    return metadataInfo to canDoFallbackScrobble
}
