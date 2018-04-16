package com.arn.scrobble

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import com.arn.scrobble.db.PendingScrobblesDao
import com.arn.scrobble.db.PendingScrobblesDb
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData


/**
 * Created by arn on 08/09/2017.
 */
class OfflineScrobbleJob : JobService() {
    override fun onStartJob(jp: JobParameters): Boolean {
        Stuff.log("onStartJob")
        JobThread(jp).start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Stuff.log("onStopJob")
        return true
    }

    inner class JobThread(private val jp: JobParameters): Thread() {
        override fun run() {
            var done = true
            val dao = PendingScrobblesDb.getDb(this@OfflineScrobbleJob).getDao()

            while (dao.allNotAutocorrectedCount > 0){
                val entry = dao.loadLastPending

                Stuff.log("offline proc: " + entry.toString())
                var corrected: Pair<String,String>?
                try {
                    corrected = LFMRequester.getCorrectedData(entry.artist, entry.track)
                } catch (e: Exception){
                    Stuff.log("job autocorrect n/w err: " + e.message)
                    done = false
                    break
                }
                if (corrected != null)
                    dao.markValidArtist(entry.artist)
                else
                    dao.deleteInvalidArtist(entry.artist)
                Thread.sleep(100)

                if (dao.allAutocorrectedCount >= BATCH_SIZE)
                    done = submitBatch(dao)
            }

            if (dao.allAutocorrectedCount > 0)
                done = submitBatch(dao)

            Stuff.log("jobFinished: " + done)
            jobFinished(jp, !done)
        }

        private fun submitBatch(dao: PendingScrobblesDao):Boolean {
            var done = true
            val entries = dao.allAutocorrected(BATCH_SIZE)
            val key: String? = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    .getString(Stuff.SESS_KEY, null) //user decided to log out
            if (key == null) {
//                done = true
                jobFinished(jp, !done)
                return done
            }
            val session = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, key)
            val scrobbleDatas = mutableListOf<ScrobbleData>()
            entries.forEach {
                val scrobbleData = ScrobbleData()
                scrobbleData.artist = it.artist
                scrobbleData.album = it.album
                scrobbleData.track = it.track
                scrobbleData.timestamp = (it.timestamp / 1000).toInt() // in secs
                if(it.duration > 10*1000)
                    scrobbleData.duration = (it.duration / 1000).toInt() // in secs
                scrobbleDatas.add(scrobbleData)
            }
            if (scrobbleDatas.isNotEmpty()) {
                try {
                    val scrobbleResults = Track.scrobble(scrobbleDatas, session)
                    scrobbleResults.forEachIndexed { i, it ->
                        if (it != null && (it.isSuccessful || it.isIgnored)) {
                            dao.delete(entries[i])
                        } else {
                            done = false
                            entries[i].state_timestamp = System.currentTimeMillis()
                            entries[i].state = STATE_SCROBBLE_ERR
                            dao.update(entries[i])
                        }
                    }

                } catch (e: Exception) {
                    Stuff.log("job scrobble n/w err: " + e.message)
                    done = false
                }
            }
            return done
        }
    }

    companion object {
        private var JOB_ID = 4
        private val BATCH_SIZE = 45 //max 50
        private val STATE_SCROBBLE_ERR = -1
        fun checkAndSchedule(context: Context, force:Boolean = false): JobInfo? {
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobs = js.allPendingJobs ?: mutableListOf()
            if (jobs.isNotEmpty()) {
                Stuff.log("Found " + jobs.size + " existing jobs")
                if (force && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    js.cancelAll()
                    JOB_ID += ((System.currentTimeMillis() % 10) + 1 ).toInt()
                } else
                    return jobs[0]
            }

            val job = JobInfo.Builder(OfflineScrobbleJob.JOB_ID, ComponentName(context, OfflineScrobbleJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(Stuff.OFFLINE_SCROBBLE_JOB_DELAY)
                    .setPersisted(true)
                    .build()
            val jsRet = js.schedule(job)
            Stuff.log("scheduling new job " + jsRet)
            return job
        }

    }
}