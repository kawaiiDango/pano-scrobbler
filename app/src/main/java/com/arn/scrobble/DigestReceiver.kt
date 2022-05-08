package com.arn.scrobble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DigestReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let {
            DigestJob.schedule(context, it)
        }
    }

}