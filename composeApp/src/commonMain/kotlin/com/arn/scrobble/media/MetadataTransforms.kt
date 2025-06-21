package com.arn.scrobble.media

expect class PlatformMediaMetadata

expect fun transformMediaMetadata(
    trackInfo: PlayingTrackInfo,
    metadata: PlatformMediaMetadata,
): MetadataInfo