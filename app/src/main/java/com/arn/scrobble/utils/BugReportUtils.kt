package com.arn.scrobble.utils

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LabeledIntent
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.arn.scrobble.App
import com.arn.scrobble.BuildConfig
import com.arn.scrobble.R
import com.arn.scrobble.ui.UiUtils.toast
import java.io.File

object BugReportUtils {

    fun mailLogs() {
        var bgRam = -1
        val manager = ContextCompat.getSystemService(App.context, ActivityManager::class.java)!!
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
            lastExitInfo = Stuff.getScrobblerExitReasons(printAll = true).firstOrNull()?.toString()
        }

        var text = ""
        text += App.context.getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME + "\n"
        text += "Android " + Build.VERSION.RELEASE + "\n"
        text += "Device: " + Build.BRAND + " " + Build.MODEL + " / " + Build.DEVICE + "\n" //Build.PRODUCT is obsolete

        val mi = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(mi)
        text += "Background RAM usage: " + bgRam + "M \n"

        if (!Stuff.isScrobblerRunning())
            text += "Background service isn't running\n"
        if (lastExitInfo != null)
            text += "Last exit reason: $lastExitInfo\n"

        text += if (App.prefs.proStatus)
            "~~~~~~~~~~~~~~~~~~~~~~~~"
        else
            "------------------------"
        text += "\n\n[Describe the issue]\n[If it is related to scrobbling, mention the media player name]\n"
        //keep the email in english

        val log = Stuff.exec("logcat -d")
        val logFile = File(App.context.cacheDir, "share/log.txt")
        logFile.parentFile!!.mkdirs()
        logFile.writeText(log)
        val logUri =
            FileProvider.getUriForFile(
                App.context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                logFile
            )

        val emailIntent = Intent(
            Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", "huh@huh.com", null
            )
        )
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "huh?")
        val resolveInfos = App.context.packageManager.queryIntentActivities(emailIntent, 0)
        val intents = arrayListOf<LabeledIntent>()
        for (info in resolveInfos) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                component = ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                putExtra(Intent.EXTRA_EMAIL, arrayOf(App.context.getString(R.string.email)))
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    App.context.getString(R.string.app_name) + " - Bug report"
                )
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_STREAM, logUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            intents.add(
                LabeledIntent(
                    intent,
                    info.activityInfo.packageName,
                    info.loadLabel(App.context.packageManager),
                    info.icon
                )
            )
        }
        if (intents.size > 0) {
            val chooser = Intent.createChooser(
                intents.removeAt(intents.size - 1),
                App.context.getString(R.string.bug_report)
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            }
            App.context.startActivity(chooser)
        } else
            App.context.toast(R.string.no_mail_apps)
    }
}