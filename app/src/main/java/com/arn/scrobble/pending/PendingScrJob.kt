package com.arn.scrobble.pending

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.xml.sax.SAXException
import timber.log.Timber


/**
 * Created by arn on 08/09/2017.
 */
class PendingScrJob : JobService() {

    private lateinit var job: Job

    override fun onStartJob(jp: JobParameters): Boolean {
        mightBeRunning = true
        job = SupervisorJob()
        PendingScrobbleTask(applicationContext, CoroutineScope(Dispatchers.IO + job)) { done ->
            mightBeRunning = false
            jobFinished(jp, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        mightBeRunning = false
        job.cancel()
        return true
    }

    class PendingScrobbleTask(
        private val context: Context,
        scope: CoroutineScope,
        private val progressCb: ((str: String) -> Unit)? = null,
        private val doneCb: ((done: Boolean) -> Unit)? = null,
    ) {
        private val dao by lazy { PanoDb.db.getPendingScrobblesDao() }
        private val lovesDao by lazy { PanoDb.db.getPendingLovesDao() }

        init {
            scope.launch {
                val success = run()
                doneCb?.invoke(success)
            }
        }

        private suspend fun run(): Boolean {
            deleteForLoggedOutServices()

            var done = submitLoves()

            while (dao.count > 0) {
                done = submitScrobbleBatch()
                if (!done) //err
                    break
            }

            return done
        }

        private suspend fun submitScrobbleBatch(): Boolean {
            var done = true
            val entries = dao.all(BATCH_SIZE)

            progressCb?.invoke(context.getString(R.string.pending_batch))

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
                            dao.update(pendingScrobble)
                        }
                    }

                    if (!MOCK && idsToDelete.isNotEmpty())
                        dao.delete(idsToDelete)

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
                    progressCb?.invoke(context.getString(R.string.submitting_loves, remaining--))
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
            dao.removeLoggedOutAccounts(loggedInServicesBitset)
            dao.deleteStateZero()
            lovesDao.removeLoggedOutAccounts(loggedInServicesBitset)
            lovesDao.deleteStateZero()
        }
    }

    companion object {
        const val JOB_ID = 10
        private val MOCK = BuildConfig.DEBUG && false
        var mightBeRunning = false // this may not be false when the job is force stopped
        private var BATCH_SIZE = 40 //max 50
        private const val DELAY = 400L

        fun checkAndSchedule(context: Context, force: Boolean = false) {
            if (PendingScrService.mightBeRunning)
                return
            val js = ContextCompat.getSystemService(context, JobScheduler::class.java)!!
            val jobs = js.allPendingJobs

            if (jobs.any { it.id == JOB_ID }) {
                Stuff.log("Found " + jobs.size + " existing jobs")
                if (force)
                    js.cancel(JOB_ID)
                else
                    return
            }

            val job = JobInfo.Builder(JOB_ID, ComponentName(context, PendingScrJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(Stuff.OFFLINE_SCROBBLE_JOB_DELAY)
                .setPersisted(true)
                .build()
            js.schedule(job)
            Stuff.log("scheduling ${PendingScrJob::class.java.simpleName}")
        }

    }
}