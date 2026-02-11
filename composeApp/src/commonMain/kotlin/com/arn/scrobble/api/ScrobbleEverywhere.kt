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
import com.arn.scrobble.db.CachedTrack
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.RegexEditsDao
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEditsDao.Companion.findAndPerformEdit
import com.arn.scrobble.edits.RegexPreset
import com.arn.scrobble.edits.RegexPresets
import com.arn.scrobble.edits.TitleParseException
import com.arn.scrobble.imageloader.StarMapper
import com.arn.scrobble.media.ScrobbleQueue
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
    val simpleEditsApplied: Boolean = false,
    val regexEditsApplied: Boolean = false,
    val presetsApplied: Boolean = false,
    val titleParseFailed: Boolean = false,
    val blockPlayerAction: BlockPlayerAction? = null,
    val userLoved: Boolean = false,
    val userPlayCount: Int = 0,
)

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
    }
}

object ScrobbleEverywhere {

    val deezerTracksCache = LruCache<String, DeezerTrack>(50)
    val lastfmTracksCache = LruCache<String, Track>(50)
    val lastfmAlbumsCache = LruCache<String, Album>(50)

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


        var simpleEditsApplied = false
        var regexEditsApplied = false
        var presetsApplied = false
        var titleParseFailed = false

        PanoDb.db.getSimpleEditsDao().findAndPerformEdit(scrobbleData)
            ?.also {
                scrobbleData = it
                simpleEditsApplied = true
            }

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
        } else if (!simpleEditsApplied && regexResults.scrobbleData != null) {
            scrobbleData = regexResults.scrobbleData
            regexEditsApplied = true
        }

        if (regexEditsApplied) {
            PanoDb.db.getSimpleEditsDao().findAndPerformEdit(scrobbleData)
                ?.also {
                    scrobbleData = it
                    simpleEditsApplied = true
                }
        }

        if (runPresets) {
            try {
                val presetsResult = RegexPresets.applyAllPresets(
                    scrobbleData,
                    trackUrlDomain,
                    simpleEditsApplied || regexEditsApplied
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

                val firstArtist = FirstArtistExtractor.extract(
                    scrobbleData.artist,
                    useAnd = scrobbleData.appId in parseTitleWithFallback
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
            simpleEditsApplied = simpleEditsApplied,
            regexEditsApplied = regexEditsApplied,
            titleParseFailed = titleParseFailed,
            scrobbleData = scrobbleData.trimmed()
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
            if (!preprocessResult.simpleEditsApplied && !preprocessResult.regexEditsApplied && preprocessResult.presetsApplied) {
                // don't try to parse title again here
                performEditsAndBlocks(preprocessResult.scrobbleData, trackUrlDomain, false)
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
        onNetworkRequestMade: suspend () -> Unit,
        fetchArtUrlOnly: Boolean = false
    ): AdditionalMetadataResult {
        val fetchMissingMetadataDeezer = PlatformStuff.mainPrefs.data.map { it.deezerApi }.first()
        val fetchMissingMetadataLastfm = PlatformStuff.mainPrefs.data.map { it.fetchAlbum }.first()
        val tidalSteelSeries = PlatformStuff.mainPrefs.data.map { it.tidalSteelSeriesApi }.first()

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

                fetchArtUrlOnly && !scrobbleData.album.isNullOrEmpty()
                    -> {
                    return fetchNowPlaying(scrobbleData, onNetworkRequestMade)
                }

                fetchMissingMetadataDeezer && (
                        scrobbleData.appId == Stuff.PACKAGE_DEEZER_WIN ||
                                scrobbleData.appId == Stuff.PACKAGE_DEEZER_WIN_EXE ||
                                scrobbleData.appId.equals(
                                    Stuff.PACKAGE_DEEZER_WIN_STORE,
                                    ignoreCase = true
                                )
                        ) -> {
                    return fetchFromDeezer(
                        scrobbleData,
                        onNetworkRequestMade
                    )
                }

                tidalSteelSeries && (
                        scrobbleData.appId == Stuff.PACKAGE_TIDAL_WIN ||
                                scrobbleData.appId == Stuff.PACKAGE_TIDAL_WIN_EXE ||
                                scrobbleData.appId.equals(
                                    Stuff.PACKAGE_TIDAL_WIN_STORE,
                                    ignoreCase = true
                                )
                        ) -> {

                    return SteelSeriesReceiverServer.getAdditionalData(scrobbleData)
                }

                fetchMissingMetadataLastfm && scrobbleData.album.isNullOrEmpty() -> {
                    return fetchLastfmTrack(scrobbleData, onNetworkRequestMade)
                }
            }
        } catch (e: ScrobbleQueue.NetworkRequestNeededException) {
            Logger.d { "Network request needed to fetch additional metadata" }
            return AdditionalMetadataResult.Empty.copy(shouldFetchAgain = true)
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
            PendingScrobblesWork.schedule(false)
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
                    PendingScrobblesWork.schedule(false)
                }
            }
        }
    }

    private fun createCacheKey(one: String, two: String): String {
        return "${one.lowercase()}||${two.lowercase()}"
    }

    private suspend fun fetchLastfmTrack(
        scrobbleData: ScrobbleData,
        onNetworkRequestMade: suspend () -> Unit,
    ): AdditionalMetadataResult {
        val artist = scrobbleData.artist
        val title = scrobbleData.track

        val cacheKey = createCacheKey(artist, title)
        var track = lastfmTracksCache[cacheKey]

        if (track == null) {
            val trackObj = Track(title, null, Artist(artist))
            onNetworkRequestMade()
            track = Requesters.lastfmUnauthedRequester.getInfo(trackObj)
                .getOrNull()
            if (track != null)
                lastfmTracksCache.put(cacheKey, track)
        }


        if (track != null)
            return extractAlbum(track, scrobbleData)

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
            album = track.album?.name,
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

    private suspend fun fetchNowPlaying(
        scrobbleData: ScrobbleData,
        onNetworkRequestMade: suspend () -> Unit,
    ): AdditionalMetadataResult {
        val cacheKeyAlbum = createCacheKey(
            scrobbleData.artist,
            scrobbleData.album ?: return AdditionalMetadataResult.Empty
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

        if (PlatformStuff.mainPrefs.data.map { it.submitNowPlaying }.first()) {
            Scrobblables.all.firstOrNull { it.userAccount.type == AccountType.LASTFM }
                ?.also {
                    onNetworkRequestMade()
                    delay(1000) // wait a bit to let lastfm update now playing
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
        onNetworkRequestMade: suspend () -> Unit,
    ): AdditionalMetadataResult {
        val cacheKey = createCacheKey(scrobbleData.artist, scrobbleData.track)
        var track = deezerTracksCache[cacheKey]

        if (track == null) {
            onNetworkRequestMade()
            track = Requesters.deezerRequester.searchTrack(
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
