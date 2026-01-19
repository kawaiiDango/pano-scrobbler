package com.arn.scrobble.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PanoTrayUtils
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Composable
actual fun NavFromOutsideEffect(
    onNavigate: (PanoRoute) -> Unit,
    isAndroidDialogActivity: Boolean,
) {
    LaunchedEffect(Unit) {
        PanoTrayUtils.onTrayMenuItemClicked.collect { id ->
            // settings
            if (id == PanoTrayUtils.ItemId.Settings.name) {
                onNavigate(PanoRoute.Prefs)
                return@collect
            }

            val splits = id.split(":", limit = 2)
            val itemId = splits.first().let { PanoTrayUtils.ItemId.valueOf(it) }
            val suffix = splits.getOrNull(1)
            val playingTrackTrayInfo = PanoNotifications.playingTrackTrayInfo.value

            val user = PlatformStuff.mainPrefs.data.map { it.currentAccount?.user }.first()
                ?: return@collect
            val scrobblingEvent =
                (playingTrackTrayInfo[suffix] as? PlayingTrackNotifyEvent.TrackPlaying)
                    ?: return@collect
            val scrobbleData = scrobblingEvent.scrobbleData


            when (itemId) {
                PanoTrayUtils.ItemId.TrackName -> {
                    val dialog = PanoRoute.Modal.MusicEntryInfo(
                        track = scrobbleData.toTrack(),
                        user = user
                    )
                    onNavigate(dialog)
                }

                PanoTrayUtils.ItemId.ArtistName -> {
                    val dialog = PanoRoute.Modal.MusicEntryInfo(
                        artist = scrobbleData.toTrack().artist,
                        user = user
                    )
                    onNavigate(dialog)
                }

                PanoTrayUtils.ItemId.AlbumName -> {
                    val dialog = PanoRoute.Modal.MusicEntryInfo(
                        album = scrobbleData.toTrack().album,
                        user = user
                    )
                    onNavigate(dialog)
                }

                PanoTrayUtils.ItemId.Edit -> {
                    val dialog = PanoRoute.Modal.EditScrobble(
                        origScrobbleData = scrobblingEvent.origScrobbleData,
                        hash = scrobblingEvent.hash
                    )
                    onNavigate(dialog)
                }

                PanoTrayUtils.ItemId.Block -> {
                    val blockedMetadata = BlockedMetadata(
                        track = scrobbleData.track,
                        artist = scrobbleData.artist,
                        album = scrobbleData.album.orEmpty(),
                        albumArtist = scrobbleData.albumArtist.orEmpty(),
                    )

                    val dialog = PanoRoute.Modal.BlockedMetadataAdd(
                        blockedMetadata = blockedMetadata,
                        hash = scrobblingEvent.hash
                    )

                    onNavigate(dialog)
                }

                PanoTrayUtils.ItemId.Error -> {
                    val errorEvent =
                        (playingTrackTrayInfo[suffix] as? PlayingTrackNotifyEvent.Error)
                            ?: return@collect

                    val scrobbleError = errorEvent.scrobbleError

                    if (scrobbleError.canFixMetadata) {
                        val dialog = PanoRoute.Modal.EditScrobble(
                            origScrobbleData = errorEvent.scrobbleData,
                            hash = scrobblingEvent.hash
                        )
                        onNavigate(dialog)
                    }
                }

                else -> {

                }
            }
        }
    }
}