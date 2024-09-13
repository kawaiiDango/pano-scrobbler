package com.arn.scrobble.search

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.spotify.SpotifyMusicItem
import com.arn.scrobble.api.spotify.SpotifySearchResponse
import com.arn.scrobble.api.spotify.SpotifySearchType
import com.arn.scrobble.db.CustomSpotifyMapping
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.doOnSuccessLoggingFaliure
import com.arn.scrobble.utils.UiUtils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageSearchVM : ViewModel() {
    private val _searchTerm = MutableSharedFlow<Pair<String, Int>>()
    private val _searchResults = MutableStateFlow<SpotifySearchResponse?>(null)
    val searchResults = _searchResults.asSharedFlow()
    private val _hasRedirect = MutableStateFlow(false)
    private val _existingMappings = MutableStateFlow<List<CustomSpotifyMapping>>(emptyList())
    val existingMappings = _existingMappings.asStateFlow()
    private var searchType: Int = -1
    private lateinit var musicEntry: MusicEntry
    private var originalMusicEntry: MusicEntry? = null

    init {
        viewModelScope.launch {
            _searchTerm
                .distinctUntilChanged()
                .debounce(500)
                .collectLatest { (term, searchType) ->
                    val results = when (searchType) {
                        Stuff.TYPE_ALBUMS ->
                            Requesters.spotifyRequester.search(term, SpotifySearchType.album, LIMIT)

                        Stuff.TYPE_ARTISTS -> Requesters.spotifyRequester.search(
                            term,
                            SpotifySearchType.artist,
                            LIMIT
                        )

                        else -> throw IllegalArgumentException("Invalid search type: $searchType")
                    }
                    results.doOnSuccessLoggingFaliure {
                        _searchResults.emit(it)
                    }
                }
        }
    }

    fun search(term: String) {
        viewModelScope.launch {
            _searchTerm.emit(term to searchType)
        }
    }

    fun setMusicEntries(
        musicEntry: MusicEntry,
        originalMusicEntry: MusicEntry?,
    ) {
        fun hasRedirect(): Boolean {
            return (musicEntry is Artist && originalMusicEntry is Artist && musicEntry.name != originalMusicEntry.name) ||
                    (musicEntry is Album && originalMusicEntry is Album &&
                            (musicEntry.name != originalMusicEntry.name ||
                                    musicEntry.artist!!.name != originalMusicEntry.artist!!.name))

        }

        this.musicEntry = musicEntry
        this.originalMusicEntry = originalMusicEntry

        _hasRedirect.value = hasRedirect()
        searchType = if (musicEntry is Album) Stuff.TYPE_ALBUMS else Stuff.TYPE_ARTISTS

        viewModelScope.launch {

            val customSpotifyMapping = withContext(Dispatchers.IO) {
                when (musicEntry) {
                    is Album -> PanoDb.db.getCustomSpotifyMappingsDao()
                        .searchAlbum(musicEntry.artist!!.name, musicEntry.name)

                    is Artist -> PanoDb.db.getCustomSpotifyMappingsDao()
                        .searchArtist(musicEntry.name)

                    else -> null
                }
            }

            val customSpotifyMappingOrig = if (_hasRedirect.value)
                withContext(Dispatchers.IO) {
                    when (originalMusicEntry) {
                        is Album -> PanoDb.db.getCustomSpotifyMappingsDao()
                            .searchAlbum(originalMusicEntry.artist!!.name, musicEntry.name)

                        is Artist -> PanoDb.db.getCustomSpotifyMappingsDao()
                            .searchArtist(originalMusicEntry.name)

                        else -> null
                    }
                }
            else
                null

            _existingMappings.value = listOfNotNull(customSpotifyMapping, customSpotifyMappingOrig)
        }
    }

    fun deleteExistingMappings() {
        if (existingMappings.value.isNotEmpty()) {
            existingMappings.value.forEach {
                revokeUriPermission(it)

                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        PanoDb.db.getCustomSpotifyMappingsDao().delete(it)
                    }
                }
            }
        }
    }

    private fun revokeUriPermission(mapping: CustomSpotifyMapping) {
        if (mapping.fileUri != null) {
            try {
                PlatformStuff.application.contentResolver.releasePersistableUriPermission(
                    mapping.fileUri.toUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
            }
        }
    }

    private fun createCustomMapping(
        musicEntry: MusicEntry,
        spotifyItem: SpotifyMusicItem?,
        fileUri: Uri?
    ): CustomSpotifyMapping {
        return when (musicEntry) {
            is Album -> CustomSpotifyMapping(
                artist = musicEntry.artist!!.name,
                album = musicEntry.name,
                spotifyId = spotifyItem?.id,
                fileUri = fileUri?.toString()
            )

            is Artist -> CustomSpotifyMapping(
                artist = musicEntry.name,
                spotifyId = spotifyItem?.id,
                fileUri = fileUri?.toString()
            )

            else -> throw IllegalArgumentException("Invalid item type: $spotifyItem")
        }
    }

    fun insertCustomMappings(
        spotifyItem: SpotifyMusicItem?,
        fileUri: Uri?
    ) {
        val mappings = mutableListOf<CustomSpotifyMapping>()
        mappings += createCustomMapping(musicEntry, spotifyItem, fileUri)

        // revoke uri permission for existing mappings
        existingMappings.value.forEach { revokeUriPermission(it) }

        // create another mapping for the redirected artist/album
        if (_hasRedirect.value)
            mappings += createCustomMapping(originalMusicEntry!!, spotifyItem, fileUri)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PanoDb.db.getCustomSpotifyMappingsDao()
                    .insert(mappings)
            }
        }
    }


    fun setImage(uri: Uri?) {
        uri ?: return

        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            PlatformStuff.application.contentResolver.takePersistableUriPermission(uri, takeFlags)

            insertCustomMappings(null, uri)
        } catch (e: SecurityException) {
            PlatformStuff.application.toast(e.localizedMessage ?: e.toString())
        }
    }

    companion object {
        private const val LIMIT = 40
    }
}
