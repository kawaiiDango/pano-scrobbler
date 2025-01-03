package com.arn.scrobble.utils

expect object BugReportUtils {
    suspend fun mail()

    fun saveLogsToFile(): String?
}