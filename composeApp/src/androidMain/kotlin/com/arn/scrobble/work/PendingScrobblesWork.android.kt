package com.arn.scrobble.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arn.scrobble.utils.AndroidStuff
import java.util.concurrent.TimeUnit

actual object PendingScrobblesWork : CommonWorkImpl(PendingScrobblesWorker.NAME) {
    private const val RETRY_DELAY_HOURS = 1L

    actual fun schedule(force: Boolean) {
        if (!AndroidStuff.isMainProcess) {
            // todo find a better way to schedule from other processes
            if (force) {
                WorkEnqueuerReceiver.broadcast(
                    AndroidStuff.applicationContext,
                    name,
                    force
                )
            }

            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PlatformWorker>()
            .setInputData(
                workDataOf(
                    PlatformWorker.WORK_NAME_KEY to name
                )
            )
            .addTag(name)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                RETRY_DELAY_HOURS,
                TimeUnit.HOURS
            )
            .apply {
                // works fine in fg without expedited
                if (!force) {
                    setInitialDelay(30, TimeUnit.SECONDS)
                }
            }
            .build()

        WorkManager.getInstance(AndroidStuff.applicationContext)
            .enqueueUniqueWork(
                name,
                if (force)
                    ExistingWorkPolicy.REPLACE
                else
                    ExistingWorkPolicy.KEEP,
                workRequest
            )
    }
}