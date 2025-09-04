package com.arn.scrobble.work

import co.touchlab.kermit.Logger

actual object PendingScrobblesWork : CommonWorkImpl(PendingScrobblesWorker.NAME) {
    const val RETRY_DELAY_HOURS = 1L

    override fun checkAndSchedule(force: Boolean) {
        val retryPolicy = RetryPolicy(
            initialDelayMillis = RETRY_DELAY_HOURS * 60 * 60 * 1000,
            backoffFactor = 4.0,
        )

        val state = state()
        if (state == CommonWorkState.RUNNING || !force && state == CommonWorkState.ENQUEUED) {
            Logger.w { "Not rescheduling $uniqueName" }
            return
        }

        if (force) {
            DesktopWorkManager.cancelWork(uniqueName)
        }

        DesktopWorkManager.scheduleWork(
            uniqueName,
            30 * 1000,
            { PendingScrobblesWorker(it) },
            retryPolicy,
        )
    }
}