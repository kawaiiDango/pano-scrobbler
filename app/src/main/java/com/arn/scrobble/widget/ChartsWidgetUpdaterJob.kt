package com.arn.scrobble.widget

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.scheduleExpeditedCompat
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.pref.WidgetPrefs
import de.umass.lastfm.Album
import de.umass.lastfm.ImageSize
import de.umass.lastfm.MusicEntry
import de.umass.lastfm.PaginatedResult
import de.umass.lastfm.Period
import de.umass.lastfm.Session
import de.umass.lastfm.Track
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar


class ChartsWidgetUpdaterJob : JobService() {

    private lateinit var job: Job
    private val widgetPrefs by lazy { WidgetPrefs(applicationContext) }

    override fun onStartJob(jp: JobParameters): Boolean {
        job = SupervisorJob()

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        val widgetTimePeriods = WidgetTimePeriods(this)

        val appWidgetIdToPeriodStr = mutableMapOf<Int, String>()
        appWidgetManager.getAppWidgetIds(ComponentName(this, ChartsWidgetProvider::class.java))
            .forEach { id ->
                if (widgetPrefs[id].lastUpdated != null && widgetPrefs[id].period == null) { // set default value for old prefs
                    widgetPrefs[id].period = Period.ONE_MONTH.string
                }
                widgetPrefs[id].period?.let { period ->
                    appWidgetIdToPeriodStr[id] = period
                }
            }

        suspend fun updateData(
            all: Triple<List<ChartsWidgetListItem>, List<ChartsWidgetListItem>, List<ChartsWidgetListItem>>,
            periodStr: String, appWidgetIds: Collection<Int>
        ) {
            val (artists, albums, tracks) = all

            widgetPrefs.chartsData(Stuff.TYPE_ARTISTS, periodStr).dataJson = artists
            widgetPrefs.chartsData(Stuff.TYPE_ALBUMS, periodStr).dataJson = albums
            widgetPrefs.chartsData(Stuff.TYPE_TRACKS, periodStr).dataJson = tracks

            appWidgetIds.forEach { id ->
                widgetPrefs[id].lastUpdated = System.currentTimeMillis()
                widgetPrefs[id].periodName = widgetTimePeriods.periodsMap[periodStr]?.name
            }

            delay(1000) // wait for apply()

            appWidgetIds.forEach {
                ChartsListUtils.updateWidget(it)
            }
        }

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            jobFinished(jp, false)
        }

        CoroutineScope(Dispatchers.IO + job).launch(exHandler) {
            for (periodStr in appWidgetIdToPeriodStr.values.toSet()) {
                val timePeriod = widgetTimePeriods.periodsMap[periodStr]
                    ?: widgetTimePeriods.periodsMap[Period.ONE_MONTH.string]!! // default to 1 month

                val cal = Calendar.getInstance()
                cal.setMidnight()

                val prevTimePeriod =
                    if (timePeriod.period != null && timePeriod.period != Period.OVERALL) {
                        val duration = timePeriod.period.toDuration(endTime= cal.timeInMillis)
                        timePeriod.period.toTimePeriod(endTime= cal.timeInMillis - duration)

                    } else {
                        cal.timeInMillis = timePeriod.start

                        when (periodStr) {
                            TimePeriodType.WEEK.toString() -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                            TimePeriodType.MONTH.toString() -> cal.add(Calendar.MONTH, -1)
                            TimePeriodType.YEAR.toString() -> cal.add(Calendar.YEAR, -1)
                            else -> null
                        }?.let {
                            TimePeriod(cal.timeInMillis, timePeriod.start)
                        }
                    }

                val (artists, albums, tracks) = listOf(
                    Stuff.TYPE_ARTISTS,
                    Stuff.TYPE_ALBUMS,
                    Stuff.TYPE_TRACKS
                ).map { type ->
                    var session: Session? = null
                    val pr = LFMRequester(this@ChartsWidgetUpdaterJob, this)
                        .execHere<PaginatedResult<out MusicEntry>> {
                            getChartsWithStonks(
                                type,
                                timePeriod,
                                prevTimePeriod,
                                1,
                                null,
                                limit = 50
                            )
                            session = lastfmSession
                        }
                    if (session?.result?.isSuccessful != true) {
                        cancel()
                    }
                    pr!!.pageResults!!.map {
                        val subtitle = when (it) {
                            is Album -> it.artist
                            is Track -> it.artist
                            else -> ""
                        }

                        val imgUrl = if (it is Album) it.getWebpImageURL(ImageSize.LARGE) else null

                        ChartsWidgetListItem(
                            it.name,
                            subtitle,
                            it.playcount,
                            imgUrl ?: "",
                            it.stonksDelta
                        )
                    }
                }

                updateData(
                    Triple(artists, albums, tracks),
                    periodStr,
                    appWidgetIdToPeriodStr.keys
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
        private const val JOB_ID = 11
        private const val ONE_SHOT_JOB_ID = 12

        fun checkAndSchedule(context: Context, runImmediately: Boolean) {
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobs = js.allPendingJobs

            if (runImmediately) {
                JobInfo.Builder(
                    ONE_SHOT_JOB_ID,
                    ComponentName(context, ChartsWidgetUpdaterJob::class.java)
                )
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .scheduleExpeditedCompat(js) {
                        setMinimumLatency(1)
                        setOverrideDeadline(1)
                    }
            }

            if (jobs.any { it.id == JOB_ID }) {
                Stuff.log("Found " + jobs.size + " existing jobs")
                return
            }

            val job =
                JobInfo.Builder(JOB_ID, ComponentName(context, ChartsWidgetUpdaterJob::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(Stuff.CHARTS_WIDGET_REFRESH_INTERVAL)
                    .setPersisted(true)
                    .build()
            js.schedule(job)
            Stuff.log("scheduling ${ChartsWidgetUpdaterJob::class.java.simpleName}")
        }

        fun cancel(context: Context) {
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val id = js.allPendingJobs.firstOrNull { it.id == JOB_ID }?.id
            id?.let { js.cancel(it) }
            Stuff.log("cancelled ${ChartsWidgetUpdaterJob::class.java.simpleName}")
        }
    }
}