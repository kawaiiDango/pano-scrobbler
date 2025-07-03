package com.arn.scrobble.api.github

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

object Github {

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/kawaiiDango/pano-scrobbler/releases/latest"
    private const val UPDATE_CHECK_INTERVAL = 60 * 60 * 1000L // 1 hour

    suspend fun checkForUpdates(
        client: HttpClient,
        json: Json,
        currentVersionCode: Int,
        lastUpdateCheckTime: Long,
        setLastUpdateCheckTime: suspend (Long) -> Unit,
    ): GithubReleases? {
        val now = System.currentTimeMillis()
        if ((now - lastUpdateCheckTime) <= UPDATE_CHECK_INTERVAL)
            return null

        runCatching {
            client.get(GITHUB_API_URL).bodyAsText().let {
                json.decodeFromString<GithubReleases>(it)
            }
        }.onSuccess { releases ->
            setLastUpdateCheckTime(now)

            if (releases.versionCode > currentVersionCode) {
                return releases
            }
        }.onFailure {
            System.err.println("Failed to check for updates: ${it.message}")
        }

        return null
    }
}
