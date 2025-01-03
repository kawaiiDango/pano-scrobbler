package com.arn.scrobble.api

import androidx.collection.LruCache
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.BlockedMetadataDao.Companion.getBlockedEntry
import com.arn.scrobble.db.CachedTrack
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.db.CachedTracksDao
import com.arn.scrobble.db.DirtyUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.RegexEditsDao.Companion.performRegexReplace
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.db.SimpleEditsDao.Companion.performEdit
import com.arn.scrobble.media.PlayingTrackInfo
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.ScrobbleError
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.work.PendingScrobblesWork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.network_error
import pano_scrobbler.composeapp.generated.resources.parse_error
import pano_scrobbler.composeapp.generated.resources.saved_as_pending
import pano_scrobbler.composeapp.generated.resources.scrobble_ignored
import pano_scrobbler.composeapp.generated.resources.state_unrecognised_artist
import java.io.IOException


/**
 * Created by arn on 18-03-2017.
 */

object ScrobbleEverywhere {

    private class IsOfflineException : IOException("Offline")

    suspend fun scrobble(
        nowPlaying: Boolean,
        trackInfo: PlayingTrackInfo,
        parseTitle: Boolean = trackInfo.ignoreOrigArtist,
    ) {
        Logger.i {
            this::scrobble.name + " " + (if (nowPlaying) "np" else "submit") + " " + trackInfo.artist + " - " + trackInfo.title
        }
        Scrobblables.current.value ?: return

        var scrobbleResults = mapOf<Scrobblable, Result<ScrobbleIgnored>>()
        var savedAsPending = false

        val scrobbleData = trackInfo.toScrobbleData()
        val scrobbleDataOrig = scrobbleData.copy()
        val prefs = PlatformStuff.mainPrefs.data.first()
        val allScrobblables = Scrobblables.all.value

        suspend fun doFallbackScrobble(): Boolean {
            if (trackInfo.canDoFallbackScrobble && parseTitle) {
                trackInfo.updateMetaFrom(scrobbleDataOrig)
                    .apply { canDoFallbackScrobble = false }

                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.TrackInfoUpdated(trackInfo)
                )

                scrobble(nowPlaying, trackInfo, false)
                return true
            }
            return false
        }

        suspend fun shouldBlockScrobble(): Boolean {
            if (PlatformStuff.billingRepository.isLicenseValid) {
                val blockedMetadata = PanoDb.db
                    .getBlockedMetadataDao()
                    .getBlockedEntry(scrobbleData)
                if (blockedMetadata != null) {
                    if (blockedMetadata.skip || blockedMetadata.mute) {
                        notifyPlayingTrackEvent(
                            PlayingTrackNotifyEvent.TrackBlocked(
                                trackInfo = trackInfo,
                                blockedMetadata = blockedMetadata,
                            )
                        )
                    }
                    return true
                }
            }
            return false
        }

        if (nowPlaying) {
            // some players put the previous song and then switch to the current song in like 150ms
            // potentially wasting an api call. sleep and throw cancellation exception in that case
            delay(Stuff.META_WAIT)

            var track: Track? = null
            var correctedArtist: String? = null


            val oldArtist = scrobbleData.artist
            val oldTrack = scrobbleData.track

            if (shouldBlockScrobble()) // check if youtube channel name was blocked
                return

            // music only items have an album field,
            // and the correct artist name on official youtube tv app
            val editsDao = PanoDb.db.getSimpleEditsDao()
            var edit = editsDao.performEdit(scrobbleData)

            var regexEdits = PanoDb.db
                .getRegexEditsDao()
                .performRegexReplace(scrobbleData, trackInfo.appId)

            val scrobbleDataBeforeParseAndLookup = scrobbleData.copy()
            if (parseTitle) { // youtube
                if (edit == null && !regexEdits.values.any { it.isNotEmpty() }) { // do not parse if a regex edit is found
                    val (parsedArtist, parsedTitle) = MetadataUtils.parseYoutubeTitle(trackInfo.origTitle)
                    scrobbleData.artist = parsedArtist ?: ""
                    scrobbleData.track = parsedTitle ?: ""
                    scrobbleData.albumArtist = ""
                    scrobbleData.album = ""
                }
            } else { // parseTitle can make it blank to show iBAD_META_S
                if (scrobbleData.artist.isBlank())
                    scrobbleData.artist = oldArtist

                if (scrobbleData.track.isBlank())
                    scrobbleData.track = oldTrack
            }

            if (regexEdits.values.any { it.isNotEmpty() })
                edit = editsDao.performEdit(scrobbleData)


            if (scrobbleData.artist.isBlank() || scrobbleData.track.isBlank()) {
                if (!doFallbackScrobble()) {
                    notifyPlayingTrackEvent(
                        PlayingTrackNotifyEvent.Error(
                            scrobbleError = ScrobbleError(
                                getString(Res.string.parse_error),
                                null,
                                trackInfo.appId,
                                canFixMetadata = true
                            ),
                            trackInfo = trackInfo.copy(albumArtist = "")
                        )
                    )
                }
                return
            } else if (scrobbleDataOrig != scrobbleData) {
                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.TrackInfoUpdated(
                        trackInfo.updateMetaFrom(scrobbleData)
                    )
                )
            }

