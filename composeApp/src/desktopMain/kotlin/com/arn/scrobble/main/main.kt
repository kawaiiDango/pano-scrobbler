package com.arn.scrobble.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.LifecycleResumeEffect
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.ExtrasProps
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.billing.BillingRepository
import com.arn.scrobble.crashreporter.CrashReporter
import com.arn.scrobble.discordrpc.DiscordRpc
import com.arn.scrobble.logger.JvmLogger
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.review.ReviewPrompter
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.themes.isSystemInDarkThemeNative
import com.arn.scrobble.ui.SerializableWindowState
import com.arn.scrobble.updates.runUpdateAction
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PanoTrayUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.toImageBitmap
import com.arn.scrobble.utils.VariantStuff
import com.arn.scrobble.utils.setAppLocale
import com.arn.scrobble.work.UpdaterWork
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.copy
import pano_scrobbler.composeapp.generated.resources.discord_rich_presence
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.fix_it_action
import pano_scrobbler.composeapp.generated.resources.ic_launcher_with_bg
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.quit
import pano_scrobbler.composeapp.generated.resources.settings
import pano_scrobbler.composeapp.generated.resources.unlove
import pano_scrobbler.composeapp.generated.resources.update_downloaded
import pano_scrobbler.composeapp.generated.resources.vd_noti
import pano_scrobbler.composeapp.generated.resources.vd_noti_err
import pano_scrobbler.composeapp.generated.resources.vd_noti_persistent
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


