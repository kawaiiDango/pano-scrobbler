package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.Stuff
import com.arn.scrobble.Stuff.mapConcurrently
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit


class ChartsWidgetUpdaterWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private lateinit var job: Job
    private val widgetPrefs by lazy { WidgetPrefs(applicationContext) }

    // runs in Dispatchers.DEFAULT
    override suspend fun doWork(): Result {
        logTimestampToFile("started")

        job = SupervisorJob()

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        val widgetTimePeriods = WidgetTimePeriods(applicationContext)

        val appWidgetIdToPeriodStr = mutableMapOf<Int, String>()
        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    applicationContext,
                    ChartsWidgetProvider::class.java
                )
            )
        var errored = false

        // don't run if the user has not checked the device recently or it already ran recently
        val lastInteractiveTime = App.prefs.lastInteractiveTime
        if ((appWidgetIds.isEmpty() ||
                    System.currentTimeMillis() - (widgetPrefs[appWidgetIds.first()].lastUpdated
                ?: 0) < Stuff.CHARTS_WIDGET_REFRESH_INTERVAL / 2 ||
                    lastInteractiveTime == null ||
                    System.currentTimeMillis() - lastInteractiveTime > Stuff.CHARTS_WIDGET_REFRESH_INTERVAL * 2)
        ) {
            logTimestampToFile("skipped")
            return Result.failure()
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
            logTimestampToFile("errored "+ throwable.message)
            errored = true
        }

        withContext(Dispatchers.IO + exHandler) {
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
        }

        return if (errored)
            Result.failure()
        else
            Result.success()
    }

    private fun logTimestampToFile(event: String) {
        if (!BuildConfig.DEBUG) return

        val file = File(applicationContext.filesDir, "timestamps.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.appendText(
            "$event: ${
                DateFormat.getDateTimeInstance().format(System.currentTimeMillis())
            }\n"
        )
    }

    companion object {

        const val NAME_ONE_TIME = "charts_widget_updater_one_time"
        const val NAME_PERIODIC = "charts_widget_updater_periodic"

        fun checkAndSchedule(context: Context, runImmediately: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            if (runImmediately) {
                val oneTimeWork = OneTimeWorkRequestBuilder<ChartsWidgetUpdaterWorker>()
                    .setConstraints(constraints)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    NAME_ONE_TIME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeWork
                )
            }

            val periodicWork = PeriodicWorkRequestBuilder<ChartsWidgetUpdaterWorker>(
                Stuff.CHARTS_WIDGET_REFRESH_INTERVAL,
                TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .setInitialDelay(Stuff.CHARTS_WIDGET_REFRESH_INTERVAL, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )

            Stuff.log("scheduling ${ChartsWidgetUpdaterWorker::class.java.simpleName}")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME_PERIODIC)
            Stuff.log("cancelled ${ChartsWidgetUpdaterWorker::class.java.simpleName}")
        }
    }
}