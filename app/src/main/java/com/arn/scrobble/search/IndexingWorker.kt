package com.arn.scrobble.search

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.Period
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.db.CachedAlbum
import com.arn.scrobble.db.CachedAlbum.Companion.toCachedAlbum
import com.arn.scrobble.db.CachedAlbumsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedArtist
import com.arn.scrobble.db.CachedArtist.Companion.toCachedArtist
import com.arn.scrobble.db.CachedArtistsDao.Companion.deltaUpdate
import com.arn.scrobble.db.CachedTrack
import com.arn.scrobble.db.CachedTrack.Companion.toCachedTrack
import com.arn.scrobble.db.CachedTracksDao.Companion.deltaUpdate
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class IndexingWorker(
    context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = PanoDb.db
    private val lastfmScrobblable = Scrobblables.current.value as? LastFm
    private val mainPrefs = PlatformStuff.mainPrefs

    override suspend fun getForegroundInfo() = ForegroundInfo(
        NAME.hashCode(),
        UiUtils.createNotificationForFgs(
            applicationContext,
            applicationContext.getString(R.string.reindex)
        )
    )

    override suspend fun doWork(): Result {

        if (lastfmScrobblable == null || lastfmScrobblable.userAccount.type != AccountType.LASTFM)
            return Result.failure()

        var errored = false
        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            setProgressAsync(workDataOf(ERROR_KEY to (throwable.message ?: "error")))
            errored = true
        }

        withContext(Dispatchers.IO + exHandler) {

            if (db.getPendingScrobblesDao().count() > 0 || db.getPendingLovesDao().count() > 0) {
                throw IllegalStateException("Cannot run when there are pending scrobbles")
            }

            var doFullIndex = workerParams.inputData.getBoolean(FULL_INDEX_KEY, false)
            if (!doFullIndex && mainPrefs.data.map { it.lastMaxIndexedScrobbleTime }
                    .first() == null)
                doFullIndex = true

            if (doFullIndex) {
                runFullIndex()
            } else {
                runDeltaIndex()
            }
        }

        return if (errored)
            Result.failure()
        else
            Result.success()
    }


    private suspend fun runFullIndex() {
        Timber.i(this::runFullIndex.name)

        val limitPerPage = 1000
        val numPages = Stuff.MAX_INDEXED_ITEMS / limitPerPage
        val maxCalls = numPages * 4
        var callsMade = 0
        lastfmScrobblable!!

        suspend fun postProgress(finished: Boolean = false) {
            if (finished)
                callsMade = maxCalls
            else
                callsMade++
            setProgress(workDataOf(PROGRESS_KEY to callsMade.toDouble() / maxCalls))
            if (!finished)
                delay(50)
        }

        val list = mutableListOf<MusicEntry>()

        val lastScrobbledTrack = lastfmScrobblable.getRecents(
            page = 1,
            limit = 1,
        ).getOrNull()?.entries?.find { it.date != null }
            ?: throw IllegalStateException("No scrobbled tracks found")

        for (i in 1..numPages) {
            val artists = lastfmScrobblable.getCharts(
                timePeriod = TimePeriod(Period.OVERALL),
                limit = limitPerPage,
                page = i,
                type = Stuff.TYPE_ARTISTS
            ).getOrThrow()
            postProgress()
            list.addAll(artists.entries)
            if (i >= artists.attr.totalPages)
                break
        }

        db.getCachedArtistsDao().apply {
            nuke()
            insert(list.map { (it as Artist).toCachedArtist() })
        }
        list.clear()


        for (i in 1..numPages) {
            val albums = lastfmScrobblable.getCharts(
                timePeriod = TimePeriod(Period.OVERALL),
                limit = limitPerPage,
                page = i,
                type = Stuff.TYPE_ALBUMS
            ).getOrThrow()
            postProgress()
            list.addAll(albums.entries)
            if (i >= albums.attr.totalPages)
                break
        }

        db.getCachedAlbumsDao().apply {
            nuke()
            insert(list.map { (it as Album).toCachedAlbum() })
        }
        list.clear()

        for (i in 1..numPages) {
            val tracks = lastfmScrobblable.getCharts(
                timePeriod = TimePeriod(Period.OVERALL),
                limit = limitPerPage,
                page = i,
                type = Stuff.TYPE_TRACKS
            ).getOrThrow()
            postProgress()
            list.addAll(tracks.entries)
            if (i >= tracks.attr.totalPages)
                break
        }

        val lovedTracksList = mutableListOf<Track>()

        for (i in 1..numPages) {
            val lovedTracks =
                lastfmScrobblable.getLoves(
                    limit = limitPerPage,
                    page = i,
                ).getOrThrow()
            postProgress()
            lovedTracksList.addAll(lovedTracks.entries)
            if (i >= lovedTracks.attr.totalPages)
                break
        }

        val tracksMap = mutableMapOf<Pair<String, String>, MusicEntry>()

        list.forEach {
            it as Track
            tracksMap[it.artist.name to it.name] = it
        }

        lovedTracksList.forEach {
            val pair = it.artist.name to it.name
            if (pair in tracksMap) {
                val existingTrack = (tracksMap[pair] as Track)
                tracksMap[pair] = existingTrack.copy(
                    userloved = true,
                    date = it.date
                )
            } else {
                list += it.copy(userloved = true)
            }
        }

        db.getCachedTracksDao().apply {
            nuke()
            insert(list.map { (it as Track).toCachedTrack() })
        }

        mainPrefs.updateData {
            it.copy(
                lastFullIndexedScrobbleTime = lastScrobbledTrack.date!!,
                lastFullIndexTime = System.currentTimeMillis(),
                lastDeltaIndexedScrobbleTime = null,
                lastDeltaIndexTime = null
            )
        }

        postProgress(finished = true)
    }

    private suspend fun runDeltaIndex(prFromRecents: PageResult<Track>? = null) {

        Timber.i(this::runDeltaIndex.name)

        val from = mainPrefs.data.map { it.lastMaxIndexedScrobbleTime }.first()
            ?: throw IllegalStateException("Full index never run")
        val to = System.currentTimeMillis()
        val limitPerPage = 1000
        val maxCalls = 15
        var currentPage = 1
        val tracks = mutableListOf<Track>()

        setProgress(workDataOf(PROGRESS_KEY to 0.5))

        if (prFromRecents == null) {
            val recentsCall = suspend {
                lastfmScrobblable!!
                    .getRecents(
                        currentPage,
                        from = from,
                        to = to,
                        limit = limitPerPage,
                    ).getOrThrow()
            }

            val firstPage = recentsCall()

            if (firstPage.attr.totalPages > maxCalls)
                throw IllegalStateException("Too many pages, run full index instead")

            tracks += firstPage.entries

            for (i in 2..firstPage.attr.totalPages) {
                currentPage = i
                val pr = recentsCall()
                tracks += pr.entries
            }
        } else {
            val lastTrack = prFromRecents.entries.lastOrNull()

            if (prFromRecents.attr.page == 1 && lastTrack != null) {
                if (lastTrack.date != null && lastTrack.date > from)
                    throw IllegalStateException("More than one page, run indexing manually")

                // todo handle pending scrobbles submitted at an earlier time

                for (track in prFromRecents.entries) {
                    if (track.date != null && track.date > from)
                        tracks += track
                    else
                        break
                }
            }
        }

        val tracksLastPlayedMap = mutableMapOf<CachedTrack, Long>()
        val trackCounts = mutableMapOf<CachedTrack, Int>()
        val albumCounts = mutableMapOf<CachedAlbum, Int>()
        val artistCounts = mutableMapOf<CachedArtist, Int>()

        tracks.forEach {
            val cachedTrack = it.toCachedTrack().apply { lastPlayed = -1 }
            val cachedAlbum = if (it.album != null) it.toCachedAlbum() else null
            val cachedArtist = it.toCachedArtist()

            val playedWhen = it.date ?: -1

            // put max time
            if (tracksLastPlayedMap[cachedTrack] == null || tracksLastPlayedMap[cachedTrack]!! < playedWhen)
                tracksLastPlayedMap[cachedTrack] = playedWhen

            trackCounts[cachedTrack] = (trackCounts[cachedTrack] ?: 0) + 1
            if (cachedAlbum != null)
                albumCounts[cachedAlbum] = (albumCounts[cachedAlbum] ?: 0) + 1
            artistCounts[cachedArtist] = (artistCounts[cachedArtist] ?: 0) + 1
        }


        trackCounts.forEach { (track, count) ->
            track.lastPlayed = tracksLastPlayedMap[track] ?: -1
            db.getCachedTracksDao().deltaUpdate(track, count)
        }

        albumCounts.forEach { (album, count) ->
            db.getCachedAlbumsDao().deltaUpdate(album, count)
        }

        artistCounts.forEach { (artist, count) ->
            db.getCachedArtistsDao().deltaUpdate(artist, count)
        }

        tracks.firstOrNull()?.let { track ->
            mainPrefs.updateData {
                it.copy(
                    lastDeltaIndexedScrobbleTime = track.date!!,
                    lastDeltaIndexTime = System.currentTimeMillis()
                )
            }
        }

        setProgress(workDataOf(PROGRESS_KEY to 1.0))
    }

    companion object {
        const val PROGRESS_KEY = "progress"
        const val ERROR_KEY = "error"
        const val FULL_INDEX_KEY = "fullIndex"
        private const val NAME = "indexing_worker"

        fun schedule(context: Context, forceFullIndex: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeWork = OneTimeWorkRequestBuilder<IndexingWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(FULL_INDEX_KEY to forceFullIndex))
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeWork
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }

        fun flow(context: Context) =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(NAME)

    }
}