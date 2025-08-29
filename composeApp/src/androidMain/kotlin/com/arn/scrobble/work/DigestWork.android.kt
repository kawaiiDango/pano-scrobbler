package com.arn.scrobble.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

actual object DigestWork : CommonWork {
    override fun checkAndSchedule(force: Boolean) {
        val workManager = WorkManager.getInstance(AndroidStuff.applicationContext)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val scheduleTimes = runBlocking { DigestWorker.getScheduleTimes() }

        fun enqueue(digestType: DigestType) {
            val inputData = workDataOf(
                PlatformWorker.WORK_NAME_KEY to digestType.name
            )

            val work = OneTimeWorkRequestBuilder<PlatformWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(digestType.name)
                .setInitialDelay(
                    scheduleTimes[digestType]!! - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS
                )
                .build()

            workManager.enqueueUniqueWork(digestType.name, ExistingWorkPolicy.REPLACE, work)
        }

        val dailyTestDigests = false
        if (PlatformStuff.isDebug && dailyTestDigests)
            enqueue(DigestType.DIGEST_DAILY)
        enqueue(DigestType.DIGEST_WEEKLY)
        enqueue(DigestType.DIGEST_MONTHLY)

        Logger.i { "scheduling ${DigestWorker::class.java.simpleName}" }
    }

    override fun getProgress(): Flow<CommonWorkProgress> {
        throw NotImplementedError("Not implemented")
    }

    override fun cancel() {
        WorkManager.getInstance(AndroidStuff.applicationContext).apply {
            cancelUniqueWork(DigestType.DIGEST_DAILY.name)
            cancelUniqueWork(DigestType.DIGEST_WEEKLY.name)
            cancelUniqueWork(DigestType.DIGEST_MONTHLY.name)
        }
    }
}