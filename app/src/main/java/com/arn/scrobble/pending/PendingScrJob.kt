package com.arn.scrobble.pending

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import androidx.annotation.StringRes
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingLove
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.Scrobblable
import de.umass.lastfm.Result
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
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
        OfflineScrobbleTask(applicationContext, CoroutineScope(Dispatchers.IO + job)) { done ->
            mightBeRunning = false
            jobFinished(jp, done)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        mightBeRunning = false
        job.cancel()
        return true
    }

    class OfflineScrobbleTask(
        private val context: Context,
        scope: CoroutineScope,
        private val progressCb: ((str: String) -> Unit)? = null,
        private val doneCb: ((done: Boolean) -> Unit)? = null,
    ) {
        private val dao by lazy { PanoDb.getDb(context).getScrobblesDao() }
        private val lovesDao by lazy { PanoDb.getDb(context).getLovesDao() }
        private val prefs by lazy { MainPrefs(context) }

        init {
            scope.launch {
                val success = run()
                doneCb?.invoke(success)
            }
        }

        private suspend fun run(): Boolean {
            var done = submitLoves()

            if (Stuff.FETCH_TRACK_INFO) {
                var scrobbles = dao.allEmptyAlbumORAlbumArtist(1000)
                var count = scrobbles.size
                if (!prefs.fetchAlbumArtist)
                    scrobbles = scrobbles.filter { it.album == "" }
                scrobbles.forEach {
                    try {
                        progressCb?.invoke(context.getString(R.string.pending_n_remaining, count--))
                        val track: Track? = Track.getInfo(it.artist, it.track, Stuff.LAST_KEY)
                        if (track != null) {
                            if (it.album == "" && !track.album.isNullOrEmpty()) {
                                it.album = track.album
                                if (!track.albumArtist.isNullOrEmpty())
                                    it.albumArtist = track.albumArtist
                            } else if (!track.albumArtist.isNullOrEmpty() &&
                                prefs.fetchAlbumArtist &&
                                it.album.equals(track.album, ignoreCase = true) &&
                                (it.albumArtist == "" || it.artist == it.albumArtist)
                            )
                                it.albumArtist = track.albumArtist
                            it.autoCorrected = 1
                            dao.update(it)
                        }
                    } catch (e: Exception) {
                    }
                    delay(DELAY)
                }
            }

            while (dao.count > 0) {
                done = submitScrobbleBatch()
                if (!done) //err
                    break
            }

            return done
        }

        private fun submitScrobbleBatch(): Boolean {
            var done = true
            val entries = dao.all(BATCH_SIZE)
            val scrobblablesMap = Scrobblable.getScrobblablesMap(prefs)

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
                    val scrobbleResults = mutableMapOf</*@StringRes */Int, ScrobbleResult>()
                    //if an error occurs, there will be only one result

                    scrobblablesMap.forEach { (stringId, scrobblable) ->
                        val filteredData by lazy { filterForService(stringId, scrobbleDataToEntry) }
                        if (scrobblable != null && filteredData.isNotEmpty()) {
                            scrobbleResults[stringId] = scrobblable.scrobble(filteredData)
                        }
                    }

                    val idsToDelete = mutableListOf<Int>()
                    scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
                        var state = pendingScrobble.state

                        Stuff.SERVICE_BIT_POS.forEach { (id, pos) ->
                            val enabled = state and (1 shl pos) != 0
                            if (enabled && (!scrobbleResults.containsKey(id) /* logged out */ ||
                                        scrobbleResults[id]!!.errorCode == 7 ||
                                        scrobbleResults[id]!!.isSuccessful)
                            )
                                state = state and (1 shl pos).inv()
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

                    scrobbleResults.forEach { (id, result) ->
                        if (!result.isSuccessful) {
                            Stuff.log(
                                "OfflineScrobble: err for " + context.getString(id) +
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
            val scrobblablesMap = Scrobblable.getScrobblablesMap(prefs, supportsLove = true)

            try {
                val entries = lovesDao.all(100)
                var remaining = entries.size
                for (entry in entries) {
                    progressCb?.invoke(context.getString(R.string.submitting_loves, remaining--))
                    val results = mutableMapOf</*@StringRes */Int, Result>()

                    scrobblablesMap.forEach { (stringId, scrobblable) ->
                        val shouldSubmit by lazy { filterOneForService(stringId, entry) }
                        if (scrobblable != null && shouldSubmit) {
                            results[stringId] = scrobblable.loveOrUnlove(
                                Track(entry.track, null, entry.artist),
                                entry.shouldLove
                            )
                        }
                    }

                    var state = entry.state
                    Stuff.SERVICE_BIT_POS.forEach { (id, pos) ->
                        val enabled = state and (1 shl pos) != 0
                        if (enabled && (!results.containsKey(id) /* logged out or is lbz */ ||
                                    results[id]!!.errorCode == 6 ||
                                    results[id]!!.errorCode == 7 ||
                                    results[id]!!.isSuccessful).also {
                                Stuff.log("love err: " + results[id])
                            }
                        )
                            state = state and (1 shl pos).inv()
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

        private fun filterOneForService(@StringRes id: Int, pl: PendingLove): Boolean {
            return (pl.state and (1 shl Stuff.SERVICE_BIT_POS[id]!!)) != 0
        }

        private fun filterForService(
            @StringRes id: Int,
            scrobbleDataToEntry: MutableMap<ScrobbleData, PendingScrobble>
        ): MutableList<ScrobbleData> {
            val filtered = mutableListOf<ScrobbleData>()
            scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
                if (pendingScrobble.state and (1 shl Stuff.SERVICE_BIT_POS[id]!!) != 0)
                    filtered += scrobbleData
            }
            return filtered
        }
    }

    companion object {
        const val JOB_ID = 10
        private const val MOCK = false
        var mightBeRunning = false // this may not be false when the job is force stopped
        private var BATCH_SIZE = 40 //max 50
        private const val DELAY = 400L

        fun checkAndSchedule(context: Context, force: Boolean = false) {
            if (PendingScrService.mightBeRunning)
                return
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
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