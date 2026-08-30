package com.arn.scrobble.api

import androidx.collection.LruCache
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.deezer.DeezerTrack
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.steelseries.SteelSeriesReceiverServer
import com.arn.scrobble.db.BlockPlayerAction
import com.arn.scrobble.db.BlockedMetadataDao.Companion.getBlockedEntry
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.RegexEditsDao
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SeenTrackAlbumAssociation
import com.arn.scrobble.db.SimpleEditsDao.Companion.findAndPerformEdit
import com.arn.scrobble.edits.RegexPreset
import com.arn.scrobble.edits.RegexPresets
import com.arn.scrobble.edits.TitleParseException
import com.arn.scrobble.imageloader.StarMapper
import com.arn.scrobble.utils.FirstArtistExtractor
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.work.PendingScrobblesWork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds


data class PreprocessResult(
    val scrobbleData: ScrobbleData,
    val userEditsApplied: Boolean = false,
    val presetsApplied: Boolean = false,
    val titleParseFailed: Boolean = false,
    val blockPlayerAction: BlockPlayerAction? = null,
    val userLoved: Boolean = false,
    private val fetchAlbumValue: Boolean = false,
) {
    val canFetchAlbum: Boolean
        get() = (fetchAlbumValue || !userEditsApplied) && scrobbleData.album.isNullOrEmpty()
}

data class AdditionalMetadataResult(
    val scrobbleData: ScrobbleData?,
    val artUrl: String?,
    val shouldFetchAgain: Boolean = false,
) {
    companion object {
        val Empty = AdditionalMetadataResult(
            scrobbleData = null,
            artUrl = null,
        )

        val FetchAgain = AdditionalMetadataResult(
            scrobbleData = null,
            artUrl = null,
            shouldFetchAgain = true
        )
    }
}

object ScrobbleEverywhere {
    private val deezerTracksCache = LruCache<String, DeezerTrack>(50)
    private val lastfmAlbumsCache = LruCache<String, Album>(50)
    private var artistWithDelimitersMaxId: Int? = null

    private val fetchAdditionalMetadataTimestamps = ArrayDeque<Long>()
    private val fetchAdditionalMetadataMutex = Mutex()

    private suspend fun throttledRequest(): Boolean {
        // was still getting java.util.NoSuchElementException: ArrayDeque is empty, so use lock
        return fetchAdditionalMetadataMutex.withLock {
            val now = System.currentTimeMillis()
            // Remove timestamps older than n seconds
            while (now - (fetchAdditionalMetadataTimestamps.firstOrNull() ?: now) > 1 * 60_000) {
                fetchAdditionalMetadataTimestamps.removeFirstOrNull()
            }
            val can = fetchAdditionalMetadataTimestamps.size < 2
            fetchAdditionalMetadataTimestamps.addLast(System.currentTimeMillis())

            can
        }
    }

