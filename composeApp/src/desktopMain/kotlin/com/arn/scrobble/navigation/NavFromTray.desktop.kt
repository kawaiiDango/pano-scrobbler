package com.arn.scrobble.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.media.PlayingTrackNotificationState
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PanoTrayUtils

@Composable
actual fun NavFromTrayEffect(
    onOpenDialog: (PanoDialog) -> Unit,
) {
    LaunchedEffect(Unit) {
        PanoTrayUtils.onTrayMenuItemClicked.collect { id ->
            val splits = id.split(":", limit = 2)
            val itemId = splits.first().let { PanoTrayUtils.ItemId.valueOf(it) }
            val suffix = splits.getOrNull(1)
            val playingTrackTrayInfo = PanoNotifications.playingTrackTrayInfo.value

            val user = Scrobblables.currentScrobblableUser ?: return@collect
            val scrobblingTrackInfo =
                (playingTrackTrayInfo[suffix] as? PlayingTrackNotificationState.Scrobbling)
                    ?.trackInfo
                    ?: return@collect

            when (itemId) {
                PanoTrayUtils.ItemId.TrackName -> {
                    val dialog = PanoDialog.MusicEntryInfo(
                        track = scrobblingTrackInfo.toTrack(),
                        user = user
                    )
                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.ArtistName -> {
                    val dialog = PanoDialog.MusicEntryInfo(
                        artist = scrobblingTrackInfo.toTrack().artist,
                        user = user
                    )
                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.Edit -> {
                    val dialog = PanoDialog.EditScrobble(
                        scrobbleData = scrobblingTrackInfo.toScrobbleData(),
                        hash = scrobblingTrackInfo.hash
                    )
                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.Block -> {
                    val blockedMetadata = BlockedMetadata(
                        track = scrobblingTrackInfo.title,
                        artist = scrobblingTrackInfo.artist,
                        album = scrobblingTrackInfo.album,
                        albumArtist = scrobblingTrackInfo.albumArtist,
                    )

                    val dialog = PanoDialog.BlockedMetadataAdd(
                        blockedMetadata = blockedMetadata,
                        hash = scrobblingTrackInfo.hash
                    )

                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.Error -> {
                    val errorState =
                        (playingTrackTrayInfo[suffix] as? PlayingTrackNotificationState.Error)
                            ?: return@collect

                    val trackInfo = errorState.trackInfo
                    val scrobbleError = errorState.scrobbleError

                    if (scrobbleError.canFixMetadata) {
                        val dialog = PanoDialog.EditScrobble(
                            scrobbleData = trackInfo.toScrobbleData(),
                            hash = trackInfo.hash
                        )
                        onOpenDialog(dialog)
                    }
                }

                else -> {

                }
            }
        }
    }
}