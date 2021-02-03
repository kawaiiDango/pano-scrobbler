package com.arn.scrobble.pending

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.AsyncTask
import androidx.annotation.StringRes
import com.arn.scrobble.*
import com.arn.scrobble.pending.db.PendingLove
import com.arn.scrobble.pending.db.PendingScrobble
import com.arn.scrobble.pending.db.PendingScrobblesDb
import com.arn.scrobble.pref.MultiPreferences
import de.umass.lastfm.*
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
        ost.doneCb = { done ->
            mightBeRunning = false
            jobFinished(jp, done)
        }
        ost.execute()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        mightBeRunning = false
        return true
    }

    class OfflineScrobbleTask(private val context: Context): AsyncTask<Unit, String, Boolean>() {
        private val dao by lazy { PendingScrobblesDb.getDb(context).getScrobblesDao() }
        private val lovesDao by lazy { PendingScrobblesDb.getDb(context).getLovesDao() }
        private val prefs by lazy { MultiPreferences(context) }
        var progressCb:((str:String)->Unit)? = null
        var doneCb:((done:Boolean)->Unit)? = null

        override fun doInBackground(vararg p0: Unit?): Boolean {
            Stuff.initCaller(context)

            var done = submitLoves()

            var aneCount = dao.getAutoCorrectedCount(false)
            var scrobbles = dao.allEmptyAlbumORAlbumArtist(1000)
            if (!prefs.getBoolean(Stuff.PREF_FETCH_AA, false))
                scrobbles = scrobbles.filter { it.album == "" }
            scrobbles.forEach {
                publishProgress(context.getString(R.string.pending_n_remaining, aneCount--))
                try {
                    val track: Track? = Track.getInfo(it.artist, it.track, Stuff.LAST_KEY)
                    if (track != null) {
                        if (it.album == "" && !track.album.isNullOrEmpty()) {
                            it.album = track.album
                            if (!track.albumArtist.isNullOrEmpty())
                                it.albumArtist = track.albumArtist
                        } else if (!track.albumArtist.isNullOrEmpty() &&
                                prefs.getBoolean(Stuff.PREF_FETCH_AA, false) &&
                                it.album.equals(track.album, ignoreCase = true) &&
                                (it.albumArtist == "" || it.artist == it.albumArtist))
                            it.albumArtist = track.albumArtist
                        if (track.listeners >= Stuff.MIN_LISTENER_COUNT)
                            it.autoCorrected = 1
                        dao.update(it)
                    }
                } catch (e: Exception) {
                }
                Thread.sleep(DELAY)
            }

            aneCount = dao.getAutoCorrectedCount(false)

            while (aneCount > 0){
                val entry = dao.oneNotAutocorrected ?: break

                publishProgress(context.getString(R.string.pending_n_remaining, aneCount))
                var correctedArtist: String?
                try {
                    correctedArtist = LFMRequester.getValidArtist(entry.artist, prefs.getStringSet(Stuff.PREF_ALLOWED_ARTISTS, null))
                } catch (e: Exception){
                    Stuff.log("OfflineScrobble: n/w err1 - " + e.message)
                    done = false
                    break
                }
                if (correctedArtist != null)
                    dao.markValidArtist(entry.artist)
                else
                    dao.deleteInvalidArtist(entry.artist)
                Thread.sleep(DELAY)

                if (aneCount >= BATCH_SIZE) {
                    done = submitScrobbleBatch()
                    if (!done) //err
                        return done
                }

                if (!MOCK)
                    aneCount = dao.getAutoCorrectedCount(false)
                else
                    aneCount--
            }

            while (dao.getAutoCorrectedCount(true) > 0) {
                done = submitScrobbleBatch()
                if (!done) //err
                    break
            }

            return done
        }

        private fun submitScrobbleBatch(): Boolean {
            var done = true
            val entries = dao.allAutocorrected(BATCH_SIZE)

            publishProgress(context.getString(R.string.pending_batch))

            val lastfmSessKey: String = prefs.getString(Stuff.PREF_LASTFM_SESS_KEY, null)
                    ?: //user logged out
                    return done

            val lastfmEnabled = !prefs.getBoolean(Stuff.PREF_LASTFM_DISABLE, false)

            val lastfmSession: Session? = if (lastfmEnabled)
                        Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey)
                    else
                        null
            val librefmSessKey: String? = prefs.getString(Stuff.PREF_LIBREFM_SESS_KEY, null)
            val librefmSession: Session? = if (librefmSessKey != null)
                        Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                                Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, librefmSessKey)
                    else
                        null
            val gnufmSessKey: String? = prefs.getString(Stuff.PREF_GNUFM_SESS_KEY, null)
            val gnufmSession: Session? = if (gnufmSessKey != null)
                        Session.createCustomRootSession(prefs.getString(Stuff.PREF_GNUFM_ROOT, null)+"2.0/",
                                Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, gnufmSessKey)
                    else
                        null

            val scrobbleDataToEntry = mutableMapOf<ScrobbleData, PendingScrobble>()
            entries.forEach {
                val scrobbleData = ScrobbleData()
                scrobbleData.artist = it.artist
                scrobbleData.album = it.album
                scrobbleData.track = it.track
                scrobbleData.albumArtist = it.albumArtist
                scrobbleData.timestamp = (it.timestamp / 1000).toInt() // in secs
                if(it.duration > 10*1000)
                    scrobbleData.duration = (it.duration / 1000).toInt() // in secs
                scrobbleDataToEntry[scrobbleData] = it
            }
            if (scrobbleDataToEntry.isNotEmpty()) {
                try {
                    val scrobbleResults = mutableMapOf<@StringRes Int, ScrobbleResult>()
                    //if an error occurs, there will be only one result
                    var filteredData: MutableList<ScrobbleData>? = null
                    if (lastfmSession != null &&
                            filterForService(R.string.lastfm, scrobbleDataToEntry).also { filteredData = it }.isNotEmpty())
                        scrobbleResults[R.string.lastfm] = try {
                            Track.scrobble(filteredData, lastfmSession)[0]
                        } catch (e: CallException) {
                            ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                        }

                    if (librefmSession != null &&
                            filterForService(R.string.librefm, scrobbleDataToEntry).also { filteredData = it }.isNotEmpty())
                        scrobbleResults[R.string.librefm] = try {
                            Track.scrobble(filteredData, librefmSession)[0]
                        } catch (e: CallException) {
                            ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                        }

                    if (gnufmSession != null &&
                            filterForService(R.string.gnufm, scrobbleDataToEntry).also { filteredData = it }.isNotEmpty())
                        scrobbleResults[R.string.gnufm] = try {
                            Track.scrobble(filteredData, gnufmSession)[0]
                        } catch (e: CallException) {
                            ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                        }
                    var token = ""
                    if (prefs.getString(Stuff.PREF_LISTENBRAINZ_TOKEN, null)?.also { token = it } != null &&
                            filterForService(R.string.listenbrainz, scrobbleDataToEntry).also { filteredData = it }.isNotEmpty())
                        scrobbleResults[R.string.listenbrainz] = ListenBrainz(token)
                                        .scrobble(filteredData!!)

                    if (prefs.getString(Stuff.PREF_LB_CUSTOM_TOKEN, null)?.also { token = it } != null &&
                            filterForService(R.string.custom_listenbrainz, scrobbleDataToEntry).also { filteredData = it }.isNotEmpty())
                        scrobbleResults[R.string.custom_listenbrainz] = ListenBrainz(token)
                                        .setApiRoot(prefs.getString(Stuff.PREF_LB_CUSTOM_ROOT, null))
                                        .scrobble(filteredData!!)

                    val idsToDelete = mutableListOf<Int>()
                    scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
                        var state = pendingScrobble.state

                        Stuff.SERVICE_BIT_POS.forEach { (id, pos) ->
                            val enabled = state and (1 shl pos) != 0
                            if (enabled && (!scrobbleResults.containsKey(id) /* logged out */ ||
                                            scrobbleResults[id]!!.errorCode == 7 ||
                                            scrobbleResults[id]!!.isSuccessful))
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
                            Stuff.log("OfflineScrobble: err for " + context.getString(id) +
                                    ": " + result)
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
                    e.printStackTrace()
                    done = false
                    return done
                }
            }
            return done
        }

        private fun submitLoves(): Boolean {
            val lastfmSessKey = prefs.getString(Stuff.PREF_LASTFM_SESS_KEY, null)
                    ?: return true //user logged out
            val lastfmEnabled = !prefs.getBoolean(Stuff.PREF_LASTFM_DISABLE, false)

            val lastfmSession: Session? = if (lastfmEnabled)
                Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, lastfmSessKey)
            else
                null
            val librefmSessKey: String? = prefs.getString(Stuff.PREF_LIBREFM_SESS_KEY, null)
            val librefmSession: Session? = if (librefmSessKey != null)
                Session.createCustomRootSession(Stuff.LIBREFM_API_ROOT,
                        Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, librefmSessKey)
            else
                null
            val gnufmSessKey: String? = prefs.getString(Stuff.PREF_GNUFM_SESS_KEY, null)
            val gnufmSession: Session? = if (gnufmSessKey != null)
                Session.createCustomRootSession(prefs.getString(Stuff.PREF_GNUFM_ROOT, null)+"2.0/",
                        Stuff.LIBREFM_KEY, Stuff.LIBREFM_KEY, gnufmSessKey)
            else
                null

            try {
                val entries = lovesDao.all(100)
                var remaining = entries.size
                for (entry in entries) {
                    publishProgress(context.getString(R.string.submitting_loves, remaining--))
                    val results = mutableMapOf<@StringRes Int, Result>()

                    if (lastfmSession != null && filterOneForService(R.string.lastfm, entry))
                        results[R.string.lastfm] = try {
                            if (entry.shouldLove)
                                Track.love(entry.artist, entry.track, lastfmSession)
                            else
                                Track.unlove(entry.artist, entry.track, lastfmSession)
                        } catch (e: CallException) {
                            ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                        }
                    if (librefmSession != null && filterOneForService(R.string.librefm, entry))
                        results[R.string.librefm] = try {
                            if (entry.shouldLove)
                                Track.love(entry.artist, entry.track, librefmSession)
                            else
                                Track.unlove(entry.artist, entry.track, librefmSession)
                        } catch (e: CallException) {
                            ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
                        }
                    if (gnufmSession != null && filterOneForService(R.string.gnufm, entry))
                        results[R.string.gnufm] = try {
                            if (entry.shouldLove)
                                Track.love(entry.artist, entry.track, gnufmSession)
                            else
                                Track.unlove(entry.artist, entry.track, gnufmSession)
                        } catch (e: CallException) {
                            ScrobbleResult.createHttp200OKResult(0, e.toString(), "")
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
                    Thread.sleep(DELAY)
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

        private fun filterForService(@StringRes id: Int, scrobbleDataToEntry: MutableMap<ScrobbleData, PendingScrobble>): MutableList<ScrobbleData> {
            val filtered = mutableListOf<ScrobbleData>()
            scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
                if (pendingScrobble.state and (1 shl Stuff.SERVICE_BIT_POS[id]!!) != 0)
                    filtered += scrobbleData
            }
            return filtered
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
        private const val DELAY = 400L

        fun checkAndSchedule(context: Context, force: Boolean = false) {
            if (PendingScrService.mightBeRunning)
                return
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobs = js.allPendingJobs

            if (jobs.any { it.id == JOB_ID }) {
                Stuff.log("Found " + jobs.size + " existing jobs")
                if (force)
                    js.cancelAll()
                else
                    return
            }

            val job = JobInfo.Builder(JOB_ID, ComponentName(context, PendingScrJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(Stuff.OFFLINE_SCROBBLE_JOB_DELAY)
                    .setPersisted(true)
                    .build()
            js.schedule(job)
            Stuff.log("scheduling PendingScrJob")
        }

    }
}