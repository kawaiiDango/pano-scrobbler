package com.arn.scrobble.media

import com.arn.scrobble.utils.Stuff

actual typealias PlatformMediaMetadata = MetadataInfo

actual fun transformMediaMetadata(
    trackInfo: PlayingTrackInfo,
    metadata: PlatformMediaMetadata,
): Pair<MetadataInfo, Boolean> {
    var artist = metadata.artist.trim()
    var album = metadata.album.trim()
    val title = metadata.title.trim()
    var albumArtist = metadata.albumArtist.trim()
    val trackNumber = metadata.trackNumber
    // a -1 value on my windows implementation means, a timeline info event hasn't been received yet
    val durationMillis = metadata.duration

    when (trackInfo.appId.lowercase()) {
        Stuff.PACKAGE_APPLE_MUSIC_WIN_EXE.lowercase(),
        Stuff.PACKAGE_APPLE_MUSIC_WIN_STORE.lowercase() -> {
            val splits = artist.split(" — ")

            if (splits.size >= 2) {
                // sometimes there are 3 splits, like "Artist — Album - EP — Station name"
                artist = splits[0].trim()
                album = splits[1].trim()
                albumArtist = "" // contains the same malformed value as artist
            }
        }
    }

    val metadataInfo = MetadataInfo(
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        trackNumber = trackNumber,
        duration = durationMillis,
        artUrl = metadata.artUrl,
        normalizedUrlHost = metadata.normalizedUrlHost,
    )

    var ignoreScrobble = false

//        Spotify ADs: MetadataInfo(trackId=, title=Advertisement, artist=Something, album=, albumArtist=Something, trackNumber=0, duration=20000, artUrl=)
    if (metadata.title == "Advertisement" && metadata.artist == metadata.albumArtist && metadata.album.isEmpty() && metadata.artist.isNotEmpty()) {
        ignoreScrobble = true
    }

    return metadataInfo to ignoreScrobble
}
