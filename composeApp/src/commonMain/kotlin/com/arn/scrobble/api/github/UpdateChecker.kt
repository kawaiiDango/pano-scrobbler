package com.arn.scrobble.api.github

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.serialization.Serializable

object UpdateChecker {

    suspend fun checkGithubForUpdates(
        lastUpdateCheckTime: Long?,
        setLastUpdateCheckTime: suspend (Long) -> Unit,
    ): GithubReleases? {
        val now = System.currentTimeMillis()
        if ((now - (lastUpdateCheckTime ?: -1)) <= UPDATE_CHECK_INTERVAL)
            return null

        Requesters.genericKtorClient.getResult<GithubReleases>(githubApiUrl)
            .onSuccess { releases ->
                setLastUpdateCheckTime(now)

                if (releases.versionCode > BuildKonfig.VER_CODE) {
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
    val assets: List<GithubReleaseAsset>,
) {
    val versionCode
        get() = tag_name.replace(".", "").toInt()

    val downloadUrl
        get() = if (PlatformStuff.isDesktop)
            html_url
        else
            assets.find { it.name.endsWith(".apk") }?.browser_download_url
}

@Serializable
data class GithubReleaseAsset(
    val name: String,
    val browser_download_url: String,
)

const val githubApiUrl = "https://api.github.com/repos/kawaiiDango/pano-scrobbler/releases/latest"
const val UPDATE_CHECK_INTERVAL = 60 * 60 * 1000L // 1 hour
