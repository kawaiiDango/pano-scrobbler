package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.NLService
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.IHasImage
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.db.CachedAlbum.Companion.toCachedAlbum
import com.arn.scrobble.db.CachedAlbumsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedArtist.Companion.toCachedArtist
import com.arn.scrobble.db.CachedArtistsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.db.CachedTracksDao.Companion.deltaUpdate
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext


class InfoVM : ViewModel() {

    private val _infoListReceiver = MutableStateFlow<List<InfoHolder>?>(null)
    val infoList = _infoListReceiver.asStateFlow()
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    private val initialEntryAndUsername = MutableStateFlow<Pair<MusicEntry, String>?>(null)
    lateinit var originalEntriesMap: Map<String, MusicEntry>

    init {
        viewModelScope.launch {
            initialEntryAndUsername
                .filterNotNull()
                .collectLatest { (entry, username) ->
                    val _username =
                        if (Scrobblables.current.value?.userAccount?.type == AccountType.LASTFM)
                            username
                        else
                            null

                    val infos = createInitialData(entry)

                    _infoListReceiver.emit(infos.map { (k, v) ->
                        InfoHolder(k, v)
                    })

                    val infosFetched = withContext(Dispatchers.IO) {
                        getInfos(infos, _username)
                    }.map { (k, v) ->
                        InfoHolder(k, v)
                    }

                    _infoListReceiver.emit(infosFetched)
                    _hasLoaded.emit(true)
                }
        }
    }

    fun setMusicEntryIfNeeded(entry: MusicEntry, username: String) {
        viewModelScope.launch {
            initialEntryAndUsername.emit(entry to username)
        }
    }

    fun updateInfo(info: InfoHolder) {
        var prevInfo: InfoHolder? = null

        val newInfos = _infoListReceiver.value?.map {
            if (it.type == info.type) {
                prevInfo = it
                info
            } else {
                it
            }
        }
        _infoListReceiver.value = newInfos

        if (prevInfo?.entry is Track && info.entry is Track) {
            if ((prevInfo?.entry as Track).userloved != info.entry.userloved && info.entry.userloved != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    ScrobbleEverywhere.loveOrUnlove(info.entry, info.entry.userloved)
                }
            }
        }
    }

    private fun createInitialData(entryp: MusicEntry): Map<String, MusicEntry> {
        val entriesMap = mutableMapOf<String, MusicEntry>()

        when (entryp) {
            is Track -> {
                val entry = entryp.copy(playcount = null, listeners = null)
                entriesMap[NLService.B_TRACK] = entry.copy(album = null)
                entriesMap[NLService.B_ARTIST] = entry.artist
                entry.album?.let { entriesMap[NLService.B_ALBUM] = it.copy(artist = entry.artist) }
            }

            is Album -> {
                val entry = entryp.copy(playcount = null, listeners = null)
                entriesMap[NLService.B_ALBUM] = entry
                entriesMap[NLService.B_ARTIST] = entry.artist!!
            }

            is Artist -> {
                entriesMap[NLService.B_ARTIST] = entryp.copy(playcount = null, listeners = null)
            }
        }
        originalEntriesMap = entriesMap

        return entriesMap
    }

    private suspend fun getInfos(
        infoMapp: Map<String, MusicEntry>,
        username: String?
    ) = supervisorScope {
        val db = PanoDb.db
        val infoMap = infoMapp.toMutableMap()

        fun doDirtyDeltaUpdates(
            artist: Artist?,
            album: Album?,
            track: Track?,
            albumArtist: Artist?
        ) {
            if (username == null)
                return

            track?.let {
                if (it.userplaycount != null)
                    db.getCachedTracksDao()
                        .deltaUpdate(
                            it.toCachedTrack()
                                .apply { userPlayCount = it.userplaycount },
                            0,
                            DirtyUpdate.DIRTY_ABSOLUTE
                        )
            }
            album?.let {
                if (it.userplaycount != null)
                    db.getCachedAlbumsDao()
                        .deltaUpdate(
                            it.toCachedAlbum().apply { userPlayCount = it.userplaycount },
                            0, DirtyUpdate.DIRTY_ABSOLUTE
                        )
            }
            artist?.let {
                if (it.userplaycount != null)
                    db.getCachedArtistsDao()
                        .deltaUpdate(
                            it.toCachedArtist().apply { userPlayCount = it.userplaycount },
                            0,
                            DirtyUpdate.DIRTY_ABSOLUTE
                        )
            }
            albumArtist?.let {
                if (it.userplaycount != null)
                    db.getCachedArtistsDao()
                        .deltaUpdate(
                            it.toCachedArtist().apply { userPlayCount = it.userplaycount },
                            0,
                            DirtyUpdate.DIRTY_ABSOLUTE
                        )
            }
        }

        var albumArtist: Artist? = null
        var album = infoMap[NLService.B_ALBUM] as? Album
        val artist = infoMap[NLService.B_ARTIST] as? Artist
        val track = infoMap[NLService.B_TRACK] as? Track

        val trackDef = async {
            track?.let {
                Requesters.lastfmUnauthedRequester.getInfo(it, username)
            }?.getOrNull()
        }

        if (track != null) {
            val trackInfo = trackDef.await()
            if (trackInfo != null) {
                infoMap[NLService.B_TRACK] = trackInfo
            }

            if (album == null)
                album = trackInfo?.album
            albumArtist = trackInfo?.album?.artist

        }

        val artistDef = async {
            artist?.let {
                Requesters.lastfmUnauthedRequester.getInfo(it, username)
            }?.getOrNull()
        }

        val albumArtistDef = async {
            if (albumArtist != null && albumArtist.name.lowercase() != artist?.name?.lowercase())
                albumArtist.let {
                    Requesters.lastfmUnauthedRequester.getInfo(it, username)
                }.getOrNull()
            else
                null
        }

        val albumDef = async {
            album?.let {
                Requesters.lastfmUnauthedRequester.getInfo(it, username)
            }?.getOrNull()
        }

        artistDef.await()?.let {
            infoMap[NLService.B_ARTIST] = it
        }
        albumArtistDef.await()?.let {
            infoMap[NLService.B_ALBUM_ARTIST] = it
        }
        albumDef.await()?.let {
            infoMap[NLService.B_ALBUM] = it
        }

        // dirty delta updates only for lastfm and self
        if (Scrobblables.current.value?.userAccount?.type == AccountType.LASTFM &&
            Scrobblables.current.value?.userAccount?.user?.isSelf == true
        ) {
            doDirtyDeltaUpdates(
                artistDef.await(),
                albumDef.await(),
                trackDef.await(),
                albumArtistDef.await()
            )
        }

        infoMap.toMap()
    }

    data class InfoHolder(
        val type: String,
        val entry: MusicEntry,
        val headerExpanded: Boolean = false,
        val wikiExpanded: Boolean = false,
        val trackListExpanded: Boolean = false,
    ) {
        var hasImage: Boolean = if (entry is IHasImage) entry.webp300 != null else false
    }
}