package com.arn.scrobble.work


expect object DigestWork : CommonWork {
    fun schedule(
        weeklyDigestTime: Long,
        monthlyDigestTime: Long,
    )
}