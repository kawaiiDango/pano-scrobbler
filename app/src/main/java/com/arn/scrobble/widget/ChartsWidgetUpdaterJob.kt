package com.arn.scrobble.widget

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.ForceLogException
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.mapConcurrently
import com.arn.scrobble.Stuff.scheduleExpeditedCompat
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.Stuff.setUserFirstDayOfWeek
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
import de.umass.lastfm.Track
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.DateFormat
import java.util.Calendar


class ChartsWidgetUpdaterJob : JobService() {

    private lateinit var job: Job
    private val widgetPrefs by lazy { WidgetPrefs(applicationContext) }

    override fun onStartJob(jp: JobParameters): Boolean {

        logTimestampToFile("started")

        job = SupervisorJob()

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        val widgetTimePeriods = WidgetTimePeriods(this)

        val appWidgetIdToPeriodStr = mutableMapOf<Int, String>()
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(ComponentName(this, ChartsWidgetProvider::class.java))
        var errored = false

        // expedited job scheduled, don't run the periodic job
        if (jp.jobId == JOB_ID_PERIODIC &&
            ContextCompat.getSystemService(
                this,
                JobScheduler::class.java
            )!!.allPendingJobs.any { it.id == ONE_SHOT_JOB_ID }
        )
            return false

        // don't run if the user has not checked the device recently or it already ran recently
        val lastInteractiveTime = App.prefs.lastInteractiveTime
        if (jp.jobId == JOB_ID_PERIODIC &&
            (appWidgetIds.isEmpty() ||
                    System.currentTimeMillis() - (widgetPrefs[appWidgetIds.first()].lastUpdated
                ?: 0) < Stuff.CHARTS_WIDGET_REFRESH_INTERVAL / 2 ||
                    lastInteractiveTime == null ||
                    System.currentTimeMillis() - lastInteractiveTime > Stuff.CHARTS_WIDGET_REFRESH_INTERVAL * 2)
        ) {
            logTimestampToFile("skipped")
            return false
        }

        appWidgetIds.forEach { id ->
            if (widgetPrefs[id].lastUpdated != null && widgetPrefs[id].period == null) { // set default value for old prefs
                widgetPrefs[id].period = Period.ONE_MONTH.string
            }
            widgetPrefs[id].period?.let { period ->
                appWidgetIdToPeriodStr[id] = period
                widgetPrefs[id].periodName = widgetTimePeriods.periodsMap[period]?.name
            }

        }

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            logTimestampToFile("errored")
            errored = true
            jobFinished(jp, false)
        }

        CoroutineScope(Dispatchers.IO + job).launch(exHandler) {
            // support a max of 3 widgets at a time
            appWidgetIdToPeriodStr.values.toSet().take(3).mapConcurrently(3) { periodStr ->
                val timePeriod = widgetTimePeriods.periodsMap[periodStr]
                    ?: widgetTimePeriods.periodsMap[Period.ONE_MONTH.string]!! // default to 1 month

                val cal = Calendar.getInstance().setUserFirstDayOfWeek()
                cal.setMidnight()

                val prevTimePeriod =
                    if (timePeriod.period != null && timePeriod.period != Period.OVERALL) {
                        val duration = timePeriod.period.toDuration(endTime = cal.timeInMillis)
                        timePeriod.period.toTimePeriod(endTime = cal.timeInMillis - duration)

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
                ).mapConcurrently(3) { type ->
                    val pr = LFMRequester(this)
                        .execHere<PaginatedResult<out MusicEntry>> {
                            getChartsWithStonks(
                                type,
                                timePeriod,
                                prevTimePeriod,
                                1,
                                null,
                                limit = 50
                            )
                        }
                    if (pr == null || pr.pageResults == null) {// todo check if not 200
                        errored = true
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

                if (!isActive || errored)
                    return@mapConcurrently

                widgetPrefs.chartsData(Stuff.TYPE_ARTISTS, periodStr).dataJson = artists
                widgetPrefs.chartsData(Stuff.TYPE_ALBUMS, periodStr).dataJson = albums
                widgetPrefs.chartsData(Stuff.TYPE_TRACKS, periodStr).dataJson = tracks
            }

            if (!errored)
                appWidgetIdToPeriodStr.keys.forEach { id ->
                    widgetPrefs[id].lastUpdated = System.currentTimeMillis()
                }

            delay(1000) // wait for apply()

            ChartsListUtils.updateWidget(appWidgetIdToPeriodStr.keys.toIntArray())

            logTimestampToFile("finished")

            jobFinished(jp, false)
        }
        return true
    }

    private fun logTimestampToFile(event: String) {
        if (!BuildConfig.DEBUG) return

        val file = File(filesDir, "timestamps.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.appendText(
            "$event: ${
                DateFormat.getDateTimeInstance().format(System.currentTimeMillis())
            }\n"
        )
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val reason = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.stopReason else ""
        if (BuildConfig.DEBUG)
            Timber.tag(Stuff.TAG)
                .e(ForceLogException("${ChartsWidgetUpdaterJob::class.simpleName} force stopped $reason"))
        logTimestampToFile("stopped $reason")
        job.cancel()
        return true
    }

    companion object {
        private const val JOB_ID_PERIODIC = 11
        private const val ONE_SHOT_JOB_ID = 12

        fun checkAndSchedule(context: Context, runImmediately: Boolean) {
            val js = ContextCompat.getSystemService(context, JobScheduler::class.java)!!
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

            if (jobs.any { it.id == JOB_ID_PERIODIC }) {
                Stuff.log("Found " + jobs.size + " existing jobs")
                return
            }

            val job =
                JobInfo.Builder(
                    JOB_ID_PERIODIC,
                    ComponentName(context, ChartsWidgetUpdaterJob::class.java)
                )
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(Stuff.CHARTS_WIDGET_REFRESH_INTERVAL)
                    .setPersisted(true)
                    .build()
            js.schedule(job)
            Stuff.log("scheduling ${ChartsWidgetUpdaterJob::class.java.simpleName}")
        }

        fun cancel(context: Context) {
            val js = ContextCompat.getSystemService(context, JobScheduler::class.java)!!
            val id = js.allPendingJobs.firstOrNull { it.id == JOB_ID_PERIODIC }?.id
            id?.let { js.cancel(it) }
            Stuff.log("cancelled ${ChartsWidgetUpdaterJob::class.java.simpleName}")
        }
    }
}