package com.arn.scrobble

import android.app.job.JobParameters
import android.app.job.JobService
import android.app.job.JobInfo
import android.content.ComponentName
import com.arn.scrobble.db.PendingScrobblesDb
import android.app.job.JobScheduler
import android.content.Context
import android.preference.PreferenceManager
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData


/**
 * Created by arn on 08/09/2017.
 */
class OfflineScrobbleJob : JobService() {
    override fun onStartJob(jp: JobParameters): Boolean {
        var done = true
        val dao = PendingScrobblesDb.getDb(this).getDao()

        while (dao.allNotAutocorrectedCount > 0 && dao.allAutocorrectedCount < BATCH_SIZE){
            val entry = dao.loadLastPending
            Stuff.log("db count: " + dao.count)
            Stuff.log(entry.toString())
            var corrected: Pair<String,String>? = null
            try {
                corrected = LFMRequester.getCorrectedData(entry.artist, entry.track)
            } catch (e: Exception){
                Stuff.log("job autocorrect n/w err: " + e.message)
                done = false
                break
            }
            if (corrected != null) {
                entry.artist = corrected.first
                entry.track = corrected.second
                entry.autoCorrected = 1
                dao.update(entry)
            } else
                dao.delete(entry) //invalid

        }
        val entries = dao.allAutocorrected(BATCH_SIZE)

        val key: String = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                .getString(Stuff.SESS_KEY, null) ?: //user decided to log out
                return !done
        val session = Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, key)

        val scrobbleDatas = mutableListOf<ScrobbleData>()
        entries.forEach {
            val scrobbleData = ScrobbleData()
            scrobbleData.artist = it.artist
            scrobbleData.track = it.track
            scrobbleData.timestamp = (it.timestamp / 1000).toInt() // in secs
            scrobbleData.duration = (it.duration / 1000).toInt() // in secs
            scrobbleDatas.add(scrobbleData)
        }
        try {
            val scrobbleResults = Track.scrobble(scrobbleDatas, session)
            scrobbleResults.forEachIndexed { i, it ->
                if (it != null && !it.isSuccessful && !it.isIgnored) {
                    dao.delete(entries[i])
                } else {
                    done = false
                    entries[i].state_timestamp = System.currentTimeMillis()
                    entries[i].state = STATE_SCROBBLE_ERR
                    dao.update(entries[i])
                }
            }

        } catch (e: Exception){
            Stuff.log("job scrobble n/w err: " + e.message)
            done = false
        }

        //TODO: scrobble
         jobFinished(jp, !done)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return false
    }
    companion object {
        val JOB_ID = 4
        val BATCH_SIZE = 45 //max 50
        val STATE_SCROBBLE_ERR = -1
        fun checkAndSchedule(context: Context): Boolean {
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobs = js.allPendingJobs ?: return false
            if (jobs.any { it.id == JOB_ID})
                return false

            val job = JobInfo.Builder(OfflineScrobbleJob.JOB_ID, ComponentName(context, OfflineScrobbleJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(30*1000)
                    .setPersisted(true)
                    .build()
            js.schedule(job)
            return true
        }

    }
}