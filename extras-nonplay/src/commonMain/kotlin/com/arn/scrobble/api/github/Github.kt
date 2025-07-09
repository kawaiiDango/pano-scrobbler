package com.arn.scrobble.api.github

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

object Github {

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/kawaiiDango/pano-scrobbler/releases/latest"

    suspend fun getLatestRelease(
        client: HttpClient,
        json: Json,
    ): Result<GithubReleases> {
        return runCatching {
            client.get(GITHUB_API_URL).bodyAsText().let {
                json.decodeFromString<GithubReleases>(it)
            }
        }
    }
}
