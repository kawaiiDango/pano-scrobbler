package com.arn.scrobble.widget

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.pref.WidgetPrefs
import de.umass.lastfm.Period
import de.umass.lastfm.Session
import de.umass.lastfm.User
import kotlinx.coroutines.*


class ChartsWidgetUpdaterJob : JobService() {

    private lateinit var job: Job

    override fun onStartJob(jp: JobParameters): Boolean {
        job = SupervisorJob()

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val widgetPrefs = WidgetPrefs(applicationContext)
        val mprefs = MainPrefs(applicationContext)

        val username = mprefs.lastfmUsername ?: return false
        val lastfmSession =
            Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, mprefs.lastfmSessKey)

        val appWidgetIdToPeriodInt = mutableMapOf<Int, Int>()
        appWidgetManager.getAppWidgetIds(ComponentName(this, ChartsWidgetProvider::class.java))
            .forEach { id ->
                widgetPrefs[id].period?.let { period ->
                    appWidgetIdToPeriodInt[id] = period
                }
            }

        suspend fun updateData(
            all: Triple<ArrayList<ChartsWidgetListItem>, ArrayList<ChartsWidgetListItem>, ArrayList<ChartsWidgetListItem>>,
            periodInt: Int, appWidgetIds: Collection<Int>
        ) {
            val (artists, albums, tracks) = all

            widgetPrefs.chartsData(Stuff.TYPE_ARTISTS, periodInt).data =
                ObjectSerializeHelper.convertToString(artists)
            widgetPrefs.chartsData(Stuff.TYPE_ALBUMS, periodInt).data =
                ObjectSerializeHelper.convertToString(albums)
            widgetPrefs.chartsData(Stuff.TYPE_TRACKS, periodInt).data =
                ObjectSerializeHelper.convertToString(tracks)

            appWidgetIds.forEach { id ->
                widgetPrefs[id].lastUpdated = System.currentTimeMillis()
            }

            delay(200) // wait for apply()

            appWidgetIds.forEach {
                appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.appwidget_list)
            }
        }

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            jobFinished(jp, false)
        }

        CoroutineScope(Dispatchers.IO + job).launch(exHandler) {
            for (periodInt in appWidgetIdToPeriodInt.values.toSet()) {
                if (periodInt == -1)
                    continue
                val period = Period.values()[periodInt]

                val artists = async {
                    val pr = User.getTopArtists(username, period, 50, 1, lastfmSession)
                    pr.username!!
                    ArrayList(
                        pr.pageResults!!.map { ChartsWidgetListItem(it.name, "", it.playcount) }
                    )
                }
                val albums = async {
                    val pr = User.getTopAlbums(username, period, 50, 1, lastfmSession)
                    pr.username!!
                    ArrayList(
                        pr.pageResults!!.map {
                            ChartsWidgetListItem(
                                it.name,
                                it.artist,
                                it.playcount
                            )
                        }
                    )
                }
                val tracks = async {
                    val pr = User.getTopTracks(username, period, 50, 1, lastfmSession)
                    pr.username!!
                    ArrayList(
                        pr.pageResults!!.map {
                            ChartsWidgetListItem(
                                it.name,
                                it.artist,
                                it.playcount
                            )
                        }
                    )
                }

                updateData(
                    Triple(artists.await(), albums.await(), tracks.await()),
                    periodInt,
                    appWidgetIdToPeriodInt.keys
                )
            }
            jobFinished(jp, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        job.cancel()
        return true
    }

    companion object {
        const val JOB_ID = 11
        const val ONE_SHOT_JOB_ID = 12
        var mightBeRunning = false // this may not be false when the job is force stopped
        private const val INTERVAL = 15 * 60 * 1000L //15 mins is the minimum

        fun checkAndSchedule(context: Context, runImmediately: Boolean) {
            if (mightBeRunning)
                return
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobs = js.allPendingJobs

            if (runImmediately) {
                val job = JobInfo.Builder(
                    ONE_SHOT_JOB_ID,
                    ComponentName(context, ChartsWidgetUpdaterJob::class.java)
                )
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setMinimumLatency(1)
                    .setOverrideDeadline(1)
                    .build()
                js.schedule(job)
            }

            if (jobs.any { it.id == JOB_ID }) {
                Stuff.log("Found " + jobs.size + " existing jobs")
                return
            }

            val job =
                JobInfo.Builder(JOB_ID, ComponentName(context, ChartsWidgetUpdaterJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(INTERVAL)
                    .setPersisted(true)
                    .build()
            js.schedule(job)
            Stuff.log("scheduling WidgetUpdaterJob")
        }

        fun cancel(context: Context) {
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val id = js.allPendingJobs.firstOrNull { it.id == JOB_ID }?.id
            id?.let { js.cancel(it) }
            Stuff.log("cancelled WidgetUpdaterJob")
        }
    }
}