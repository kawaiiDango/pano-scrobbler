package com.arn.scrobble.pending

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.scrobbleable.Scrobblable
import com.arn.scrobble.scrobbleable.Scrobblables
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.xml.sax.SAXException
import timber.log.Timber
import java.util.concurrent.TimeUnit


class PendingScrobblesWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    private val scrobblesDao by lazy { PanoDb.db.getPendingScrobblesDao() }
    private val lovesDao by lazy { PanoDb.db.getPendingLovesDao() }

    override suspend fun doWork(): Result {
        var errored: Boolean

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            errored = true
        }

        withContext(Dispatchers.IO + exHandler) {
            errored = !run()
        }

        return if (errored) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    private suspend fun run(): Boolean {
        deleteForLoggedOutServices()

        var done = submitLoves()

        while (scrobblesDao.count > 0) {
            done = submitScrobbleBatch()
            if (!done) //err
                break
        }

        return done
    }

    private suspend fun submitScrobbleBatch(): Boolean {
        var done = true
        val entries = scrobblesDao.all(BATCH_SIZE)

        setProgress(
            workDataOf(
                PROGRESS_KEY to
                applicationContext.getString(R.string.pending_batch)
            )
        )

        val scrobbleDataToEntry = mutableMapOf<ScrobbleData, PendingScrobble>()
        entries.forEach {
            val scrobbleData = ScrobbleData()
            scrobbleData.artist = it.artist
            scrobbleData.album = it.album
            scrobbleData.track = it.track
            scrobbleData.albumArtist = it.albumArtist
            scrobbleData.timestamp = (it.timestamp / 1000).toInt() // in secs
            if (it.duration > 10 * 1000)
                scrobbleData.duration = (it.duration / 1000).toInt() // in secs
            scrobbleDataToEntry[scrobbleData] = it
        }
        if (scrobbleDataToEntry.isNotEmpty()) {
            try {
                val scrobbleResults = mutableMapOf<Scrobblable, ScrobbleResult>()
                //if an error occurs, there will be only one result

                Scrobblables.all.forEach {
                    val filteredData by lazy { filterForService(it, scrobbleDataToEntry) }
                    if (filteredData.isNotEmpty()) {
                        scrobbleResults[it] = it.scrobble(filteredData)
                        // Rate Limit Exceeded - Too many scrobbles in a short period. Please try again later
                        if (scrobbleResults[it]?.errorCode == 29) {
                            return false
                        }
                    }
                }

                val idsToDelete = mutableListOf<Int>()
                scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
                    var state = pendingScrobble.state

                    scrobbleResults.forEach { (scrobblable, result) ->
                        if (result.errorCode == 6 ||
                            result.errorCode == 7 ||
                            result.isSuccessful
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
                    if (!result.isSuccessful) {
                        Stuff.log(
                            "OfflineScrobble: err for " + scrobblable.userAccount.type.ordinal +
                                    ": " + result
                        )
                        done = false
                    }
                }

            } catch (e: SAXException) {
                Stuff.log("OfflineScrobble: SAXException " + e.message)
                if (BATCH_SIZE != 1) {
                    BATCH_SIZE = 1
                    done = true //try again
                } else
                    done = false
                return done
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Stuff.log("OfflineScrobble: n/w err - ")
                Timber.tag(Stuff.TAG).w(e)
                done = false
                return done
            }
        }
        return done
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
                        results[it] = it.loveOrUnlove(
                            Track(entry.track, null, entry.artist),
                            entry.shouldLove
                        )
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
            return true
        } catch (e: Exception) {
            Stuff.log("OfflineScrobble: n/w err submitLoves - " + e.message)
            return false
        }
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
        private val MOCK = BuildConfig.DEBUG && true
        private var BATCH_SIZE = 40 //max 50
        private const val DELAY = 400L
        const val PROGRESS_KEY = "progress"
        const val NAME = "pending_scrobbles"

        fun checkAndSchedule(context: Context, force: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PendingScrobblesWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    5,
                    TimeUnit.MINUTES
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