package com.arn.scrobble.api.github

object Github {

    // stub. Actual code to check for updates outside of Play Store should not exist here
    suspend fun checkForUpdates(
        client: Any,
        json: Any,
        currentVersionCode: Int,
        lastUpdateCheckTime: Long?,
        setLastUpdateCheckTime: suspend (Long) -> Unit,
    ): GithubReleases? {
        return null
    }
}
