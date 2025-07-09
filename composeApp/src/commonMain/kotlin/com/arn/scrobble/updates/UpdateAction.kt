package com.arn.scrobble.updates

import com.arn.scrobble.api.github.GithubReleases
import kotlinx.serialization.Serializable

@Serializable
data class UpdateAction(val urlOrFilePath: String, val version: String, val changelog: String)

expect fun runUpdateAction(
    updateAction: UpdateAction,
)

expect suspend fun doAfterUpdateCheck(
    releases: GithubReleases,
): UpdateAction?