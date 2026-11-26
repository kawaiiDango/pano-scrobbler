package com.arn.scrobble.work

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine


actual object DigestWork : CommonWork {
    actual fun schedule(
        weeklyDigestTime: Long,
        monthlyDigestTime: Long,
    ) {
        fun enqueue(digestType: DigestType, scheduleAt: Long) {
            val retryPolicy = RetryPolicy(
                maxAttempts = 1,
            )
            DesktopWorkManager.scheduleWork(
                digestType.name,
                scheduleAt - System.currentTimeMillis(),
                { DigestWorker(digestType, it) },
                retryPolicy,
            )
        }

        val dailyTestDigests = false
        if (BuildKonfig.DEBUG && dailyTestDigests)
            enqueue(DigestType.DIGEST_DAILY, System.currentTimeMillis() + 15_000L)
        enqueue(DigestType.DIGEST_WEEKLY, weeklyDigestTime)
        enqueue(DigestType.DIGEST_MONTHLY, monthlyDigestTime)

        Logger.i { "scheduling ${DigestWorker::class.java.simpleName}" }
    }

    override fun getProgress(): Flow<CommonWorkProgress> {
        throw NotImplementedError("Not implemented")
    }

    override fun state(): Flow<CommonWorkState?> {
        return combine(
            DesktopWorkManager.state(DigestType.DIGEST_MONTHLY.name),
            DesktopWorkManager.state(DigestType.DIGEST_WEEKLY.name)
        ) { weekly, monthly ->
            weekly ?: monthly
        }
    }

    override fun cancel() {
        DesktopWorkManager.cancelWork(DigestType.DIGEST_DAILY.name)
        DesktopWorkManager.cancelWork(DigestType.DIGEST_WEEKLY.name)
        DesktopWorkManager.cancelWork(DigestType.DIGEST_MONTHLY.name)
    }
}