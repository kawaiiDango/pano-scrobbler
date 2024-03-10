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
import com.arn.scrobble.utils.MetadataUtils
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.Stuff.putSingle
import kotlinx.coroutines.delay
import java.io.IOException


/**
 * Created by arn on 18-03-2017.
 */

object ScrobbleEverywhere {

    suspend fun scrobble(
        nowPlaying: Boolean,
        trackInfo: PlayingTrackInfo,
        parseTitle: Boolean = trackInfo.ignoreOrigArtist
    ) {
        Stuff.log(
            this::scrobble.name + " " +
                    (if (nowPlaying) "np" else "submit")
                    + " " + trackInfo.artist + " - " + trackInfo.title
        )

        Scrobblables.current ?: return

        var scrobbleResults = mapOf<Scrobblable, Result<ScrobbleIgnored>>()
        var savedAsPending = false

        val scrobbleData = trackInfo.toScrobbleData()
        val scrobbleDataOrig = scrobbleData.copy()
        val context = App.context
        val prefs = App.prefs


        suspend fun doFallbackScrobble(): Boolean {
            if (trackInfo.canDoFallbackScrobble && parseTitle) {
                trackInfo.updateMetaFrom(scrobbleDataOrig)
                    .apply { canDoFallbackScrobble = false }

                val i = Intent(NLService.iMETA_UPDATE_S)
                    .setPackage(context.packageName)
                    .putSingle(trackInfo)
                context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)

                scrobble(nowPlaying, trackInfo, false)
                return true
            }
            return false
        }

        fun shouldBlockScrobble(): Boolean {
            if (prefs.proStatus) {
                val blockedMetadata = PanoDb.db
                    .getBlockedMetadataDao()
                    .getBlockedEntry(scrobbleData)
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
                .performRegexReplace(scrobbleData, trackInfo.packageName)

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
                    val i = Intent(NLService.iBAD_META_S)
                        .setPackage(context.packageName)
                        .putSingle(trackInfo.copy(albumArtist = ""))
                        .putSingle(
                            ScrobbleError(
                                context.getString(R.string.parse_error),
                                null,
                                trackInfo.packageName,
                            )
                        )
                    context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
                }
                return
            } else if (scrobbleDataOrig != scrobbleData) {
                val i = Intent(NLService.iMETA_UPDATE_S)
                    .setPackage(context.packageName)
                    .putSingle(trackInfo.updateMetaFrom(scrobbleData))

                context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
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
                    .performRegexReplace(scrobbleData, trackInfo.packageName)

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

            scrobbleResults = Scrobblables.all.mapConcurrently(5) {
                if (Stuff.isOnline || it.userAccount.type == AccountType.FILE)
                    it to it.scrobble(scrobbleData)
                else
                    it to Result.failure(IOException("Offline"))
            }.toMap()

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
                    timestamp = scrobbleData.timestamp
                    duration = scrobbleData.duration ?: -1
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

        val failedTextLines = mutableListOf<String>()
        var ignored = false
        scrobbleResults.forEach { (scrobblable, result) ->
            if (result.isFailure) {
                val errMsg = scrobbleResults[scrobblable]
                    ?.exceptionOrNull()
                    ?.also { it.printStackTrace() }
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
            if (ignored) {
                val i = Intent(NLService.iBAD_META_S)
                    .setPackage(context.packageName)
                    .putSingle(trackInfo.updateMetaFrom(scrobbleData))
                    .putSingle(
                        ScrobbleError(
                            "",
                            failedText,
                            trackInfo.packageName,
                        )
                    )
                context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)
            } else if (!nowPlaying) { // suppress now non-critical now playing errors to not scare users
                val i = Intent(NLService.iOTHER_ERR_S)
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
                context.sendBroadcast(i, NLService.BROADCAST_PERMISSION)

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

    suspend fun delete(track: Track): List<Result<Unit>> {
        val results = Scrobblables.all.mapConcurrently(5) {
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
