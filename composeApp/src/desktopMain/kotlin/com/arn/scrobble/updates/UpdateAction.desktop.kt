package com.arn.scrobble.updates

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.github.GithubReleases
import com.arn.scrobble.utils.DesktopStuff
import kotlin.system.exitProcess

actual suspend fun doAfterUpdateCheck(releases: GithubReleases): UpdateAction? {
    val updateFile = AutoUpdater.update(releases) ?: return null

    return UpdateAction(
        urlOrFilePath = updateFile.absolutePath,
        version = releases.tag_name.let {
            val verCode = it.toInt()
            "${verCode / 100}.${verCode % 100}"
        },
        changelog = releases.body,
    )
}

actual fun runUpdateAction(updateAction: UpdateAction) {
    DesktopStuff.prepareToExit()

    try {
        if (DesktopStuff.os == DesktopStuff.Os.Windows) {
            // there doesn't seem to be a way to pass arguments to the installer when launching with explorer.exe
            ProcessBuilder("explorer.exe", updateAction.urlOrFilePath)
                .start()
        } else if (DesktopStuff.os == DesktopStuff.Os.Linux) {
            val appDir = System.getenv("APPDIR")
            val relauncher = "$appDir/usr/bin/relaunch.sh"
            ProcessBuilder(relauncher, updateAction.urlOrFilePath)
                .inheritIO()
                .start()
        }
    } catch (e: Exception) {
        Logger.e(e) { "Failed to relaunch after update" }
    }

    exitProcess(0)
}