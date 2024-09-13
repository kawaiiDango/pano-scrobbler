package com.arn.scrobble.api.github

import com.arn.scrobble.BuildConfig
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

class Updater {

    suspend fun checkGithubForUpdates(): GithubReleases? {
        val lastUpdateCheckTime =
            PlatformStuff.mainPrefs.data.map { it.lastUpdateCheckTime }.first()
        val now = System.currentTimeMillis()
        if ((now - (lastUpdateCheckTime ?: -1)) <= UPDATE_CHECK_INTERVAL)
            return null

        Requesters.genericKtorClient.getResult<GithubReleases>(githubApiUrl)
            .onSuccess { releases ->
                PlatformStuff.mainPrefs.updateData { it.copy(lastUpdateCheckTime = now) }

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
