package com.arn.scrobble.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff

class ReschedulerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in arrayOf(
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
        )
            reschedule(context)
    }

    private fun reschedule(context: Context) {
        // a BroadcastReceiver is initialized before the Application class's onCreate() method is called
        AndroidStuff.applicationContext = context.applicationContext

        if (PlatformStuff.isTv)
            return

        DigestWork.reschedule()
    }
}