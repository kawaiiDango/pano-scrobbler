package com.arn.scrobble

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.arn.scrobble.widget.ChartsWidgetProvider
import com.arn.scrobble.widget.ChartsWidgetUpdaterWorker

class ReschedulerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in arrayOf(
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED
            )
        )
            reschedule(context)
    }

    companion object {
        fun reschedule(context: Context) {
            // widget updater
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, ChartsWidgetProvider::class.java)
            )

            if (appWidgetIds.isNotEmpty())
                ChartsWidgetUpdaterWorker.checkAndSchedule(context, false)

            DigestWorker.schedule(context)
        }
    }
}
