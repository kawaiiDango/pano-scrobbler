package com.arn.scrobble.work

actual object PendingScrobblesWork : CommonWorkImpl(PendingScrobblesWorker.NAME) {
    const val RETRY_DELAY_HOURS = 1L

    actual fun schedule(force: Boolean) {
        val retryPolicy = RetryPolicy(
            initialDelayMillis = RETRY_DELAY_HOURS * 60 * 60 * 1000,
            backoffFactor = 4.0,
        )

        if (force) {
            DesktopWorkManager.cancelWork(uniqueName)
        }

        DesktopWorkManager.scheduleWork(
            uniqueName,
            30 * 1000,
            { PendingScrobblesWorker(it) },
            retryPolicy,
            replace = force,
        )
    }
}