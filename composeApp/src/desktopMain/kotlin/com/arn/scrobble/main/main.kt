package com.arn.scrobble.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import coil3.SingletonImageLoader
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.logger.JvmLogger
import com.arn.scrobble.media.PlayingTrackNotificationState
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PanoTrayUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.toImageBitmap
import com.arn.scrobble.utils.setAppLocale
import com.arn.scrobble.webview.web.CustomURLStreamHandlerFactory
import com.arn.scrobble.work.DesktopWorkManager
import javafx.application.Platform
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.copy
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.fix_it_action
import pano_scrobbler.composeapp.generated.resources.ic_launcher_with_bg
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.quit
import pano_scrobbler.composeapp.generated.resources.unlove
import pano_scrobbler.composeapp.generated.resources.vd_noti
import pano_scrobbler.composeapp.generated.resources.vd_noti_err
import pano_scrobbler.composeapp.generated.resources.vd_noti_persistent
import java.awt.image.BufferedImage
import java.net.URL
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private fun init(lockName: String) {
    // init: run once

    PanoNativeComponents.load()
    preventMultipleInstances(lockName)

    URL.setURLStreamHandlerFactory(CustomURLStreamHandlerFactory())

    Stuff.mainPrefsInitialValue = runBlocking { PlatformStuff.mainPrefs.data.first() }

    Logger.setLogWriters(
        JvmLogger(
            logToFile = true,
            redirectStderr = true
        )
    )
    Logger.setTag("scrobbler")
    Logger.setMinSeverity(
        if (PlatformStuff.isDebug) Severity.Debug else Severity.Info
    )

    setAppLocale(Stuff.mainPrefsInitialValue.locale, force = false)

    PanoNativeComponents.load()
    if (DesktopStuff.os == DesktopStuff.Os.Linux) {
        // fix for javafx on linux
        PanoNativeComponents.setEnvironmentVariable("GDK_BACKEND", "x11")
    }
    PanoNativeComponents.init()
//    test()
}

