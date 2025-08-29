package com.arn.scrobble.api

import androidx.collection.LruCache
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.deezer.DeezerTrack
import com.arn.scrobble.api.itunes.ItunesTrackResponse
import com.arn.scrobble.api.itunes.ItunesWrapperType
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.steelseries.SteelSeriesReceiverServer
import com.arn.scrobble.db.BlockPlayerAction
import com.arn.scrobble.db.BlockedMetadataDao.Companion.getBlockedEntry
import com.arn.scrobble.db.CachedTrack
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.RegexEditsDao
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEditsDao.Companion.performEdit
import com.arn.scrobble.edits.RegexPreset
import com.arn.scrobble.edits.RegexPresets
import com.arn.scrobble.edits.TitleParseException
import com.arn.scrobble.utils.FirstArtistExtractor
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.redactedMessage
import com.arn.scrobble.work.PendingScrobblesWork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


data class PreprocessResult(
    val scrobbleData: ScrobbleData,
    val edited: Boolean = false,
    val presetsApplied: Set<RegexPreset> = emptySet(),
    val titleParseFailed: Boolean = false,
    val blockPlayerAction: BlockPlayerAction? = null,
    val userLoved: Boolean = false,
    val userPlayCount: Int = 0,
)

private class NetworkRequestNeededException(
    message: String = "Network request needed",
    cause: Throwable? = null
) : Exception(message, cause)

object ScrobbleEverywhere {

    val itunesArtistsCache = LruCache<String, String>(100)
    val itunesTracksCache = LruCache<String, ItunesTrackResponse>(50)
    val spotifyTrackIdToArtistCache = LruCache<String, String>(100)
    val deezerTracksCache = LruCache<String, DeezerTrack>(50)
    val lastfmTracksCache = LruCache<String, Track>(50)

    private suspend fun performEditsAndBlocks(
        scrobbleData: ScrobbleData,
        runPresets: Boolean
    ): PreprocessResult {
        var scrobbleData = scrobbleData
        if (PlatformStuff.billingRepository.isLicenseValid) {
            val blockedMetadata = PanoDb.db
                .getBlockedMetadataDao()
                .getBlockedEntry(scrobbleData)

            if (blockedMetadata != null)
                return PreprocessResult(
                    scrobbleData,
                    blockPlayerAction = blockedMetadata.blockPlayerAction
                )
        }

        var edited = false
        var presetsApplied = emptySet<RegexPreset>()
        var titleParseFailed = false

        val regexes = PanoDb.db
            .getRegexEditsDao()
            .allFlow().first()

        val regexResults = RegexEditsDao.performRegexReplace(scrobbleData, regexes)

        if (regexResults.blockPlayerAction != null) {
            return PreprocessResult(
                scrobbleData,
                blockPlayerAction = regexResults.blockPlayerAction
            )
        } else if (regexResults.scrobbleData != null) {
            scrobbleData = regexResults.scrobbleData
            edited = true
        }

        PanoDb.db.getSimpleEditsDao().performEdit(scrobbleData)
            ?.also {
                scrobbleData = it
                edited = true
            }

        if (runPresets) {
            try {
                val presetsResult = RegexPresets.applyAllPresets(scrobbleData, edited)

                if (presetsResult != null) {
                    scrobbleData = presetsResult.scrobbleData
                    presetsApplied = presetsResult.appliedPresets.toSet()
                    edited = true
                }
            } catch (e: TitleParseException) {
                titleParseFailed = true
            }

            // extract first artist if enabled
            if (!titleParseFailed &&
                scrobbleData.appId in
                PlatformStuff.mainPrefs.data.map { it.extractFirstArtistPackages }.first()
            ) {
                val firstArtist = FirstArtistExtractor.extract(
                    scrobbleData.artist,
                    useAnd = scrobbleData.appId in Stuff.IGNORE_ARTIST_META_WITH_FALLBACK
                )

                if (firstArtist != scrobbleData.artist)
                    edited = true

                scrobbleData = scrobbleData.copy(
                    artist = firstArtist,
                    albumArtist = if (scrobbleData.albumArtist == scrobbleData.artist && firstArtist != scrobbleData.artist)
                        firstArtist
                    else
                        scrobbleData.albumArtist
                )
            }
        }

        return PreprocessResult(
            presetsApplied = presetsApplied,
            edited = edited,
            titleParseFailed = titleParseFailed,
            scrobbleData = scrobbleData.trimmed()
        )
    }

