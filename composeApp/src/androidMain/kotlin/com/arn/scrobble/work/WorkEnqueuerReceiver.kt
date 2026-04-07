package com.arn.scrobble.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WorkEnqueuerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val name = intent.getStringExtra(NAME_KEY) ?: return
        val force = intent.getBooleanExtra(FORCE_KEY, false)

        val pendingResult = goAsync()

        when (name) {
            PendingScrobblesWorker.NAME -> PendingScrobblesWork.schedule(force)
        }

        pendingResult.finish()
    }

    companion object {
        private const val NAME_KEY = "workName"
        private const val FORCE_KEY = "force"

        fun broadcast(context: Context, workName: String, force: Boolean) {
            val intent = Intent(context, WorkEnqueuerReceiver::class.java)
                .putExtra(NAME_KEY, workName)
                .putExtra(FORCE_KEY, force)

            context.sendBroadcast(intent)
        }
    }
}
