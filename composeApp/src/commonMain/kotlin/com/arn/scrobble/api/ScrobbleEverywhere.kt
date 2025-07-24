package com.arn.scrobble.api

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.itunes.ItunesWrapperType
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
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
    val presetsApplied: Set<RegexPreset> = emptySet(),
    val titleParseFailed: Boolean = false,
    val blockPlayerAction: BlockPlayerAction? = null,
    val userLoved: Boolean = false,
    val userPlayCount: Int = 0,
)

object ScrobbleEverywhere {

    suspend fun preprocessMetadata(
        origScrobbleData: ScrobbleData,
        extras: Map<String, String>
    ): PreprocessResult {
        var scrobbleData = origScrobbleData

        Logger.i { "preprocessMetadata " + scrobbleData.artist + " - " + scrobbleData.track }

        val prefs = PlatformStuff.mainPrefs.data.first()

        suspend fun performEditsAndBlocks(runPresets: Boolean): PreprocessResult {
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
                    scrobbleData = scrobbleData.copy(
                        artist = it.artist,
                        album = it.album,
                        track = it.track,
                        albumArtist = it.albumArtist.takeIf { it.isNotBlank() }
                    )
                    edited = true
                }

            if (!edited && runPresets) {
                try {
                    val presetsResult = RegexPresets.applyAllPresets(scrobbleData)

                    if (presetsResult != null) {
                        scrobbleData = presetsResult.scrobbleData
                        presetsApplied = presetsResult.appliedPresets.toSet()
                    }
                } catch (e: TitleParseException) {
                    titleParseFailed = true
                }
            }

            return PreprocessResult(
                presetsApplied = presetsApplied,
                titleParseFailed = titleParseFailed,
                scrobbleData = scrobbleData
            )
        }

        val preprocessResult = performEditsAndBlocks(true)

        if (preprocessResult.blockPlayerAction != null) return preprocessResult

        if (scrobbleData.album.isNullOrEmpty()) {

            val track = if (prefs.fetchAlbum)
                getValidTrack(scrobbleData.artist, scrobbleData.track)
            else
                null

            // cancellable
            delay(10)
            if (track != null) {
                scrobbleData = scrobbleData.copy(
                    artist = track.artist.name,
                    track = track.name,
                    album = track.album?.name,
                    albumArtist = track.album?.artist?.name,
                )
            }
        }

        // get first artist and album artist
        if (Stuff.isOnline) {
            when (scrobbleData.appId?.lowercase()) {
                Stuff.PACKAGE_APPLE_MUSIC,
                Stuff.PACKAGE_APPLE_MUSIC_WIN.lowercase() ->
                    fetchFirstArtistFromItunes(scrobbleData, extras)?.let {
                        scrobbleData = it
                    }

                Stuff.PACKAGE_SPOTIFY ->
                    fetchFirstArtistFromSpotify(scrobbleData, extras)?.let {
                        scrobbleData = it
                    }
            }
        }

        val preprocessResult2 = if (preprocessResult.presetsApplied.isNotEmpty()) {
            // run the edits again, as users could have edited existing scrobbles
            // don't try to parse title again here
            performEditsAndBlocks(false)
        } else {
            preprocessResult.copy(scrobbleData = scrobbleData)
        }

        val cachedTrack: CachedTrack? =
            if (prefs.lastMaxIndexTime != null)
                PanoDb.db.getCachedTracksDao()
                    .findExact(scrobbleData.artist, scrobbleData.track)
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

    private suspend fun fetchFirstArtistFromItunes(
        scrobbleData: ScrobbleData,
        extras: Map<String, String>
    ): ScrobbleData? {
        val trackId = extras[Stuff.METADATA_KEY_MEDIA_ID]?.toLongOrNull()

        val track = if (trackId == null) {
            Requesters.itunesRequester.searchTrack(
                scrobbleData.artist + " " + scrobbleData.track,
                limit = 3
            ).onFailure {
                Logger.w(it) { "Failed to search iTunes for track" }
            }
                .getOrNull()
                ?.results
                ?.firstOrNull {
                    it.wrapperType == ItunesWrapperType.track &&
                            it.artistName.equals(scrobbleData.artist, ignoreCase = true) &&
                            it.trackName.equals(scrobbleData.track, ignoreCase = true) &&
                            it.collectionName.equals(scrobbleData.album, ignoreCase = true)
                }
        } else {
            Requesters.itunesRequester.lookupTrack(trackId)
                .onFailure {
                    Logger.w(it) { "Failed to look up iTunes track" }
                }
                .getOrNull()
                ?.results
                ?.firstOrNull { it.wrapperType == ItunesWrapperType.track }
        }

        if (track == null) {
            return null
        }

        val artistName = if (
            track.artistName.contains(" & ") ||
            track.artistName.contains(", ")
        ) {
            Requesters.itunesRequester.lookupArtist(track.artistId)
                .onFailure {
                    Logger.w(it) { "Failed to look up iTunes artist" }
                }
                .getOrNull()
                ?.results
                ?.firstOrNull { it.wrapperType == ItunesWrapperType.artist }
                ?.artistName
        } else {
            track.artistName
        }

        val albumArtistName =
            if (track.collectionArtistName == null ||
                track.collectionArtistId == null ||
                track.collectionArtistId == track.artistId
            ) {
                artistName
            } else if (
                track.collectionArtistName.contains(" & ") ||
                track.collectionArtistName.contains(", ")
            ) {
                Requesters.itunesRequester.lookupArtist(track.collectionArtistId)
                    .onFailure {
                        Logger.w(it) { "Failed to look up iTunes album artist" }
                    }
                    .getOrNull()
                    ?.results
                    ?.firstOrNull { it.wrapperType == ItunesWrapperType.artist }
                    ?.artistName
            } else {
                track.collectionArtistName
            }

        if (artistName != null) {
            return scrobbleData.copy(
                artist = artistName,
                albumArtist = albumArtistName
            )
        }
        return null
    }

    private suspend fun fetchFirstArtistFromSpotify(
        scrobbleData: ScrobbleData,
        extras: Map<String, String>
    ): ScrobbleData? {
        if (!scrobbleData.artist.contains(", ")) {
            return null
        }

        // todo remove later
        if (!PlatformStuff.isDebug) return null

        val trackId = extras[Stuff.METADATA_KEY_MEDIA_ID]?.removePrefix("spotify:track:")
            ?: return null

        val firstArtistName = Requesters.spotifyRequester.track(trackId)
            .onFailure {
                Logger.w(it) { "Failed to search Spotify for track" }
            }
            .getOrNull()
            ?.artists
            ?.firstOrNull()
            ?.name

        if (firstArtistName != null) {
            return scrobbleData.copy(
                artist = firstArtistName,
            )
        }
        return null
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

    private var lastNpInfoTime = 0L
    private var lastNpInfoCount = 0

    private suspend fun getValidTrack(artist: String, track: String): Track? {
        val now = System.currentTimeMillis()
        if (now - lastNpInfoTime < Stuff.TRACK_INFO_WINDOW) {
            lastNpInfoCount++
            if (lastNpInfoCount >= Stuff.TRACK_INFO_REQUESTS)
                return null
        } else {
            lastNpInfoTime = now
            lastNpInfoCount = 0
        }

        if (Stuff.isOnline) {
            val trackObj = Track(track, null, Artist(artist))

            Requesters.lastfmUnauthedRequester.getInfo(trackObj)
                .onSuccess {
                    return it
                }
        }

        return null
    }

}
