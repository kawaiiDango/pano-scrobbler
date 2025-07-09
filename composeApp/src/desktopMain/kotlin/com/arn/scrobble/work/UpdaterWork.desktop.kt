package com.arn.scrobble.work

import kotlin.time.Duration.Companion.hours

actual object UpdaterWork : CommonWorkImpl(UpdaterWorker.NAME) {

    override fun checkAndSchedule(force: Boolean) {

        if (force)
            DesktopWorkManager.cancelWork(uniqueName)

        DesktopWorkManager.scheduleWork(
            uniqueName,
            if (force)
                0
            else
                12.hours.inWholeMilliseconds,
            { UpdaterWorker(it) },
        )
    }
}