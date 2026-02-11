package com.arn.scrobble.media

import android.media.MediaMetadata
import android.os.Build
import com.arn.scrobble.utils.Stuff
import java.util.Locale

actual typealias PlatformMediaMetadata = MediaMetadata

actual fun transformMediaMetadata(
    trackInfo: PlayingTrackInfo,
    metadata: PlatformMediaMetadata,
): Pair<MetadataInfo, Boolean> {
    var albumArtist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.trim() ?: ""
    // do not scrobble empty artists, ads will get scrobbled
    var artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?: ""
    var album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim() ?: ""
    var title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: ""
    val trackNumber = metadata.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER).toInt()
//    val artUrl = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)?.trim() ?: ""
    var durationMillis = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
    if (durationMillis < -1 || trackInfo.appId in Stuff.IGNORE_DURATION)
        durationMillis = -1

//    metadata.getLong(Stuff.METADATA_KEY_YOUTUBE_WIDTH).takeIf { it != 0L }
//        ?.let { extrasMap[Stuff.METADATA_KEY_YOUTUBE_WIDTH] = it.toString() }
//
//    metadata.getLong(Stuff.METADATA_KEY_YOUTUBE_HEIGHT).takeIf { it != 0L }
//        ?.let { extrasMap[Stuff.METADATA_KEY_YOUTUBE_HEIGHT] = it.toString() }
//
//    metadata.getString(Stuff.METADATA_KEY_AM_ARTIST_ID)
//        ?.let { extrasMap[Stuff.METADATA_KEY_AM_ARTIST_ID] = it }


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
                artist = artist.take(idx)
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
                artist = artist.dropLast(extra.length)
            title = album
            album = ""
            albumArtist = ""
        }

        Stuff.PACKAGE_HUAWEI_MUSIC -> {
            if (Build.MANUFACTURER.lowercase(Locale.ENGLISH) == Stuff.MANUFACTURER_HUAWEI) {
                // Extra check for the manufacturer, because 'com.android.mediacenter' could match other music players.
                val extra = " - $album"
                if (artist.endsWith(extra))
                    artist = artist.dropLast(extra.length)
                albumArtist = ""
            }
        }

        Stuff.PACKAGE_YANDEX_MUSIC -> {
            albumArtist = ""
        }

        Stuff.PACKAGE_SPOTIFY -> {
            // this is for removing "smart shuffle" etc
            val idx = artist.lastIndexOf(" • ")
            if (idx != -1)
                artist = artist.take(idx)
        }

        Stuff.PACKAGE_NINTENDO_MUSIC -> {
            if (artist.isEmpty())
                artist = Stuff.ARTIST_NINTENDO_MUSIC
        }

        Stuff.PACKAGE_APPLE_MUSIC_CLASSICAL -> {
            // Apple Music Classical puts the full name of the composer in the album artist tag
            if (albumArtist.isNotEmpty()) {
                artist = albumArtist
                albumArtist = ""
            }
        }

        /*
        Stuff.PACKAGE_APPLE_MUSIC -> {
            // https://developer.apple.com/documentation/applemusicapi/songs/attributes-data.dictionary
            //
            // com.apple.android.music.playback.metadata.SHOW_COMPOSER_AS_ARTIST becomes 1 in that case

            val isClassical =
                metadata.getLong("com.apple.android.music.playback.metadata.SHOW_COMPOSER_AS_ARTIST") == 1L

            if (isClassical) {
                val composer = metadata.getString(MediaMetadata.METADATA_KEY_COMPOSER)
                if (!composer.isNullOrEmpty()) {
                    artist = composer
                    albumArtist = ""
                }
            }
        }

         */
    }

//    if (trackInfo.appId in Stuff.IGNORE_ARTIST_META)
//        trackInfo.artist = trackInfo.artist.substringBeforeLast(" - Topic")

    // auto generated artist channels usually have square videos
//    val canDoFallbackScrobble = trackInfo.ignoreOrigArtist && (
//            trackInfo.appId in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK ||
//                    youtubeHeight > 0 && youtubeWidth > 0 && youtubeHeight == youtubeWidth
//            )

    val metadataInfo = MetadataInfo(
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        trackNumber = trackNumber,
        duration = durationMillis,
        artUrl = null,
        normalizedUrlHost = null,
    )

    val ignoreScrobble = metadata.getLong(METADATA_KEY_ADVERTISEMENT) != 0L

    return metadataInfo to ignoreScrobble
}

private const val METADATA_KEY_ADVERTISEMENT = "android.media.metadata.ADVERTISEMENT"
