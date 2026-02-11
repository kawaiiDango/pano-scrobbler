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
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.R
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.Stuff.setMidnight
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

    override suspend fun getForegroundInfo() = PanoNotifications.createForegroundInfo(
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

        val appWidgetIds =
            appWidgetManager.getAppWidgetIds(
                ComponentName(
                    applicationContext,
                    ChartsWidgetProvider::class.java
                )
            )
        val widgetPrefs = AndroidStuff.widgetPrefs.data.first()
        val firstDayOfWeek = PlatformStuff.mainPrefs.data.map { it.firstDayOfWeek }.first()

        // don't run if it already ran recently
        if (!isOneTimeWork && (appWidgetIds.isEmpty() ||
                    (System.currentTimeMillis() - widgetPrefs.lastFetched) < Stuff.CHARTS_WIDGET_REFRESH_INTERVAL_HOURS * 60 * 60 * 1000L / 2)
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
                    val timePeriod = period.toTimePeriod(firstDayOfWeek)
                    val cal = Calendar.getInstance()
                    cal.setMidnight()
                    if (firstDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY)
                        cal.firstDayOfWeek = firstDayOfWeek

                    val prevTimeLastfmPeriod =
                        if (timePeriod.lastfmPeriod != null && timePeriod.lastfmPeriod != LastfmPeriod.OVERALL) {
                            val duration =
                                timePeriod.lastfmPeriod.toDuration(endTime = cal.timeInMillis)
                            timePeriod.lastfmPeriod.toTimePeriod(endTime = cal.timeInMillis - duration)

                        } else {
                            cal.timeInMillis = timePeriod.start

                            when (period) {
                                WidgetPeriods.THIS_WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                                WidgetPeriods.THIS_MONTH -> cal.add(Calendar.MONTH, -1)
                                WidgetPeriods.THIS_YEAR -> cal.add(Calendar.YEAR, -1)
                                else -> null
                            }?.let {
                                TimePeriod(cal.timeInMillis, timePeriod.start)
                            }
                        }

                    var noData = false

                    val (artists, albums, tracks) = listOf(
                        Stuff.TYPE_ARTISTS,
                        Stuff.TYPE_ALBUMS,
                        Stuff.TYPE_TRACKS
                    ).map { type ->
                        if (noData) {
                            return@map kotlin.Result.success(emptyList())
                        }

                        scrobblable.getChartsWithStonks(
                            type,
                            timePeriod,
                            prevTimeLastfmPeriod,
                            1,
                            limit = 50
                        )
                            .onSuccess {
                                if (type != Stuff.TYPE_ALBUMS && it.entries.isEmpty()) {
                                    noData = true
                                }
                            }.onFailure {
                                throw it
                            }
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
                        val chartsData = it.charts.toMutableMap()
                        val chartsDataForPeriod = chartsData[period]
                        chartsData[period] = WidgetPrefs.ChartsData(
                            artists = artists.getOrElse { chartsDataForPeriod?.artists.orEmpty() },
                            albums = albums.getOrElse { chartsDataForPeriod?.albums.orEmpty() },
                            tracks = tracks.getOrElse { chartsDataForPeriod?.tracks.orEmpty() },
                            timePeriodString = timePeriod.name
                        )

                        it.copy(charts = chartsData, lastFetched = System.currentTimeMillis())
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
        if (!BuildKonfig.DEBUG) return

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

        fun schedule(context: Context, runImmediately: Boolean) {
            val constraintsBuilder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)

            if (runImmediately) {
                val inputData = Data.Builder()
                    .putString(WORK_NAME_KEY, NAME_ONE_TIME)
                    .build()

                val oneTimeWork = OneTimeWorkRequestBuilder<ChartsWidgetUpdaterWorker>()
                    .setConstraints(constraintsBuilder.build())
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
                Stuff.CHARTS_WIDGET_REFRESH_INTERVAL_HOURS.toLong(),
                TimeUnit.HOURS
            )
                .setConstraints(
                    constraintsBuilder
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setInputData(inputData)
                .setInitialDelay(
                    Stuff.CHARTS_WIDGET_REFRESH_INTERVAL_HOURS.toLong(),
                    TimeUnit.HOURS
                )
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