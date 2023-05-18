package com.arn.scrobble.pending

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.job.JobScheduler
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arn.scrobble.MainActivity
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.arn.scrobble.pref.MainPrefs
import com.arn.scrobble.themes.ColorPatchUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob


class PendingScrService : Service() {

    private lateinit var nb: NotificationCompat.Builder
    private lateinit var nm: NotificationManager
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PendingScrJob.mightBeRunning) {
            mightBeRunning = true
            val js = ContextCompat.getSystemService(this, JobScheduler::class.java)!!
            js.cancel(PendingScrJob.JOB_ID)
            doTask()
        } else
            Handler(Looper.getMainLooper()).postDelayed({ stop() }, 200)
        return START_NOT_STICKY
    }

    private fun showNotification() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val launchIntent = PendingIntent.getActivity(
            applicationContext, 8, intent,
            Stuff.updateCurrentOrImmutable
        )
        nb = NotificationCompat.Builder(applicationContext, MainPrefs.CHANNEL_NOTI_PENDING)
            .setSmallIcon(R.drawable.vd_noti)
            .setPriority(Notification.PRIORITY_MIN)
            .setContentIntent(launchIntent)
            .apply { color = (ColorPatchUtils.getNotiColor(applicationContext) ?: return@apply) }
            .setContentTitle(getString(R.string.pending_scrobbles_noti))

        nm = ContextCompat.getSystemService(this, NotificationManager::class.java)!!

        startForeground(PendingScrJob.JOB_ID, nb.build())
    }

    private fun doTask() {
        job = SupervisorJob()
        PendingScrJob.PendingScrobbleTask(
            applicationContext,
            CoroutineScope(Dispatchers.IO + job!!),
            { str ->
                nb.setContentText(str)
                nm.notify(PendingScrJob.JOB_ID, nb.build())
            },
        ) { stop() }
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
        job?.cancel()
    }

    override fun onDestroy() {
        mightBeRunning = false
        super.onDestroy()
    }

    companion object {
        var mightBeRunning = false
    }
}