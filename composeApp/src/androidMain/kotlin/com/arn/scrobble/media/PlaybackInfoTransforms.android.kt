package com.arn.scrobble.media

import android.media.session.PlaybackState
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.Stuff

actual typealias PlatformPlaybackInfo = PlaybackState

actual fun transformPlaybackState(
    trackInfo: PlayingTrackInfo,
    playbackInfo: PlatformPlaybackInfo,
    options: TransformMetadataOptions
): Pair<PlaybackInfo, Boolean> {
    val commonPlaybackState = when (playbackInfo.state) {
        PlaybackState.STATE_NONE -> CommonPlaybackState.None
        PlaybackState.STATE_PLAYING -> CommonPlaybackState.Playing
        PlaybackState.STATE_PAUSED -> CommonPlaybackState.Paused
        PlaybackState.STATE_STOPPED -> CommonPlaybackState.Stopped
        PlaybackState.STATE_BUFFERING -> CommonPlaybackState.Waiting
        else -> CommonPlaybackState.Other
    }

    val position = playbackInfo.position

    val commonPlaybackInfo = PlaybackInfo(
        appId = trackInfo.appId,
        state = commonPlaybackState,
        position = position,
        canSkip = playbackInfo.actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
    )

    var ignoreScrobble = false

    // do not scrobble spotify remote playback
    if (!options.scrobbleSpotifyRemote &&
        trackInfo.appId == Stuff.PACKAGE_SPOTIFY
        && playbackInfo.extras?.getBoolean("com.spotify.music.extra.ACTIVE_PLAYBACK_LOCAL") == false
    ) {
        Logger.i { "ignoring spotify remote playback" }
        ignoreScrobble = true
    }

    // do not scrobble youtube music ads (they are not seekable)
    // no longer works with latest YTM versions
    if (trackInfo.appId in arrayOf(
            Stuff.PACKAGE_YOUTUBE_MUSIC,
            Stuff.PACKAGE_YOUTUBE_TV
        ) &&
        playbackInfo.state == PlaybackState.STATE_PLAYING &&
        trackInfo.durationMillis > 0 &&
        playbackInfo.actions and PlaybackState.ACTION_SEEK_TO == 0L
    ) {
        Logger.i { "ignoring youtube music ad" }
        ignoreScrobble = true
    }

    return commonPlaybackInfo to ignoreScrobble
}