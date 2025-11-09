package com.arn.scrobble.api.github

import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult

object Github {

    suspend fun getLatestRelease(
        apiUrl: String,
    ) = Requesters.genericKtorClient.getResult<GithubReleases>(apiUrl)
}
