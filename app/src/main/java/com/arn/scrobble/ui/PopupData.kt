package com.arn.scrobble.ui

import com.arn.scrobble.api.github.GithubReleases

data class PopupData(
    val message: String,
    val actionText: String,
    val destinationId: Int,
    val forceNavigate: Boolean,
    val updateData: GithubReleases? = null,
)