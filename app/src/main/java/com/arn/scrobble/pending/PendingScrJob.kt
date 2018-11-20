package com.arn.scrobble.pending

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.AsyncTask
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.ListenBrainz
import com.arn.scrobble.Stuff
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.pref.MultiPreferences
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import de.umass.lastfm.scrobble.ScrobbleData
import de.umass.lastfm.scrobble.ScrobbleResult
import org.xml.sax.SAXException


/**
 * Created by arn on 08/09/2017.
 */
class PendingScrJob : JobService() {
    override fun onStartJob(jp: JobParameters): Boolean {
        mightBeRunning = true
        val ost = OfflineScrobbleTask(applicationContext)
        ost.doneCb = { done -> jobFinished(jp, done)}
        ost.execute()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        mightBeRunning = false
        return true
    }

    class OfflineScrobbleTask(context: Context): AsyncTask<Unit, String, Boolean>() {
        private val dao = PendingScrobblesDb.getDb(context).getDao()
        private val prefs = MultiPreferences(context)
        var progressCb:((str:String)->Unit)? = null
        var doneCb:((done:Boolean)->Unit)? = null

        override fun doInBackground(vararg p0: Unit?): Boolean {
            var done = true
            var aneCount = dao.allNotAutocorrectedCount

            while (aneCount > 0){
                val entry = dao.loadLastPending ?: continue

                publishProgress( "$aneCount remaining")

                var corrected: Pair<String,String>?
                try {
                    corrected = LFMRequester.getValidArtist(entry.artist, entry.track)
                } catch (e: Exception){
                    Stuff.log("OfflineScrobble: n/w err1 - " + e.message)
                    done = false
                    break

                }
                if (corrected != null)
                    dao.markValidArtist(entry.artist)
                else
                    dao.deleteInvalidArtist(entry.artist)
                Thread.sleep(400)

                if (aneCount >= BATCH_SIZE) {
                    publishProgress( "Submitting a batch, $aneCount remaining")
                    done = submitBatch()
                }

                if (!MOCK)
                    aneCount = dao.allNotAutocorrectedCount
                else
                    aneCount--
            }

            while (dao.allAutocorrectedCount > 0) {
                done = submitBatch()
                if (!done) //network wasnt stable
                    break
            }

            return done
        }


        private fun submitBatch():Boolean {
            var done = true
            val entries = dao.allAutocorrected(BATCH_SIZE)

            val lastfmSessKey: String? = prefs.getString(Stuff.PREF_LASTFM_SESS_KEY, null)
            val lastfmEnabled = !prefs.getBoolean(Stuff.PREF_LASTFM_DISABLE, false)

            val lastfmSession: Session? = if (!lastfmSessKey.isNullOrBlank() && lastfmEnabled)
                        Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey)
                    else
                        null
            val librefmSessKey: String? = prefs.getString(Stuff.PREF_LIBREFM_SESS_KEY, null)
            val librefmSession: Session? = if (!librefmSessKey.isNullOrBlank())
                        Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                                Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, librefmSessKey)
                    else
                        null

            if (librefmSession == null && lastfmSession == null) { //user logged out
                return done
            }

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
                    //TODO: handle errs for all
                    var scrobbleResults: MutableList<ScrobbleResult?>? = null
                    if (librefmSession != null)
                        scrobbleResults = Track.scrobble(scrobbleDatas, librefmSession)
                    if (lastfmSession != null)
                        scrobbleResults = Track.scrobble(scrobbleDatas, lastfmSession)

                    if (prefs.getString(Stuff.PREF_LISTENBRAINZ_USERNAME, null) != null)
                        ListenBrainz(prefs.getString(Stuff.PREF_LISTENBRAINZ_TOKEN, null))
                                        .scrobble(scrobbleDatas)

                    if (prefs.getString(Stuff.PREF_LB_CUSTOM_USERNAME, null) != null)
                        ListenBrainz(prefs.getString(Stuff.PREF_LB_CUSTOM_TOKEN, null))
                                        .setApiRoot(prefs.getString(Stuff.PREF_LB_CUSTOM_ROOT, null))
                                        .scrobble(scrobbleDatas)

                    scrobbleResults?.forEachIndexed { i, it ->
                        if (it != null && (it.isSuccessful || it.isIgnored)) {
                            if (!MOCK)
                                dao.delete(entries[i])
                        } else {
                            done = false
                            entries[i].state_timestamp = System.currentTimeMillis()
                            entries[i].state = STATE_SCROBBLE_ERR
                            dao.update(entries[i])
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
                    Stuff.log("OfflineScrobble: n/w err - " + e.message)
                    done = false
                    return done
                }
            }
            return done
        }

        override fun onProgressUpdate(vararg values: String) {
            progressCb?.invoke(values[0])
        }

        override fun onPostExecute(result: Boolean) {
            doneCb?.invoke(result)
        }
    }

    companion object {
        const val JOB_ID = 10
        private const val MOCK = false
        var mightBeRunning = false // this may not be false when the job is force stopped
        private var BATCH_SIZE = 40 //max 50
        private const val STATE_SCROBBLE_ERR = -1

        fun checkAndSchedule(context: Context, force: Boolean = false) {
            if (PendingScrService.mightBeRunning)
                return
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobs = js.allPendingJobs ?: listOf()

            if (jobs.any { it.id == JOB_ID }) {
                Stuff.log("Found " + jobs.size + " existing jobs")
                if (force)
                    js.cancelAll()
                else
                    return
            }

            val job = JobInfo.Builder(PendingScrJob.JOB_ID, ComponentName(context, PendingScrJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(Stuff.OFFLINE_SCROBBLE_JOB_DELAY)
                    .setPersisted(true)
                    .build()
            js.schedule(job)
            Stuff.log("scheduling PendingScrJob")
        }

    }
}