package com.arn.scrobble.api

import android.content.Intent
import android.util.LruCache
import com.arn.scrobble.App
import com.arn.scrobble.NLService
import com.arn.scrobble.PlayingTrackInfo
import com.arn.scrobble.R
import com.arn.scrobble.ScrobbleError
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
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
import com.arn.scrobble.pending.PendingScrobblesWorker
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.Stuff.putSingle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


/**
 * Created by arn on 18-03-2017.
 */

object ScrobbleEverywhere {

    suspend fun scrobble(
        nowPlaying: Boolean,
        trackInfo: PlayingTrackInfo,
        unparsedData: ScrobbleData? = null
    ) {
        Stuff.log(
            this::scrobble.name + " " +
                    (if (nowPlaying) "np" else "submit")
                    + " " + trackInfo.artist + " - " + trackInfo.title
        )

        coroutineScope {
            Scrobblables.current ?: return@coroutineScope

            var scrobbleResults = mapOf<Scrobblable, Result<ScrobbleIgnored>>()
            var savedAsPending = false
            val forceable = unparsedData == null

            val scrobbleData = trackInfo.toScrobbleData()
            val context = App.context
            val prefs = App.prefs

            suspend fun doFallbackScrobble(): Boolean {
                if (trackInfo.canDoFallbackScrobble && unparsedData != null) {

                    val newTrackInfo = trackInfo.updateMetaFrom(unparsedData)
                    val i = Intent(NLService.iMETA_UPDATE_S)
                        .setPackage(context.packageName)
                        .putSingle(newTrackInfo)
                    context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)

                    scrobble(nowPlaying, newTrackInfo)
                    return true
                }
                return false
            }

            fun shouldBlockScrobble(otherArtist: String?): Boolean {
                if (prefs.proStatus) {
                    val blockedMetadata = PanoDb.db
                        .getBlockedMetadataDao()
                        .getBlockedEntry(scrobbleData, otherArtist)
                    if (blockedMetadata != null) {
                        val i = Intent(NLService.iCANCEL).apply {
                            `package` = context.packageName
                            putExtra(NLService.B_HASH, trackInfo.hash)
                        }
                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)

                        if (blockedMetadata.skip || blockedMetadata.mute) {
                            val i2 = Intent(NLService.iBLOCK_ACTION_S).apply {
                                `package` = context.packageName
                                putSingle(blockedMetadata)
                                putExtra(NLService.B_HASH, trackInfo.hash)
                            }
                            context.sendBroadcast(i2, NLService.BROADCAST_PERMISSION)
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

                val editsDao = PanoDb.db.getSimpleEditsDao()
                var edit = editsDao.performEdit(scrobbleData)

                val oldArtist = scrobbleData.artist
                val oldTrack = scrobbleData.track

                val regexEdits = PanoDb.db
                    .getRegexEditsDao()
                    .performRegexReplace(scrobbleData, trackInfo.packageName)

                if (scrobbleData.artist.isBlank())
                    scrobbleData.artist = oldArtist

                if (scrobbleData.track.isBlank())
                    scrobbleData.track = oldTrack

                if (regexEdits.values.any { it.isNotEmpty() })
                    edit = editsDao.performEdit(scrobbleData)

                if (shouldBlockScrobble(unparsedData?.artist))
                    return@coroutineScope

                if (scrobbleData.artist.isBlank() || scrobbleData.track.isBlank()) {
                    if (!doFallbackScrobble()) {
                        val i = Intent(NLService.iBAD_META_S)
                            .setPackage(context.packageName)
                            .putSingle(
                                trackInfo.updateMetaFrom(scrobbleData)
                            )
                            .putSingle(
                                ScrobbleError(
                                    context.getString(R.string.parse_error),
                                    null,
                                    trackInfo.packageName,
                                    canForceScrobble = forceable
                                )
                            )
                        context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                    }
                    return@coroutineScope
                } else if (edit != null || regexEdits.values.any { it.isNotEmpty() }) {
                    val i = Intent(NLService.iMETA_UPDATE_S)
                        .setPackage(context.packageName)
                        .putSingle(trackInfo.updateMetaFrom(scrobbleData))

                    context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                }

                if (Stuff.isOnline) {
                    if (scrobbleData.album.isNullOrEmpty() && prefs.fetchAlbum)
                        track = getValidTrack(scrobbleData.artist, scrobbleData.track)

                    if (!isActive)
                        return@coroutineScope
                    val scrobbleDataBeforeAutocorrect = scrobbleData.copy()
                    if (track != null) {
                        if (scrobbleData.duration == -1)
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
                                track.album!!.name,
                                ignoreCase = true
                            ) == true &&
                            (scrobbleData.albumArtist.isNullOrEmpty() || scrobbleData.artist == scrobbleData.albumArtist)
                        ) {
                            scrobbleData.albumArtist = track.album?.artist?.name
                        }
                    }
                    correctedArtist =
                        if (track != null && ((track.listeners
                                ?: 0) >= Stuff.MIN_LISTENER_COUNT || forceable)
                        )
                            track.artist.name
                        else if (forceable)
                            scrobbleData.artist
                        else
                            getValidArtist(scrobbleData.artist)
                    if (correctedArtist != null && scrobbleData.album == "")
                        scrobbleData.artist = correctedArtist

                    if (scrobbleDataBeforeAutocorrect != scrobbleData) {
                        edit = editsDao.performEdit(scrobbleData, false)

                        PanoDb.db
                            .getRegexEditsDao()
                            .performRegexReplace(scrobbleData, trackInfo.packageName)

                        if (regexEdits.values.any { it.isNotEmpty() })
                            edit = editsDao.performEdit(scrobbleData, false)

                        if (shouldBlockScrobble(null))
                            return@coroutineScope
                    }
                }

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
                    val i = Intent(NLService.iMETA_UPDATE_S)
                        .setPackage(context.packageName)
                        .putSingle(trackInfo)

                    context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                }
                if (Stuff.isOnline) {
                    if (correctedArtist != null || edit != null) {
                        if (prefs.submitNowPlaying) {
                            scrobbleResults = Scrobblables.all.mapConcurrently(5) {
                                it to it.updateNowPlaying(scrobbleData)
                            }.toMap()
                        }
                    } else {
                        // unrecognized artist
                        if (!doFallbackScrobble()) {
                            val i = Intent(NLService.iBAD_META_S)
                                .setPackage(context.packageName)
                                .putSingle(trackInfo.updateMetaFrom(scrobbleData))
                                .putSingle(
                                    ScrobbleError(
                                        context.getString(R.string.state_unrecognised_artist),
                                        null,
                                        trackInfo.packageName,
                                        canForceScrobble = forceable
                                    )
                                )
                            context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                        }
                    }
                }
            } else { // scrobble

                // track player
                val scrobbleSource = ScrobbleSource(
                    timeMillis = scrobbleData.timestamp * 1000L,
                    pkg = trackInfo.packageName
                )
                PanoDb.db
                    .getScrobbleSourcesDao()
                    .insert(scrobbleSource)

                if (Stuff.isOnline) {
                    scrobbleResults = Scrobblables.all.mapConcurrently(5) {
                        it to it.scrobble(scrobbleData)
                    }.toMap()
                }

                if (scrobbleResults.isEmpty() ||
                    scrobbleResults.values.any { !it.isSuccess }
                ) {
                    // failed
                    val dao = PanoDb.db.getPendingScrobblesDao()
                    val entry = PendingScrobble().apply {
                        artist = scrobbleData.artist
                        album = scrobbleData.album ?: ""
                        track = scrobbleData.track
                        if (scrobbleData.albumArtist != null)
                            albumArtist = scrobbleData.albumArtist ?: ""
                        timestamp = scrobbleData.timestamp.toLong() * 1000
                        duration = scrobbleData.duration?.times(1000L) ?: -1
                    }

                    if (scrobbleResults.isEmpty())
                        Scrobblables.all.forEach {
                            entry.state =
                                entry.state or (1 shl it.userAccount.type.ordinal)
                        }
                    else
                        scrobbleResults.forEach { (scrobblable, result) ->
                            if (!result.isSuccess) {
                                entry.state =
                                    entry.state or (1 shl scrobblable.userAccount.type.ordinal)
                            }
                        }
                    if (scrobbleResults.isNotEmpty())
                        entry.autoCorrected = 1
                    dao.insert(entry)
                    savedAsPending = true
                    PendingScrobblesWorker.checkAndSchedule(context)
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

            try {
                val failedTextLines = mutableListOf<String>()
                var ignored = false
                scrobbleResults.forEach { (scrobblable, result) ->
                    if (result.isFailure) {
                        val errMsg = scrobbleResults[scrobblable]
                            ?.exceptionOrNull()
                            ?.message
                            ?: context.getString(R.string.network_error)
                        failedTextLines += "<b>" + scrobblable.userAccount.type + ":</b> $errMsg"
                    } else if (result.isSuccess && result.getOrThrow().ignored) {
                        failedTextLines += "<b>" + scrobblable.userAccount.type + ":</b> " +
                                context.getString(R.string.scrobble_ignored)
                        ignored = true
                    }
                }
                if (failedTextLines.isNotEmpty()) {
                    val failedText = failedTextLines.joinToString("<br>\n")
                    Stuff.logW("failedText= $failedText")
                    val i = if (ignored) {
                        Intent(NLService.iBAD_META_S)
                            .setPackage(context.packageName)
                            .putSingle(trackInfo.updateMetaFrom(scrobbleData))
                            .putSingle(
                                ScrobbleError(
                                    "",
                                    failedText,
                                    trackInfo.packageName,
                                    canForceScrobble = forceable
                                )
                            )
                    } else {
                        Intent(NLService.iOTHER_ERR_S)
                            .setPackage(context.packageName)
                            .putSingle(
                                ScrobbleError(
                                    if (savedAsPending)
                                        context.getString(R.string.saved_as_pending)
                                    else
                                        "",
                                    failedText,
                                    trackInfo.packageName,
                                )
                            )
                    }
                    context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                }

            } catch (e: NullPointerException) {
            }
        }
    }

