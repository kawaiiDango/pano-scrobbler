package com.arn.scrobble.work

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull


abstract class CommonWorkImpl(protected val uniqueName: String) : CommonWork {
    final override fun getProgress(): Flow<CommonWorkProgress> {
        return DesktopWorkManager.getProgress(uniqueName)
            .filterNotNull()
    }

    final override fun state(): CommonWorkState? {
        return DesktopWorkManager.state(uniqueName)
    }

    final override fun cancel() {
        DesktopWorkManager.cancelWork(uniqueName)
    }
}