package com.arn.scrobble.utils

import com.arn.scrobble.main.ScrobblerState

expect object BugReportUtils {
    fun mail(scrobblerState: ScrobblerState)

    suspend fun saveLogsToFile(logFile: PlatformFile)
}