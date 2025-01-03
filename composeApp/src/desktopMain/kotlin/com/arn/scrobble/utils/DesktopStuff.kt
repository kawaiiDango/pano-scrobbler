package com.arn.scrobble.utils

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import java.io.File

object DesktopStuff {
    const val MINIMIZED_ARG = "--minimized"

    private val execPath by lazy { System.getProperty("jpackage.app-path")?.ifEmpty { null } }

    private val resourcesPath by lazy {
        System.getProperty("compose.application.resources.dir")?.ifEmpty { null }
    }

    val iconPath by lazy {
        val resourcesPath = resourcesPath ?: return@lazy null
        val ext = if (isWindows()) "ico" else "png"
        File(resourcesPath, "app_icon.$ext").absolutePath
    }

    fun isWindows(): Boolean {
        return File.separatorChar == '\\'
    }

    fun addOrRemoveFromStartup(add: Boolean) {
        val execPath = execPath ?: return
        if (isWindows()) {
            // windows. add or remove registry key in HKCU\Software\Microsoft\Windows\CurrentVersion\Run
            PanoNativeComponents.addRemoveStartupWin(execPath, add)
        } else {
            // linux. create or delete .desktop file in ~/.config/autostart

            val desktopFile =
                File(
                    System.getProperty("user.home"),
                    ".config/autostart/${BuildKonfig.APP_NAME}.desktop"
                )
            if (add) {
                val iconPath = iconPath

                desktopFile.writeText(
                    """
                    [Desktop Entry]
                    Type=Application
                    Name=${BuildKonfig.APP_NAME}
                    Comment=${BuildKonfig.APP_NAME}
                    Icon=$iconPath
                    Exec=$execPath $MINIMIZED_ARG
                    Categories=Audio;Utility
                    """.trimIndent()
                )
            } else {
                desktopFile.delete()
            }
        }
    }

    fun isAddedToStartup(): Boolean {
        if (isWindows()) {
            // windows. check if registry key exists in HKCU\Software\Microsoft\Windows\CurrentVersion\Run

            val execPath = execPath ?: return false
            return PanoNativeComponents.isAddedToStartupWin(execPath)
        } else {
            // linux. check if .desktop file exists in ~/.config/autostart

            val desktopFile =
                File(
                    System.getProperty("user.home"),
                    ".config/autostart/${BuildKonfig.APP_NAME}.desktop"
                )
            return desktopFile.exists()
        }
    }
}