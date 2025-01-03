package com.arn.scrobble.work


actual object IndexerWork : CommonWorkImpl(IndexerWorker.NAME) {
    override fun checkAndSchedule(force: Boolean) {
        val retryPolicy = RetryPolicy(
            maxAttempts = 1,
        )

        val inputName = if (force) IndexerWorker.NAME_FULL_INDEX else IndexerWorker.NAME_DELTA_INDEX

        DesktopWorkManager.scheduleWork(
            inputName,
            30 * 1000,
            { IndexerWorker(force, it) },
            retryPolicy,
        )
    }
}