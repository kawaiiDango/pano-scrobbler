package com.arn.scrobble.api.github

import kotlin.Result

object Github {

    // stub. Actual code to check for updates outside of Play Store should not exist here
    suspend fun getLatestRelease(
        client: Any,
        json: Any,
    ): Result<GithubReleases> {
        return Result.failure(IllegalStateException("Updater is unavailable"))
    }
}
