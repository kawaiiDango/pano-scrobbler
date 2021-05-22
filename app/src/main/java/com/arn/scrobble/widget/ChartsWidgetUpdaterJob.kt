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
import com.arn.scrobble.pref.MultiPreferences
import de.umass.lastfm.*
import kotlinx.coroutines.*


class ChartsWidgetUpdaterJob : JobService() {

    override fun onStartJob(jp: JobParameters): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val pref = applicationContext.getSharedPreferences(Stuff.WIDGET_PREFS, Context.MODE_PRIVATE)
        val username = MultiPreferences(applicationContext).getString(Stuff.PREF_LASTFM_USERNAME, null) ?: return false
        val appWidgetIdToPeriodInt = mutableMapOf<Int, Int>()
        appWidgetManager.getAppWidgetIds(ComponentName(this, ChartsWidgetProvider::class.java))
            .forEach {
                appWidgetIdToPeriodInt[it] = pref.getInt(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_PERIOD, it), -1)
            }

        fun updateData(all: Triple<ArrayList<ChartsWidgetListItem>, ArrayList<ChartsWidgetListItem>, ArrayList<ChartsWidgetListItem>>,
                       periodInt:Int, appWidgetIds: Collection<Int>) {
            val (artists, albums, tracks) = all

            val editor = pref.edit()
                .putString(
                    ""+Stuff.TYPE_ARTISTS+"_"+periodInt,
                    ObjectSerializeHelper.convertToString(artists)
                )
                .putString(
                    ""+Stuff.TYPE_ALBUMS+"_"+periodInt,
                    ObjectSerializeHelper.convertToString(albums)
                )
                .putString(
                    ""+Stuff.TYPE_TRACKS+"_"+periodInt,
                    ObjectSerializeHelper.convertToString(tracks)
                )
            appWidgetIds.forEach {
                editor.putLong(Stuff.getWidgetPrefName(Stuff.PREF_WIDGET_LAST_UPDATED, it), System.currentTimeMillis())
            }
            editor.commit()


            appWidgetIds.forEach {
                appWidgetManager.notifyAppWidgetViewDataChanged(it, R.id.appwidget_list)
            }
        }

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            jobFinished(jp, false)
        }

        CoroutineScope(Dispatchers.IO + Job()).launch(exHandler) {
            for (periodInt in appWidgetIdToPeriodInt.values.toSet()) {
                if (periodInt == -1)
                    continue
                val period = Period.values()[periodInt]

                val artists = async {
                    val pr = User.getTopArtists(username, period, 50, 1, Stuff.LAST_KEY)
                    pr.username!!
                    ArrayList(
                        pr.pageResults!!.map { ChartsWidgetListItem(it.name, "", it.playcount) }
                    )
                }
                val albums = async {
                    val pr = User.getTopAlbums(username, period, 50, 1, Stuff.LAST_KEY)
                    pr.username!!
                    ArrayList(
                        pr.pageResults!!.map { ChartsWidgetListItem(it.name, it.artist, it.playcount) }
                    )
                }
                val tracks = async {
                    val pr = User.getTopTracks(username, period, 50, 1, Stuff.LAST_KEY)
                    pr.username!!
                    ArrayList(
                        pr.pageResults!!.map { ChartsWidgetListItem(it.name, it.artist, it.playcount) }
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
        return true
    }

    companion object {
        const val JOB_ID = 11
        var mightBeRunning = false // this may not be false when the job is force stopped
        private const val INTERVAL = 15 * 60 * 1000L //15 mins is the minimum

        fun checkAndSchedule(context: Context, force: Boolean = true) {
            if (mightBeRunning)
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

            val job = JobInfo.Builder(JOB_ID, ComponentName(context, ChartsWidgetUpdaterJob::class.java))
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