    suspend fun preprocessMetadata(origScrobbleData: ScrobbleData): PreprocessResult {

        Logger.i { "preprocessMetadata " + origScrobbleData.artist + " - " + origScrobbleData.track }

        val preprocessResult = performEditsAndBlocks(origScrobbleData, true)

        if (preprocessResult.blockPlayerAction != null) return preprocessResult

        val preprocessResult2 =
            if (preprocessResult.presetsApplied.isNotEmpty()) {
                // run the edits again, as users could have edited existing scrobbles
                // don't try to parse title again here
                performEditsAndBlocks(preprocessResult.scrobbleData, false)
            } else {
                preprocessResult
            }

        val cachedTrack: CachedTrack? =
            if (PlatformStuff.mainPrefs.data.map { it.lastMaxIndexTime }.first() != null)
                PanoDb.db.getCachedTracksDao()
                    .findExact(
                        preprocessResult2.scrobbleData.artist,
                        preprocessResult2.scrobbleData.track
                    )
            else
                null

        return if (cachedTrack != null)
            preprocessResult2.copy(
                userPlayCount = cachedTrack.plays,
                userLoved = cachedTrack.isLoved
            )
        else
            preprocessResult2
    }

    suspend fun fetchAdditionalMetadata(
        scrobbleData: ScrobbleData,
        trackId: String?,
        cacheOnly: Boolean,
    ): Pair<ScrobbleData?, Boolean> {
        var newScrobbleData: ScrobbleData? = null
        var shouldFetchAgain = false

        val fetchMissingMetadata =
            PlatformStuff.mainPrefs.data.map { it.fetchMissingMetadata }.first()
        val fetchMissingMetadataLastfm = PlatformStuff.mainPrefs.data.map { it.fetchAlbum }.first()
        val tidalSteelSeries = PlatformStuff.mainPrefs.data.map { it.tidalSteelSeries }.first()

        try {
            when {
                /*
                fetchMissingMetadata && (
                        scrobbleData.appId == Stuff.PACKAGE_APPLE_MUSIC ||
                                scrobbleData.appId?.lowercase() == Stuff.PACKAGE_APPLE_MUSIC_WIN_STORE.lowercase() ||
                                scrobbleData.appId == Stuff.PACKAGE_APPLE_MUSIC_WIN_EXE ||
                                scrobbleData.appId == Stuff.PACKAGE_CIDER_LINUX ||
                                scrobbleData.appId == Stuff.PACKAGE_CIDER_VARIANT_LINUX)
                    -> {
                    fetchFromItunes(
                        scrobbleData,
                        trackId
                            ?.removePrefix("/org/node/mediaplayer/cider/track/")
                            ?.toLongOrNull(),
                        cacheOnly,
                    )?.let {
                        newScrobbleData = it
                    }
                }

                fetchMissingMetadata && scrobbleData.appId == Stuff.PACKAGE_SPOTIFY -> {
                    fetchFromSpotify(
                        scrobbleData,
                        trackId
                            ?.takeIf { it.startsWith("spotify:track:") }
                            ?.removePrefix("spotify:track:"),
                        cacheOnly
                    )?.let {
                        newScrobbleData = it
                    }
                }
                 */

                fetchMissingMetadata && (
//                        scrobbleData.appId == Stuff.PACKAGE_DEEZER ||
//                                scrobbleData.appId == Stuff.PACKAGE_DEEZER_TV ||
                        scrobbleData.appId == Stuff.PACKAGE_DEEZER_WIN ||
                                scrobbleData.appId == Stuff.PACKAGE_DEEZER_WIN_EXE ||
                                scrobbleData.appId?.lowercase() == Stuff.PACKAGE_DEEZER_WIN_STORE.lowercase()
                        ) -> {
                    fetchFromDeezer(
                        scrobbleData,
                        trackId
                            ?.takeIf { it.startsWith("0.") }
                            ?.removePrefix("0."),
                        cacheOnly
                    )?.let {
                        newScrobbleData = it
                    }
                }

                tidalSteelSeries && (
                        scrobbleData.appId == Stuff.PACKAGE_TIDAL_WIN ||
                                scrobbleData.appId == Stuff.PACKAGE_TIDAL_WIN_EXE ||
                                scrobbleData.appId?.lowercase() == Stuff.PACKAGE_TIDAL_WIN_STORE.lowercase()
                        ) -> {
                    getFromSteelSeriesTidal(scrobbleData)
                        ?.let {
                            newScrobbleData = it
                        }
                }

                fetchMissingMetadataLastfm && scrobbleData.album.isNullOrEmpty() -> {
                    val track = getLastfmTrack(
                        scrobbleData.artist,
                        scrobbleData.track,
                        cacheOnly
                    )

                    if (track != null) {
                        newScrobbleData = scrobbleData.copy(
                            artist = track.artist.name,
                            track = track.name,
                            album = track.album?.name,
                            albumArtist = track.album?.artist?.name,
                        )
                    }
                }
            }
        } catch (e: NetworkRequestNeededException) {
            shouldFetchAgain = true
        }

        return newScrobbleData to shouldFetchAgain
    }

