package com.arn.scrobble.media

import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.Stuff

actual typealias PlatformMediaMetadata = MetadataInfo

actual fun transformMediaMetadata(
    trackInfo: PlayingTrackInfo,
    metadata: PlatformMediaMetadata,
): Pair<MetadataInfo, Map<String, String>> {
    var artist = metadata.artist.trim()
    var album = metadata.album.trim()
    var title = metadata.title.trim()
    var albumArtist = metadata.albumArtist.trim()
    val trackNumber = metadata.trackNumber
    // a -1 value on my windows implementation means, a timeline info event hasn't been received yet
    var durationMillis = metadata.duration

    when (trackInfo.appId.lowercase()) {
        Stuff.PACKAGE_APPLE_MUSIC_WIN.lowercase() -> {
            val splits = artist.split(" — ")

            if (splits.size >= 2) {
                // sometimes there are 3 splits, like "Artist — Album - EP — Station name"
                artist = splits[0].trim()
                album = MetadataUtils.removeSingleEp(splits[1].trim())
                albumArtist = "" // contains the same malformed value as artist
            }
        }

        Stuff.PACKAGE_CIDER_LINUX.lowercase(),
        Stuff.PACKAGE_CIDER_VARIANT_LINUX.lowercase()
            -> {
            album = MetadataUtils.removeSingleEp(album)
        }
    }

    val metadataInfo = MetadataInfo(
        appId = metadata.appId,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        trackNumber = trackNumber,
        duration = durationMillis,
    )

    return metadataInfo to emptyMap()
}