    private suspend fun performEditsAndBlocks(
        scrobbleData: ScrobbleData,
        trackUrlDomain: String?,
        runPresets: Boolean
    ): PreprocessResult {
        var scrobbleData = scrobbleData
        val blockedMetadata = PanoDb.db
            .getBlockedMetadataDao()
            .getBlockedEntry(scrobbleData)

        if (blockedMetadata != null)
            return PreprocessResult(
                scrobbleData,
                blockPlayerAction = blockedMetadata.blockPlayerAction
            )


        var userEditsApplied = false
        var presetsApplied = false
        var titleParseFailed = false
        var continueMatching = true
        var fetchAlbumValue = false

        PanoDb.db.getSimpleEditsDao().findAndPerformEdit(scrobbleData)
            ?.also { (newScrobbleData, _continueMatching) ->
                scrobbleData = newScrobbleData
                userEditsApplied = true
                continueMatching = _continueMatching
            }

        if (continueMatching) {
            val regexes = PanoDb.db
                .getRegexEditsDao()
                .enabledFlow()
                .first()

            val regexResults = RegexEditsDao.performRegexReplace(scrobbleData, regexes)

            if (regexResults.blockPlayerAction != null) {
                return PreprocessResult(
                    scrobbleData,
                    blockPlayerAction = regexResults.blockPlayerAction
                )
            } else if (regexResults.scrobbleData != null) {
                scrobbleData = regexResults.scrobbleData
                userEditsApplied = true
                continueMatching = regexResults.matches.last().continueMatching
                fetchAlbumValue = regexResults.matches.any { it.replacement?.fetchAlbum == true }
            }
        }

        if (continueMatching) {
            PanoDb.db.getSimpleEditsDao().findAndPerformEdit(scrobbleData)
                ?.also { (newScrobbleData, _continueMatching) ->
                    scrobbleData = newScrobbleData
                    userEditsApplied = true
                    continueMatching = _continueMatching
                }
        }

        if (runPresets && continueMatching) {
            try {
                val presetsResult = RegexPresets.applyAllPresets(
                    scrobbleData,
                    trackUrlDomain,
                    userEditsApplied
                )

                if (presetsResult != null) {
                    scrobbleData = presetsResult.scrobbleData
                    presetsApplied = true
                }
            } catch (e: TitleParseException) {
                titleParseFailed = true
            }

            // extract first artist if enabled
            if (!titleParseFailed &&
                scrobbleData.appId in
                PlatformStuff.mainPrefs.data.map { it.extractFirstArtistPackages }.first()
            ) {
                val parseTitleWithFallback =
                    PlatformStuff.mainPrefs.data.map {
                        it.getRegexPresetApps(RegexPreset.parse_title_with_fallback)
                    }.first()

                val artistWithDelimitersDao = PanoDb.db.getArtistsWithDelimitersDao()

                val maxId = artistWithDelimitersDao.maxId()

                val updatedUserAllowlist = if (artistWithDelimitersMaxId != maxId)
                    artistWithDelimitersDao.allFlow().first()
                else
                    null

                val firstArtist = FirstArtistExtractor.extract(
                    scrobbleData.artist,
                    useAnd = scrobbleData.appId in parseTitleWithFallback,
                    updatedUserAllowlist = updatedUserAllowlist
                )

                if (firstArtist != scrobbleData.artist)
                    presetsApplied = true

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
            userEditsApplied = userEditsApplied,
            titleParseFailed = titleParseFailed,
            scrobbleData = scrobbleData.trimmed(),
            fetchAlbumValue = fetchAlbumValue,
        )
    }

    suspend fun preprocessMetadata(
        origScrobbleData: ScrobbleData,
        trackUrlDomain: String?,
    ): PreprocessResult {

        Logger.i { "preprocessMetadata " + origScrobbleData.artist + " - " + origScrobbleData.track }

        val preprocessResult = performEditsAndBlocks(origScrobbleData, trackUrlDomain, true)

        if (preprocessResult.blockPlayerAction != null) return preprocessResult

        val preprocessResult2 =
            if (!preprocessResult.userEditsApplied && preprocessResult.presetsApplied) {
                // don't try to parse title again here
                performEditsAndBlocks(preprocessResult.scrobbleData, trackUrlDomain, false)
            } else {
                preprocessResult
            }

        val seenTrack = PanoDb.db.getSeenEntitiesDao()
            .getTrackWithLovedState(
                preprocessResult2.scrobbleData.artist,
                preprocessResult2.scrobbleData.track
            )

        return if (seenTrack != null)
            preprocessResult2.copy(
                userLoved = seenTrack.isLoved == true
            )
        else
            preprocessResult2
    }

    suspend fun fetchMissingMetadata(
        scrobbleData: ScrobbleData,
        canRequest: suspend () -> Boolean = ::throttledRequest,
    ): AdditionalMetadataResult {
        when {
            PlatformStuff.mainPrefs.data.map { it.deezerApi }.first() && (
                    scrobbleData.appId == Stuff.PACKAGE_DEEZER_WIN ||
                            scrobbleData.appId == Stuff.PACKAGE_DEEZER_WIN_EXE ||
                            scrobbleData.appId.equals(
                                Stuff.PACKAGE_DEEZER_WIN_STORE,
                                ignoreCase = true
                            )
                    ) -> {
                return fetchFromDeezer(scrobbleData, canRequest)
            }

            PlatformStuff.mainPrefs.data.map { it.tidalSteelSeriesApi }.first() && (
                    scrobbleData.appId == Stuff.PACKAGE_TIDAL_WIN ||
                            scrobbleData.appId == Stuff.PACKAGE_TIDAL_WIN_EXE ||
                            scrobbleData.appId.equals(
                                Stuff.PACKAGE_TIDAL_WIN_STORE,
                                ignoreCase = true
                            )
                    ) -> {

                return SteelSeriesReceiverServer.getAdditionalData(scrobbleData)
            }
        }

        return AdditionalMetadataResult.Empty
    }

    suspend fun nowPlaying(scrobbleData: ScrobbleData): Map<Scrobblable, Result<ScrobbleResult>> {
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

        val pendingScrobblesDao = PanoDb.db.getPendingScrobblesDao()

        val failed = scrobbleResults.isEmpty() || scrobbleResults.values.any { !it.isSuccess }

        if (failed) {
            val services = if (scrobbleResults.isEmpty())
                PlatformStuff.mainPrefs.data
                    .map { it.scrobbleAccounts.map { it.type } }.first()
            else
                scrobbleResults
                    .mapNotNull { (scrobblable, result) ->
                        if (!result.isSuccess) scrobblable.userAccount.type else null
                    }


            val exceptions = scrobbleResults.values.mapNotNull { it.exceptionOrNull() }

            pendingScrobblesDao.insert(scrobbleData, ScrobbleEvent.scrobble, services, exceptions)
            PendingScrobblesWork.schedule(false)
        } else {
            // all success
            if (pendingScrobblesDao.canForceRetry())
                PendingScrobblesWork.schedule(true)
        }

        // if it has album, update cache

        if (!scrobbleData.album.isNullOrEmpty()) {
            PanoDb.db.getSeenEntitiesDao().saveRecentTracks(
                listOf(scrobbleData.toTrack()),
                mayHaveAlbumArt = false,
                savedLoved = false,
                priority = SeenTrackAlbumAssociation.Priority.MEDIA_PLAYER
            )
        }
    }

    suspend fun loveOrUnlove(track: Track, love: Boolean) {
        if (track.artist.name.isEmpty() || track.name.isEmpty())
            return


        // update the cache
        PanoDb.db.getSeenEntitiesDao()
            .saveLovedTracks(
                listOf(
                    track.copy(userloved = love)
                )
            )

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

                val exceptions = loveResults.values.mapNotNull { it.exceptionOrNull() }

                if (services.isNotEmpty()) {
                    dao.insert(scrobbleData, ScrobbleEvent.love, services, exceptions)
                    PendingScrobblesWork.schedule(false)
                }
            }
        }
    }

    private fun createCacheKey(one: String, two: String): String {
        return "${one.lowercase()}||${two.lowercase()}"
    }

    suspend fun guessAlbumFromCacheLastfm(
        scrobbleData: ScrobbleData,
        canRequest: suspend () -> Boolean = ::throttledRequest,
    ): AdditionalMetadataResult {
        if (!scrobbleData.album.isNullOrEmpty())
            return AdditionalMetadataResult.Empty

        if (!PlatformStuff.mainPrefs.data.map { it.fetchAlbum }.first())
            return AdditionalMetadataResult.Empty

        val album = PanoDb.db.getSeenEntitiesDao().getBestAlbumsForTrack(
            scrobbleData.artist,
            scrobbleData.track
        ).firstOrNull()

        if (album != null) {
            return AdditionalMetadataResult(
                scrobbleData = scrobbleData.copy(
                    album = album.album,
                ),
                artUrl = album.artUrl,
            )
        }

        val artist = scrobbleData.artist
        val title = scrobbleData.track

        var fetchedTrack: Track? = null

        val trackObj = Track(title, null, Artist(artist))

        if (!canRequest())
            return AdditionalMetadataResult.FetchAgain

        Requesters.lastfmUnauthedRequester.getTrackInfo2(trackObj)
            .onSuccess {
                fetchedTrack = it
            }

        if (fetchedTrack != null)
            return extractAlbum(fetchedTrack, scrobbleData)

        return AdditionalMetadataResult.Empty
    }

    private fun extractAlbum(
        track: Track,
        scrobbleData: ScrobbleData,
    ): AdditionalMetadataResult {
        val albumArtistName = track.album?.artist?.name

        if (track.album != null) {
            var cacheKey = createCacheKey(
                track.artist.name,
                track.album.name
            )
            lastfmAlbumsCache.put(cacheKey, track.album)

            // also cache with album artist if different
            if (albumArtistName != null && albumArtistName != track.artist.name) {
                cacheKey = createCacheKey(
                    albumArtistName,
                    track.album.name
                )
                lastfmAlbumsCache.put(cacheKey, track.album)
            }
        }

        val sd = scrobbleData.copy(
            artist = track.artist.name,
            track = track.name,
            album = track.album?.name?.removeSuffix(" - Single"),
            albumArtist = albumArtistName,
        )

        val artUrl = track.album?.image?.lastOrNull()?.url?.let {
            it.takeIf { StarMapper.STAR_PATTERN !in it }
        }

        return AdditionalMetadataResult(
            scrobbleData = sd,
            artUrl = artUrl,
        )
    }

    suspend fun fetchNowPlayingAlbumArt(
        scrobbleData: ScrobbleData,
        canRequest: suspend () -> Boolean = ::throttledRequest,
    ): AdditionalMetadataResult {
        if (scrobbleData.album.isNullOrEmpty())
            return AdditionalMetadataResult.Empty

        val cacheKeyAlbum = createCacheKey(
            scrobbleData.artist,
            scrobbleData.album
        )

        val album =
            lastfmAlbumsCache[cacheKeyAlbum] ?: scrobbleData.albumArtist?.let { albumArtist ->
                lastfmAlbumsCache[createCacheKey(scrobbleData.album, albumArtist)]
            }

        if (album != null) {
            return AdditionalMetadataResult(
                scrobbleData = null,
                artUrl = album.image?.lastOrNull()?.url?.let {
                    it.takeIf { StarMapper.STAR_PATTERN !in it }
                },
            )
        }

        // check db cache
        val dao = PanoDb.db.getSeenEntitiesDao()

        val bestAlbum = dao.getBestAlbumsForTrack(
            scrobbleData.albumArtist ?: scrobbleData.artist,
            scrobbleData.track
        ).firstOrNull { it.artUpdatedAt != null }

        if (bestAlbum?.artUrl != null) {
            return AdditionalMetadataResult(
                scrobbleData = null,
                artUrl = bestAlbum.artUrl,
            )
        }

        // look up online
        if (PlatformStuff.mainPrefs.data.map { it.submitNowPlaying }.first()) {
            Scrobblables.all.firstOrNull { it.userAccount.type == AccountType.LASTFM }
                ?.also {
                    if (!canRequest())
                        return AdditionalMetadataResult.FetchAgain
                    delay(1.seconds) // wait a bit to let lastfm update now playing
                }
                ?.getRecents(1, includeNowPlaying = true, limit = 1)
                ?.onSuccess {
                    val npTrack = it.entries.firstOrNull {
                        it.album?.name?.equals(scrobbleData.album, ignoreCase = true) == true
                    }
                    if (npTrack != null) {
                        return extractAlbum(
                            npTrack,
                            scrobbleData,
                        ).copy(scrobbleData = null)
                    } else {
                        Logger.i { "no matching now playing album found" }
                    }
                }
        }

        return AdditionalMetadataResult.Empty
    }

    private suspend fun fetchFromDeezer(
        scrobbleData: ScrobbleData,
        canRequest: suspend () -> Boolean = ::throttledRequest,
    ): AdditionalMetadataResult {
        val cacheKey = createCacheKey(scrobbleData.artist, scrobbleData.track)
        var track = deezerTracksCache[cacheKey]

        if (track == null) {
            if (!canRequest())
                return AdditionalMetadataResult.FetchAgain

            Requesters.deezerRequester.searchTrack(
                scrobbleData.artist,
                scrobbleData.track,
                limit = 5
            ).onFailure {
                Logger.w(it) { "Failed to search Deezer for track" }
            }.onSuccess {
                track = it.data.firstOrNull {
                    it.title.equals(scrobbleData.track, ignoreCase = true)
                    // the album may be absent in scrobbleData, and the artist may contain multiple artists,
                    // so we don't check them here
                }
            }
        }

        if (track != null) {
            deezerTracksCache.put(cacheKey, track)

            val sd = scrobbleData.copy(
                artist = track.artist.name,
                albumArtist = null,
                album = scrobbleData.album ?: track.album.title
            )

            return AdditionalMetadataResult(
                scrobbleData = sd,
                artUrl = track.album.cover_medium,
            )
        }

        return AdditionalMetadataResult.Empty
    }
}
