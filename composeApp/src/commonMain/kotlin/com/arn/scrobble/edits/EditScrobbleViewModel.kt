package com.arn.scrobble.edits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.LastfmUnscrobbler
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.ScrobbleIgnoredException
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.SimpleEditsDao.Companion.insertReplaceLowerCase
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.lastfm_reauth
import pano_scrobbler.composeapp.generated.resources.required_fields_empty

class EditScrobbleViewModel : ViewModel() {
    private val _result = MutableSharedFlow<Result<ScrobbleData>>()
    val result = _result.asSharedFlow()

    private val _updatedAlbum = MutableSharedFlow<String>()
    val updatedAlbum = _updatedAlbum.asSharedFlow()

    private val _updatedAlbumArtist = MutableSharedFlow<String>()
    val updatedAlbumArtist = _updatedAlbumArtist.asSharedFlow()

    private val _suggestRegexEdit = MutableStateFlow<RegexEdit?>(null)
    val regexRecommendation = _suggestRegexEdit.asStateFlow()


    fun doEdit(
        origScrobbleData: ScrobbleData,
        newScrobbleData: ScrobbleData,
        msid: String?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = validateAsync(
                origScrobbleData,
                newScrobbleData,
                msid,
            ).recoverCatching {
                if (it is LastfmUnscrobbler.CookiesInvalidatedException) {
                    throw LastfmUnscrobbler.CookiesInvalidatedException(getString(Res.string.lastfm_reauth))
                } else if (it is ScrobbleIgnoredException) {
                    if (System.currentTimeMillis() - it.scrobbleTime >= Stuff.LASTFM_MAX_PAST_SCROBBLE) {
                        it.altAction()
                        throw ApiException(
                            -1,
                            "Scrobble too old, edit saved only for future scrobbles"
                        )
                    }
                }
                throw it
            }
            _result.emit(r)
        }
    }

    private suspend fun validateAsync(
        origScrobbleData: ScrobbleData,
        newScrobbleData: ScrobbleData,
        msid: String?,
    ): Result<ScrobbleData> {
        val track = newScrobbleData.track.trim()
        val origTrack = origScrobbleData.track
        var album = newScrobbleData.album?.trim()?.ifEmpty { null }
        val origAlbum = origScrobbleData.album
        var albumArtist = newScrobbleData.albumArtist?.trim()?.ifEmpty { null }
        val artist = newScrobbleData.artist.trim()
        val origArtist = origScrobbleData.artist
        val timeMillis = origScrobbleData.timestamp
        val isNowPlaying = timeMillis == 0L

        val regexEditsLearnt = PlatformStuff.mainPrefs.data.map { it.regexEditsLearnt }.first()
        val fetchAlbum = PlatformStuff.mainPrefs.data.map { it.fetchAlbum }.first()
        val fetchAlbumAndAlbumArtist =
            album.isNullOrBlank() && origAlbum.isNullOrBlank() && fetchAlbum
        val rescrobbleRequired = !isNowPlaying && (fetchAlbumAndAlbumArtist ||
                (track.equals(origTrack, ignoreCase = true) &&
                        artist.equals(origArtist, ignoreCase = true)
                        && (album != origAlbum || !albumArtist.isNullOrBlank())))
        var scrobbleData = ScrobbleData(
            artist = artist,
            track = track,
            timestamp = if (timeMillis > 0) timeMillis else System.currentTimeMillis(),
            album = album,
            albumArtist = albumArtist,
            duration = origScrobbleData.duration,
            appId = origScrobbleData.appId,
        )
        val scrobblable = Scrobblables.current.value
        val scrobbleResult: Result<ScrobbleIgnored>

        val origTrackObj = Track(
            origTrack,
            null,
            Artist(origArtist),
            date = timeMillis,
            msid = msid
        )

        suspend fun saveEdit() {
            if (!(track == origTrack && artist == origArtist && album == origAlbum && albumArtist == "")) {
                val dao = PanoDb.db.getSimpleEditsDao()
                val e = SimpleEdit(
                    artist = artist,
                    album = album ?: "",
                    albumArtist = albumArtist ?: "",
                    track = track,
                    origArtist = origArtist,
                    origAlbum = origAlbum ?: "",
                    origTrack = origTrack,
                )
                dao.insertReplaceLowerCase(e)
                dao.deleteLegacy(
                    origArtist.hashCode().toString() + origAlbum.hashCode()
                        .toString() + origTrack.hashCode().toString()
                )
            }
        }

        if (track.isBlank() || artist.isBlank()) {
            return Result.failure(IllegalArgumentException(getString(Res.string.required_fields_empty)))
        }

        if (!isNowPlaying && track == origTrack &&
            artist == origArtist && album == origAlbum && albumArtist == "" &&
            !(album == "" && fetchAlbum)
        ) {
            return Result.success(newScrobbleData)
        }

        if (fetchAlbumAndAlbumArtist) {
            val newTrack = Track(track, null, Artist(artist))

            val fetchedTrack = Requesters.lastfmUnauthedRequester
                .getInfo(newTrack)
                .getOrNull()

            if (album.isNullOrBlank() && fetchedTrack?.album != null) {
                album = fetchedTrack.album.name
                scrobbleData = scrobbleData.copy(album = album)
                _updatedAlbum.emit(album)
            }
            if (albumArtist.isNullOrBlank() && fetchedTrack?.album?.artist != null) {
                albumArtist = fetchedTrack.album.artist.name
                scrobbleData = scrobbleData.copy(albumArtist = albumArtist)
                _updatedAlbumArtist.emit(albumArtist)
            }
        }

        if (scrobblable != null) {
            scrobbleResult = scrobblable.scrobble(scrobbleData)
            if (scrobbleResult.map { it.ignored }.getOrNull() == true) {
                return Result.failure(ScrobbleIgnoredException(timeMillis, ::saveEdit))
            } else {
                if (!isNowPlaying) {
                    // The user might submit the edit after it has been scrobbled, so delete anyways
                    val deleteResult = scrobblable.delete(origTrackObj)
                    if (deleteResult.isSuccess)
                        CachedTracksDao.deltaUpdateAll(origTrackObj, -1, DirtyUpdate.BOTH)
                    else if (deleteResult.exceptionOrNull() is LastfmUnscrobbler.CookiesInvalidatedException) {
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

        saveEdit()

        // suggest regex edit
//        if (!regexEditsLearnt) {
//            val dao = PanoDb.db.getRegexEditsDao()
//
//            val presetsAvailable = (RegexPresets.presetKeys - dao.allPresets().first()
//                .map { it.preset }.toSet())
//                .mapIndexed { index, key ->
//                    RegexPresets.getPossiblePreset(
//                        RegexEdit(order = index, preset = key)
//                    )
//                }
//
//            if (presetsAvailable.isNotEmpty()) {
//                val suggestedRegexReplacements = dao.performRegexReplace(
//                    scrobbleData,
//                    presetsAvailable,
//                ).fieldsMatched
//
//                val firstSuggestion =
//                    suggestedRegexReplacements.values.firstOrNull { it.isNotEmpty() }?.firstOrNull()
//
//                val replacementsInEdit =
//                    dao.performRegexReplace(scrobbleData, presetsAvailable)
//
//                if (firstSuggestion != null && replacementsInEdit.scrobbleData == null) {
//                    _suggestRegexEdit.emit(firstSuggestion)
//                }
//            }
//        }

        return Result.success(scrobbleData)
    }


}