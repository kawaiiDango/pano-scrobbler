package com.arn.scrobble.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arn.scrobble.utils.AndroidStuff
import java.util.concurrent.TimeUnit

actual object PendingScrobblesWork : CommonWorkImpl(PendingScrobblesWorker.NAME) {
    const val RETRY_DELAY_HOURS = 3L

    override fun checkAndSchedule(force: Boolean) {
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
                if (force) {
                    setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                } else {
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