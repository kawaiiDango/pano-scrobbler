package com.arn.scrobble.pending

import android.content.Context
import androidx.work.BackoffPolicy
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
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.db.PendingScrobbleWithSource
import com.arn.scrobble.main.App
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.UiUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit


class PendingScrobblesWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    private val scrobblesDao by lazy { PanoDb.db.getPendingScrobblesDao() }
    private val lovesDao by lazy { PanoDb.db.getPendingLovesDao() }

    override suspend fun getForegroundInfo() = ForegroundInfo(
        NAME.hashCode(),
        UiUtils.createNotificationForFgs(
            applicationContext,
            applicationContext.getString(R.string.pending_scrobbles_noti)
        )
    )

    override suspend fun doWork(): Result {
        // do not retry if recently failed
        if (System.currentTimeMillis() - App.prefs.lastPendingScrobblesFailureTime < 2 * 60 * 60 * 1000)
            return Result.failure()

        // do not run if offline, I was unable to infer from the docs whether this constraint is applied to expedited work
        if (!Stuff.isOnline)
            return Result.failure()

        var errored: Boolean

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            errored = true
        }

        withContext(Dispatchers.IO + exHandler) {
            errored = !run()
        }

        return if (errored) {
            App.prefs.lastPendingScrobblesFailureTime = System.currentTimeMillis()
            Result.retry()
        } else {
            Result.success()
        }
    }


    private suspend fun run(): Boolean {
        deleteForLoggedOutServices()

        if (!submitLoves())
            return false

        if (!submitScrobbles())
            return false

        return true
    }

    private suspend fun submitScrobbles(): Boolean {
        val entries = scrobblesDao.allFlow(HARD_LIMIT).first()

        for (chunk in entries.chunked(BATCH_SIZE)) {
            if (!submitScrobbleBatch(chunk))
                return false
        }

        return true
    }

    private suspend fun submitScrobbleBatch(entries: List<PendingScrobbleWithSource>): Boolean {
        setProgress(
            workDataOf(
                PROGRESS_KEY to
                        applicationContext.getString(R.string.pending_batch)
            )
        )

        val scrobbleDataToEntry = mutableMapOf<ScrobbleData, PendingScrobble>()
        entries.forEach {
            val sd = ScrobbleData(
                artist = it.pendingScrobble.artist,
                album = it.pendingScrobble.album,
                track = it.pendingScrobble.track,
                albumArtist = it.pendingScrobble.albumArtist,
                timestamp = it.pendingScrobble.timestamp,
                duration = it.pendingScrobble.duration.takeIf { it > 30 * 1000 },
                packageName = it.pkg
            )
            scrobbleDataToEntry[sd] = it.pendingScrobble
        }
        if (scrobbleDataToEntry.isNotEmpty()) {
            try {
                val scrobbleResults = mutableMapOf<Scrobblable, kotlin.Result<ScrobbleIgnored>>()
                //if an error occurs, there will be only one result

                Scrobblables.all.forEach {
                    val filteredData by lazy { filterForService(it, scrobbleDataToEntry) }
                    if (filteredData.isNotEmpty()) {
                        val result = it.scrobble(filteredData)

                        if (result.isFailure) {
                            // Rate Limit Exceeded - Too many scrobbles in a short period. Please try again later
                            if ((result.exceptionOrNull() as? ApiException)?.code == 29)
                                return false
                        }

                        scrobbleResults[it] = result
                    }
                }

                val idsToDelete = mutableListOf<Int>()
                scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
                    var state = pendingScrobble.state

                    scrobbleResults.forEach { (scrobblable, result) ->
                        val err = result.exceptionOrNull() as? ApiException

                        if (err?.code == 6 ||
                            err?.code == 7 ||
                            result.isSuccess
                        ) {
                            state = state and (1 shl scrobblable.userAccount.type.ordinal).inv()
                        }
                    }

                    if (state == 0)
                        idsToDelete += pendingScrobble._id
                    else if (state != pendingScrobble.state) {
                        pendingScrobble.state = state
                        scrobblesDao.update(pendingScrobble)
                    }
                }

                if (!MOCK && idsToDelete.isNotEmpty())
                    scrobblesDao.delete(idsToDelete)

                scrobbleResults.forEach { (scrobblable, result) ->
                    if (result.isFailure) {
                        Timber.w(
                            "PendingScrobblesWorker: err for " + scrobblable.userAccount.type.ordinal +
                                    ": " + result
                        )
                        return false
                    }
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w("PendingScrobblesWorker: n/w err - ")
                Timber.tag(Stuff.TAG).w(e)
                return false
            }
        }
        return true
    }

    private suspend fun submitLoves(): Boolean {
        try {
            val entries = lovesDao.all(100)
            var remaining = entries.size
            for (entry in entries) {
                setProgress(
                    workDataOf(
                        PROGRESS_KEY to
                                applicationContext.getString(R.string.submitting_loves, remaining--)
                    )
                )
                val results = mutableMapOf<Scrobblable, Boolean>()

                Scrobblables.all.forEach {
                    val shouldSubmit by lazy { filterOneForService(it, entry) }
                    if (shouldSubmit) {
                        val track = Track(
                            name = entry.track,
                            artist = Artist(entry.artist),
                            album = null // todo fix
//                            album = entry.album?.let { Album(it) }
                        )

                        val result = it.loveOrUnlove(track, entry.shouldLove)

                        if (result.isFailure) {
                            // Rate Limit Exceeded - Too many scrobbles in a short period. Please try again later
                            if ((result.exceptionOrNull() as? ApiException)?.code == 29)
                                return false
                        }

                        results[it] = result.isSuccess
                    }
                }

                var state = entry.state

                results.forEach { (scrobblable, success) ->
                    if (success)
                        state = state and (1 shl scrobblable.userAccount.type.ordinal).inv()
                }

                if (state == 0 && !MOCK)
                    lovesDao.delete(entry)
                else if (state != entry.state) {
                    entry.state = state
                    lovesDao.update(entry)
                }
                delay(DELAY)
            }
        } catch (e: Exception) {
            Timber.w("PendingScrobblesWorker: n/w err submitLoves - " + e.message)
            return false
        }
        return true
    }

    private fun filterOneForService(scrobblable: Scrobblable, pl: PendingLove): Boolean {
        return (pl.state and (1 shl scrobblable.userAccount.type.ordinal)) != 0
    }

    private fun filterForService(
        scrobblable: Scrobblable,
        scrobbleDataToEntry: MutableMap<ScrobbleData, PendingScrobble>
    ): MutableList<ScrobbleData> {
        val filtered = mutableListOf<ScrobbleData>()
        scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
            if (pendingScrobble.state and (1 shl scrobblable.userAccount.type.ordinal) != 0)
                filtered += scrobbleData
        }
        return filtered
    }

    private fun deleteForLoggedOutServices() {
        var loggedInServicesBitset = 0
        Scrobblables.all.forEach {
            loggedInServicesBitset =
                loggedInServicesBitset or (1 shl it.userAccount.type.ordinal)
        }
        scrobblesDao.removeLoggedOutAccounts(loggedInServicesBitset)
        scrobblesDao.deleteStateZero()
        lovesDao.removeLoggedOutAccounts(loggedInServicesBitset)
        lovesDao.deleteStateZero()
    }


    companion object {
        private val MOCK = BuildConfig.DEBUG && false
        private var BATCH_SIZE = 40 //max 50
        private const val HARD_LIMIT = 2500 //max 50
        private const val DELAY = 400L
        const val PROGRESS_KEY = "progress"
        const val NAME = "pending_scrobbles"
        const val RETRY_DELAY_HOURS = 3L

        fun checkAndSchedule(context: Context, force: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PendingScrobblesWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    RETRY_DELAY_HOURS,
                    TimeUnit.HOURS
                )
                .apply {
                    if (force) {
                        setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                    } else {
                        setInitialDelay(30, TimeUnit.SECONDS)
                    }
                }
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    NAME,
                    if (force)
                        ExistingWorkPolicy.REPLACE
                    else
                        ExistingWorkPolicy.KEEP,
                    workRequest
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }

    }
}