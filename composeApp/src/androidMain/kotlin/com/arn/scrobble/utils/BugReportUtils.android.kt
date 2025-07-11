package com.arn.scrobble.utils

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.ui.PanoSnackbarVisuals
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.email
import pano_scrobbler.composeapp.generated.resources.no_mail_apps
import java.io.File

actual object BugReportUtils {

    actual suspend fun mail() {
        var bgRam = -1
        val manager =
            ContextCompat.getSystemService(AndroidStuff.application, ActivityManager::class.java)!!
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
        text += BuildKonfig.APP_NAME + " v" + BuildKonfig.VER_NAME + " " + BuildKonfig.BUILD_DATE + "\n"
        text += "Android " + Build.VERSION.RELEASE + "\n"
        text += "Device: " + Build.BRAND + " " + Build.MODEL + " / " + Build.DEVICE + "\n" //Build.PRODUCT is obsolete

        val mi = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(mi)
        text += "Background RAM usage: " + bgRam + "M \n"

        if (!PlatformStuff.isScrobblerRunning())
            text += "Background service isn't running\n"
        if (lastExitInfo != null)
            text += "Last exit reason: $lastExitInfo\n"

        text += if (PlatformStuff.billingRepository.isLicenseValid)
            "~~~~~~~~~~~~~~~~~~~~~~~~"
        else
            "------------------------"
        text += "\n\n[Describe the issue]\n[If it is related to scrobbling, mention the media player name]\n"
        //keep the email in english

        val emailAddress = getString(Res.string.email)
        val sendTo = Intent(Intent.ACTION_SENDTO)
        val uriText = "mailto:" + Uri.encode(emailAddress) +
                "?subject=" + Uri.encode(BuildKonfig.APP_NAME + " - Bug report") +
                "&body=" + Uri.encode(text)
        val uri = uriText.toUri()
        sendTo.setData(uri)
        sendTo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            AndroidStuff.application.startActivity(sendTo)
        } catch (e: Exception) {
            e.printStackTrace()
            Stuff.globalSnackbarFlow.tryEmit(
                PanoSnackbarVisuals(
                    getString(Res.string.no_mail_apps),
                    isError = true
                )
            )
        }
    }

    actual fun saveLogsToFile(): String? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AndroidStuff.getScrobblerExitReasons().let {
                it.take(5).forEachIndexed { index, applicationExitInfo ->
                    Logger.w("${index + 1}. $applicationExitInfo", tag = "exitReasons")
                }
            }
        }

        val log = Stuff.exec("logcat -d *:I")
        val logFile = File(AndroidStuff.application.cacheDir, "share/pano-scrobbler.log")
        logFile.parentFile!!.mkdirs()
        logFile.writeText(log)
//        val logUri =
//            FileProvider.getUriForFile(
//                AndroidStuff.application,
//                "${BuildConfig.APPLICATION_ID}.fileprovider",
//                logFile
//            )

        return logFile.absolutePath
    }
}