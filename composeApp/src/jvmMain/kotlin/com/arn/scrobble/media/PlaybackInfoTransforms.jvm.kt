package com.arn.scrobble.media

import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.Stuff


actual typealias PlatformPlaybackInfo = PlaybackInfo

actual fun transformPlaybackState(
    trackInfo: PlayingTrackInfo,
    playbackInfo: PlatformPlaybackInfo,
    scrobbleSpotifyRemote: Boolean,
): Pair<PlaybackInfo, Boolean> {
    val commonPlaybackInfo = playbackInfo
    var ignoreScrobble = false

    /*
    Spotify ads sample:

[6/14/26, 9:42 PM] Info: (scrobbler) MetadataInfo(title=—, artist=, album=, albumArtist=, trackNumber=0, duration=15177, artUrl=null, normalizedUrlHost=null) None 0744bc20
[6/14/26, 9:42 PM] Info: (scrobbler) PlaybackInfo(state=Playing, position=2780, canSkip=false) lastPlaybackState: None 0744bc20
[6/14/26, 9:43 PM] Info: (scrobbler) MetadataInfo(title=—, artist=, album=, albumArtist=, trackNumber=0, duration=20000, artUrl=null, normalizedUrlHost=null) Playing 0744bc20
[6/14/26, 9:43 PM] Info: (scrobbler) PlaybackInfo(state=Playing, position=0, canSkip=false) lastPlaybackState: None 0744bc20

     */
    val isSpotify = (trackInfo.appId == Stuff.PACKAGE_SPOTIFY_WIN_EXE ||
            trackInfo.appId.equals(Stuff.PACKAGE_SPOTIFY_WIN_STORE, ignoreCase = true))

    if (isSpotify &&
        (playbackInfo.state == CommonPlaybackState.Playing &&
                !playbackInfo.canSkip &&
                trackInfo.album.isEmpty() &&
                trackInfo.trackNumber == 0)
    ) {
        Logger.i { "ignoring spotify ad" }
        ignoreScrobble = true
    } else if (isSpotify && !scrobbleSpotifyRemote) {
        Logger.i { "ignoring spotify remote playback" }
        ignoreScrobble = true
    }

    return commonPlaybackInfo to ignoreScrobble
}