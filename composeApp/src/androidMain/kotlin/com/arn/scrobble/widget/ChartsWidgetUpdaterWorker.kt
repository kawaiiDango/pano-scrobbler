package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.charts.AllPeriods
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.setUserFirstDayOfWeek
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit


class ChartsWidgetUpdaterWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    val workName = inputData.getString(WORK_NAME_KEY)!!
    val isOneTimeWork = workName == NAME_ONE_TIME

    override suspend fun getForegroundInfo() = AndroidStuff.createForegroundInfoNotification(
        applicationContext.getString(R.string.charts)
    )

    // runs in Dispatchers.DEFAULT
    override suspend fun doWork(): Result {
        // not logged in
        val scrobblable = Scrobblables.current
            ?: return Result.failure(
                Data.Builder()
                    .putString("reason", "Not logged in")
                    .build()
            )

        logTimestampToFile("started")

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        val widgetTimePeriods = WidgetTimePeriods()

        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    applicationContext,
                    ChartsWidgetProvider::class.java
                )
            )
        val widgetPrefs = AndroidStuff.widgetPrefs.data.first()
        val lastInteractiveTime =
            PlatformStuff.mainPrefs.data.map { it.lastInteractiveTime }.first()


        // don't run if the user has not checked the device recently or it already ran recently
        if (!isOneTimeWork && (appWidgetIds.isEmpty() ||
                    System.currentTimeMillis() - widgetPrefs.lastFetched < Stuff.CHARTS_WIDGET_REFRESH_INTERVAL / 2 ||
                    lastInteractiveTime == null ||
                    System.currentTimeMillis() - lastInteractiveTime > Stuff.CHARTS_WIDGET_REFRESH_INTERVAL * 2)
        ) {
            logTimestampToFile("skipped")
            return Result.failure(
                Data.Builder()
                    .putString("reason", "Not enough widgets or not enough interaction")
                    .build()
            )
        }

        var errorData: Data? = null

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            logTimestampToFile("errored " + throwable.redactedMessage)
            errorData = Data.Builder()
                .putString("reason", throwable.redactedMessage)
                .build()
        }

        withContext(exHandler) {
            // support a max of 3 periods
            widgetPrefs.widgets
                .values
                .map { it.period }
                .toSet()
                .take(3)
                .mapConcurrently(3) { period ->
                    val timePeriod = widgetTimePeriods.toTimePeriod(period)
                    val cal = Calendar.getInstance().setUserFirstDayOfWeek()
                    cal.setMidnight()

                    val prevTimeLastfmPeriod =
                        if (timePeriod.lastfmPeriod != null && timePeriod.lastfmPeriod != LastfmPeriod.OVERALL) {
                            val duration =
                                timePeriod.lastfmPeriod.toDuration(endTime = cal.timeInMillis)
                            timePeriod.lastfmPeriod.toTimePeriod(endTime = cal.timeInMillis - duration)

                        } else {
                            cal.timeInMillis = timePeriod.start

                            when (period) {
                                AllPeriods.THIS_WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                                AllPeriods.THIS_MONTH -> cal.add(Calendar.MONTH, -1)
                                AllPeriods.THIS_YEAR -> cal.add(Calendar.YEAR, -1)
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
                        scrobblable.getChartsWithStonks(
                            type,
                            timePeriod,
                            prevTimeLastfmPeriod,
                            1,
                            limit = 50
                        )
                            .map { pr ->
                                pr.entries.map {
                                    val subtitle = when (it) {
                                        is Album -> it.artist!!.name
                                        is Track -> it.artist.name
                                        else -> null
                                    }

                                    val imgUrl = if (it is Album) it.webp300 else null

                                    ChartsWidgetListItem(
                                        it.name,
                                        subtitle,
                                        it.playcount?.toInt() ?: 0,
                                        imgUrl ?: "",
                                        it.stonksDelta
                                    )
                                }
                            }
                    }

                    AndroidStuff.widgetPrefs.updateData {
                        val chartsData = it.chartsData.toMutableMap()
                        val chartsDataForPeriod = chartsData[period] ?: emptyMap()
                        chartsData[period] = mapOf(
                            Stuff.TYPE_ARTISTS to (artists.getOrNull()
                                ?: chartsDataForPeriod[Stuff.TYPE_ARTISTS] ?: emptyList()),
                            Stuff.TYPE_ALBUMS to (albums.getOrNull()
                                ?: chartsDataForPeriod[Stuff.TYPE_ALBUMS] ?: emptyList()),
                            Stuff.TYPE_TRACKS to (tracks.getOrNull()
                                ?: chartsDataForPeriod[Stuff.TYPE_TRACKS] ?: emptyList())
                        )

                        it.copy(chartsData = chartsData, lastFetched = System.currentTimeMillis())
                    }
                }

            delay(1000) // wait for apply()

            ChartsListUtils.updateWidgets(appWidgetIds)

            logTimestampToFile("finished")
        }

        return if (errorData != null)
            Result.failure(errorData)
        else
            Result.success()
    }

    private fun logTimestampToFile(event: String) {
        if (!PlatformStuff.isDebug) return

        val file = File(PlatformStuff.filesDir, "timestamps.txt")
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
        private const val WORK_NAME_KEY = "uniqueWorkName"

        fun checkAndSchedule(context: Context, runImmediately: Boolean) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            if (runImmediately) {
                val inputData = Data.Builder()
                    .putString(WORK_NAME_KEY, NAME_ONE_TIME)
                    .build()

                val oneTimeWork = OneTimeWorkRequestBuilder<ChartsWidgetUpdaterWorker>()
                    .setConstraints(constraints)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    NAME_ONE_TIME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeWork
                )

                Logger.i { "scheduling ${ChartsWidgetUpdaterWorker::class.java.simpleName} runImmediately" }
            }

            val inputData = Data.Builder()
                .putString(WORK_NAME_KEY, NAME_PERIODIC)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<ChartsWidgetUpdaterWorker>(
                Stuff.CHARTS_WIDGET_REFRESH_INTERVAL,
                TimeUnit.MILLISECONDS
            )
                .setConstraints(constraints)
                .setInputData(inputData)
                .setInitialDelay(Stuff.CHARTS_WIDGET_REFRESH_INTERVAL, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )

            Logger.i { "scheduling ${ChartsWidgetUpdaterWorker::class.java.simpleName}" }
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME_PERIODIC)
            Logger.i { "cancelled ${ChartsWidgetUpdaterWorker::class.java.simpleName}" }
        }
    }
}