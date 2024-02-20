package com.arn.scrobble.ui

import com.arn.scrobble.api.github.GithubReleases
import com.google.android.material.snackbar.Snackbar

data class SnackbarData(
    val message: String,
    val actionText: String,
    val destinationId: Int,
    val updateData: GithubReleases? = null,
    val duration: Int = Snackbar.LENGTH_INDEFINITE,
)