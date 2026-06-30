package com.arn.scrobble.media

import com.arn.scrobble.utils.Stuff


actual typealias PlatformPlaybackInfo = PlaybackInfo

actual fun transformPlaybackState(
    trackInfo: PlayingTrackInfo,
    playbackInfo: PlatformPlaybackInfo,
    options: TransformMetadataOptions
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
    if ((trackInfo.appId == Stuff.PACKAGE_SPOTIFY_WIN_EXE ||
                trackInfo.appId.equals(Stuff.PACKAGE_SPOTIFY_WIN_STORE, ignoreCase = true)) &&
        (playbackInfo.state == CommonPlaybackState.Playing &&
                !playbackInfo.canSkip &&
                trackInfo.album.isEmpty() &&
                trackInfo.trackNumber == 0)
    ) {
        ignoreScrobble = true
    }

    return commonPlaybackInfo to ignoreScrobble
}