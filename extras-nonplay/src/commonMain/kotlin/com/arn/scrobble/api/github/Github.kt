package com.arn.scrobble.api.github

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType

object Github {

    private const val githubApiUrl =
        "https://api.github.com/repos/kawaiiDango/pano-scrobbler/releases/latest"
    private const val UPDATE_CHECK_INTERVAL = 60 * 60 * 1000L // 1 hour

    suspend fun checkForUpdates(
        client: HttpClient,
        currentVersionCode: Int,
        lastUpdateCheckTime: Long,
        setLastUpdateCheckTime: suspend (Long) -> Unit,
    ): GithubReleases? {
        val now = System.currentTimeMillis()
        if ((now - lastUpdateCheckTime) <= UPDATE_CHECK_INTERVAL)
            return null

        runCatching {
            client.get(githubApiUrl) {
                contentType(ContentType.Application.Json)
            }
                .body<GithubReleases>()
        }.onSuccess { releases ->
            setLastUpdateCheckTime(now)

            if (releases.versionCode > currentVersionCode) {
                return releases
            }
        }.onFailure { it.printStackTrace() }

        return null
    }
}
