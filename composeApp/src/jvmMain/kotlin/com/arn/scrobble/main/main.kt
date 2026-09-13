package com.arn.scrobble.main

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.pollSystemTheme
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.automation.Automation
import com.arn.scrobble.billing.LicenseState
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.discordrpc.DiscordRpc
import com.arn.scrobble.logger.JavaUtilFileLogger
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.navigation.PanoNavigationType
import com.arn.scrobble.pref.AppItem
import com.arn.scrobble.themes.AppTheme
import com.arn.scrobble.themes.DayNightMode
import com.arn.scrobble.ui.SerializableWindowState
import com.arn.scrobble.updates.runUpdateAction
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.DesktopStuff.migrateAppImageDesktopFile
import com.arn.scrobble.utils.LocaleUtils
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PanoTrayUtils
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.stateInWithCache
import com.arn.scrobble.utils.VariantStuff
import com.arn.scrobble.utils.findSkiaLayer
import com.arn.scrobble.utils.hackContentPane
import com.arn.scrobble.utils.setAppLocale
import com.arn.scrobble.work.DesktopWorkManager
import com.arn.scrobble.work.UpdaterWork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.DensityQualifier
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.LanguageQualifier
import org.jetbrains.compose.resources.RegionQualifier
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.ScriptQualifier
import org.jetbrains.compose.resources.ThemeQualifier
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
import java.awt.Cursor
import java.awt.Dimension
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.lang.reflect.Constructor
import java.util.Locale
import javax.swing.SwingUtilities
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes


@OptIn(InternalResourceApi::class)
private fun initHeadlessResourceEnvironment() {

    val constructor: Constructor<ResourceEnvironment> = ResourceEnvironment::class.java
        .getDeclaredConstructor(
            LanguageQualifier::class.java,
            ScriptQualifier::class.java,
            RegionQualifier::class.java,
            ThemeQualifier::class.java,
            DensityQualifier::class.java
        )
    constructor.isAccessible = true

    var lastLocale: Locale? = null
    var lastResourceEnvironment: ResourceEnvironment? = null

    fun environmentFn(): ResourceEnvironment {
        val locale = Locale.getDefault()
        if (locale != lastLocale || lastResourceEnvironment == null) {
            lastLocale = locale
            lastResourceEnvironment = constructor.newInstance(
                LanguageQualifier(locale.language),
                ScriptQualifier(locale.script),
                RegionQualifier(locale.country),
                ThemeQualifier.LIGHT, // safe default, no Skiko needed
                DensityQualifier.MDPI  // safe default, no Swing needed
            )
        }

        return lastResourceEnvironment
    }

    // The file-level var compiles to a static field on the facade class
    // ResourceEnvironmentKt is the facade for ResourceEnvironment.kt
    val facadeClass = Class.forName("org.jetbrains.compose.resources.ResourceEnvironmentKt")
    val field = facadeClass.getDeclaredField("getResourceEnvironment")
    field.isAccessible = true
    field.set(null, ::environmentFn)
}

