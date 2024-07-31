package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Period
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodType
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.main.App
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.setUserFirstDayOfWeek
import com.arn.scrobble.utils.UiUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit


class ChartsWidgetUpdaterWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    private val widgetPrefs by lazy { WidgetPrefs(applicationContext) }

    override suspend fun getForegroundInfo() = ForegroundInfo(
        NAME_ONE_TIME.hashCode(),
        UiUtils.createNotificationForFgs(
            applicationContext,
            applicationContext.getString(R.string.pref_widget_charts)
        )
    )

    // runs in Dispatchers.DEFAULT
    override suspend fun doWork(): Result {
        // not logged in
        Scrobblables.current ?: return Result.failure()

        logTimestampToFile("started")

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
                widgetPrefs[id].period = Period.MONTH.value
            }
            widgetPrefs[id].period?.let { period ->
                appWidgetIdToPeriodStr[id] = period
                widgetPrefs[id].periodName = widgetTimePeriods.periodsMap[period]?.name
            }

        }

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            logTimestampToFile("errored " + throwable.message)
            errored = true
        }

        withContext(exHandler) {
            // support a max of 3 widgets at a time
            appWidgetIdToPeriodStr.values.toSet().take(3).mapConcurrently(3) { periodStr ->
                val timePeriod = widgetTimePeriods.periodsMap[periodStr]
                    ?: widgetTimePeriods.periodsMap[Period.MONTH.value]!! // default to 1 month

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
                    Scrobblables.current!!.getChartsWithStonks(
                        type,
                        timePeriod,
                        prevTimePeriod,
                        1,
                        limit = 50
                    )
                        .map { pr ->
                            pr.entries.map {
                                val subtitle = when (it) {
                                    is Album -> it.artist!!.name
                                    is Track -> it.artist.name
                                    else -> ""
                                }

                                val imgUrl = if (it is Album) it.webp300 else null

                                ChartsWidgetListItem(
                                    it.name,
                                    subtitle,
                                    it.playcount ?: 0,
                                    imgUrl ?: "",
                                    it.stonksDelta
                                )
                            }
                        }

                }

                artists.onSuccess {
                    widgetPrefs.chartsData(Stuff.TYPE_ARTISTS, periodStr).dataJson = it
                }

                albums.onSuccess {
                    widgetPrefs.chartsData(Stuff.TYPE_ALBUMS, periodStr).dataJson = it
                }

                tracks.onSuccess {
                    widgetPrefs.chartsData(Stuff.TYPE_TRACKS, periodStr).dataJson = it
                }
            }

            if (!errored)
                appWidgetIdToPeriodStr.keys.forEach { id ->
                    widgetPrefs[id].lastUpdated = System.currentTimeMillis()
                }

            delay(1000) // wait for apply()

            ChartsListUtils.updateWidgets(appWidgetIdToPeriodStr.keys.toIntArray())

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

            Timber.i("scheduling ${ChartsWidgetUpdaterWorker::class.java.simpleName}")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME_PERIODIC)
            Timber.i("cancelled ${ChartsWidgetUpdaterWorker::class.java.simpleName}")
        }
    }
}