private fun init() {
    // init: run once

    Logger.setLogWriters(
        JvmLogger(
            logToFile = true,
            redirectStderr = !BuildKonfig.DEBUG
        )
    )
    Logger.setTag("scrobbler")
    Logger.setMinSeverity(
        if (BuildKonfig.DEBUG) Severity.Debug else Severity.Info
    )

    LocaleUtils.setAppLocale(LocaleUtils.locale.value, activityContext = null)

    PanoNativeComponents.init()

    VariantStuff.billingRepository = BillingRepository(
        null,
        Stuff.billingClientData,
        PlatformStuff::openInBrowser
    )

    VariantStuff.crashReporter = CrashReporter(null)
    VariantStuff.reviewPrompter = ReviewPrompter
    VariantStuff.extrasProps = ExtrasProps

    DiscordRpc.start()

//    TestStuff.test()
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
    DesktopStuff.setSystemProperties()
    PanoNativeComponents.load()

    if (cmdlineArgs.automationCommand != null) {
        // handle automation command
        PanoNativeComponents.sendIpcCommand(
            cmdlineArgs.automationCommand,
            cmdlineArgs.automationArg ?: "",
        )
        return
    } else if (!BuildKonfig.DEBUG)
        preventMultipleInstances()

    var wmClassNameSet = false

    val initialPrefs = runBlocking { Stuff.initializeMainPrefsCache() }
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
        val trayIconTheme by remember {
            PlatformStuff.mainPrefs.data.map { it.trayIconTheme }
        }
            .collectAsState(initialPrefs.trayIconTheme)
        var trayData by remember { mutableStateOf<PanoTrayUtils.TrayData?>(null) }
        val trayIconNotPlaying = painterResource(Res.drawable.vd_noti_persistent)
        val trayIconPlaying = painterResource(Res.drawable.vd_noti)
        val trayIconError = painterResource(Res.drawable.vd_noti_err)
        val windowOpenTrigger = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val trayState = rememberTrayState()

        // restore window state
        val windowState = rememberWindowState(
            size = initialPrefs.windowState?.let {
                DpSize(it.width.dp, it.height.dp)
            }
                ?: DpSize(800.dp, 600.dp),
            placement = if (initialPrefs.windowState?.isMaximized == true)
                WindowPlacement.Maximized
            else
                WindowPlacement.Floating
        )

        fun onExit() {
            DesktopStuff.prepareToExit()
            exitApplication()
            exitProcess(0)
        }

        fun openIfNeeded() {
            windowOpenTrigger.tryEmit(Unit)
            windowCreated = true
            windowShown = true
        }

        LaunchedEffect(trayIconTheme, isSystemInDarkTheme, windowShown) {
            combine(
                PanoNotifications.playingTrackTrayInfo,
                DiscordRpc.wasSuccessFul,
                Stuff.globalUpdateAction,
            ) { playingTrackInfo, discordRpcSuccessful, updateAction ->
                val trayIconPainter = when {
                    playingTrackInfo.isEmpty() -> trayIconNotPlaying
                    playingTrackInfo.values.any { it is PlayingTrackNotifyEvent.Error } -> trayIconError
                    else -> trayIconPlaying
                }

                var tooltipText = BuildKonfig.APP_NAME

                playingTrackInfo.values.firstOrNull()
                    ?.let {
                        val appId = it.scrobbleData.appId

                        tooltipText =
                            if ((it as? PlayingTrackNotifyEvent.TrackPlaying)?.nowPlaying == false)
                                "✔️ "
                            else
                                ""

                        tooltipText += it.scrobbleData.track
                        tooltipText += "\n" + it.scrobbleData.artist

                        if (appId != null)
                            tooltipText += "\n" + AppItem(
                                appId,
                                PlatformStuff.loadApplicationLabel(appId)
                            ).friendlyLabel
                    }

                val trayItems = mutableListOf<Pair<String, String>>()

                // tracks

                playingTrackInfo.forEach { (notiKey, playingTrackState) ->
                    when (playingTrackState) {
                        is PlayingTrackNotifyEvent.TrackPlaying -> {
                            val scrobbleData = playingTrackState.scrobbleData
                            val nowPlaying = playingTrackState.nowPlaying

                            val playingState =
                                if (nowPlaying)
                                    "🎵 "
                                else
                                    "✔️ "

                            val lovedString =
                                if (playingTrackState.userLoved)
                                    "❤️ " + getString(Res.string.unlove)
                                else
                                    "🤍 " + getString(Res.string.love)

                            trayItems += PanoTrayUtils.ItemId.TrackName.withSuffix(notiKey) to
                                    playingState + scrobbleData.track

                            trayItems += PanoTrayUtils.ItemId.ArtistName.withSuffix(notiKey) to
                                    "🎙️ " + scrobbleData.artist

                            if (!scrobbleData.album.isNullOrEmpty()) {
                                trayItems += PanoTrayUtils.ItemId.AlbumName.withSuffix(notiKey) to
                                        "💿 " + scrobbleData.album
                            }

                            trayItems += PanoTrayUtils.ItemId.Separator.name to ""

                            trayItems += PanoTrayUtils.ItemId.Love.withSuffix(notiKey) to
                                    lovedString
                            trayItems += PanoTrayUtils.ItemId.Edit.withSuffix(notiKey) to
                                    "✏️ " +
                                    getString(Res.string.edit)

                            if (playingTrackState.nowPlaying) {
                                trayItems += PanoTrayUtils.ItemId.Cancel.withSuffix(notiKey) to
                                        "❌ " +
                                        getString(Res.string.cancel)
                            }

                            trayItems += PanoTrayUtils.ItemId.Block.withSuffix(notiKey) to
                                    "⛔ " +
                                    getString(Res.string.block)
                            trayItems += PanoTrayUtils.ItemId.Copy.withSuffix(notiKey) to
                                    "📋 " +
                                    getString(Res.string.copy)
                        }

                        is PlayingTrackNotifyEvent.Error -> {
                            val scrobbleError = playingTrackState.scrobbleError

                            trayItems += PanoTrayUtils.ItemId.Error.withSuffix(notiKey) to
                                    scrobbleError.title
                        }
                    }

                    trayItems += PanoTrayUtils.ItemId.Separator.name to ""
                }

                if (discordRpcSuccessful == true) {
                    trayItems += PanoTrayUtils.ItemId.DiscordRpcDisabled.name to "✔️ " + getString(
                        Res.string.discord_rich_presence
                    )
                }

                updateAction?.let {
                    trayItems += PanoTrayUtils.ItemId.Update.name to "🔄️ " + getString(
                        Res.string.update_downloaded
                    ) + ": " + it.version
                }

                // always show these
                if (!windowShown)
                    trayItems += PanoTrayUtils.ItemId.Open.name to getString(Res.string.fix_it_action)

                trayItems += PanoTrayUtils.ItemId.Settings.name to getString(Res.string.settings)

                trayItems += PanoTrayUtils.ItemId.Exit.name to getString(Res.string.quit)

                Triple(tooltipText, trayIconPainter, trayItems)
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
                onOpenIfNeeded = ::openIfNeeded,
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

            PlatformStuff.mainPrefs.updateData {
                it.copy(
                    windowState = if (ws.isMaximized)
                        it.windowState?.copy(isMaximized = true) ?: ws
                    else
                        ws
                )
            }
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
            var trayMenuPos by remember { mutableStateOf<Point?>(null) }

            LaunchedEffect(trayData) {
                var delayJob: Job? = null

                if (!trayMouseListenerSet && trayData != null) {
                    val trayIcon = SystemTray.getSystemTray().trayIcons?.firstOrNull()

                    if (trayIcon != null) {
                        trayIcon.addMouseListener(
                            object : MouseListener {
                                override fun mouseClicked(e: MouseEvent?) {
                                    // open main window on left double click
                                    when (e?.button) {
                                        MouseEvent.BUTTON1 if e.clickCount == 2 -> {
                                            openIfNeeded()
                                            trayMenuPos = null
                                            delayJob?.cancel()
                                        }

                                        MouseEvent.BUTTON1 if e.clickCount == 1 -> {
                                            delayJob = GlobalScope.launch {
                                                delay(100)
                                                trayMenuPos = e.locationOnScreen
                                            }
                                        }

                                        MouseEvent.BUTTON3 -> {
                                            delayJob?.cancel()
                                            trayMenuPos = e.locationOnScreen
                                        }
                                    }
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

            if (trayMenuPos != null && trayData != null) {
                TrayWindow(
                    location = trayMenuPos!!,
                    menuItemIds = trayData!!.menuItemIds,
                    menuItemTexts = trayData!!.menuItemTexts,
                ) {
                    trayMenuPos = null
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
            if (!DesktopStuff.noUpdateCheck &&
                PlatformStuff.mainPrefs.data.map { it.autoUpdates }
                    .first()
            ) {
                // this app runs at startup, so wait for an internet connection
                delay(1.minutes)
                UpdaterWork.schedule(true)
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

                LaunchedEffect(isSystemInDarkTheme) {
                    if (isSystemInDarkTheme && DesktopStuff.os == DesktopStuff.Os.Windows)
                        PanoNativeComponents.applyDarkModeToWindow(window.windowHandle)
                }

                LaunchedEffect(Unit) {
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

                AppTheme {
                    PanoAppContent()
                }
            }
        }
    }
}

private suspend fun trayMenuClickListener(
    onOpenIfNeeded: () -> Unit,
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

            PanoTrayUtils.ItemId.Open,
            PanoTrayUtils.ItemId.Settings -> {
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
                    (playingTrackTrayInfo[suffix] as? PlayingTrackNotifyEvent.TrackPlaying)
                        ?: return@collect
                val scrobbleData = scrobblingState.scrobbleData

                when (itemId) {
                    PanoTrayUtils.ItemId.Love -> {
                        notifyPlayingTrackEvent(
                            PlayingTrackNotifyEvent.TrackLovedUnloved(
                                hash = scrobblingState.hash,
                                scrobbleData = scrobblingState.scrobbleData,
                                msid = scrobblingState.msid,
                                notiKey = scrobblingState.notiKey,
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
    location: Point,
    menuItemIds: List<String>,
    menuItemTexts: List<String>,
    onDismiss: () -> Unit,
) {
    val graphicsConfig = remember(location) {
        // defaultScreenDevice changes on a multi-monitor setup, LocalDensity.current does not seem to update

        // Get the screen device that contains the mouse pointer
        val genv = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screenDevice = genv.screenDevices.firstOrNull { device ->
            device.defaultConfiguration.bounds.contains(location)
        } ?: genv.defaultScreenDevice

        screenDevice.defaultConfiguration
    }

    val xScaled = location.x / graphicsConfig.defaultTransform.scaleX.toFloat()
    val yScaled = location.y / graphicsConfig.defaultTransform.scaleY.toFloat()

    var visible by remember { mutableStateOf(false) }
    val state = rememberWindowState(
        position = WindowPosition.Absolute(0.dp, 0.dp),
        size = DpSize.Unspecified,
        placement = WindowPlacement.Floating
    )

    SwingWindow(
        visible = visible,
        onCloseRequest = onDismiss,
        decoration = WindowDecoration.Undecorated(),
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        state = state,
        init = { window ->
            // hide it from the taskbar
            window.type = Window.Type.UTILITY
        }
    ) {
        LaunchedEffect(Unit) {
            val screenBounds = graphicsConfig.bounds
            val screenInsets =
                Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfig)

            // Calculate usable screen area (absolute coordinates)
            val usableX = screenBounds.x + screenInsets.left
            val usableY = screenBounds.y + screenInsets.top
            val usableWidth =
                screenBounds.width - screenInsets.left - screenInsets.right
            val usableHeight =
                screenBounds.height - screenInsets.top - screenInsets.bottom

            val winSize = window.size
            var newX = xScaled.toInt()
            var newY = yScaled.toInt()

            // Adjust X if out of bounds
            if (newX + winSize.width > usableX + usableWidth) {
                newX = usableX + usableWidth - winSize.width
            }
            if (newX < usableX) {
                newX = usableX
            }

            // Adjust Y if out of bounds
            if (newY + winSize.height > usableY + usableHeight) {
                newY = usableY + usableHeight - winSize.height
            }
            if (newY < usableY) {
                newY = usableY
            }

            state.position = WindowPosition.Absolute(newX.dp, newY.dp)
            visible = true
        }

        LifecycleResumeEffect(Unit) {
            onPauseOrDispose {
                onDismiss()
            }
        }

        AppTheme {
            Surface(
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(max = 300.dp)
                        .padding(vertical = 8.dp)
                ) {
                    menuItemIds.zip(menuItemTexts).forEach { (id, text) ->
                        val isClickable = !id.endsWith("Disabled")

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
                                    .fillMaxWidth()
                                    .then(
                                        if (isClickable)
                                            Modifier.clickable {
                                                PanoTrayUtils.onTrayMenuItemClickedFn(id)
                                                onDismiss()
                                            }
                                        else
                                            Modifier.alpha(0.5f)
                                    )
                                    .padding(vertical = 4.dp, horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}