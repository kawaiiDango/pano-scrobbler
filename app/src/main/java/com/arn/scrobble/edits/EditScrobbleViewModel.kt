package com.arn.scrobble.edits

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.NLService
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
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
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEdit
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEdit
import com.arn.scrobble.db.SimpleEditsDao.Companion.insertReplaceLowerCase
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class EditScrobbleViewModel : ViewModel() {
    private val _result = MutableSharedFlow<Result<ScrobbleData>>()
    val result = _result.asSharedFlow()

    private val _updatedAlbum = MutableSharedFlow<String>()
    val updatedAlbum = _updatedAlbum.asSharedFlow()

    private val _updatedAlbumArtist = MutableSharedFlow<String>()
    val updatedAlbumArtist = _updatedAlbumArtist.asSharedFlow()

    private val _suggestRegexEdit = MutableStateFlow<RegexEdit?>(null)
    val regexRecommendation = _suggestRegexEdit.asStateFlow()

    private val context = PlatformStuff.application

    fun doEdit(
        origScrobbleData: ScrobbleData,
        newScrobbleData: ScrobbleData,
        msid: String?,
        hash: Int,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val r = validateAsync(
                origScrobbleData,
                newScrobbleData,
                msid,
                hash,
            ).recoverCatching {
                if (it is LastfmUnscrobbler.CookiesInvalidatedException) {
                    throw LastfmUnscrobbler.CookiesInvalidatedException(context.getString(R.string.lastfm_reauth))
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
        hash: Int,
    ): Result<ScrobbleData> {
        val track = newScrobbleData.track.trim()
        val origTrack = origScrobbleData.track
        var album = newScrobbleData.album?.trim() ?: ""
        val origAlbum = origScrobbleData.album ?: ""
        var albumArtist = newScrobbleData.albumArtist?.trim() ?: ""
        val artist = newScrobbleData.artist.trim()
        val origArtist = origScrobbleData.artist
        val timeMillis = origScrobbleData.timestamp
        val isNowPlaying = timeMillis == 0L

        val regexEditsLearnt = PlatformStuff.mainPrefs.data.map { it.regexEditsLearnt }.first()
        val fetchAlbum = PlatformStuff.mainPrefs.data.map { it.fetchAlbum }.first()
        val fetchAlbumAndAlbumArtist = album.isBlank() && origAlbum.isBlank() && fetchAlbum
        val rescrobbleRequired = !isNowPlaying && (fetchAlbumAndAlbumArtist ||
                (track.equals(origTrack, ignoreCase = true) &&
                        artist.equals(origArtist, ignoreCase = true)
                        && (album != origAlbum || albumArtist.isNotBlank())))
        val scrobbleData = ScrobbleData(
            artist = artist,
            track = track,
            timestamp = if (timeMillis > 0) timeMillis else System.currentTimeMillis(),
            album = album,
            albumArtist = albumArtist,
            duration = origScrobbleData.duration,
            packageName = origScrobbleData.packageName,
        )
        val lastfmScrobblable =
            Scrobblables.all.value.firstOrNull { it.userAccount.type == AccountType.LASTFM }
        val lastfmScrobbleResult: Result<ScrobbleIgnored>

        val origTrackObj = Track(
            origTrack,
            null,
            Artist(origArtist),
            date = timeMillis,
            msid = msid
        )

        fun saveEdit() {
            if (!(track == origTrack && artist == origArtist && album == origAlbum && albumArtist == "")) {
                val dao = PanoDb.db.getSimpleEditsDao()
                val e = SimpleEdit(
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    track = track,
                    origArtist = origArtist,
                    origAlbum = origAlbum,
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
            return Result.failure(IllegalArgumentException(context.getString(R.string.required_fields_empty)))
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

            if (album.isBlank() && fetchedTrack?.album != null) {
                album = fetchedTrack.album.name
                scrobbleData.album = fetchedTrack.album.name
                _updatedAlbum.emit(album)
            }
            if (albumArtist.isBlank() && fetchedTrack?.album?.artist != null) {
                albumArtist = fetchedTrack.album.artist.name
                scrobbleData.albumArtist = albumArtist
                _updatedAlbumArtist.emit(albumArtist)
            }
        }

        // edit lastfm first
        if (lastfmScrobblable != null) {
            lastfmScrobbleResult = lastfmScrobblable.scrobble(scrobbleData)
            if (lastfmScrobbleResult.map { it.ignored }.getOrNull() == true) {
                return Result.failure(ScrobbleIgnoredException(timeMillis, ::saveEdit))
            } else {
                if (!isNowPlaying) {
                    // The user might submit the edit after it has been scrobbled, so delete anyways
                    val deleteResult = lastfmScrobblable.delete(origTrackObj)
                    if (deleteResult.isSuccess)
                        CachedTracksDao.deltaUpdateAll(origTrackObj, -1, DirtyUpdate.BOTH)
                    else if (deleteResult.exceptionOrNull() is LastfmUnscrobbler.CookiesInvalidatedException) {
                        return Result.failure(deleteResult.exceptionOrNull()!!)
                    }
                } else {
                    lastfmScrobblable.updateNowPlaying(scrobbleData)
                }

                if (rescrobbleRequired)
                    lastfmScrobblable.scrobble(scrobbleData)
            }

            val _artist = Artist(artist)

            val trackObj = Track(
                track,
                Album(album, _artist),
                _artist,
                date = timeMillis
            )

            CachedTracksDao.deltaUpdateAll(trackObj, 1, DirtyUpdate.BOTH)
        }


        // scrobble everywhere else (delete first)
        Scrobblables.all.value
            .filter {
                it.userAccount.type != AccountType.LASTFM &&
                        it.userAccount.type != AccountType.PLEROMA &&
                        it.userAccount.type != AccountType.FILE

            }
            .forEach {
                if (!isNowPlaying)
                    it.delete(origTrackObj)
                // ListenBrainz cannot have two scrobbles with the same timestamp and delete is not immediate
                // so add 1 sec
                if (it is ListenBrainz)
                    it.scrobble(scrobbleData.copy(timestamp = scrobbleData.timestamp + 1000))
                else
                    it.scrobble(scrobbleData)
                if (isNowPlaying)
                    it.updateNowPlaying(scrobbleData)
            }

        // track player
        scrobbleData.packageName?.let {
            val scrobbleSource =
                ScrobbleSource(timeMillis = scrobbleData.timestamp, pkg = it)
            PanoDb.db.getScrobbleSourcesDao().insert(scrobbleSource)
        }

        saveEdit()

        // suggest regex edit
        if (!regexEditsLearnt) {
            val dao = PanoDb.db.getRegexEditsDao()

            val presetsAvailable = (RegexPresets.presetKeys - dao.allPresets().first()
                .map { it.preset }.toSet())
                .mapIndexed { index, key ->
                    RegexPresets.getPossiblePreset(
                        RegexEdit(order = index, preset = key)
                    )
                }

            if (presetsAvailable.isNotEmpty()) {
                val suggestedRegexReplacements = dao.performRegexReplace(
                    scrobbleData,
                    null,
                    presetsAvailable,
                )

                val firstSuggestion =
                    suggestedRegexReplacements.values.firstOrNull { it.isNotEmpty() }?.firstOrNull()

                val replacementsInEdit =
                    dao.performRegexReplace(scrobbleData, null, presetsAvailable)

                if (firstSuggestion != null && replacementsInEdit.values.all { it.isEmpty() }) {
                    _suggestRegexEdit.emit(firstSuggestion)
                }
            }
        }

        if (!isNowPlaying) {
            lockScrobble(hash, false)
            context.sendBroadcast(
                Intent(NLService.iCANCEL)
                    .putExtra(NLService.B_HASH, hash)
                    .setPackage(context.packageName),
                NLService.BROADCAST_PERMISSION
            )
        }

        return Result.success(scrobbleData)
    }

    fun lockScrobble(hash: Int, lock: Boolean) {
        if (hash == -1) return

        // do not scrobble until the dialog is dismissed

        val intent = Intent(NLService.iSCROBBLE_SUBMIT_LOCK_S)
            .setPackage(context.packageName)
            .putExtra(NLService.B_LOCKED, lock)
            .putExtra(NLService.B_HASH, hash)

        context.sendBroadcast(intent, NLService.BROADCAST_PERMISSION)
    }
}