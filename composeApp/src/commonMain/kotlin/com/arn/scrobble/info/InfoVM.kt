package com.arn.scrobble.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEverywhere
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.CachedAlbum.Companion.toCachedAlbum
import com.arn.scrobble.db.CachedAlbumsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedArtist.Companion.toCachedArtist
import com.arn.scrobble.db.CachedArtistsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.db.CachedTracksDao.Companion.deltaUpdate
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext


class InfoVM : ViewModel() {

    private val _infoMap = MutableStateFlow<Map<Int, MusicEntry>?>(null)
    val infoMap = _infoMap.asStateFlow()
    private val _infoLoaded = MutableStateFlow(false)
    val infoLoaded = _infoLoaded.asStateFlow()
    private val initialEntryAndUsername = MutableStateFlow<Pair<MusicEntry, String>?>(null)
    lateinit var originalEntriesMap: Map<Int, MusicEntry>

    private val _userTags = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val userTags = _userTags.asStateFlow()
    val userTagsHistory = PlatformStuff.mainPrefs.data.map { it.tagHistory }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

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

                    _userTags.emit(emptyMap())
                    _infoMap.emit(infos)

                    val infosFetched = withContext(Dispatchers.IO) {
                        getInfos(infos, _username)
                    }

                    _infoMap.emit(infosFetched)
                    _infoLoaded.emit(true)
                }
        }
    }

    fun setMusicEntryIfNeeded(entry: MusicEntry, username: String) {
        viewModelScope.launch {
            initialEntryAndUsername.emit(entry to username)
        }
    }

    fun setLoved(track: Track, loved: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            ScrobbleEverywhere.loveOrUnlove(track, loved)
        }
    }

    private fun createInitialData(entryp: MusicEntry): Map<Int, MusicEntry> {
        val entriesMap = mutableMapOf<Int, MusicEntry>()

        when (entryp) {
            is Track -> {
                val entry = entryp.copy(playcount = null, listeners = null)
                entriesMap[Stuff.TYPE_TRACKS] = entry.copy(album = null)
                entriesMap[Stuff.TYPE_ARTISTS] = entry.artist
                entry.album?.let { entriesMap[Stuff.TYPE_ALBUMS] = it.copy(artist = entry.artist) }
            }

            is Album -> {
                val entry = entryp.copy(playcount = null, listeners = null)
                entriesMap[Stuff.TYPE_ALBUMS] = entry
                entriesMap[Stuff.TYPE_ARTISTS] = entry.artist!!
            }

            is Artist -> {
                entriesMap[Stuff.TYPE_ARTISTS] = entryp.copy(playcount = null, listeners = null)
            }
        }
        originalEntriesMap = entriesMap

        return entriesMap
    }

    private suspend fun getInfos(
        infoMapp: Map<Int, MusicEntry>,
        username: String?,
    ) = supervisorScope {
        val db = PanoDb.db
        val infoMap = infoMapp.toMutableMap()

        suspend fun doDirtyDeltaUpdates(
            artist: Artist?,
            album: Album?,
            track: Track?,
            albumArtist: Artist?,
        ) {
            if (username == null)
                return

            track?.let {
                if (it.userplaycount != null)
                    db.getCachedTracksDao()
                        .deltaUpdate(
                            it.toCachedTrack().copy(userPlayCount = it.userplaycount),
                            0,
                            DirtyUpdate.DIRTY_ABSOLUTE
                        )
            }
            album?.let {
                if (it.userplaycount != null)
                    db.getCachedAlbumsDao()
                        .deltaUpdate(
                            it.toCachedAlbum().copy(userPlayCount = it.userplaycount),
                            0, DirtyUpdate.DIRTY_ABSOLUTE
                        )
            }
            artist?.let {
                if (it.userplaycount != null)
                    db.getCachedArtistsDao()
                        .deltaUpdate(
                            it.toCachedArtist().copy(userPlayCount = it.userplaycount),
                            0,
                            DirtyUpdate.DIRTY_ABSOLUTE
                        )
            }
            albumArtist?.let {
                if (it.userplaycount != null)
                    db.getCachedArtistsDao()
                        .deltaUpdate(
                            it.toCachedArtist().copy(userPlayCount = it.userplaycount),
                            0,
                            DirtyUpdate.DIRTY_ABSOLUTE
                        )
            }
        }

        var albumArtist: Artist? = null
        var album = infoMap[Stuff.TYPE_ALBUMS] as? Album
        val artist = infoMap[Stuff.TYPE_ARTISTS] as? Artist
        val track = infoMap[Stuff.TYPE_TRACKS] as? Track

        val trackDef = async {
            track?.let {
                Requesters.lastfmUnauthedRequester.getInfo(it, username)
            }?.getOrNull()
        }

        if (track != null) {
            val trackInfo = trackDef.await()
            if (trackInfo != null) {
                infoMap[Stuff.TYPE_TRACKS] = trackInfo
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
            infoMap[Stuff.TYPE_ARTISTS] = it
        }
        albumArtistDef.await()?.let {
            infoMap[Stuff.TYPE_ALBUM_ARTISTS] = it
        }
        albumDef.await()?.let {
            infoMap[Stuff.TYPE_ALBUMS] = it
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

    // user tags

    fun loadTagsIfNeeded(type: Int) {
        if (type in userTags.value)
            return
        val entry = infoMap.value?.get(type) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            populateTagHistoryIfNeeded()

            val tags = (Scrobblables.current.value as? LastFm)
                ?.getUserTagsFor(entry)
                ?.map {
                    it.toptags.tag.map { it.name }
                        .toSet()
                }
                ?.getOrNull() ?: emptySet()

            _userTags.value += (type to tags)

        }
    }

    fun deleteTag(type: Int, tag: String) {
        val existingTags = userTags.value[type] ?: return
        val entry = infoMap.value?.get(type) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val tags = existingTags.filter { it != tag }.toSet()
            _userTags.value += (type to tags)

            (Scrobblables.current.value as? LastFm)
                ?.removeUserTagFor(entry, tag)

        }
    }

    fun addTag(type: Int, newTags: String) {
        val existingTags = userTags.value[type] ?: return
        val entry = infoMap.value?.get(type) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val newTagsList = splitTags(newTags)

            if (newTagsList.isEmpty())
                return@launch

            val tags = existingTags + newTagsList

            _userTags.value += (type to tags)

            PlatformStuff.mainPrefs.updateData {
                it.copy(
                    tagHistory = (it.tagHistory + newTagsList).distinct()
                        .take(Stuff.MAX_HISTORY_ITEMS)
                )
            }

            (Scrobblables.current.value as? LastFm)
                ?.addUserTagsFor(entry, newTagsList.joinToString(","))
        }
    }

    private suspend fun populateTagHistoryIfNeeded() {
        val mainPrefs = PlatformStuff.mainPrefs
        if (mainPrefs.data.map { it.userTopTagsFetched }.first()) return

        (Scrobblables.current.value as? LastFm)
            ?.userGetTopTags(limit = 20)
            ?.map { it.toptags.tag }
            ?.onSuccess {
                val tags = it.reversed().map { it.name }
                mainPrefs.updateData {
                    it.copy(tagHistory = tags, userTopTagsFetched = true)
                }
            }
    }

    private fun splitTags(tags: String) = tags.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}