    private fun createCacheKey(id: String, country: String) = "$id||$country"

    private suspend fun fetchFromItunes(
        scrobbleData: ScrobbleData,
        trackId: Long?,
        cacheOnly: Boolean
    ): ScrobbleData? {
        val country = PlatformStuff.mainPrefs.data.map { it.itunesCountryP }.first()

        val track = if (trackId == null) {
            val query = scrobbleData.artist + " " + scrobbleData.track
            val cacheKey = createCacheKey(query, country)

            val response = itunesTracksCache[cacheKey]
                ?: if (!cacheOnly && Stuff.isOnline)
                    Requesters.itunesRequester.searchTrack(
                        query,
                        country = country,
                        limit = 5
                    ).onFailure {
                        Logger.w(it) { "Failed to search iTunes for track" }
                    }
                        .getOrNull()
                        ?.also { itunesTracksCache.put(cacheKey, it) }
                else
                    throw NetworkRequestNeededException()

            response
                ?.results
                ?.firstOrNull {
                    it.wrapperType == ItunesWrapperType.track &&
                            it.artistName.equals(scrobbleData.artist, ignoreCase = true) &&
                            it.trackName.equals(scrobbleData.track, ignoreCase = true) &&
                            (scrobbleData.album == null || it.collectionName != null &&
                                    it.collectionName.replace(" - (Single|EP)$".toRegex(), "")
                                        .equals(scrobbleData.album, ignoreCase = true))
                }
        } else {
            val cacheKey = createCacheKey(trackId.toString(), country)

            val response = itunesTracksCache[cacheKey]
                ?: if (!cacheOnly && Stuff.isOnline)
                    Requesters.itunesRequester.lookupTrack(trackId)
                        .onFailure {
                            Logger.w(it) { "Failed to look up iTunes track" }
                        }
                        .getOrNull()
                        ?.also { itunesTracksCache.put(cacheKey, it) }
                else
                    throw NetworkRequestNeededException()

            response
                ?.results
                ?.firstOrNull { it.wrapperType == ItunesWrapperType.track }
        }

        if (track == null) {
            return null
        }

        val artistCacheKey = createCacheKey(track.artistId.toString(), country)
        val artistName = itunesArtistsCache[artistCacheKey]
            ?: if (!cacheOnly && Stuff.isOnline)
                Requesters.itunesRequester.lookupArtist(track.artistId)
                    .onFailure {
                        Logger.w(it) { "Failed to look up iTunes artist" }
                    }
                    .getOrNull()
                    ?.results
                    ?.firstOrNull { it.wrapperType == ItunesWrapperType.artist }
                    ?.artistName
                    ?.also { itunesArtistsCache.put(artistCacheKey, it) }
            else
                throw NetworkRequestNeededException()

        val albumArtistName =
            if (track.collectionArtistName == null ||
                track.collectionArtistId == null ||
                track.collectionArtistId == track.artistId
            ) {
                artistName
            } else {
                val albumArtistCacheKey =
                    createCacheKey(track.collectionArtistId.toString(), country)

                itunesArtistsCache[albumArtistCacheKey]
                    ?: if (!cacheOnly && Stuff.isOnline)
                        Requesters.itunesRequester.lookupArtist(track.collectionArtistId)
                            .onFailure {
                                Logger.w(it) { "Failed to look up iTunes album artist" }
                            }
                            .getOrNull()
                            ?.results
                            ?.firstOrNull { it.wrapperType == ItunesWrapperType.artist }
                            ?.artistName
                            ?.also { itunesArtistsCache.put(albumArtistCacheKey, it) }
                    else
                        throw NetworkRequestNeededException()
            }

        if (artistName != null) {
            return scrobbleData.copy(
                artist = artistName,
                album = scrobbleData.album ?: track.collectionName,
                albumArtist = albumArtistName
            )
        }
        return null
    }

