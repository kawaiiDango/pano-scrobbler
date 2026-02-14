package com.arn.scrobble.utils

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.logger.JavaUtilFileLogger
import com.arn.scrobble.ui.PanoSnackbarVisuals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

actual object BugReportUtils {

    actual fun mail() {
        var bgRam = -1
        val manager =
            AndroidStuff.applicationContext.getSystemService(ActivityManager::class.java)!!
        for (proc in manager.runningAppProcesses) {
            if (proc?.processName?.contains(Stuff.SCROBBLER_PROCESS_NAME) == true) {
                // https://stackoverflow.com/questions/2298208/how-do-i-discover-memory-usage-of-my-application-in-android
                val memInfo = manager.getProcessMemoryInfo(intArrayOf(proc.pid)).first()
                bgRam = memInfo.totalPss / 1024
                break
            }
        }

        var lastExitInfo: String? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lastExitInfo = AndroidStuff.getScrobblerExitReasons().firstOrNull()?.toString()
        }

        var text = ""
        text += BuildKonfig.APP_NAME + " v" + BuildKonfig.VER_NAME +
                (if (VariantStuff.billingRepository.needsActivationCode) " GH" else "") + "\n"
        text += "Android " + Build.VERSION.RELEASE + "\n"
        text += "Device: " + Build.BRAND + " " + Build.MODEL + " / " + Build.DEVICE + "\n" //Build.PRODUCT is obsolete

        val mi = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(mi)
        text += "Background RAM usage: " + bgRam + "M \n"

        if (!PlatformStuff.isNotificationListenerEnabled())
            text += "Notification Listener is not enabled\n"
        else if (!PlatformStuff.isScrobblerRunning())
            text += "Background service isn't running\n"
        if (lastExitInfo != null)
            text += "Last exit reason: $lastExitInfo\n"

        text += if (VariantStuff.billingRepository.licenseState.value == LicenseState.VALID)
            "~~~~~~~~~~~~~~~~~~~~~~~~"
        else
            "------------------------"
        text += "\n\n[Describe the issue]\n[If it is related to scrobbling, mention the media player name]\n"
        //keep the email in english

        val reportTo = Stuff.xorWithKey(
            Stuff.BUG_REPORT_TO,
            BuildKonfig.APP_ID
        )
        val subject = BuildKonfig.APP_NAME + " - Bug report"
        val uri = "mailto:".toUri() // this filters email-only apps
        val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = uri
            putExtra(Intent.EXTRA_EMAIL, arrayOf(reportTo))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            AndroidStuff.applicationContext.startActivity(sendToIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Stuff.globalSnackbarFlow.tryEmit(
                PanoSnackbarVisuals(
                    e.redactedMessage,
                    isError = true
                )
            )
        }
    }

    actual suspend fun saveLogsToFile(logFile: PlatformFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AndroidStuff.getScrobblerExitReasons().let {
                it.take(5).forEachIndexed { index, applicationExitInfo ->
                    Logger.w("${index + 1}. $applicationExitInfo", tag = "exitReasons")
                }
            }
        }

        val command = "logcat -d *:I"

        try {
            withContext(Dispatchers.IO) {
                val process = Runtime.getRuntime().exec(command)
                process.inputStream.use { input ->
                    logFile.overwrite { output ->
                        input.copyTo(output)
                    }
                }
            }

        } catch (e: IOException) {
            Logger.e(e) { "Failed to read logcat output" }
        }

        if (PlatformStuff.mainPrefs.data.map { it.logToFileOnAndroid }.first()) {
            logFile.writeAppend { output ->
                JavaUtilFileLogger.mergeLogFilesTo(output)
            }
        }
    }
}