package com.arn.scrobble.media

import com.arn.scrobble.utils.Stuff

actual typealias PlatformMediaMetadata = MetadataInfo

// from web-scrobbler connectors.ts
// grep -oP "\*://\*\.[a-zA-Z0-9\-\.]+/" connectors.ts | sort -u
private val WILDCARD_DOMAINS = setOf(
    "afrocharts.com", "anghami.com", "bagelradio.com", "bandcamp.com",
    "basspistol.com", "bbc.co.uk", "blocsonic.com",
    "burntable.com", "calm.com", "edbangerrecords.com", "epicmusictime.com",
    "epidemicsound.com", "filmmusic.io", "fmspins.com", "freegalmusic.com",
    "freemusicarchive.org", "frisky.fm", "fungjai.com", "getworkdonemusic.com",
    "grrif.ch", "iheart.com", "imago.fm", "invidio.us", "iramanusantara.org",
    "jazzandrain.com", "keakie.com", "kexp.org", "live365.com", "liveone.com",
    "melodia.com.br", "mixcloud.com", "musicme.com", "musify.club",
    "musiqueapproximative.net", "mystreamplayer.com", "naxosmusiclibrary.com",
    "playirish.ie", "pretzel.rocks", "provoda.ch", "qobuz.com", "radio-mb.com",
    "radiobob.de", "relaxingbeats.com", "simulatorradio.com", "stingray.com",
    "subvert.fm", "supla.fi", "truckers.fm", "tunegenie.com",
    "vagalume.com.br", "vk-save.com", "weibo.com",
    "xray.fm"
)

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
    var normalizedUrlHost = metadata.normalizedUrlHost?.removePrefix("www.")

    if (normalizedUrlHost != null) {
        for (wildcardDomain in WILDCARD_DOMAINS) {
            if (normalizedUrlHost?.endsWith(wildcardDomain) == true) {
                normalizedUrlHost = wildcardDomain
                break
            }
        }
    }

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
        normalizedUrlHost = normalizedUrlHost,
    )

    var ignoreScrobble = false

//        Spotify ADs: MetadataInfo(trackId=, title=Advertisement, artist=Something, album=, albumArtist=Something, trackNumber=0, duration=20000, artUrl=)
    if (metadata.title == "Advertisement" && metadata.artist == metadata.albumArtist && metadata.album.isEmpty() && metadata.artist.isNotEmpty()) {
        ignoreScrobble = true
    }

    return metadataInfo to ignoreScrobble
}