    private suspend fun fetchFromSpotify(
        scrobbleData: ScrobbleData,
        trackId: String?,
        cacheOnly: Boolean,
    ): ScrobbleData? {
        if (!scrobbleData.artist.contains(", ")) {
            return null
        }

        if (trackId == null) return null

        val firstArtistName =
            if (scrobbleData.albumArtist != null &&
                (scrobbleData.artist == scrobbleData.albumArtist || // the artist itself may have a ", " in it
                        scrobbleData.artist.startsWith(scrobbleData.albumArtist + ", "))
            ) {
                // sometimes, the first artist is the album artist
                scrobbleData.albumArtist
            } else {
                val country = PlatformStuff.mainPrefs.data.map { it.spotifyCountryP }.first()

                val cacheKey = createCacheKey(trackId, country)

                spotifyTrackIdToArtistCache[cacheKey]
                    ?: if (!cacheOnly && Stuff.isOnline) {
                        Requesters.spotifyRequester.track(trackId, country)
                            .onFailure {
                                Logger.w(it) { "Failed to search Spotify for track" }
                            }
                            .getOrNull()
                            ?.artists
                            ?.firstOrNull()
                            ?.name
                            ?.also { spotifyTrackIdToArtistCache.put(cacheKey, it) }
                    } else
                        throw NetworkRequestNeededException()

            }

        if (firstArtistName != null) {
            return scrobbleData.copy(
                artist = firstArtistName,
            )
        }
        return null
    }

    private suspend fun fetchFromDeezer(
        scrobbleData: ScrobbleData,
        trackId: String?,
        cacheOnly: Boolean,
    ): ScrobbleData? {
        // on android, the trackId is non-null and scrobbleData has the album
        if (trackId != null && !scrobbleData.artist.contains(", ")) {
            return null
        }

        val cacheKey = trackId ?: (scrobbleData.artist + " - " + scrobbleData.track)

        var track = deezerTracksCache[cacheKey]

        if (track == null && !cacheOnly && Stuff.isOnline) {
            track = if (trackId != null) {
                Requesters.deezerRequester.lookupTrack(trackId.toLong())
                    .onFailure {
                        Logger.w(it) { "Failed to look up Deezer track" }
                    }
                    .getOrNull()
            } else {
                Requesters.deezerRequester.searchTrack(
                    scrobbleData.artist,
                    scrobbleData.track,
                    limit = 5
                ).onFailure {
                    Logger.w(it) { "Failed to search Deezer for track" }
                }.getOrNull()?.data?.firstOrNull {
                    it.title.equals(scrobbleData.track, ignoreCase = true)
                    // the album may be absent in scrobbleData, and the artist may contain multiple artists,
                    // so we don't check them here
                }
            }
        } else
            throw NetworkRequestNeededException()

        if (track != null) {
            deezerTracksCache.put(cacheKey, track)

            return scrobbleData.copy(
                artist = track.artist.name,
                albumArtist = null,
                album = scrobbleData.album ?: track.album.title
            )
        }

        return null
    }

    private suspend fun getFromSteelSeriesTidal(
        scrobbleData: ScrobbleData,
    ): ScrobbleData? {
        if (!SteelSeriesReceiverServer.serverStartAttempted) {
            SteelSeriesReceiverServer.startServer()

            // wait for data to be available
            delay(2000)
        }

        // wait for data to be available
        delay(2000)

        return SteelSeriesReceiverServer.putAlbum(scrobbleData)
    }

    suspend fun nowPlaying(scrobbleData: ScrobbleData): Map<Scrobblable, Result<ScrobbleIgnored>> {
        return Scrobblables.all.mapConcurrently(5) {
            it to it.updateNowPlaying(scrobbleData)
        }.toMap()
    }

