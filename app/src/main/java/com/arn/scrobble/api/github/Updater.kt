package com.arn.scrobble.api.github

import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable

class Updater {
    private val prefs = App.prefs

    suspend fun checkGithubForUpdates(): Flow<GithubReleases> {
        val now = System.currentTimeMillis()
        if (prefs.checkForUpdates != true
            || (now - (prefs.lastUpdateCheckTime ?: -1)) <= UPDATE_CHECK_INTERVAL
        )
            return emptyFlow()

        Requesters.genericKtorClient.getResult<GithubReleases>(githubApiUrl)
            .onSuccess { releases ->
                prefs.lastUpdateCheckTime = now

                if (releases.versionCode > BuildConfig.VERSION_CODE) {
                    return flowOf(releases)
                }
            }
            .onFailure { it.printStackTrace() }

        return emptyFlow()
    }
}

@Serializable
data class GithubReleases(
    val tag_name: String,
    val name: String,
    val body: String,
    val published_at: String,
    val html_url: String,
    val assets: List<GithubReleaseAsset>
) {
    val versionCode
        get() = Integer.parseInt(tag_name.replace(".", ""))

    val downloadUrl
        get() = assets.find { it.name.endsWith(".apk") }?.browser_download_url
}

@Serializable
data class GithubReleaseAsset(
    val name: String,
    val browser_download_url: String
)

const val githubApiUrl = "https://api.github.com/repos/kawaiiDango/pano-scrobbler/releases/latest"
const val UPDATE_CHECK_INTERVAL = 60 * 60 * 1000L // 1 hour
