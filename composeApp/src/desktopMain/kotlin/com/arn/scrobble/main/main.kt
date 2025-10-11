package com.arn.scrobble.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.logger.JvmLogger
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.themes.isSystemInDarkThemeNative
import com.arn.scrobble.ui.SerializableWindowState
import com.arn.scrobble.updates.runUpdateAction
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PanoTrayUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.toImageBitmap
import com.arn.scrobble.utils.setAppLocale
import com.arn.scrobble.work.UpdaterWork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
import pano_scrobbler.composeapp.generated.resources.update_downloaded
import pano_scrobbler.composeapp.generated.resources.vd_noti
import pano_scrobbler.composeapp.generated.resources.vd_noti_err
import pano_scrobbler.composeapp.generated.resources.vd_noti_persistent
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.nio.charset.Charset
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


private fun init() {
    // init: run once

    if (!PlatformStuff.isDebug)
        preventMultipleInstances()

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

    PanoNativeComponents.init()

    // do a dummy query before the application starts to prevent a segfault on Linux
    // no idea why that happens, and why this fixes it
//    runBlocking {
//        PanoDb.db.useReaderConnection { }
//    }

//    test()
}

private fun preventMultipleInstances() {
    val isSingleInstance = !PanoNativeComponents.sendIpcCommand(
        Automation.DESKTOP_FOCUS_EXISTING,
        "",
    )

    if (!isSingleInstance) {
        exitProcess(1)
    }
}

