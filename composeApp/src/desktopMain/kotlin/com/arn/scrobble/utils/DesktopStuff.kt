package com.arn.scrobble.utils

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.automation.Automation
import java.io.File

object DesktopStuff {
    enum class Os {
        Windows, Macos, Linux
    }

    private lateinit var cmdlineArgs: CmdlineArgs

    private const val MINIMIZED_ARG = "minimized"

    private const val DATA_DIR_ARG = "data-dir"

    private val execDirPath =
        File(ProcessHandle.current().info().command().get()).parentFile.absolutePath

    val appDataRoot: String by lazy {
        cmdlineArgs.dataDir ?: getDefaultDataDir()
    }

    val webViewDir by lazy {
        File(appDataRoot, "webview")
    }

    val logsDir by lazy { File(appDataRoot, "logs") }

    val iconPath by lazy {
        when (os) {
            Os.Windows -> "$execDirPath\\pano-scrobbler.ico"
            Os.Linux -> System.getenv("APPDIR")?.let { "$it/pano-scrobbler.svg" }
            Os.Macos -> null
        }
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
        var automationCommand: String? = null
        var automationArg: String? = null

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

                "--${Automation.ENABLE}",
                "--${Automation.DISABLE}",
                "--${Automation.LOVE}",
                "--${Automation.UNLOVE}",
                "--${Automation.CANCEL}",
                "--${Automation.ALLOWLIST}",
                "--${Automation.BLOCKLIST}" -> {
                    automationCommand = args[index].removePrefix("--")
                    index++
                    if (index < args.size) {
                        automationArg = args[index]
                    }
                }

                else -> {
                    println("Unknown argument ignored: ${args[index]}")
                }
            }
            index++
        }

        return CmdlineArgs(
            minimized = minimized,
            dataDir = dataDir,
            automationCommand = automationCommand,
            automationArg = automationArg
        ).also {
            cmdlineArgs = it
        }
    }

    fun addOrRemoveFromStartup(add: Boolean) {
        when (os) {
            Os.Windows -> {
                // not implemented. will not implement for windows
            }

            Os.Linux -> {
                // linux. create or delete .desktop file in ~/.config/autostart

                val appImagePath = System.getenv("APPIMAGE") ?: return
                val desktopFile =
                    File(
                        System.getProperty("user.home"),
                        ".config/autostart/${BuildKonfig.APP_NAME}.desktop"
                    )

                desktopFile.parentFile.mkdirs()

                if (add) {
                    desktopFile.writeText(
                        """
                        [Desktop Entry]
                        Type=Application
                        Name=${BuildKonfig.APP_NAME}
                        Comment=Feature packed music tracker
                        Terminal=false
                        Exec="$appImagePath" --$MINIMIZED_ARG
                        X-GNOME-Autostart-enabled=true
                        StartupWMClass=pano-scrobbler
                        Categories=Utility;
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
        return when (os) {
            Os.Windows -> {
                // not implemented. will not implement for windows
                false
            }

            Os.Linux -> {
                val appImagePath = System.getenv("APPIMAGE") ?: return false
                val desktopFile =
                    File(
                        System.getProperty("user.home"),
                        ".config/autostart/${BuildKonfig.APP_NAME}.desktop"
                    )
                desktopFile.exists() && desktopFile.readText().contains(appImagePath)
            }

            Os.Macos -> {
                false
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

    fun setSystemPropertiesForGraalvm() {
        var prop = "java.home"
        if (System.getProperty(prop) == null) {
            System.setProperty(prop, execDirPath)

            prop = "compose.application.configure.swing.globals"
            if (System.getProperty(prop) == null)
                System.setProperty(prop, "true")

            prop = "java.library.path"
            if (System.getProperty(prop) == null)
                System.setProperty(prop, execDirPath)

            prop = "skiko.data.path"
            if (System.getProperty(prop) == null)
                System.setProperty(prop, File(PlatformStuff.cacheDir, "skiko").absolutePath)
        }

    }

}