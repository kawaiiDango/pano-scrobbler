package com.arn.scrobble.utils

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.logger.JavaUtilFileLogger
import io.ktor.http.encodeURLPathPart
import io.ktor.http.encodeURLQueryComponent


actual object BugReportUtils {
    actual fun mail() {
        val reportTo = Stuff.xorWithKey(
            Stuff.BUG_REPORT_TO,
            BuildKonfig.APP_ID
        )
            .encodeURLPathPart()
        val subject = (BuildKonfig.APP_NAME + " - Bug report").encodeURLQueryComponent()
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

        // the ram values don't correspond to that shown in task manager (are too low)
//        text += "RAM usage: $freeMemory M / $totalMemory M\n"

        text += if (VariantStuff.billingRepository.licenseState.value == LicenseState.VALID)
            "~~~~~~~~~~~~~~~~~~~~~~~~"
        else
            "------------------------"
        text += "\n\n[Describe the issue]\n[If it is related to scrobbling, mention the media player name]\n"

        text = text.encodeURLQueryComponent()
        PlatformStuff.openInBrowser("mailto:$reportTo?subject=$subject&body=$text")
    }

    actual suspend fun saveLogsToFile(logFile: PlatformFile) {
        logFile.writeAppend { output ->
            JavaUtilFileLogger.mergeLogFilesTo(output)
        }
    }

}