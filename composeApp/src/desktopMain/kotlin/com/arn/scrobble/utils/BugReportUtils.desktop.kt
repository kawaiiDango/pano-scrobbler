package com.arn.scrobble.utils

import com.arn.scrobble.BuildKonfig
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.bug_report
import pano_scrobbler.composeapp.generated.resources.email
import java.io.File
import java.net.URLEncoder

actual object BugReportUtils {
    actual suspend fun mail() {
        val emailAddress = URLEncoder.encode(getString(Res.string.email), Charsets.UTF_8)
        val subject = URLEncoder.encode(getString(Res.string.bug_report), Charsets.UTF_8)
//        val runtime = Runtime.getRuntime()
//        val totalMemory = runtime.totalMemory() / (1024 * 1024)
//        val freeMemory = runtime.freeMemory() / (1024 * 1024)


        val osName = System.getProperty("os.name")
        val osVersion = System.getProperty("os.version")
        val arch = System.getProperty("os.arch")

        var text = ""
        text += BuildKonfig.APP_NAME + " v" + BuildKonfig.VER_NAME + "\n"
        text += "$osName $osVersion\n"
        text += "Arch: $arch\n"

        // todo the values don't correspond to that shown in task manager (are too low)
//        text += "RAM usage: $freeMemory M / $totalMemory M\n"

        text += if (PlatformStuff.billingRepository.isLicenseValid)
            "~~~~~~~~~~~~~~~~~~~~~~~~"
        else
            "------------------------"
        text += "\n\n[Describe the issue]\n[If it is related to scrobbling, mention the media player name]\n"

        text = URLEncoder.encode(text, Charsets.UTF_8)
        PlatformStuff.openInBrowser(
            "mailto:${emailAddress}?subject=$subject&body=$text"
        )
    }

    actual fun saveLogsToFile(): String? {
        val logsDir = File(PlatformStuff.filesDir, "logs")

        if (!logsDir.exists()) {
            return null
        }

        // get the most recent log file
        // return null if there are no log files

        val logFiles = logsDir.listFiles { file ->
            file.isFile && file.name.endsWith(".log")
        } ?: emptyArray()

        val logFile = logFiles.maxByOrNull { it.lastModified() } ?: return null

        return logFile.absolutePath
    }

}