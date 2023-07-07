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
import com.arn.scrobble.App.Companion.prefs
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
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
import com.arn.scrobble.scrobbleable.AccountType
import com.arn.scrobble.scrobbleable.GnuFm
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.ui.UiUtils
import de.umass.lastfm.Album
import de.umass.lastfm.Artist
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Period
import de.umass.lastfm.Track
import de.umass.lastfm.User
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class IndexingWorker(
    context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = PanoDb.db
    private val lastfmScrobblable = Scrobblables.current as? GnuFm
    private val session by lazy { lastfmScrobblable!!.sessionCopy() }
    private val username by lazy { lastfmScrobblable!!.userAccount.user.name }

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
            if (!doFullIndex && prefs.lastMaxIndexedScrobbleTime == null)
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
        Stuff.log(this::runFullIndex.name)

        val limitPerPage = 1000
        val numPages = Stuff.MAX_INDEXED_ITEMS / limitPerPage
        val maxCalls = numPages * 4
        var callsMade = 0

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

        val lastScrobbledTrack = User.getRecentTracks(
            null,
            1,
            1,
            session
        ).pageResults.find { it.playedWhen != null }
            ?: throw IllegalStateException("No scrobbled tracks found")

        for (i in 1..numPages) {
            val artists = User.getTopArtists(
                username,
                Period.OVERALL,
                limitPerPage,
                i,
                session
            )
            postProgress()
            list.addAll(artists.pageResults)
            if (i >= artists.totalPages)
                break
        }

        db.getCachedArtistsDao().apply {
            nuke()
            insert(list.map { (it as Artist).toCachedArtist() })
        }
        list.clear()


        for (i in 1..numPages) {
            val albums = User.getTopAlbums(
                username,
                Period.OVERALL,
                limitPerPage,
                i,
                session
            )
            postProgress()
            list.addAll(albums.pageResults)
            if (i >= albums.totalPages)
                break
        }

        db.getCachedAlbumsDao().apply {
            nuke()
            insert(list.map { (it as Album).toCachedAlbum() })
        }
        list.clear()

        for (i in 1..numPages) {
            val tracks = User.getTopTracks(
                username,
                Period.OVERALL,
                limitPerPage,
                i,
                session
            )
            postProgress()
            list.addAll(tracks.pageResults)
            if (i >= tracks.totalPages)
                break
        }

        val lovedTracksList = mutableListOf<Track>()

        for (i in 1..numPages) {
            val lovedTracks =
                User.getLovedTracks(
                    username,
                    limitPerPage,
                    i,
                    session
                )
            postProgress()
            lovedTracksList.addAll(lovedTracks.pageResults)
            if (i >= lovedTracks.totalPages)
                break
        }

        val tracksMap = mutableMapOf<Pair<String, String>, MusicEntry>()

        list.forEach {
            it as Track
            tracksMap[it.artist to it.name] = it
        }

        lovedTracksList.forEach {
            val pair = it.artist to it.name
            if (pair in tracksMap) {
                val existingTrack = tracksMap[pair] as Track
                existingTrack.isLoved = true
                existingTrack.playedWhen = it.playedWhen
            } else {
                it.isLoved = true
                list += it
            }
        }

        db.getCachedTracksDao().apply {
            nuke()
            insert(list.map { (it as Track).toCachedTrack() })
        }

        prefs.lastFullIndexedScrobbleTime = lastScrobbledTrack.playedWhen.time
        prefs.lastFullIndexTime = System.currentTimeMillis()

        prefs.lastDeltaIndexedScrobbleTime = null
        prefs.lastDeltaIndexTime = null

        postProgress(finished = true)
    }

    private suspend fun runDeltaIndex(prFromRecents: PaginatedResult<Track>? = null) {

        Stuff.log(this::runDeltaIndex.name)

        val from = prefs.lastMaxIndexedScrobbleTime
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
                        null,
                        from = from,
                        to = to,
                        limit = limitPerPage,
                    )
            }

            val firstPage = recentsCall()

            if (firstPage.totalPages > maxCalls)
                throw IllegalStateException("Too many pages, run full index instead")

            tracks += firstPage.pageResults

            for (i in 2..firstPage.totalPages) {
                currentPage = i
                val pr = recentsCall()
                tracks += pr.pageResults
            }
        } else {
            val lastTrack = prFromRecents.pageResults.lastOrNull()

            if (prFromRecents.page == 1 && lastTrack != null) {
                if (lastTrack.playedWhen.time > from)
                    throw IllegalStateException("More than one page, run indexing manually")

                // todo handle pending scrobbles submitted at an earlier time

                for (track in prFromRecents.pageResults) {
                    if (!track.isNowPlaying && track.playedWhen != null && track.playedWhen.time > from)
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
            val cachedAlbum = if (!it.album.isNullOrEmpty()) it.toCachedAlbum() else null
            val cachedArtist = it.toCachedArtist()

            val playedWhen = it.playedWhen?.time ?: -1

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

        tracks.firstOrNull()?.let {
            prefs.lastDeltaIndexedScrobbleTime = it.playedWhen!!.time
            prefs.lastDeltaIndexTime = System.currentTimeMillis()
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

        fun livedata(context: Context) =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(NAME)


    }
}