            if (Stuff.isOnline) {
                if (scrobbleData.album.isNullOrEmpty() && prefs.fetchAlbum)
                    track = getValidTrack(scrobbleData.artist, scrobbleData.track)

                // cancellable
                delay(10)
                if (track != null) {
                    if (scrobbleData.duration == -1L)
                        scrobbleData.duration = track.duration

                    if (scrobbleData.album == "") {
                        scrobbleData.artist = track.artist.name
                        track.album?.let {
                            scrobbleData.album = it.name
                            scrobbleData.albumArtist = it.artist?.name
                        }
                        scrobbleData.track = track.name
                    } else if (!track.album?.artist?.name.isNullOrEmpty() &&
                        prefs.fetchAlbum &&
                        scrobbleData.album?.equals(
                            track.album.name,
                            ignoreCase = true
                        ) == true &&
                        (scrobbleData.albumArtist.isNullOrEmpty() || scrobbleData.artist == scrobbleData.albumArtist)
                    ) {
                        scrobbleData.albumArtist = track.album?.artist?.name
                    }
                }

                correctedArtist =
                    if (track != null && ((track.listeners
                            ?: 0) >= Stuff.MIN_LISTENER_COUNT || !parseTitle)
                    )
                        track.artist.name
                    else if (!parseTitle)
                        scrobbleData.artist
                    else
                        getValidArtist(scrobbleData.artist)
                if (correctedArtist != null && scrobbleData.album == "")
                    scrobbleData.artist = correctedArtist
            }

            if (scrobbleDataBeforeParseAndLookup != scrobbleData) {
                edit = editsDao.performEdit(scrobbleData, false)

                regexEdits = PanoDb.db
                    .getRegexEditsDao()
                    .performRegexReplace(scrobbleData, trackInfo.appId)

                if (regexEdits.values.any { it.isNotEmpty() })
                    edit = editsDao.performEdit(scrobbleData, false)
            }

            if (scrobbleDataOrig != scrobbleData && shouldBlockScrobble())
                return