private fun init() {
    // init: run once

    Logger.setLogWriters(
        JavaUtilFileLogger(
            isEnabled = true,
            redirectStderr = !BuildKonfig.DEBUG && System.getenv("PANO_KEEP_STDERR") == null,
            printToStd = true
        )
    )
    Logger.setTag("scrobbler")
    Logger.setMinSeverity(
        if (BuildKonfig.DEBUG) Severity.Debug else Severity.Info
    )

    LocaleUtils.setAppLocale(LocaleUtils.locale.value, activityContext = null)

    PanoNativeComponents.init()

    DiscordRpc.start()
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

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val cmdlineArgs = DesktopStuff.parseCmdlineArgs(args)
    DesktopStuff.setSystemProperties()
    PanoNativeComponents.load()

    if (DesktopStuff.IS_WINDOWS && !BuildKonfig.DEBUG) {
        val attached = PanoNativeComponents.attachParentConsoleWindows()

        if (attached) {
            System.setOut(PrintStream(FileOutputStream("CONOUT$"), true, Charsets.UTF_8))
            System.setErr(PrintStream(FileOutputStream("CONOUT$"), true, Charsets.UTF_8))
        }
    }

    if (cmdlineArgs.automationCommand != null) {
        // handle automation command
        PanoNativeComponents.sendIpcCommand(
            cmdlineArgs.automationCommand,
            cmdlineArgs.automationArg ?: "",
        )

        if (cmdlineArgs.automationCommand == Automation.DESKTOP_QUIT) {
            // give the command some time to be sent before quitting
            Thread.sleep(1500)
        }

        return
    } else {
        val logFilePath = PlatformStuff.logsDir.resolve("pano-native-components.log")
        PanoNativeComponents.setLogFilePath(logFilePath.absolutePath)

        if (!BuildKonfig.DEBUG)
            preventMultipleInstances()
    }

    val initialPrefs = runBlocking { Stuff.initializeMainPrefsCache() }

    init()
    initHeadlessResourceEnvironment()

    // ------------------------------- shutdown hook

    Runtime.getRuntime().addShutdownHook(Thread {
        PanoNativeComponents.stopEventLoop()
        DesktopWorkManager.clearAll()
        PanoDb.db.close()
    })

    // ------------------------------- tray menu

    var trayData by mutableStateOf<PanoTrayUtils.TrayData?>(null)

    val dayNightPref =
        PlatformStuff.mainPrefs.data.stateInWithCache(Stuff.appScope) { it.themeDayNight }
    val isTranslucent =
        PlatformStuff.mainPrefs.data.stateInWithCache(Stuff.appScope) { it.themeAlpha < 1f }
    val isBlur =
        PlatformStuff.mainPrefs.data.stateInWithCache(Stuff.appScope) { it.themeBlurMainWindow }

    val iconsDir = System.getProperty("pano.icons.path")
        ?: "".takeIf { DesktopStuff.IS_WINDOWS }
        ?: System.getenv("APPDIR")?.let { "$it/usr/share/icons" }
        ?: "${DesktopStuff.execDirPath}/icons".takeIf { File(it).exists() }
        ?: ""

    combine(
        PanoNotifications.playingTrackTrayInfo,
        DiscordRpc.wasSuccessful,
        Stuff.globalUpdateAction,
    ) { playingTrackInfo, discordRpcSuccessful, updateAction ->

        var tooltipText = BuildKonfig.APP_NAME

        playingTrackInfo.values.firstOrNull()
            ?.let {
                val appId = it.scrobbleData.appId

                tooltipText =
                    if ((it as? PlayingTrackNotifyEvent.TrackPlaying)?.nowPlaying == false)
                        "✔️ "
                    else
                        ""

                tooltipText +=
                    if ((it as? PlayingTrackNotifyEvent.TrackPlaying)?.userLoved == true)
                        "❤️ "
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

        if (discordRpcSuccessful) {
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
//        if (!windowShown)
        trayItems += PanoTrayUtils.ItemId.Open.name to getString(Res.string.fix_it_action)

        trayItems += PanoTrayUtils.ItemId.Settings.name to getString(Res.string.settings)

        trayItems += PanoTrayUtils.ItemId.Exit.name to getString(Res.string.quit)

        trayData = PanoTrayUtils.TrayData(
            tooltip = tooltipText,
            iconType = when {
                playingTrackInfo.isEmpty() -> PanoTrayUtils.TrayIconState.Idle
                playingTrackInfo.values.any { it is PlayingTrackNotifyEvent.Error } -> PanoTrayUtils.TrayIconState.Error
                else -> PanoTrayUtils.TrayIconState.Scrobbling
            },
            menuItemIds = trayItems.map { it.first },
            menuItemTexts = trayItems.map { it.second }
        )

        trayData?.let { trayData ->
            val iconNamePrefix = "pano-scrobbler-"
            val iconNameMiddle = trayData.iconType.name.lowercase()
            val iconNameSuffix = if (DesktopStuff.IS_WINDOWS) ""
            else if (System.getenv("APPDIR") != null)
                "-appimage-symbolic"
            else
                "-symbolic"

            PanoNativeComponents.setTray(
                iconName = "$iconNamePrefix$iconNameMiddle$iconNameSuffix",
                tooltip = trayData.tooltip,
                iconsDir = iconsDir,
                menuItemIds = trayData.menuItemIds.toTypedArray(),
                menuItemTexts = trayData.menuItemTexts.toTypedArray(),
            )
        }
    }.launchIn(Stuff.appScope)

    // ------------------------ UI

    var windowShown by mutableStateOf(!cmdlineArgs.minimized)
    var windowCreated by mutableStateOf(windowShown)
    val openOrQuitTrigger = MutableSharedFlow<OpenOrQuitAction>(extraBufferCapacity = 1)

    fun openIfNeeded() {
        openOrQuitTrigger.tryEmit(OpenOrQuitAction.OPEN)
        windowCreated = true
        windowShown = true
    }

    fun quitNaturally() {
        // let all the disposable effects run
        windowCreated = false
        windowShown = false
        openOrQuitTrigger.tryEmit(OpenOrQuitAction.QUIT)
    }

    Stuff.appScope.launch {
        trayMenuClickListener(
            onOpenIfNeeded = ::openIfNeeded,
            onExit = ::quitNaturally
        )
    }

    Stuff.appScope.launch {
        // init this to prevent a white flash and fix the tray menu window size
        VariantStuff.billingRepository.licenseState
            .filterNot { it == LicenseState.UNKNOWN }
            .first()
    }

    Stuff.appScope.launch {
        if (DesktopStuff.IS_LINUX)
            migrateAppImageDesktopFile()

        if (!DesktopStuff.noUpdateCheck && initialPrefs.autoUpdates) {
            // this app runs at startup, so wait for an internet connection
            delay(1.minutes)
            UpdaterWork.schedule(true)
        }
    }

    ComposeUiFlags.pollSystemTheme = false
    // https://youtrack.jetbrains.com/issue/CMP-10423/DirectX-Smooth-window-resize
    // this kills transparent windows
    // System.setProperty("skiko.rendering.windows.direct3DSynchronousLiveResize", "true")

    var firstCompositionDone = false

    if (cmdlineArgs.minimized) {
        val result = runBlocking {
            openOrQuitTrigger.first()
        }

        if (result == OpenOrQuitAction.QUIT) {
            exitProcess(0)
        }
    }

//    while (true) {
//        if (!windowCreated)
//            runBlocking {
//                windowOpenTrigger.first()
//            }

    return application {

        if (!firstCompositionDone) {
            // set the WM class name to avoid issues with some Linux desktop environments
            // do it after compose inits the swing framework, but before any window gets shown, else high dpi scaling breaks
            if (DesktopStuff.IS_LINUX) {
                try {
                    val awtAppClassNameField =
                        Class.forName("sun.awt.X11.XToolkit").getDeclaredField("awtAppClassName")
                    awtAppClassNameField.isAccessible = true
                    awtAppClassNameField.set(null, "pano-scrobbler")
                } catch (e: Exception) {
                    Logger.e { "Failed to set AWT app class name: ${e.message}" }
                }
            }

            firstCompositionDone = true
        }

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

        // leak test
//        LaunchedEffect(Unit) {
//            while (true) {
//                delay(1000)
//                windowShown = !windowShown
//            }
//        }

        // have a forever running LaunchedEffect so that application {} doesn't exit when the window is closed
        LaunchedEffect(Unit) {
            openOrQuitTrigger.first { it == OpenOrQuitAction.QUIT }
        }

        if (windowCreated) {
            LaunchedEffect(Unit) {
                combine(isTranslucent, isBlur) { a, b -> a to b }
                    .drop(1)
                    .collect {
                        val wasShown = windowShown
                        Stuff.appScope.launch {
                            windowCreated = false
                            if (wasShown) {
                                delay(100.milliseconds)
                                windowCreated = true
                            }
                        }
                    }
            }

            if (!DesktopStuff.IS_LINUX) {
                // never deinit on linux, as it causes native memory leaks on reinit
                LaunchedEffect(windowShown) {
                    if (!windowShown) {
                        delay(3.minutes)
                        Logger.i { "running cleanup" }
                        windowCreated = false
                    }
                }
            }

            val isTranslucentAwtWindow = remember {
                (isTranslucent.value && !isBlur.value || isBlur.value && DesktopStuff.IS_LINUX) &&
                        VariantStuff.billingRepository.licenseState.value == LicenseState.VALID
            }
            val minDim = 480

            SwingWindow(
                onCloseRequest = { windowShown = false },
                state = windowState,
                title = BuildKonfig.APP_NAME,
                visible = windowShown,
                transparent = isTranslucentAwtWindow,
                decoration = if (isTranslucentAwtWindow)
                    WindowDecoration.Undecorated()
                else
                    WindowDecoration.SystemDefault,
                icon = painterResource(Res.drawable.ic_launcher_with_bg),
                init = { window ->

                    if (!BuildKonfig.DEBUG) {
                        window.exceptionHandler = null
                    }

                    val isBlur = isBlur.value &&
                            VariantStuff.billingRepository.licenseState.value == LicenseState.VALID

                    if (isBlur && !isTranslucentAwtWindow) {
                        window.background = java.awt.Color.BLACK
                        window.findSkiaLayer()?.transparency = true
                        window.hackContentPane()
                    }

                    SwingUtilities.invokeLater {

                        val isDark = dayNightPref.value == DayNightMode.DARK ||
                                dayNightPref.value == DayNightMode.SYSTEM &&
                                PanoNativeComponents.onDarkModeChangeFlow.value == true

                        PanoNativeComponents.applyWindowEffects(
                            window.windowHandle,
                            isDark,
                            isBlur
                        )
                    }
                }
            ) {
                LaunchedEffect(Unit) {
                    openOrQuitTrigger
                        .filter { it == OpenOrQuitAction.OPEN }
                        .collect {
                            window.isMinimized = false
                            window.toFront()
                        }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        val ws = SerializableWindowState(
                            width = windowState.size.width.value,
                            height = windowState.size.height.value,
                            isMaximized = windowState.placement == WindowPlacement.Maximized,
                        )

                        Stuff.appScope.launch {
                            PlatformStuff.mainPrefs.updateData {
                                it.copy(
                                    windowState = if (ws.isMaximized)
                                        it.windowState?.copy(isMaximized = true) ?: ws
                                    else
                                        ws
                                )
                            }
                        }
                    }
                }

                val swingDensity = LocalDensity.current

                AppTheme {
                    val composeDensity = LocalDensity.current

                    val densityMultiplier = if (DesktopStuff.IS_WINDOWS)
                        1f
                    else
                        composeDensity.density / swingDensity.density

                    LifecycleStartEffect(Unit) {
                        window.minimumSize = Dimension(
                            (minDim * densityMultiplier).toInt(),
                            (minDim * densityMultiplier).toInt()
                        )

                        onStopOrDispose { }
                    }

                    if (DesktopStuff.IS_WINDOWS) {
                        LaunchedEffect(Unit) {
                            combine(
                                dayNightPref,
                                PanoNativeComponents.onDarkModeChangeFlow.filterNotNull()
                            ) { dayNight, isDarkMode ->
                                dayNight == DayNightMode.DARK ||
                                        dayNight == DayNightMode.SYSTEM && isDarkMode
                            }
                                .drop(1)
                                .collect { isDark ->
                                    PanoNativeComponents.applyWindowEffects(
                                        window.windowHandle,
                                        isDark,
                                        false
                                    )
                                }
                        }
                    }

                    @Composable
                    fun draggableWrapper(it: @Composable ((windowTitleActions: WindowTitleActions) -> Unit)) {
                        val windowTitleActions = remember {
                            object : WindowTitleActions {
                                override fun minimize() {
                                    windowState.isMinimized = true
                                }

                                override fun maximizeRestore() {
                                    windowState.placement =
                                        if (windowState.placement == WindowPlacement.Maximized)
                                            WindowPlacement.Floating
                                        else
                                            WindowPlacement.Maximized
                                }

                                override fun close() {
                                    windowShown = false
                                }
                            }
                        }

                        WindowDraggableArea(
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.MOVE_CURSOR)))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { windowTitleActions.maximizeRestore() }
                                    )
                                }
                        ) {
                            it(windowTitleActions)
                        }
                    }

                    // https://youtrack.jetbrains.com/issue/CMP-8821/LocalWindowInfo.current.containerSize-is-first-initialized-to-00
                    val initialNavigationType = remember {
                        when {
                            windowState.size.width >= WIDTH_DP_EXPANDED_LOWER_BOUND.dp
                                -> PanoNavigationType.PERMANENT_NAVIGATION_DRAWER

                            windowState.size.width >= WIDTH_DP_MEDIUM_LOWER_BOUND.dp
                                -> PanoNavigationType.NAVIGATION_RAIL

                            else -> PanoNavigationType.BOTTOM_NAVIGATION
                        }
                    }

                    PanoAppContent(
                        draggableWrapper = if (isTranslucentAwtWindow) ::draggableWrapper else null,
                        fallbackNavigationType = initialNavigationType
                    )
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

private enum class OpenOrQuitAction {
    OPEN,
    QUIT
}