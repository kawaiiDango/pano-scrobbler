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
    var durationMillis = metadata.duration

    val canDoFallbackScrobble = trackInfo.ignoreOrigArtist &&
            trackInfo.appId in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK

    when (trackInfo.appId) {
        Stuff.PACKAGE_APPLE_MUSIC_WIN -> {
            // remove  - Single,  - EP at the end
            artist = MetadataUtils.removeSingleEp(artist)
            val idx = artist.lastIndexOf(" — ")
            if (idx != -1) {
                // The left part becomes the artist; the right part becomes the album.
                val newArtist = artist.substring(0, idx).trim()
                val newAlbum = artist.substring(idx + 3).trim()
                artist = newArtist
                album = newAlbum
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