    suspend fun loveOrUnlove(track: Track, love: Boolean) {
        Stuff.log(this::loveOrUnlove.name + " " + love)

        if (track.artist.name.isEmpty() || track.name.isEmpty())
            return


        // update the cache
        PanoDb.db.getCachedTracksDao().apply {
            val tr = findExact(track.artist.name, track.name) ?: track.toCachedTrack()
            tr.isLoved = love
            insert(listOf(tr))
        }

        val dao = PanoDb.db.getPendingLovesDao()
        val pl = dao.find(track.artist.name, track.name)
        if (pl != null) {
            if (pl.shouldLove == !love) {
                pl.shouldLove = love
                Scrobblables.all.forEach {
                    pl.state = pl.state or (1 shl it.userAccount.type.ordinal)
                }
                dao.update(pl)
            }
        } else {
            val successes = Scrobblables.all.mapConcurrently(5) {
                it to it.loveOrUnlove(track, love).isSuccess
            }.toMap()

            if (successes.values.any { !it }) {
                val entry = PendingLove()
                entry.artist = track.artist.name
                entry.track = track.name
                entry.shouldLove = love
                successes.forEach { (scrobblable, success) ->
                    if (!success)
                        entry.state =
                            entry.state or (1 shl scrobblable.userAccount.type.ordinal)
                }
                if (entry.state != 0) {
                    dao.insert(entry)
                    PendingScrobblesWorker.checkAndSchedule(App.context)
                }
            }
        }
    }

    suspend fun delete(track: Track): Boolean {
        val results = Scrobblables.all.mapConcurrently(5) {
            it.delete(track)
        }

        val success = results.all { it }

        if (success)
            CachedTracksDao.deltaUpdateAll(track, -1, DirtyUpdate.BOTH)

        return success
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
        if (validArtistsCache[artist] != null && validArtistsCache[artist].isEmpty())
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
