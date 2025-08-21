package com.arn.scrobble.media

import android.media.MediaMetadata
import android.os.Build
import com.arn.scrobble.utils.Stuff
import java.util.Locale

actual typealias PlatformMediaMetadata = MediaMetadata

actual fun transformMediaMetadata(
    trackInfo: PlayingTrackInfo,
    metadata: PlatformMediaMetadata,
): Pair<MetadataInfo, Map<String, String>> {
    val trackId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID) ?: ""
    var albumArtist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim() ?: ""
    // do not scrobble empty artists, ads will get scrobbled
    var artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?: ""
    var album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim() ?: ""
    var title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: ""
    val trackNumber = metadata.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER).toInt()
    var durationMillis = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
    if (durationMillis < -1)
        durationMillis = -1

    val extrasMap = mutableMapOf<String, String>()

    metadata.getLong(Stuff.METADATA_KEY_YOUTUBE_WIDTH).takeIf { it != 0L }
        ?.let { extrasMap[Stuff.METADATA_KEY_YOUTUBE_WIDTH] = it.toString() }

    metadata.getLong(Stuff.METADATA_KEY_YOUTUBE_HEIGHT).takeIf { it != 0L }
        ?.let { extrasMap[Stuff.METADATA_KEY_YOUTUBE_HEIGHT] = it.toString() }

    metadata.getString(Stuff.METADATA_KEY_AM_ARTIST_ID)
        ?.let { extrasMap[Stuff.METADATA_KEY_AM_ARTIST_ID] = it }


    when (trackInfo.appId) {
        Stuff.PACKAGE_PANDORA -> {
            artist = artist.replace("^Ofln - ".toRegex(), "")
            albumArtist = ""
        }

        Stuff.PACKAGE_PODCAST_ADDICT -> {
            if (albumArtist != "") {
                artist = albumArtist
                albumArtist = ""
            }
            val idx = artist.lastIndexOf(" • ")
            if (idx != -1)
                artist = artist.substring(0, idx)
        }

        Stuff.PACKAGE_SONOS,
        Stuff.PACKAGE_SONOS2,
            -> {
            metadata.getString(MediaMetadata.METADATA_KEY_COMPOSER)?.let {
                artist = it
                albumArtist = ""
            }
        }

        Stuff.PACKAGE_DIFM -> {
            val extra = " - $album"
            if (artist.endsWith(extra))
                artist = artist.substring(0, artist.length - extra.length)
            title = album
            album = ""
            albumArtist = ""
        }

        Stuff.PACKAGE_HUAWEI_MUSIC -> {
            if (Build.MANUFACTURER.lowercase(Locale.ENGLISH) == Stuff.MANUFACTURER_HUAWEI) {
                // Extra check for the manufacturer, because 'com.android.mediacenter' could match other music players.
                val extra = " - $album"
                if (artist.endsWith(extra))
                    artist = artist.substring(0, artist.length - extra.length)
                albumArtist = ""
            }
        }

        Stuff.PACKAGE_YANDEX_MUSIC -> {
            albumArtist = ""
        }

        Stuff.PACKAGE_SPOTIFY -> {
            // goddamn spotify

            // this is now done in regex presets
//            if (albumArtist.isNotEmpty() && albumArtist != artist &&
//                !MetadataUtils.isVariousArtists(albumArtist)
//            )
//                artist = albumArtist

            // this is for removing "smart shuffle" etc
            val idx = artist.lastIndexOf(" • ")
            if (idx != -1)
                artist = artist.substring(0, idx)
        }

        Stuff.PACKAGE_NINTENDO_MUSIC -> {
            if (artist.isEmpty())
                artist = Stuff.ARTIST_NINTENDO_MUSIC
        }
    }

//    if (trackInfo.appId in Stuff.IGNORE_ARTIST_META)
//        trackInfo.artist = trackInfo.artist.substringBeforeLast(" - Topic")

    // auto generated artist channels usually have square videos
//    val canDoFallbackScrobble = trackInfo.ignoreOrigArtist && (
//            trackInfo.appId in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK ||
//                    youtubeHeight > 0 && youtubeWidth > 0 && youtubeHeight == youtubeWidth
//            )

    val metadataInfo = MetadataInfo(
        appId = trackInfo.appId,
        trackId = trackId,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        trackNumber = trackNumber,
        duration = durationMillis,
    )

    return metadataInfo to extrasMap
}
