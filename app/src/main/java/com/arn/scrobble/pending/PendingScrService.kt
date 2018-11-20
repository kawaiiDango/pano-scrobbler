package com.arn.scrobble.pending

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.arn.scrobble.Main
import com.arn.scrobble.NLService.Companion.NOTI_ID_FG
import com.arn.scrobble.R


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
        }
        return Service.START_NOT_STICKY
    }

    private fun startForegroundService() {
        val intent = Intent(applicationContext, Main::class.java)
        val launchIntent = PendingIntent.getActivity(applicationContext, 8, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
        val nb = NotificationCompat.Builder(applicationContext, NOTI_ID_FG)
                .setSmallIcon(R.drawable.ic_noti)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(launchIntent)
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorNoti))
                .setContentTitle(getString(R.string.pending_scrobbles_noti))

        startForeground(PendingScrJob.JOB_ID, nb.build())

        val ost = PendingScrJob.OfflineScrobbleTask(applicationContext)
        ost.progressCb = { str ->
            nb.setContentText(str)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(PendingScrJob.JOB_ID, nb.build())
        }
        ost.doneCb = { done -> stop()}
        ost.execute()

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