    suspend fun scrobble(scrobbleData: ScrobbleData) {
        Logger.i { "scrobble " + scrobbleData.artist + " - " + scrobbleData.track }

        // track player
        if (scrobbleData.appId != null) {
            val scrobbleSource = ScrobbleSource(
                timeMillis = scrobbleData.timestamp,
                pkg = scrobbleData.appId
            )
            PanoDb.db
                .getScrobbleSourcesDao()
                .insert(scrobbleSource)
        }

        val scrobbleResults = Scrobblables.all.mapConcurrently(5) {
            it to it.scrobble(scrobbleData)
        }.toMap()

        if (scrobbleResults.isEmpty() ||
            scrobbleResults.values.any { !it.isSuccess }
        ) {
            // failed
            val services = if (scrobbleResults.isEmpty())
                PlatformStuff.mainPrefs.data
                    .map { it.scrobbleAccounts.map { it.type } }.first()
            else
                scrobbleResults
                    .mapNotNull { (scrobblable, result) ->
                        if (!result.isSuccess) scrobblable.userAccount.type else null
                    }

            val dao = PanoDb.db.getPendingScrobblesDao()
            val entry = PendingScrobble(
                scrobbleData = scrobbleData,
                event = ScrobbleEvent.scrobble,
                services = services.toSet(),
                lastFailedTimestamp = System.currentTimeMillis(),
                lastFailedReason = scrobbleResults.values.firstOrNull { it.isFailure }
                    ?.exceptionOrNull()?.redactedMessage?.take(100)
            )

            dao.insert(entry)
            PendingScrobblesWork.checkAndSchedule()
        } else {
            // successful

            val album = scrobbleData.album?.let {
                Album(it, Artist(scrobbleData.albumArtist ?: scrobbleData.artist))
            }
            CachedTracksDao.deltaUpdateAll(
                Track(
                    scrobbleData.track,
                    album,
                    Artist(scrobbleData.artist),
                    date = scrobbleData.timestamp
                ),
                1,
                DirtyUpdate.DIRTY
            )
        }
    }

    suspend fun loveOrUnlove(track: Track, love: Boolean) {
        if (track.artist.name.isEmpty() || track.name.isEmpty())
            return


        // update the cache
        PanoDb.db.getCachedTracksDao().apply {
            val tr = findExact(track.artist.name, track.name) ?: track.toCachedTrack()
            val newTr = tr.copy(isLoved = love)
            insert(listOf(newTr))
        }

        val dao = PanoDb.db.getPendingScrobblesDao()
        val pl = dao.findLoved(track.artist.name, track.name)
        val allScrobblables = Scrobblables.all
        if (pl != null) {
            if (pl.event == ScrobbleEvent.unlove) {
                val services = pl.services + allScrobblables.map { it.userAccount.type }
                val newPl = pl.copy(services = services, event = ScrobbleEvent.love)
                dao.update(newPl)
            }
        } else {
            val loveResults = allScrobblables.mapConcurrently(5) {
                it to it.loveOrUnlove(track, love)
            }.toMap()

            if (loveResults.values.any { !it.isSuccess }) {
                val services = loveResults.mapNotNull { (scrobblable, result) ->
                    if (!result.isSuccess)
                        scrobblable.userAccount.type
                    else
                        null
                }

                val scrobbleData = ScrobbleData(
                    artist = track.artist.name,
                    track = track.name,
                    album = track.album?.name,
                    albumArtist = track.album?.artist?.name,
                    timestamp = System.currentTimeMillis(),
                    duration = track.duration,
                    appId = null
                )

                val entry = PendingScrobble(
                    scrobbleData = scrobbleData,
                    event = ScrobbleEvent.love,
                    services = services.toSet(),
                    lastFailedTimestamp = System.currentTimeMillis(),
                    lastFailedReason = loveResults.values.firstOrNull { it.isFailure }
                        ?.exceptionOrNull()?.redactedMessage?.take(100)
                )

                if (entry.services.isNotEmpty()) {
                    dao.insert(entry)
                    PendingScrobblesWork.checkAndSchedule()
                }
            }
        }
    }

    private suspend fun getLastfmTrack(artist: String, track: String, cacheOnly: Boolean): Track? {
        val cacheKey = "$artist||$track"
        val cachedTrack = lastfmTracksCache[cacheKey]
        if (cachedTrack != null) {
            return cachedTrack
        }

        val trackObj = Track(track, null, Artist(artist))

        if (!cacheOnly && Stuff.isOnline) {
            Requesters.lastfmUnauthedRequester.getInfo(trackObj)
                .onSuccess {
                    lastfmTracksCache.put(cacheKey, it)
                    return it
                }
        }

        return null
    }

}
