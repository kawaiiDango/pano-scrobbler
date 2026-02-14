package com.arn.scrobble.utils

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.work.DesktopWorkManager
import java.io.File
import java.net.URI
import java.net.URISyntaxException


object DesktopStuff {
    enum class Os {
        Windows, Macos, Linux
    }

    private lateinit var cmdlineArgs: CmdlineArgs

    private const val MINIMIZED_ARG = "minimized"

    private const val DATA_DIR_ARG = "data-dir"
    private const val NO_UPDATE_CHECK_ARG = "no-update-check"

    private val execDirPath =
        File(ProcessHandle.current().info().command().get()).parentFile.absolutePath

    val appDataRoot: File by lazy { getDataDir() }

    val webViewDir by lazy {
        File(appDataRoot, "webview").also { it.mkdirs() }
    }

    val os = when (BuildKonfig.OS_ORDINAL) {
        Os.Windows.ordinal -> Os.Windows
        Os.Macos.ordinal -> Os.Macos
        else -> Os.Linux
    }

    val noUpdateCheck: Boolean
        get() = os == Os.Linux && System.getenv("APPDIR").isNullOrEmpty() ||
                cmdlineArgs.noUpdateCheck

    fun parseCmdlineArgs(args: Array<String>): CmdlineArgs {
        var minimized = false
        var dataDir: String? = null
        var noUpdate = false
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

                "--$NO_UPDATE_CHECK_ARG" -> {
                    noUpdate = true
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
            noUpdateCheck = noUpdate,
            automationCommand = automationCommand,
            automationArg = automationArg
        ).also {
            cmdlineArgs = it
        }
    }

    fun linuxAutostartFile(): File? {
        return when (os) {
            Os.Linux -> {
                System.getenv("XDG_CONFIG_HOME")?.ifEmpty { null }?.let {
                    File(it, "autostart/pano-scrobbler.desktop")
                } ?: File(
                    System.getProperty("user.home"),
                    ".config/autostart/pano-scrobbler.desktop"
                )
            }

            else -> null
        }
    }

    fun linuxAutostartExec(): String? {
        return when (os) {
            Os.Linux -> {
                val execPath = if (System.getenv("APPIMAGE") != null)
                    System.getenv("APPIMAGE").let { "\"$it\"" }
                else
                    File(
                        ProcessHandle.current().info().command().get()
                    ).absolutePath.let { "\"$it\"" }

                "$execPath --$MINIMIZED_ARG"
            }

            else -> null
        }
    }

    fun addOrRemoveFromStartup(add: Boolean) {
        when (os) {
            Os.Windows -> {
                // not implemented. will not implement for windows
            }

            Os.Linux -> {
                // linux. create or delete .desktop file in ~/.config/autostart
                val execPath = linuxAutostartExec() ?: return

                val desktopFile = linuxAutostartFile() ?: return

                desktopFile.parentFile.mkdirs()

                if (add) {
                    desktopFile.writeText(
                        """
                        [Desktop Entry]
                        Type=Application
                        Name=${BuildKonfig.APP_NAME}
                        Comment=Feature packed music tracker
                        Terminal=false
                        Exec=$execPath
                        Icon=pano-scrobbler
                        X-GNOME-Autostart-enabled=true
                        StartupWMClass=pano-scrobbler
                        Categories=AudioVideo;Audio;
                        NoDisplay=false
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
                val execPath = linuxAutostartExec() ?: return false
                val desktopFile = linuxAutostartFile() ?: return false

                desktopFile.exists() && desktopFile.readText().contains(execPath)
            }

            Os.Macos -> {
                false
            }
        }
    }

    private fun getDataDir(): File {
        val defaultDir = when (os) {
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
        }

        val dirFile = cmdlineArgs.dataDir?.let { File(it) }
            ?: File(defaultDir, BuildKonfig.APP_NAME.lowercase().replace(' ', '-'))

        dirFile.mkdirs()

        return dirFile
    }

    fun setSystemProperties() {
        // for graalvm native image
        var prop = "java.home"
        if (System.getProperty(prop) == null) {
            System.setProperty(prop, execDirPath)

            prop = "compose.application.configure.swing.globals"
            if (System.getProperty(prop) == null)
                System.setProperty(prop, "true")

            prop = "sun.java2d.dpiaware"
            if (System.getProperty(prop) == null)
                System.setProperty(prop, "true")
        }

        // proxies
        prop = "java.net.useSystemProxies"
        if (System.getProperty(prop) == null)
            System.setProperty(prop, "true")

        // http_proxy and https_proxy are picked up by java.net.useSystemProxies on Linux
        val socksProxy = System.getenv("SOCKS_PROXY") ?: System.getenv("socks_proxy")

        try {
            if (!socksProxy.isNullOrEmpty()) {
                val uri = URI(socksProxy)
                System.setProperty("socksProxyHost", uri.host)
                System.setProperty("socksProxyPort", uri.port.toString())
            }
        } catch (e: URISyntaxException) {
            Logger.e(e) { "Error while setting up socks proxy from environment" }
        }
    }

    fun getLibraryPath(name: String): String {
        val libDir = System.getProperty("pano.native.components.path")
            ?.ifEmpty { null }
            ?.let { File(it).absolutePath }
            ?: execDirPath

        return when (os) {
            Os.Windows -> "$libDir\\$name.dll"
            Os.Linux -> "$libDir/lib$name.so"
            Os.Macos -> "$libDir/$name.dylib"
        }
    }

    fun normalizeAppId(appId: String): String {
        return when {
            os != Os.Linux ->
                appId

            // KDE Connect
            appId.startsWith(Stuff.PACKAGE_KDE_CONNECT_LINUX) ->
                Stuff.PACKAGE_KDE_CONNECT_LINUX

            // Chromium
            appId.split('.').last().startsWith("instance") ->
                appId.substringBeforeLast('.')

            else -> appId
        }
    }

    fun prepareToExit() {
        PanoNativeComponents.stopListeningMedia()
        DesktopWorkManager.clearAll()
        PanoDb.db.close()
    }
}