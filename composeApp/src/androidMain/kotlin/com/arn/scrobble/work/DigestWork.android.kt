package com.arn.scrobble.work

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.utils.AndroidStuff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

actual object DigestWork : CommonWork {
    const val DIGEST_TAG = "DIGEST"
    const val DAILY_TEST_DIGESTS = false

    override fun checkAndSchedule(force: Boolean) {
        val workManager = try {
            WorkManager.getInstance(AndroidStuff.applicationContext)
        } catch (e: IllegalStateException) {
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()


        fun enqueue(digestType: DigestType, scheduleAt: Long) {
            val inputData = workDataOf(
                PlatformWorker.WORK_NAME_KEY to digestType.name
            )

            val work = OneTimeWorkRequestBuilder<PlatformWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .addTag(digestType.name)
                .addTag(DIGEST_TAG)
                .setInitialDelay(
                    scheduleAt - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS
                )
                .build()

            workManager.enqueueUniqueWork(digestType.name, ExistingWorkPolicy.REPLACE, work)
        }

        val scheduleTimes = runBlocking { DigestWorker.getScheduleTimes() }
        if (BuildKonfig.DEBUG && DAILY_TEST_DIGESTS)
            enqueue(DigestType.DIGEST_DAILY, System.currentTimeMillis() + 15_000L)
        enqueue(DigestType.DIGEST_WEEKLY, scheduleTimes[DigestType.DIGEST_WEEKLY]!!)
        enqueue(DigestType.DIGEST_MONTHLY, scheduleTimes[DigestType.DIGEST_MONTHLY]!!)

        Logger.i { "scheduling ${DigestWorker::class.java.simpleName}" }
    }

    override fun getProgress(): Flow<CommonWorkProgress> {
        throw NotImplementedError("Not implemented")
    }

    override fun state(): Flow<CommonWorkState?> {
        return WorkManager.getInstance(AndroidStuff.applicationContext)
            .getWorkInfosByTagFlow(DIGEST_TAG)
            .map {
                val expectedSize = if (BuildKonfig.DEBUG && DAILY_TEST_DIGESTS) 3 else 2
                if (it.size < expectedSize) {
                    null
                } else
                    it.minOfOrNull {
                        mapWorkState(it.state)
                    }
            }
    }

    override fun cancel() {
        WorkManager.getInstance(AndroidStuff.applicationContext).apply {
            cancelUniqueWork(DigestType.DIGEST_DAILY.name)
            cancelUniqueWork(DigestType.DIGEST_WEEKLY.name)
            cancelUniqueWork(DigestType.DIGEST_MONTHLY.name)
        }
    }
}