private fun preventMultipleInstances(uniqueId: String) {
    val isSingleInstance = PanoNativeComponents.isSingleInstance(uniqueId)

    if (!isSingleInstance) {
        PanoNativeComponents.notify(
            "Already running",
            "Please close the existing instance before starting a new one."
        )
        exitProcess(1)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val cmdlineArgs = DesktopStuff.parseCmdlineArgs(args)
    val lockName = "pano-scrobbler-" + (cmdlineArgs.dataDir ?: "default-data-dir").hashCode()
        .toString() + ".lock"
    init(lockName)

    return application {
        var windowShown by remember { mutableStateOf(!cmdlineArgs.minimized) }
        val windowState = rememberWindowState()
        val isSystemInDarkTheme = isSystemInDarkTheme()
        var trayData by remember { mutableStateOf<PanoTrayUtils.TrayData?>(null) }
        val trayIconNotPlaying = painterResource(Res.drawable.vd_noti_persistent)
        val trayIconPlaying = painterResource(Res.drawable.vd_noti)
        val trayIconError = painterResource(Res.drawable.vd_noti_err)
        val appIdToNames by PlatformStuff.mainPrefs.data
            .map { it.seenApps.associate { it.appId to it.friendlyLabel } }
            .collectAsState(emptyMap())
        val windowOpenTrigger = remember { MutableSharedFlow<Unit>() }

        fun onExit() {
            DesktopWorkManager.clearAll()
            PanoDb.destroyInstance()
            exitApplication()
        }

        LaunchedEffect(Unit) {
            PanoNotifications.playingTrackTrayInfo.mapLatest {
                val trayIconPainter = when {
                    it.isEmpty() -> trayIconNotPlaying
                    it.values.any { it is PlayingTrackNotificationState.Error } -> trayIconError
                    else -> trayIconPlaying
                }

                val tooltip = it.entries.firstOrNull {
                    val it = it.value
                    it is PlayingTrackNotificationState.Scrobbling
                }?.let { (appId, it) ->
                    it.trackInfo.title + "\n" +
                            it.trackInfo.artist + "\n" +
                            (appIdToNames[appId] ?: appId)
                }
                    ?: BuildKonfig.APP_NAME


                val trayItems = mutableListOf<Pair<String, String>>()

                // tracks

                it.forEach { (appId, playingTrackState) ->
                    when (playingTrackState) {
                        is PlayingTrackNotificationState.Scrobbling -> {
                            val trackInfo = playingTrackState.trackInfo
                            val nowPlaying = playingTrackState.nowPlaying

                            val playingState =
                                if (nowPlaying)
                                    ""
                                else
                                    "[✓] "

                            val lovedString =
                                if (trackInfo.userLoved)
                                    "❤️ " + getString(Res.string.unlove)
                                else
                                    "🤍 " + getString(Res.string.love)

                            trayItems += PanoTrayUtils.ItemId.TrackName.withSuffix(appId) to
                                    playingState + trackInfo.title
                            trayItems += PanoTrayUtils.ItemId.ArtistName.withSuffix(appId) to
                                    trackInfo.artist

                            trayItems += PanoTrayUtils.ItemId.Separator.name to ""

                            trayItems += PanoTrayUtils.ItemId.Love.withSuffix(appId) to
                                    lovedString
                            trayItems += PanoTrayUtils.ItemId.Edit.withSuffix(appId) to
                                    "✏️ " +
                                    getString(Res.string.edit)
                            trayItems += PanoTrayUtils.ItemId.Cancel.withSuffix(appId) to
                                    "❌ " +
                                    getString(Res.string.cancel)
                            trayItems += PanoTrayUtils.ItemId.Block.withSuffix(appId) to
                                    "⛔ " +
                                    getString(Res.string.block)
                            trayItems += PanoTrayUtils.ItemId.Copy.withSuffix(appId) to
                                    "📋 " +
                                    getString(Res.string.copy)
                        }

                        is PlayingTrackNotificationState.Error -> {
                            val scrobbleError = playingTrackState.scrobbleError

                            trayItems += PanoTrayUtils.ItemId.Error.withSuffix(appId) to
                                    scrobbleError.title
                        }
                    }

                    trayItems += PanoTrayUtils.ItemId.Separator.name to ""
                }

                // always show these

                trayItems += PanoTrayUtils.ItemId.Open.name to getString(Res.string.fix_it_action)
                trayItems += PanoTrayUtils.ItemId.Exit.name to getString(Res.string.quit)

                Triple(tooltip, trayIconPainter, trayItems)
            }
                .distinctUntilChanged()
                .collectLatest { (tooltip, trayIconPainter, trayItems) ->
                    val iconSize = 64

                    val bmp = trayIconPainter.toImageBitmap(
                        darkTint = !isSystemInDarkTheme,
                        size = Size(
                            iconSize.toFloat(),
                            iconSize.toFloat()
                        )
                    )
                    val argb = IntArray(bmp.width * bmp.height)
                    bmp.readPixels(argb)

                    trayData = PanoTrayUtils.TrayData(
                        tooltip = tooltip,
                        argb = argb,
                        iconSize = iconSize,
                        menuItemIds = trayItems.map { it.first }.toTypedArray(),
                        menuItemTexts = trayItems.map { it.second }.toTypedArray()
                    )
                }
        }

        LaunchedEffect(Unit) {
            trayMenuClickListener(
                onOpenIfNeeded = {
                    if (!windowShown) {
                        windowShown = true
                    } else {
                        windowOpenTrigger.emit(Unit)
                    }
                },
                onExit = ::onExit
            )
        }

        LaunchedEffect(Unit) {
            Platform.setImplicitExit(false)
        }

        LaunchedEffect(windowShown) {
            if (!windowShown) {
                delay(60.seconds)
                // todo: cleanup, also make it 60 seconds
                Logger.d { "running cleanup" }
                SingletonImageLoader.reset()
                Platform.exit()
            }
        }

        // use the AWT tray for macOS instead
        if (DesktopStuff.os == DesktopStuff.Os.Macos) {
            val trayState = rememberTrayState()

            LaunchedEffect(Unit) {
                PanoNotifications.setNotifyFn { title, body ->
                    trayState.sendNotification(
                        Notification(
                            title = title,
                            message = body,
                            type = Notification.Type.Info
                        )
                    )
                }
            }

            trayData?.let { trayData ->
                Tray(
                    icon = trayData.argb.let {
                        val bufferedImage = BufferedImage(
                            trayData.iconSize, trayData.iconSize, BufferedImage.TYPE_INT_ARGB
                        )
                        bufferedImage.setRGB(
                            0,
                            0,
                            trayData.iconSize,
                            trayData.iconSize,
                            it,
                            0,
                            trayData.iconSize
                        )
                        bufferedImage.toPainter()
                    },
                    tooltip = trayData.tooltip,
                    state = trayState
                ) {
                    trayData.menuItemIds.zip(trayData.menuItemTexts).forEach { (id, text) ->
                        if (id == PanoTrayUtils.ItemId.Separator.name) {
                            Separator()
                        } else {
                            Item(text = text) {
                                PanoTrayUtils.onTrayMenuItemClickedFn(id)
                            }
                        }
                    }
                }
            }
        } else {
            LaunchedEffect(Unit) {
                PanoNotifications.setNotifyFn { title, body ->
                    PanoNativeComponents.notify(title, body)
                }
            }

            LaunchedEffect(trayData) {
                trayData?.let { trayData ->
                    PanoNativeComponents.setTray(
                        tooltip = trayData.tooltip,
                        argb = trayData.argb,
                        iconSize = trayData.iconSize,
                        menuItemIds = trayData.menuItemIds,
                        menuItemTexts = trayData.menuItemTexts,
                    )
                }
            }
        }

        if (windowShown) {
            Window(
                onCloseRequest = {
                    windowShown = false
                },
                state = windowState,
                title = BuildKonfig.APP_NAME,
                icon = painterResource(Res.drawable.ic_launcher_with_bg)
            ) {
                LaunchedEffect(Unit) {
                    if (isSystemInDarkTheme)
                        PanoNativeComponents.applyDarkModeToWindow(window.windowHandle)

                    windowOpenTrigger.collect {
                        window.toFront()
                        window.windowHandle
                    }
                }

                val navController = rememberNavController()

                AppTheme {
                    PanoAppContent(
                        navController
                    )
                }
            }
        }
    }
}

private suspend fun trayMenuClickListener(
    onOpenIfNeeded: suspend () -> Unit,
    onExit: () -> Unit
) {
    PanoTrayUtils.onTrayMenuItemClicked.collect { id ->
        val splits = id.split(":", limit = 2)
        val itemId = splits.first().let { PanoTrayUtils.ItemId.valueOf(it) }
        val suffix = splits.getOrNull(1)
        val playingTrackTrayInfo = PanoNotifications.playingTrackTrayInfo.value

        when (itemId) {
            PanoTrayUtils.ItemId.Exit -> {
                onExit()
            }

            PanoTrayUtils.ItemId.Open -> {
                onOpenIfNeeded()
            }

            PanoTrayUtils.ItemId.Error -> {
                val errorState =
                    (playingTrackTrayInfo[suffix] as? PlayingTrackNotificationState.Error)
                        ?: return@collect

                val scrobbleError = errorState.scrobbleError

                if (scrobbleError.canFixMetadata) {
                    onOpenIfNeeded()
                }
            }

            else -> {
                val scrobblingTrackInfo =
                    (playingTrackTrayInfo[suffix] as? PlayingTrackNotificationState.Scrobbling)
                        ?.trackInfo
                        ?: return@collect

                when (itemId) {
                    PanoTrayUtils.ItemId.Love -> {
                        notifyPlayingTrackEvent(
                            PlayingTrackNotifyEvent.TrackLovedUnloved(
                                hash = scrobblingTrackInfo.hash,
                                loved = !scrobblingTrackInfo.userLoved
                            )
                        )
                    }

                    PanoTrayUtils.ItemId.Cancel -> {
                        notifyPlayingTrackEvent(
                            PlayingTrackNotifyEvent.TrackCancelled(
                                hash = scrobblingTrackInfo.hash,
                                showUnscrobbledNotification = false,
                                markAsScrobbled = true
                            )
                        )
                    }

                    PanoTrayUtils.ItemId.Copy -> {
                        val text = "${scrobblingTrackInfo.artist} - ${scrobblingTrackInfo.title}"
                        PlatformStuff.copyToClipboard(text)
                    }

                    else -> {
                        onOpenIfNeeded()
                    }
                }
            }
        }
    }
}


private fun test() {
    // test stuff
    val properties = System.getProperties()
    println("\n\nSystem properties:")
    properties.forEach { (key, value) -> println("$key: $value") }
    println("\n\n")

//    GlobalScope.launch {
//        delay(5000)
//        PanoNotifications.notifyAppDetected("com.arn.scrobble", "Scrobble")
//    }
}