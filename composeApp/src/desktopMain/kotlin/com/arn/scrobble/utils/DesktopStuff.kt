package com.arn.scrobble.utils

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import java.io.File

object DesktopStuff {
    enum class Os {
        WINDOWS, MACOS, LINUX
    }

    const val MINIMIZED_ARG = "--minimized"

    private val execPath by lazy { System.getProperty("jpackage.app-path")?.ifEmpty { null } }

    val resourcesPath by lazy {
        System.getProperty("compose.application.resources.dir")!!
    }

    val appDataRoot: String by lazy {
        when (os) {
            Os.WINDOWS -> {
                System.getenv("APPDATA")?.ifEmpty { null }
                    ?: System.getProperty("user.home")
            }

            Os.LINUX -> {
                System.getenv("XDG_DATA_HOME")?.ifEmpty { null }
                    ?: (System.getProperty("user.home") + "/.local/share")
            }

            Os.MACOS -> {
                System.getProperty("user.home") + "/Library/Application Support"
            }
        }
    }

    val iconPath: String by lazy {
        val ext = when (os) {
            Os.WINDOWS -> "ico"
            Os.LINUX -> "png"
            Os.MACOS -> "icns"
        }
        File(resourcesPath, "app_icon.$ext").absolutePath
    }

    val os by lazy {
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("windows")) {
            Os.WINDOWS
        } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
            Os.MACOS
        } else {
            Os.LINUX
        }
    }

    fun addOrRemoveFromStartup(add: Boolean) {
        val execPath = execPath ?: return
        when (os) {
            Os.WINDOWS -> {
                // windows. add or remove registry key in HKCU\Software\Microsoft\Windows\CurrentVersion\Run
                PanoNativeComponents.addRemoveStartupWin(execPath, add)
            }

            Os.LINUX -> {
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

            Os.MACOS -> {
                // todo: macos. add or remove launch agent in ~/Library/LaunchAgents
            }
        }
    }

    fun isAddedToStartup(): Boolean {
        when (os) {
            Os.WINDOWS -> {

                val execPath = execPath ?: return false
                return PanoNativeComponents.isAddedToStartupWin(execPath)
            }

            Os.LINUX -> {

                val desktopFile =
                    File(
                        System.getProperty("user.home"),
                        ".config/autostart/${BuildKonfig.APP_NAME}.desktop"
                    )
                return desktopFile.exists()
            }

            Os.MACOS -> {
                return false
            }
        }
    }
}