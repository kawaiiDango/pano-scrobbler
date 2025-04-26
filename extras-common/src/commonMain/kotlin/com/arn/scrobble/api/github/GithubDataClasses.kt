package com.arn.scrobble.api.github

import kotlinx.serialization.Serializable

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

    fun getDownloadUrl(platformSubstring: String): List<GithubReleaseAsset> =
        assets.filter { it.name.contains(platformSubstring) }
}

@Serializable
data class GithubReleaseAsset(
    val name: String,
    val browser_download_url: String,
)