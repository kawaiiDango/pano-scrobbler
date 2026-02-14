package com.arn.scrobble.utils

expect object BugReportUtils {
    fun mail()

    suspend fun saveLogsToFile(logFile: PlatformFile)
}