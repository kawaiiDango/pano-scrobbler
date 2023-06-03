package com.arn.scrobble

import android.app.NotificationManager
import android.content.Context
import android.text.Html
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arn.scrobble.Stuff.isChannelEnabled
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.Stuff.setUserFirstDayOfWeek
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.ListenbrainzRanges
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.themes.ColorPatchUtils
import com.arn.scrobble.ui.UiUtils
import de.umass.lastfm.Period
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DigestWorker(context: Context, private val workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {
    private val nm by lazy {
        ContextCompat.getSystemService(
            applicationContext,
            NotificationManager::class.java
        )!!
    }
    private val prefs = App.prefs
    private val cal by lazy { Calendar.getInstance().setUserFirstDayOfWeek() }

    override suspend fun getForegroundInfo() = ForegroundInfo(
        this::class.hashCode(),
        UiUtils.createNotificationForFgs(
            applicationContext,
            applicationContext.getString(R.string.graph_weekly)
        )
    )

    override suspend fun doWork(): Result {
        var errored = false

        if (nm.isChannelEnabled(
                prefs.sharedPreferences,
                MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY
            )
        ) {
            val coExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                throwable.printStackTrace()
                errored = true
            }
            val period = when (workerParameters.inputData.getString(Stuff.ARG_ACTION)) {
                DAILY,
                WEEKLY -> Period.WEEK

                MONTHLY -> Period.ONE_MONTH
                else -> throw IllegalArgumentException("Unknown action")
            }

            withContext(Dispatchers.IO + coExceptionHandler) {
                fetchAndNotify(period)
                // yearly digest
                if (period == Period.ONE_MONTH && cal[Calendar.MONTH] == Calendar.DECEMBER) {
                    fetchAndNotify(Period.TWELVE_MONTHS)
                }
            }

        } else {
            errored = true
        }

        schedule(applicationContext)

        return if (errored)
            Result.failure()
        else
            Result.success()
    }

    private suspend fun fetchAndNotify(period: Period) {
        supervisorScope {
            val limit = 3
            val notificationTextList = mutableListOf<String>()
            val scrobblable = Scrobblables.current ?: return@supervisorScope

            val timePeriod = TimePeriod(period).apply {
                tag = when (period) {
                    Period.WEEK -> ListenbrainzRanges.week.name
                    Period.ONE_MONTH -> ListenbrainzRanges.month.name
                    Period.TWELVE_MONTHS -> ListenbrainzRanges.year.name
                    else -> null
                }
            }

            val artists = async {
                scrobblable.getCharts(Stuff.TYPE_ARTISTS, timePeriod, 1, null, limit = limit)
            }
            val albums = async {
                scrobblable.getCharts(Stuff.TYPE_ALBUMS, timePeriod, 1, null, limit = limit)
            }
            val tracks = async {
                scrobblable.getCharts(Stuff.TYPE_TRACKS, timePeriod, 1, null, limit = limit)
            }

            val resultsMap = mapOf(
                R.string.top_artists to artists,
                R.string.top_albums to albums,
                R.string.top_tracks to tracks
            )
            resultsMap.forEach { (titleRes, defered) ->

                val kResult = kotlin.runCatching { defered.await() }
                val result = kResult.getOrNull() ?: return@forEach
                if (result.pageResults.isEmpty()) return@forEach

                val title = applicationContext.getString(titleRes)
                val text = result.pageResults.joinToString { it.name }
                notificationTextList += "<b>$title:</b>\n$text"
            }

            val title = applicationContext.getString(
                when (period) {
                    Period.WEEK -> R.string.digest_weekly
                    Period.ONE_MONTH -> R.string.digest_monthly
                    Period.TWELVE_MONTHS -> R.string.graph_yearly
                    else -> throw IllegalArgumentException("Invalid period")
                }
            )

            if (notificationTextList.isEmpty()) {
                return@supervisorScope
            }

            val notificationText = Html.fromHtml(notificationTextList.joinToString("<br>\n"))

            val channelId = if (period == Period.WEEK)
                MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY
            else
                MainPrefs.CHANNEL_NOTI_DIGEST_MONTHLY

            val launchPi = NavDeepLinkBuilder(applicationContext)
                .setComponentName(MainActivity::class.java)
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.myHomePagerFragment)
                .setArguments(bundleOf(Stuff.ARG_TAB to 2))
                .createPendingIntent()

            val collageArgs = bundleOf(Stuff.ARG_TYPE to Stuff.TYPE_ALL)
                .putSingle(timePeriod)

            val collagePi = MainDialogActivity.createDestinationPendingIntent(
                R.id.collageGeneratorFragment,
                collageArgs
            )

            val nb = NotificationCompat.Builder(applicationContext, channelId)
                .apply {
                    color = (ColorPatchUtils.getNotiColor(applicationContext) ?: return@apply)
                }
                .setSmallIcon(R.drawable.vd_charts)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(title)
                .setContentIntent(launchPi)
                .addAction(
                    Stuff.getNotificationAction(
                        R.drawable.vd_mosaic,
                        "🖼️",
                        applicationContext.getString(R.string.create_collage),
                        collagePi
                    )
                )
                .setContentText(notificationText)
                .setShowWhen(true)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(notificationText)
                )
                .setVisibility(
                    if (prefs.notificationsOnLockscreen)
                        NotificationCompat.VISIBILITY_PUBLIC
                    else
                        NotificationCompat.VISIBILITY_SECRET
                )

            nm.notify(channelId, period.ordinal, nb.build())
        }
    }

    companion object {
        const val DAILY = "DIGEST_DAILY"
        const val WEEKLY = "DIGEST_WEEKLY"
        const val MONTHLY = "DIGEST_MONTHLY"

        fun schedule(
            context: Context,
            existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
        ) {
            val workManager = WorkManager.getInstance(context)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val scheduleTimes = getScheduleTimes()

            fun enqueue(period: String) {
                val inputData = Data.Builder()
                    .putString(Stuff.ARG_ACTION, period)
                    .build()

                val work = OneTimeWorkRequestBuilder<DigestWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .setInitialDelay(
                        scheduleTimes[period]!! - System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS
                    )
                    .build()

                workManager.enqueueUniqueWork(period, existingWorkPolicy, work)
            }

            val dailyTestDigests = true
            if (BuildConfig.DEBUG && dailyTestDigests)
                enqueue(DAILY)
            enqueue(WEEKLY)
            enqueue(MONTHLY)

            Stuff.log("scheduling ${DigestWorker::class.java.simpleName}")
        }

        private fun getScheduleTimes(): Map<String, Long> {
            val prefs = App.prefs

            if (prefs.digestSeconds == null)
                prefs.digestSeconds = (60..(30 * 60)).random()

            val secondsToAdd = -(prefs.digestSeconds ?: 60)

            val timesMap = mutableMapOf<String, Long>()

            val now = System.currentTimeMillis()

            val cal = Calendar.getInstance()
            cal.setUserFirstDayOfWeek()
            cal[Calendar.DAY_OF_WEEK] = cal.firstDayOfWeek

            cal.setMidnight()

            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
            if (cal.timeInMillis < now)
                cal.add(Calendar.WEEK_OF_YEAR, 1)

            timesMap[WEEKLY] = cal.timeInMillis

            cal.timeInMillis = now
            cal.setMidnight()

            cal[Calendar.DAY_OF_MONTH] = 1
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
            if (cal.timeInMillis < now)
                cal.add(Calendar.MONTH, 1)

            timesMap[MONTHLY] = cal.timeInMillis

            cal.timeInMillis = now
            cal.setMidnight()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
//            cal.add(Calendar.MINUTE, 1)
            if (cal.timeInMillis < now)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            timesMap[DAILY] = cal.timeInMillis


            return timesMap
        }

    }
}