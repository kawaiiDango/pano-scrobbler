package com.arn.scrobble

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.text.Html
import androidx.core.app.NotificationCompat
import com.arn.scrobble.Stuff.isChannelEnabled
import com.arn.scrobble.Stuff.putSingle
import com.arn.scrobble.Stuff.scheduleExpeditedCompat
import com.arn.scrobble.Stuff.setMidnight
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.friends.UserAccountSerializable
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.scrobbleable.ScrobblableEnum
import com.arn.scrobble.scrobbleable.Scrobblables
import com.arn.scrobble.themes.ColorPatchUtils
import de.umass.lastfm.Period
import de.umass.lastfm.Session
import de.umass.lastfm.User
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DigestJob : JobService() {

    private val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val prefs by lazy { MainPrefs(this) }
    private val cal by lazy { Calendar.getInstance()}

    override fun onStartJob(jp: JobParameters): Boolean {
        scheduleAlarms(applicationContext)

        val lastfmAccount = Scrobblables.byType(ScrobblableEnum.LASTFM) ?: return false

        if (nm.isChannelEnabled(
                prefs.sharedPreferences,
                MainPrefs.CHANNEL_NOTI_DIGEST_WEEKLY
            )
        ) {
            val coExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                throwable.printStackTrace()
                jobFinished(jp, true)
            }
            val period = when (jp.extras.getString(Stuff.ARG_ACTION)) {
                NLService.iDIGEST_WEEKLY -> Period.WEEK
                NLService.iDIGEST_MONTHLY -> Period.ONE_MONTH
                else -> throw IllegalArgumentException("Unknown action")
            }
            GlobalScope.launch(coExceptionHandler) {
                fetchAndNotify(period, lastfmAccount)

                // yearly digest
                if (period == Period.ONE_MONTH && cal[Calendar.MONTH] == Calendar.DECEMBER) {
                    fetchAndNotify(Period.TWELVE_MONTHS, lastfmAccount)
                }
                jobFinished(jp, false)
            }

        } else {
            jobFinished(jp, false)
        }
        return true
    }

    private suspend fun fetchAndNotify(period: Period, lastfmAccount: UserAccountSerializable) {
        supervisorScope {
            val limit = 3
            val notificationTextList = mutableListOf<String>()
            val lastfmSession =
                Session.createSession(Stuff.LAST_KEY, Stuff.LAST_SECRET, prefs.lastfmSessKey)

            val lastfmUsername = lastfmAccount.user.name

            val artists = async {
                User.getTopArtists(lastfmUsername, period, limit, 1, lastfmSession)
            }
            val albums = async {
                User.getTopAlbums(lastfmUsername, period, limit, 1, lastfmSession)
            }
            val tracks = async {
                User.getTopTracks(lastfmUsername, period, limit, 1, lastfmSession)
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

                val title = getString(titleRes)
                val text = result.pageResults.joinToString { it.name }
                notificationTextList += "<b>$title:</b>\n$text"
            }

            val title = getString(
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

            var intent: Intent


            intent = Intent(applicationContext, MainActivity::class.java)
                .putExtra(Stuff.DIRECT_OPEN_KEY, Stuff.DL_CHARTS)
            val launchIntent = PendingIntent.getActivity(
                applicationContext, 10 + Period.values().size, intent,
                Stuff.updateCurrentOrImmutable
            )

            intent = Intent(applicationContext, MainDialogActivity::class.java)
                .putExtra(Stuff.ARG_TYPE, Stuff.TYPE_ALL)
                .putSingle(TimePeriod(this@DigestJob, period))
                .putSingle(lastfmAccount.user)
            val collageIntent = PendingIntent.getActivity(
                applicationContext, 100 + period.ordinal, intent,
                Stuff.updateCurrentOrImmutable
            )

            val nb = NotificationCompat.Builder(applicationContext, channelId)
                .apply {
                    color = (ColorPatchUtils.getNotiColor(applicationContext) ?: return@apply)
                }
                .setSmallIcon(R.drawable.vd_charts)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentTitle(title)
                .setContentIntent(launchIntent)
                .addAction(
                    Stuff.getNotificationAction(
                        R.drawable.vd_mosaic,
                        "🖼️",
                        getString(R.string.create_collage),
                        collageIntent
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
                    if (prefs.notiLockscreen)
                        NotificationCompat.VISIBILITY_PUBLIC
                    else
                        NotificationCompat.VISIBILITY_SECRET
                )

            nm.notify(channelId, period.ordinal, nb.build())
        }
    }

    override fun onStopJob(p0: JobParameters?) = true

    companion object {
        private const val JOB_ID_1 = 13
        private const val JOB_ID_2 = 14

        fun schedule(context: Context, actionString: String) {

            val id = when (actionString) {
                NLService.iDIGEST_WEEKLY -> JOB_ID_1
                NLService.iDIGEST_MONTHLY -> JOB_ID_2
                else -> return
            }

            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            JobInfo.Builder(id, ComponentName(context, DigestJob::class.java))
                .setExtras(
                    PersistableBundle().apply {
                        putString(Stuff.ARG_ACTION, actionString)
                    }
                )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .scheduleExpeditedCompat(js) {
                    setMinimumLatency(10 * 1000)
                    setOverrideDeadline(TimeUnit.DAYS.toMillis(1))
                }
            Stuff.log("scheduling ${DigestJob::class.java.simpleName}")
        }

        fun scheduleAlarms(context: Context) {
            val prefs = MainPrefs(context)

            if (prefs.digestSeconds == null)
                prefs.digestSeconds = (60..3600).random()

            val secondsToAdd = -(prefs.digestSeconds ?: 60)

            val weeklyIntent = PendingIntent.getBroadcast(
                context, 20,
                Intent(NLService.iDIGEST_WEEKLY, null, context, DigestReceiver::class.java),
                Stuff.updateCurrentOrImmutable
            )

            val monthlyIntent = PendingIntent.getBroadcast(
                context,
                21,
                Intent(NLService.iDIGEST_MONTHLY, null, context, DigestReceiver::class.java),
                Stuff.updateCurrentOrImmutable
            )

            val now = System.currentTimeMillis()

            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY

            cal.setMidnight()

            cal[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
            if (cal.timeInMillis < now)
                cal.add(Calendar.WEEK_OF_YEAR, 1)
            val nextWeek = cal.timeInMillis

            cal.timeInMillis = now
            cal.setMidnight()

            cal[Calendar.DAY_OF_MONTH] = 1
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
            if (cal.timeInMillis < now)
                cal.add(Calendar.MONTH, 1)
            val nextMonth = cal.timeInMillis

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC, nextWeek, weeklyIntent)
            alarmManager.set(AlarmManager.RTC, nextMonth, monthlyIntent)


            val dailyTestDigests = false
            if (BuildConfig.DEBUG && dailyTestDigests) {
                val dailyIntent = PendingIntent.getBroadcast(
                    context, 22,
                    Intent(NLService.iDIGEST_WEEKLY, null, context, DigestReceiver::class.java),
                    Stuff.updateCurrentOrImmutable
                )

                cal.timeInMillis = now
                cal.setMidnight()
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.add(Calendar.SECOND, secondsToAdd)
//                cal.add(Calendar.SECOND, 20)
                if (cal.timeInMillis < now)
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                val nextDay = cal.timeInMillis
                alarmManager.set(AlarmManager.RTC, nextDay, dailyIntent)
            }
        }

    }
}