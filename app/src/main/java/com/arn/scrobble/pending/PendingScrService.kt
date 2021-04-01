package com.arn.scrobble.pending

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import com.arn.scrobble.Main
import com.arn.scrobble.NLService.Companion.NOTI_ID_FG
import com.arn.scrobble.R
import com.arn.scrobble.Stuff
import com.google.android.material.color.MaterialColors


class PendingScrService: Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!PendingScrJob.mightBeRunning) {
            mightBeRunning = true
            val js = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            js.cancel(PendingScrJob.JOB_ID)
            startForegroundService()
        } else
            stopSelf()
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val intent = Intent(applicationContext, Main::class.java)
        val launchIntent = PendingIntent.getActivity(applicationContext, 8, intent,
            Stuff.updateCurrentOrImmutable)
        val nb = NotificationCompat.Builder(applicationContext, NOTI_ID_FG)
                .setSmallIcon(R.drawable.vd_noti)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(launchIntent)
                .setColor(MaterialColors.getColor(
                        ContextThemeWrapper(applicationContext, R.style.ColorPatch_Pink),
                        R.attr.colorNoti, null))
                .setContentTitle(getString(R.string.pending_scrobbles_noti))

        startForeground(PendingScrJob.JOB_ID, nb.build())

        val ost = PendingScrJob.OfflineScrobbleTask(applicationContext)
        ost.progressCb = { str ->
            nb.setContentText(str)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(PendingScrJob.JOB_ID, nb.build())
        }
        ost.doneCb = { done -> stop()}
        ost.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        mightBeRunning = false
        super.onDestroy()
    }

    companion object {
        var mightBeRunning = false
    }
}