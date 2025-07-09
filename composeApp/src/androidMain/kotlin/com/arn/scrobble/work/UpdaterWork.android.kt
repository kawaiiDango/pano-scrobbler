package com.arn.scrobble.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arn.scrobble.utils.AndroidStuff
import java.util.concurrent.TimeUnit

actual object UpdaterWork : CommonWorkImpl(UpdaterWorker.NAME) {
    override fun checkAndSchedule(force: Boolean) {
        val workManager = WorkManager.getInstance(AndroidStuff.application)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            PlatformWorker.WORK_NAME_KEY to name
        )

        val work = OneTimeWorkRequestBuilder<PlatformWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(name)
            .apply {
                if (force) {
//                    setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                } else {
                    setInitialDelay(
                        1,
                        TimeUnit.DAYS
                    )
                }
            }
            .build()

        workManager.enqueueUniqueWork(
            name,
            ExistingWorkPolicy.REPLACE,
            work
        )
    }
}