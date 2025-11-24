package com.arn.scrobble.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.media.PlayingTrackNotifyEvent
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

            val user = Scrobblables.currentAccount.value?.user ?: return@collect
            val scrobblingEvent =
                (playingTrackTrayInfo[suffix] as? PlayingTrackNotifyEvent.TrackPlaying)
                    ?: return@collect
            val scrobbleData = scrobblingEvent.scrobbleData


            when (itemId) {
                PanoTrayUtils.ItemId.TrackName -> {
                    val dialog = PanoDialog.MusicEntryInfo(
                        track = scrobbleData.toTrack(),
                        user = user
                    )
                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.ArtistName -> {
                    val dialog = PanoDialog.MusicEntryInfo(
                        artist = scrobbleData.toTrack().artist,
                        user = user
                    )
                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.AlbumName -> {
                    val dialog = PanoDialog.MusicEntryInfo(
                        album = scrobbleData.toTrack().album,
                        user = user
                    )
                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.Edit -> {
                    val dialog = PanoDialog.EditScrobble(
                        origScrobbleData = scrobblingEvent.origScrobbleData,
                        hash = scrobblingEvent.hash
                    )
                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.Block -> {
                    val blockedMetadata = BlockedMetadata(
                        track = scrobbleData.track,
                        artist = scrobbleData.artist,
                        album = scrobbleData.album.orEmpty(),
                        albumArtist = scrobbleData.albumArtist.orEmpty(),
                    )

                    val dialog = PanoDialog.BlockedMetadataAdd(
                        blockedMetadata = blockedMetadata,
                        hash = scrobblingEvent.hash
                    )

                    onOpenDialog(dialog)
                }

                PanoTrayUtils.ItemId.Error -> {
                    val errorEvent =
                        (playingTrackTrayInfo[suffix] as? PlayingTrackNotifyEvent.Error)
                            ?: return@collect

                    val scrobbleError = errorEvent.scrobbleError

                    if (scrobbleError.canFixMetadata) {
                        val dialog = PanoDialog.EditScrobble(
                            origScrobbleData = errorEvent.scrobbleData,
                            hash = scrobblingEvent.hash
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