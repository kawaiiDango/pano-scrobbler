package com.arn.scrobble.api.github

import com.arn.scrobble.main.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import kotlinx.serialization.Serializable

class Updater {
    private val prefs = App.prefs

    suspend fun checkGithubForUpdates(): GithubReleases? {
        val now = System.currentTimeMillis()
        if ((now - (prefs.lastUpdateCheckTime ?: -1)) <= UPDATE_CHECK_INTERVAL)
            return null

        Requesters.genericKtorClient.getResult<GithubReleases>(githubApiUrl)
            .onSuccess { releases ->
                prefs.lastUpdateCheckTime = now

                if (releases.versionCode > BuildConfig.VERSION_CODE) {
                    return releases
                }
            }
            .onFailure { it.printStackTrace() }

        return null
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
        get() = tag_name.replace(".", "").toInt()

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
