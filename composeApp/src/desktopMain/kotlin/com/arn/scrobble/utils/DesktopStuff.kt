package com.arn.scrobble.utils

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import java.io.File

object DesktopStuff {
    enum class Os {
        Windows, Macos, Linux
    }

    private lateinit var cmdlineArgs: CmdlineArgs

    private const val MINIMIZED_ARG = "minimized"

    private const val DATA_DIR_ARG = "data-dir"

    private val execPath by lazy {
        System.getenv("APPIMAGE")
            ?.ifEmpty { null }
            ?: System.getProperty("jpackage.app-path")
                ?.ifEmpty { null }
    }

    val resourcesPath by lazy {
        System.getProperty("compose.application.resources.dir")!!
    }

    val appDataRoot: String by lazy {
        cmdlineArgs.dataDir ?: getDefaultDataDir()
    }

    val iconPath: String by lazy {
        val ext = when (os) {
            Os.Windows -> "ico"
            Os.Linux -> "png"
            Os.Macos -> "icns"
        }
        File(resourcesPath, "app_icon.$ext").absolutePath
    }

    val os by lazy {
        val osName = System.getProperty("os.name").lowercase()
        if (osName.contains("windows")) {
            Os.Windows
        } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
            Os.Macos
        } else {
            Os.Linux
        }
    }

    fun parseCmdlineArgs(args: Array<String>): CmdlineArgs {
        var minimized = false
        var dataDir: String? = null

        var index = 0
        while (index < args.size) {
            when (args[index]) {
                "-${MINIMIZED_ARG[0]}", "--$MINIMIZED_ARG" -> {
                    minimized = true
                }

                "-${DATA_DIR_ARG[0]}", "--$DATA_DIR_ARG" -> {
                    index++
                    if (index < args.size) {
                        dataDir = args[index]
                    } else {
                        throw IllegalArgumentException("Missing value for argument: ${args[index - 1]}")
                    }
                }

                else -> {
                    println("Unknown argument ignored: ${args[index]}")
                }
            }
            index++
        }

        return CmdlineArgs(minimized, dataDir).also {
            cmdlineArgs = it
        }
    }

    fun addOrRemoveFromStartup(add: Boolean) {
        val execPath = execPath ?: return
        when (os) {
            Os.Windows -> {
                // windows. add or remove registry key in HKCU\Software\Microsoft\Windows\CurrentVersion\Run
                PanoNativeComponents.addRemoveStartupWin(execPath, add)
            }

            Os.Linux -> {
                // linux. create or delete .desktop file in ~/.config/autostart

                val desktopFile =
                    File(
                        System.getProperty("user.home"),
                        ".config/autostart/${BuildKonfig.APP_NAME}.desktop"
                    )
                if (add) {
                    desktopFile.writeText(
                        """
                        [Desktop Entry]
                        Type=Application
                        Name=${BuildKonfig.APP_NAME}
                        Comment=${BuildKonfig.APP_NAME}
                        Exec="$execPath" --$MINIMIZED_ARG
                        X-GNOME-Autostart-enabled=true
                        Categories=Utility
                        """.trimIndent()
                    )
                } else {
                    desktopFile.delete()
                }
            }

            Os.Macos -> {
                // will not implement for macos
            }
        }
    }

    fun isAddedToStartup(): Boolean {
        when (os) {
            Os.Windows -> {

                val execPath = execPath ?: return false
                return PanoNativeComponents.isAddedToStartupWin(execPath)
            }

            Os.Linux -> {

                val desktopFile =
                    File(
                        System.getProperty("user.home"),
                        ".config/autostart/${BuildKonfig.APP_NAME}.desktop"
                    )
                return desktopFile.exists() && desktopFile.readText().contains(execPath ?: "")
            }

            Os.Macos -> {
                return false
            }
        }
    }

    private fun getDefaultDataDir(): String {
        return when (os) {
            Os.Windows -> {
                System.getenv("APPDATA")?.ifEmpty { null }
                    ?: System.getProperty("user.home")
            }

            Os.Linux -> {
                System.getenv("XDG_DATA_HOME")?.ifEmpty { null }
                    ?: (System.getProperty("user.home") + "/.local/share")
            }

            Os.Macos -> {
                System.getProperty("user.home") + "/Library/Application Support"
            }
        }.let {
            File(it, BuildKonfig.APP_NAME.lowercase().replace(' ', '-')).apply {
                if (!exists()) {
                    mkdirs()
                }
            }.absolutePath
        }
    }

}