            val cachedTrack: CachedTrack? =
                if (prefs.lastMaxIndexTime != null)
                    PanoDb.db.getCachedTracksDao()
                        .findExact(scrobbleData.artist, scrobbleData.track)
                else
                    null
            if (edit != null || cachedTrack != null || track != null) {
                trackInfo.updateMetaFrom(scrobbleData).apply {
                    cachedTrack?.let {
                        userPlayCount = it.plays
                        userLoved = it.isLoved
                    }
                }

                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.TrackInfoUpdated(trackInfo)
                )
            }
            if (Stuff.isOnline) {
                if (correctedArtist != null || edit != null) {
                    if (prefs.submitNowPlaying) {
                        scrobbleResults = allScrobblables.mapConcurrently(5) {
                            it to it.updateNowPlaying(scrobbleData)
                        }.toMap()
                    }
                } else {
                    // unrecognized artist
                    if (!doFallbackScrobble()) {

                        notifyPlayingTrackEvent(
                            PlayingTrackNotifyEvent.Error(
                                scrobbleError = ScrobbleError(
                                    getString(Res.string.state_unrecognised_artist),
                                    null,
                                    trackInfo.appId,
                                    canFixMetadata = true
                                ),
                                trackInfo = trackInfo.updateMetaFrom(scrobbleData)
                            )
                        )
                    }
                }
            }
        } else { // scrobble

            // track player
            val scrobbleSource = ScrobbleSource(
                timeMillis = scrobbleData.timestamp,
                pkg = trackInfo.appId
            )
            PanoDb.db
                .getScrobbleSourcesDao()
                .insert(scrobbleSource)

            scrobbleResults = allScrobblables.mapConcurrently(5) {
                if (Stuff.isOnline || it.userAccount.type == AccountType.FILE)
                    it to it.scrobble(scrobbleData)
                else
                    it to Result.failure(IsOfflineException())
            }.toMap()

            if (scrobbleResults.isEmpty() ||
                scrobbleResults.values.any { !it.isSuccess }
            ) {
                // failed
                var state = 0
                if (scrobbleResults.isEmpty())
                    allScrobblables.forEach {
                        state = state or (1 shl it.userAccount.type.ordinal)
                    }
                else
                    scrobbleResults.forEach { (scrobblable, result) ->
                        if (!result.isSuccess) {
                            state = state or (1 shl scrobblable.userAccount.type.ordinal)
                        }
                    }

                val dao = PanoDb.db.getPendingScrobblesDao()
                val entry = PendingScrobble(
                    artist = scrobbleData.artist,
                    album = scrobbleData.album ?: "",
                    track = scrobbleData.track,
                    albumArtist = scrobbleData.albumArtist ?: "",
                    timestamp = scrobbleData.timestamp,
                    duration = scrobbleData.duration ?: -1,
                    autoCorrected = if (scrobbleResults.isNotEmpty()) 1 else 0,
                    state = state
                )

                dao.insert(entry)
                savedAsPending = true
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

        val failedTextLines = mutableListOf<String>()
        var ignored = false
        scrobbleResults.forEach { (scrobblable, result) ->
            if (result.isFailure) {
                val exception = scrobbleResults[scrobblable]?.exceptionOrNull()

                if (exception is IsOfflineException)
                    return

                val errMsg = exception
                    ?.also { it.printStackTrace() }
                    ?.message
                    ?: getString(Res.string.network_error)
                failedTextLines += "<b>" + scrobblable.userAccount.type + ":</b> $errMsg"
            } else if (result.isSuccess && result.getOrThrow().ignored) {
                failedTextLines += "<b>" + scrobblable.userAccount.type + ":</b> " +
                        getString(Res.string.scrobble_ignored)
                ignored = true
            }
        }
        if (failedTextLines.isNotEmpty()) {
            val failedText = failedTextLines.joinToString("<br>\n")
            Logger.w { "failedText= $failedText" }
            if (ignored) {

                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.Error(
                        trackInfo = trackInfo.updateMetaFrom(scrobbleData),
                        scrobbleError = ScrobbleError(
                            "",
                            failedText,
                            trackInfo.appId,
                            canFixMetadata = true
                        ),
                    )
                )
            } else if (!nowPlaying) { // suppress now non-critical now playing errors to not scare users
                notifyPlayingTrackEvent(
                    PlayingTrackNotifyEvent.Error(
                        trackInfo = trackInfo.updateMetaFrom(scrobbleData),
                        ScrobbleError(
                            if (savedAsPending)
                                getString(Res.string.saved_as_pending)
                            else
                                "",
                            failedText,
                            trackInfo.appId,
                            canFixMetadata = false
                        )
                    )
                )
            }
        }
    }

    suspend fun loveOrUnlove(track: Track, love: Boolean) {
        Logger.i { this::loveOrUnlove.name + " " + love }

        if (track.artist.name.isEmpty() || track.name.isEmpty())
            return


        // update the cache
        PanoDb.db.getCachedTracksDao().apply {
            val tr = findExact(track.artist.name, track.name) ?: track.toCachedTrack()
            val newTr = tr.copy(isLoved = love)
            insert(listOf(newTr))
        }

        val dao = PanoDb.db.getPendingLovesDao()
        val pl = dao.find(track.artist.name, track.name)
        val allScrobblables = Scrobblables.all.value
        if (pl != null) {
            if (pl.shouldLove == !love) {
                var state = pl.state
                allScrobblables.forEach {
                    state = state or (1 shl it.userAccount.type.ordinal)
                }
                val newPl = pl.copy(state = state, shouldLove = love)
                dao.update(newPl)
            }
        } else {
            val successes = allScrobblables.mapConcurrently(5) {
                it to it.loveOrUnlove(track, love).isSuccess
            }.toMap()

            if (successes.values.any { !it }) {
                var state = 0
                successes.forEach { (scrobblable, success) ->
                    if (!success)
                        state = state or (1 shl scrobblable.userAccount.type.ordinal)
                }

                val entry = PendingLove(
                    artist = track.artist.name,
                    track = track.name,
                    shouldLove = love,
                    state = state
                )

                if (entry.state != 0) {
                    dao.insert(entry)
                    PendingScrobblesWork.checkAndSchedule()
                }
            }
        }
    }

    suspend fun delete(track: Track): List<Result<Unit>> {
        val allScrobblables = Scrobblables.all.value

        val results = allScrobblables.mapConcurrently(5) {
            it.delete(track)
                .onFailure { it.printStackTrace() }
        }

        val success = results.all { it.isSuccess }

        if (success)
            CachedTracksDao.deltaUpdateAll(track, -1, DirtyUpdate.BOTH)

        return results
    }


    private val validArtistsCache = LruCache<String, String>(30)

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

        val trackObj = Track(track, null, Artist(artist))

        Requesters.lastfmUnauthedRequester.getInfo(trackObj)
            .onSuccess {
                validArtistsCache.put(artist, it.artist.name)
                return it
            }

        return null
    }

    private suspend fun getValidArtist(artist: String): String? {
        if (validArtistsCache[artist] != null && validArtistsCache[artist]!!.isEmpty())
            return null
        else if (validArtistsCache[artist] != null)
            return validArtistsCache[artist]
        else {
            val artistInfo = Requesters.lastfmUnauthedRequester.getInfo(Artist(artist))
                .getOrNull()

            if (artistInfo != null && artistInfo.name.trim() != "" &&
                artistInfo.listeners!! >= Stuff.MIN_LISTENER_COUNT
            ) {
                validArtistsCache.put(artist, artistInfo.name)
                return artistInfo.name
            } else
                validArtistsCache.put(artist, "")
        }
        return null
    }

}
