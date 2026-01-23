package com.arn.scrobble.edits

import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.ScrobbleIgnoredException
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.SimpleEditsDao.Companion.insertReplaceLowerCase
import com.arn.scrobble.db.SimpleEditsDao.Companion.performEdit
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.rank_change_no_change
import pano_scrobbler.composeapp.generated.resources.required_fields_empty

class EditScrobbleUtils(private val viewModelScope: CoroutineScope) {
    private val _result = MutableSharedFlow<Pair<ScrobbleData?, Result<Unit>>>()
    val result = _result.asSharedFlow()

    private val _updatedAlbum = MutableSharedFlow<Pair<ScrobbleData, String>>()
    val updatedAlbum = _updatedAlbum.asSharedFlow()

    private val _updatedAlbumArtist = MutableSharedFlow<Pair<ScrobbleData, String>>()
    val updatedAlbumArtist = _updatedAlbumArtist.asSharedFlow()

    private val _editData = MutableSharedFlow<Pair<String, Track>>(extraBufferCapacity = 1)
    val editDataFlow = _editData.asSharedFlow()

    fun doEdit(
        simpleEdit: SimpleEdit,
        origScrobbleData: ScrobbleData?,
        msid: String?,
        hash: Int?,
        key: String?,
        save: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (origScrobbleData != null) {
                val newScrobbleData = simpleEdit.performEdit(origScrobbleData)

                val r = scrobbleAndDelete(
                    origScrobbleData,
                    newScrobbleData,
                    msid,
                    isNowPlaying = hash != null
                )
                    .onSuccess { editedSd ->
                        if (save)
                            PanoDb.db.getSimpleEditsDao().insertReplaceLowerCase(simpleEdit)

                        if (hash != null) { // from notification
                            notifyPlayingTrackEvent(
                                PlayingTrackNotifyEvent.TrackScrobbleLocked(
                                    hash = hash,
                                    locked = false
                                ),
                            )

                            notifyPlayingTrackEvent(
                                PlayingTrackNotifyEvent.TrackCancelled(
                                    hash = hash,
                                    showUnscrobbledNotification = false,
                                )
                            )
                        }

                        if (key != null) { // from scrobble history
                            _editData.emit(key to editedSd.toTrack())
                        }
                    }
                    .recoverCatching {
                        if (it is ScrobbleIgnoredException) {
                            if (System.currentTimeMillis() - it.scrobbleTime >= Stuff.LASTFM_MAX_PAST_SCROBBLE && save) {
                                PanoDb.db.getSimpleEditsDao().insertReplaceLowerCase(simpleEdit)
                                throw ApiException(
                                    -1,
                                    "Scrobble too old, edit saved only for future scrobbles"
                                )
                            }
                        }
                        throw it
                    }
                _result.emit(origScrobbleData to r.map { })
            } else if (save) {
                PanoDb.db.getSimpleEditsDao().insertReplaceLowerCase(simpleEdit)
                _result.emit(origScrobbleData to Result.success(Unit))
            }
        }
    }

    private suspend fun scrobbleAndDelete(
        _origScrobbleData: ScrobbleData,
        _newScrobbleData: ScrobbleData,
        msid: String?,
        isNowPlaying: Boolean
    ): Result<ScrobbleData> {
        val newScrobbleData = _newScrobbleData.trimmed()
        val origScrobbleData = _origScrobbleData.trimmed()

        val track = newScrobbleData.track
        val origTrack = origScrobbleData.track
        var album = newScrobbleData.album
        val origAlbum = origScrobbleData.album
        var albumArtist = newScrobbleData.albumArtist
        val origAlbumArtist = origScrobbleData.albumArtist
        val artist = newScrobbleData.artist
        val origArtist = origScrobbleData.artist
        val timeMillis = origScrobbleData.timestamp

        if (track.isEmpty() || artist.isEmpty()) {
            return Result.failure(IllegalArgumentException(getString(Res.string.required_fields_empty)))
        }

        val fetchAlbum = PlatformStuff.mainPrefs.data.map { it.fetchAlbum }.first()
        val fetchAlbumAndAlbumArtist =
            album.isNullOrEmpty() && origAlbum.isNullOrEmpty() && fetchAlbum
        val rescrobbleRequired = !isNowPlaying && (fetchAlbumAndAlbumArtist ||
                (track.equals(origTrack, ignoreCase = true) &&
                        artist.equals(origArtist, ignoreCase = true)
                        && (album != origAlbum || !albumArtist.isNullOrEmpty())))
        var scrobbleData = ScrobbleData(
            artist = artist,
            track = track,
            timestamp = if (timeMillis > 0) timeMillis else System.currentTimeMillis(),
            album = album,
            albumArtist = albumArtist,
            duration = origScrobbleData.duration,
            appId = origScrobbleData.appId,
        )
        val scrobblable = Scrobblables.current
        val scrobbleResult: Result<ScrobbleIgnored>

        val origTrackObj = Track(
            origTrack,
            null,
            Artist(origArtist),
            date = timeMillis,
            msid = msid
        )

        if (track == origTrack &&
            artist == origArtist && album == origAlbum && albumArtist == origAlbumArtist &&
            !(album.isNullOrEmpty() && fetchAlbum)
        ) {
            return Result.failure(IllegalArgumentException(getString(Res.string.rank_change_no_change)))
        }

        if (fetchAlbumAndAlbumArtist) {
            val newTrack = Track(track, null, Artist(artist))

            val fetchedTrack = Requesters.lastfmUnauthedRequester
                .getInfo(newTrack)
                .getOrNull()

            if (album.isNullOrEmpty() && fetchedTrack?.album != null) {
                album = fetchedTrack.album.name
                scrobbleData = scrobbleData.copy(album = album)
                _updatedAlbum.emit(_origScrobbleData to album)
            }
            if (albumArtist.isNullOrEmpty() && fetchedTrack?.album?.artist != null) {
                albumArtist = fetchedTrack.album.artist.name
                scrobbleData = scrobbleData.copy(albumArtist = albumArtist)
                _updatedAlbumArtist.emit(_origScrobbleData to albumArtist)
            }
        }

        if (scrobblable != null) {
            scrobbleResult = scrobblable.scrobble(scrobbleData)
            if (scrobbleResult.map { it.ignored }.getOrNull() == true) {
                return Result.failure(ScrobbleIgnoredException(timeMillis))
            } else {
                if (!isNowPlaying) {
                    // The user might submit the edit after it has been scrobbled, so delete anyways
                    val deleteResult = scrobblable.delete(origTrackObj)
                    if (deleteResult.isSuccess)
                        CachedTracksDao.deltaUpdateAll(origTrackObj, -1, DirtyUpdate.BOTH)
                    else if (deleteResult.exceptionOrNull() is LastFm.CookiesInvalidatedException) {
                        return Result.failure(deleteResult.exceptionOrNull()!!)
                    }
                } else {
                    scrobblable.updateNowPlaying(scrobbleData)
                }

                if (rescrobbleRequired)
                    scrobblable.scrobble(scrobbleData)
            }

            val _artist = Artist(artist)

            val trackObj = Track(
                track,
                album?.let { Album(it, _artist) },
                _artist,
                date = timeMillis
            )

            CachedTracksDao.deltaUpdateAll(trackObj, 1, DirtyUpdate.BOTH)
        }

        // track player
        scrobbleData.appId?.let {
            val scrobbleSource =
                ScrobbleSource(timeMillis = scrobbleData.timestamp, pkg = it)
            PanoDb.db.getScrobbleSourcesDao().insert(scrobbleSource)
        }

        return Result.success(scrobbleData)
    }

    fun deleteSimpleEdit(simpleEdit: SimpleEdit) {
        viewModelScope.launch(Dispatchers.IO) {
            PanoDb.db.getSimpleEditsDao().delete(simpleEdit)
            _result.emit(null to Result.success(Unit))
        }
    }
}