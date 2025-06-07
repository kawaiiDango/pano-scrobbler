package com.arn.scrobble.media

import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.Stuff

actual typealias PlatformMediaMetadata = MetadataInfo

actual fun transformMediaMetadata(
    trackInfo: PlayingTrackInfo,
    metadata: PlatformMediaMetadata,
): Pair<MetadataInfo, Boolean> {
    var artist = metadata.artist.trim()
    var album = metadata.album.trim()
    var title = metadata.title.trim()
    var albumArtist = metadata.album_artist.trim()
    val trackNumber = metadata.track_number
    // a -1 value on my windows implementation means, a timeline info event hasn't been received yet
    var durationMillis = metadata.duration

    val canDoFallbackScrobble = trackInfo.ignoreOrigArtist &&
            trackInfo.appId in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK

    when (trackInfo.appId) {
        Stuff.PACKAGE_APPLE_MUSIC_WIN -> {
            val splits = artist.split(" — ")

            if (splits.size >= 2) {
                // sometimes there are 3 splits, like "Artist — Album - EP — Artist"
                artist = splits[0].trim()
                album = MetadataUtils.removeSingleEp(splits[1].trim())
                albumArtist = "" // contains the same malformed value as artist
            }
        }

        Stuff.PACKAGE_CIDER_LINUX,
        Stuff.PACKAGE_CIDER_VARIANT_LINUX
            -> {
            album = MetadataUtils.removeSingleEp(album)
        }
    }

    val metadataInfo = MetadataInfo(
        app_id = metadata.app_id,
        title = title,
        artist = artist,
        album = album,
        album_artist = albumArtist,
        track_number = trackNumber,
        duration = durationMillis,
    )

    return metadataInfo to canDoFallbackScrobble
}