fun main(args: Array<String>) {
    val cmdlineArgs = DesktopStuff.parseCmdlineArgs(args)
    DesktopStuff.setSystemPropertiesForGraalvm()
    PanoNativeComponents.load()

    if (cmdlineArgs.automationCommand != null) {
        // handle automation command
        PanoNativeComponents.sendIpcCommand(
            cmdlineArgs.automationCommand,
            cmdlineArgs.automationArg ?: "",
        )
        return
    }

    var wmClassNameSet = false

    init()

    return application {

        if (DesktopStuff.os == DesktopStuff.Os.Linux && !wmClassNameSet) {
            // set the WM class name to avoid issues with some Linux desktop environments
            // do it after compose inits the swing framework, but before any window gets shown, else high dpi scaling breaks
            try {
                val awtAppClassNameField =
                    Class.forName("sun.awt.X11.XToolkit").getDeclaredField("awtAppClassName")
                awtAppClassNameField.isAccessible = true
                awtAppClassNameField.set(null, "pano-scrobbler")
            } catch (e: Exception) {
                Logger.e(e) { "Failed to set AWT app class name" }
            }

            wmClassNameSet = true
        }

        var windowShown by remember { mutableStateOf(!cmdlineArgs.minimized) }
        var windowCreated by remember { mutableStateOf(windowShown) }
        val isSystemInDarkTheme by isSystemInDarkThemeNative()
        val trayIconTheme by PlatformStuff.mainPrefs.data
            .map { it.trayIconTheme }
            .collectAsState(Stuff.mainPrefsInitialValue.trayIconTheme)
        var trayData by remember { mutableStateOf<PanoTrayUtils.TrayData?>(null) }
        val trayIconNotPlaying = painterResource(Res.drawable.vd_noti_persistent)
        val trayIconPlaying = painterResource(Res.drawable.vd_noti)
        val trayIconError = painterResource(Res.drawable.vd_noti_err)
        val appIdToNames by PlatformStuff.mainPrefs.data
            .map { it.seenApps }
            .collectAsState(Stuff.mainPrefsInitialValue.seenApps)
        val windowOpenTrigger = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val trayState = rememberTrayState()

        // restore window state
        val storedWindowState = Stuff.mainPrefsInitialValue.windowState
        val windowState = rememberWindowState(
            size = storedWindowState?.let {
                DpSize(it.width.dp, it.height.dp)
            }
                ?: DpSize(800.dp, 600.dp),
            placement = if (storedWindowState?.isMaximized == true)
                WindowPlacement.Maximized
            else
                WindowPlacement.Floating
        )

        fun onExit() {
            DesktopStuff.prepareToExit()
            exitApplication()
            exitProcess(0)
        }

        LaunchedEffect(trayIconTheme, isSystemInDarkTheme) {
            combine(
                PanoNotifications.playingTrackTrayInfo,
                Stuff.globalUpdateAction
            ) { playingTrackInfo, updateAction ->
                val trayIconPainter = when {
                    playingTrackInfo.isEmpty() -> trayIconNotPlaying
                    playingTrackInfo.values.any { it is PlayingTrackNotifyEvent.Error } -> trayIconError
                    else -> trayIconPlaying
                }

                val tooltip = playingTrackInfo.entries.firstOrNull()
                    ?.let { (appId, it) ->
                        val playingState =
                            if ((it as? PlayingTrackNotifyEvent.TrackScrobbling)?.nowPlaying == false)
                                "✔️ "
                            else
                                ""

                        playingState + it.scrobbleData.track + "\n" +
                                it.scrobbleData.artist + "\n" +
                                (appIdToNames[appId]?.ifEmpty { null } ?: appId)
                    }
                    ?: BuildKonfig.APP_NAME


                val trayItems = mutableListOf<Pair<String, String>>()

                // tracks

                playingTrackInfo.forEach { (appId, playingTrackState) ->
                    when (playingTrackState) {
                        is PlayingTrackNotifyEvent.TrackScrobbling -> {
                            val scrobbleData = playingTrackState.scrobbleData
                            val nowPlaying = playingTrackState.nowPlaying

                            val playingState =
                                if (nowPlaying)
                                    "▶️ "
                                else
                                    "✔️ "

                            val lovedString =
                                if (playingTrackState.userLoved)
                                    "❤️ " + getString(Res.string.unlove)
                                else
                                    "🤍 " + getString(Res.string.love)

                            trayItems += PanoTrayUtils.ItemId.TrackName.withSuffix(appId) to
                                    playingState + scrobbleData.track

                            trayItems += PanoTrayUtils.ItemId.ArtistName.withSuffix(appId) to
                                    "🎙️ " + scrobbleData.artist

                            if (!scrobbleData.album.isNullOrEmpty()) {
                                trayItems += PanoTrayUtils.ItemId.AlbumName.withSuffix(appId) to
                                        "💿 " + scrobbleData.album
                            }

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

                        is PlayingTrackNotifyEvent.Error -> {
                            val scrobbleError = playingTrackState.scrobbleError

                            trayItems += PanoTrayUtils.ItemId.Error.withSuffix(appId) to
                                    scrobbleError.title
                        }
                    }

                    trayItems += PanoTrayUtils.ItemId.Separator.name to ""
                }

                updateAction?.let {
                    trayItems += PanoTrayUtils.ItemId.Update.name to "🔄️ " + getString(
                        Res.string.update_downloaded
                    ) + ": " + it.version
                }

                // always show these

                trayItems += PanoTrayUtils.ItemId.Open.name to getString(Res.string.fix_it_action)

                trayItems += PanoTrayUtils.ItemId.Exit.name to getString(Res.string.quit)

                Triple(tooltip, trayIconPainter, trayItems)
            }
                .distinctUntilChanged()
                .collectLatest { (tooltip, trayIconPainter, trayItems) ->
                    val iconSize = 128f

                    val bmp = trayIconPainter.toImageBitmap(
                        darkTint = when (trayIconTheme) {
                            DayNightMode.SYSTEM -> !isSystemInDarkTheme
                            DayNightMode.LIGHT -> false
                            DayNightMode.DARK -> true
                        },
                        size = Size(iconSize, iconSize)
                    )

                    trayData = PanoTrayUtils.TrayData(
                        tooltip = tooltip,
                        bitmap = bmp,
                        iconSize = iconSize.toInt(),
                        menuItemIds = trayItems.map { it.first },
                        menuItemTexts = trayItems.map { it.second }
                    )
                }
        }

        LaunchedEffect(Unit) {
            trayMenuClickListener(
                onOpenIfNeeded = {
                    windowOpenTrigger.emit(Unit)
                    windowCreated = true
                    windowShown = true
                },
                onExit = ::onExit
            )
        }

        LaunchedEffect(windowState.size, windowState.placement) {
            delay(5.seconds)

            val ws = SerializableWindowState(
                width = windowState.size.width.value,
                height = windowState.size.height.value,
                isMaximized = windowState.placement == WindowPlacement.Maximized,
            )
            PlatformStuff.mainPrefs.updateData { it.copy(windowState = ws) }
        }

        if (DesktopStuff.os != DesktopStuff.Os.Linux) {
            // never deinit on linux, as it causes native memory leaks on reinit
            LaunchedEffect(windowShown) {
                if (!windowShown) {
                    delay(5.minutes)
                    Logger.i { "running cleanup" }
                    windowCreated = false
                }
            }
        }

        // leak test
//        LaunchedEffect(Unit) {
//            while (true) {
//                delay(1000)
//                windowShown = !windowShown
//            }
//        }

        // the AWT tray doesn't work on KDE
        if (DesktopStuff.os != DesktopStuff.Os.Linux) {

            trayData?.let { trayData ->
                Tray(
                    icon = BitmapPainter(trayData.bitmap),
                    tooltip = trayData.tooltip,
                    state = trayState
                )
            }

            var trayMouseListenerSet by remember { mutableStateOf(false) }
            var trayClickedEvent by remember { mutableStateOf<PanoTrayUtils.TrayClickEvent?>(null) }

            LaunchedEffect(trayData) {
                if (!trayMouseListenerSet && trayData != null) {
                    val trayIcon = SystemTray.getSystemTray().trayIcons?.firstOrNull()
                    if (trayIcon != null) {
                        trayIcon.addMouseListener(
                            object : MouseListener {
                                override fun mouseClicked(e: MouseEvent?) {
                                    trayClickedEvent = PanoTrayUtils.TrayClickEvent(
                                        x = e?.x ?: 0,
                                        y = e?.y ?: 0,
                                        button = e?.button ?: 0
                                    )
                                }

                                override fun mousePressed(e: MouseEvent?) {
                                }

                                override fun mouseReleased(e: MouseEvent?) {
                                }

                                override fun mouseEntered(e: MouseEvent?) {
                                }

                                override fun mouseExited(e: MouseEvent?) {
                                }
                            }
                        )
                        trayMouseListenerSet = true
                    }
                }
            }

            if (trayClickedEvent != null && trayData != null) {
                TrayWindow(
                    x = trayClickedEvent!!.x,
                    y = trayClickedEvent!!.y,
                    menuItemIds = trayData!!.menuItemIds,
                    menuItemTexts = trayData!!.menuItemTexts,
                ) {
                    trayClickedEvent = null
                }
            }
        } else {
            LaunchedEffect(trayData) {
                trayData?.let { trayData ->
                    PanoNativeComponents.setTray(
                        tooltip = trayData.tooltip,
                        argb = trayData.bitmap.let { bmp ->
                            val argb = IntArray(bmp.width * bmp.height)
                            bmp.readPixels(argb)
                            argb
                        },
                        iconSize = trayData.iconSize,
                        menuItemIds = trayData.menuItemIds.toTypedArray(),
                        menuItemTexts = trayData.menuItemTexts.toTypedArray(),
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            PanoNotifications.setNotifyFn { title, body ->
                if (DesktopStuff.os == DesktopStuff.Os.Macos) {
                    trayState.sendNotification(
                        Notification(
                            title = title,
                            message = body,
                            type = Notification.Type.Info
                        )
                    )
                } else {
                    PanoNativeComponents.notify(title, body)
                }
            }
        }

        LaunchedEffect(Unit) {
            if (!DesktopStuff.noUpdateCheck && Stuff.mainPrefsInitialValue.autoUpdates) {
                // this app runs at startup, so wait for an internet connection
                delay(1.minutes)
                UpdaterWork.checkAndSchedule(true)
            }
        }

        if (windowCreated) {
            Window(
                onCloseRequest = { windowShown = false },
                state = windowState,
                title = BuildKonfig.APP_NAME,
                visible = windowShown,
                icon = painterResource(Res.drawable.ic_launcher_with_bg)
            ) {
                val density = LocalDensity.current

                LaunchedEffect(Unit) {
                    if (isSystemInDarkTheme && DesktopStuff.os == DesktopStuff.Os.Windows)
                        PanoNativeComponents.applyDarkModeToWindow(window.windowHandle)

                    if (!BuildKonfig.DEBUG) {
                        val minDim = if (DesktopStuff.os == DesktopStuff.Os.Windows)
                            with(density) { 480.dp.roundToPx() }
                        else
                            480

                        window.minimumSize = Dimension(minDim, minDim)
                    }

                    windowOpenTrigger.collect {
                        window.isMinimized = false
                        window.toFront()
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

            PanoTrayUtils.ItemId.Update -> {
                Stuff.globalUpdateAction.value?.let {
                    runUpdateAction(it)
                }
            }

            PanoTrayUtils.ItemId.Error -> {
                val errorState =
                    (playingTrackTrayInfo[suffix] as? PlayingTrackNotifyEvent.Error)
                        ?: return@collect

                val scrobbleError = errorState.scrobbleError

                if (scrobbleError.canFixMetadata) {
                    onOpenIfNeeded()
                }
            }

            else -> {
                val scrobblingState =
                    (playingTrackTrayInfo[suffix] as? PlayingTrackNotifyEvent.TrackScrobbling)
                        ?: return@collect
                val scrobbleData = scrobblingState.scrobbleData

                when (itemId) {
                    PanoTrayUtils.ItemId.Love -> {
                        notifyPlayingTrackEvent(
                            PlayingTrackNotifyEvent.TrackLovedUnloved(
                                hash = scrobblingState.hash,
                                loved = !scrobblingState.userLoved
                            )
                        )
                    }

                    PanoTrayUtils.ItemId.Cancel -> {
                        notifyPlayingTrackEvent(
                            PlayingTrackNotifyEvent.TrackCancelled(
                                hash = scrobblingState.hash,
                                showUnscrobbledNotification = false,
                            )
                        )
                    }

                    PanoTrayUtils.ItemId.Copy -> {
                        val text = "${scrobbleData.artist} - ${scrobbleData.track}"
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TrayWindow(
    x: Int,
    y: Int,
    menuItemIds: List<String>,
    menuItemTexts: List<String>,
    onDismiss: () -> Unit,
) {
    val densityFactor = remember {
        // defaultScreenDevice changes on a multi-monitor setup, LocalDensity.current does not seem to update
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
            .defaultTransform.scaleX.toFloat()
    }

    // convert x, y to dp
    val xDp = (x / densityFactor).dp
    val yDp = (y / densityFactor).dp

    SwingWindow(
        onCloseRequest = onDismiss,
        decoration = WindowDecoration.Undecorated(),
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        state = rememberWindowState(
            position = WindowPosition(xDp, yDp),
            size = DpSize.Unspecified,
            placement = WindowPlacement.Floating
        ),
        init = { window ->
            // hide it from the taskbar
            window.type = Window.Type.UTILITY
            window.addWindowListener(
                object : WindowListener {

                    override fun windowOpened(p0: WindowEvent?) {
                        // Get screen size
                        val screenSize = Toolkit.getDefaultToolkit().screenSize
                        val winSize = window.size
                        var newX = window.location.x
                        var newY = window.location.y

                        // Adjust X if out of bounds
                        if (newX + winSize.width > screenSize.width) {
                            newX = screenSize.width - winSize.width
                        }
                        if (newX < 0) newX = 0

                        // Adjust Y if out of bounds
                        if (newY + winSize.height > screenSize.height) {
                            newY = screenSize.height - winSize.height
                        }
                        if (newY < 0) newY = 0

                        window.setLocation(newX, newY)
                    }

                    override fun windowClosing(p0: WindowEvent?) {
                    }

                    override fun windowClosed(p0: WindowEvent?) {
                    }

                    override fun windowIconified(p0: WindowEvent?) {
                    }

                    override fun windowDeiconified(p0: WindowEvent?) {
                    }

                    override fun windowActivated(p0: WindowEvent?) {
                    }

                    override fun windowDeactivated(p0: WindowEvent?) {
                        // close the window when deactivated
                        onDismiss()
                    }
                }
            )
        }
    ) {
        AppTheme {
            Surface(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.padding(8.dp)
            ) {
                var actualWidth by remember { mutableStateOf(0) }

                Column(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(vertical = 8.dp)
                        .onSizeChanged {
                            actualWidth = it.width
                        },
                ) {
                    menuItemIds.zip(menuItemTexts).forEach { (id, text) ->
                        if (id == PanoTrayUtils.ItemId.Separator.name) {
                            Spacer(
                                modifier = Modifier
                                    .height(8.dp)
                            )
                        } else {
                            Text(
                                text = text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .then(
                                        if (actualWidth > 0)
                                            Modifier.width(actualWidth.dp)
                                        else
                                            Modifier
                                    )
                                    .clickable {
                                        PanoTrayUtils.onTrayMenuItemClickedFn(id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 4.dp, horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun test() {
    // test stuff
    val properties = System.getProperties()
    Logger.i("\n\nSystem properties:")
    properties.forEach { (key, value) -> Logger.i("$key: $value") }

    // supported charsets

    val charsets = Charset.availableCharsets()
    Logger.i("\n\nAvailable charsets:")
    charsets.forEach { (name, charset) ->
        Logger.i("$name: ${charset.displayName()}")
    }

//    GlobalScope.launch {
//        delay(5000)
//        PanoNotifications.notifyAppDetected("com.arn.scrobble", "Scrobble")
//    }
}