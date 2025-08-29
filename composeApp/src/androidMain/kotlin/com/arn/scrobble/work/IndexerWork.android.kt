package com.arn.scrobble.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arn.scrobble.utils.AndroidStuff

actual object IndexerWork : CommonWorkImpl(IndexerWorker.NAME) {
    override fun checkAndSchedule(force: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputName = if (force) IndexerWorker.NAME_FULL_INDEX else IndexerWorker.NAME_DELTA_INDEX

        val oneTimeWork = OneTimeWorkRequestBuilder<PlatformWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    PlatformWorker.WORK_NAME_KEY to inputName,
                )
            )
            .addTag(inputName)
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .build()

        WorkManager.getInstance(AndroidStuff.applicationContext)
            .enqueueUniqueWork(
                name,
                ExistingWorkPolicy.REPLACE,
                oneTimeWork
            )
    }
}