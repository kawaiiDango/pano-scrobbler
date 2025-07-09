package com.arn.scrobble.work

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.github.Github
import com.arn.scrobble.api.github.GithubReleases
import com.arn.scrobble.updates.doAfterUpdateCheck
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.done
import pano_scrobbler.composeapp.generated.resources.downloading
import pano_scrobbler.composeapp.generated.resources.loading
import pano_scrobbler.composeapp.generated.resources.no_updates
import pano_scrobbler.composeapp.generated.resources.update_available

class UpdaterWorker(
    override val setProgress: suspend (CommonWorkProgress) -> Unit
) : CommonWorker {
    override suspend fun doWork(): CommonWorkerResult {
        val mainPrefs = PlatformStuff.mainPrefs
        if (PlatformStuff.noUpdateCheck) {
            return CommonWorkerResult.Failure("Update check disabled")
        }

        setProgress(
            CommonWorkProgress(
                getString(Res.string.loading),
                0.1f
            )
        )

        val latestRelease: Result<GithubReleases> = Github.getLatestRelease(
            client = Requesters.genericKtorClient,
            json = Stuff.myJson,
        )

        latestRelease.onSuccess { release ->
            mainPrefs.updateData { it.copy(lastUpdateCheckTime = System.currentTimeMillis()) }

            if (release.versionCode > BuildKonfig.VER_CODE || MOCK) {
                val updatingMessage = if (PlatformStuff.isDesktop)
                    getString(Res.string.downloading)
                else
                    getString(Res.string.update_available, release.versionCode)

                setProgress(CommonWorkProgress(updatingMessage, 0.2f))

                val updateAction = doAfterUpdateCheck(release)

                if (updateAction != null) {
                    PanoNotifications.notifyUpdater(updateAction)
                    Stuff.globalUpdateAction.value = updateAction
                }

                setProgress(CommonWorkProgress(getString(Res.string.done), 1f))
            } else {
                setProgress(
                    CommonWorkProgress(
                        getString(Res.string.no_updates),
                        1f
                    )
                )
            }
        }.onFailure { e ->
            setProgress(CommonWorkProgress(e.redactedMessage, 1f))
        }
        
        delay(1000)

        UpdaterWork.checkAndSchedule(false)

        return if (latestRelease.isSuccess)
            CommonWorkerResult.Success
        else
            CommonWorkerResult.Failure(
                latestRelease.exceptionOrNull()?.redactedMessage ?: "Unknown error"
            )
    }

    companion object {
        const val NAME = "updater"
        private val MOCK = PlatformStuff.isDebug && false
    }
}