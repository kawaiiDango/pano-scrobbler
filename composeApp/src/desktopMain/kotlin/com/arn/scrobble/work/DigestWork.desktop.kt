package com.arn.scrobble.work

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking


actual object DigestWork : CommonWork {
    override fun checkAndSchedule(force: Boolean) {
        val scheduleTimes = runBlocking { DigestWorker.getScheduleTimes() }

        fun enqueue(digestType: DigestType) {
            val retryPolicy = RetryPolicy(
                maxAttempts = 1,
            )
            DesktopWorkManager.scheduleWork(
                digestType.name,
                scheduleTimes[digestType]!! - System.currentTimeMillis(),
                { DigestWorker(digestType, it) },
                retryPolicy,
            )
        }

        val dailyTestDigests = false
        if (BuildKonfig.DEBUG && dailyTestDigests)
            enqueue(DigestType.DIGEST_DAILY)
        enqueue(DigestType.DIGEST_WEEKLY)
        enqueue(DigestType.DIGEST_MONTHLY)

        Logger.i { "scheduling ${DigestWorker::class.java.simpleName}" }
    }

    override fun getProgress(): Flow<CommonWorkProgress> {
        throw NotImplementedError("Not implemented")
    }

    override fun state(): CommonWorkState? {
        throw NotImplementedError("Not implemented")
    }

    override fun cancel() {
        DesktopWorkManager.cancelWork(DigestType.DIGEST_DAILY.name)
        DesktopWorkManager.cancelWork(DigestType.DIGEST_WEEKLY.name)
        DesktopWorkManager.cancelWork(DigestType.DIGEST_MONTHLY.name)
    }
}