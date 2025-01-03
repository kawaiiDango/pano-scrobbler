package com.arn.scrobble.main

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation.compose.rememberNavController
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.io.RollingFileLogWriterConfig
import coil3.SingletonImageLoader
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.PanoNativeComponents
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.db.BlockedMetadata
import com.arn.scrobble.media.PlayingTrackNotificationState
import com.arn.scrobble.media.PlayingTrackNotifyEvent
import com.arn.scrobble.media.notifyPlayingTrackEvent
import com.arn.scrobble.navigation.PanoRoute
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.block
import pano_scrobbler.composeapp.generated.resources.cancel
import pano_scrobbler.composeapp.generated.resources.close
import pano_scrobbler.composeapp.generated.resources.edit
import pano_scrobbler.composeapp.generated.resources.fix_it_action
import pano_scrobbler.composeapp.generated.resources.ic_launcher_with_bg
import pano_scrobbler.composeapp.generated.resources.love
import pano_scrobbler.composeapp.generated.resources.settings
import pano_scrobbler.composeapp.generated.resources.unlove
import pano_scrobbler.composeapp.generated.resources.vd_noti
import pano_scrobbler.composeapp.generated.resources.vd_noti_err
import pano_scrobbler.composeapp.generated.resources.vd_noti_persistent
import java.io.File
import java.net.URL
import kotlin.time.Duration.Companion.seconds

private fun init() {
    // init: run once
    URL.setURLStreamHandlerFactory(CustomURLStreamHandlerFactory())

    Stuff.mainPrefsInitialValue = runBlocking { PlatformStuff.mainPrefs.data.first() }
    Logger.setTag("scrobbler")
    Logger.setMinSeverity(
        if (PlatformStuff.isDebug) Severity.Debug else Severity.Info
    )

    val logsDir = File(PlatformStuff.filesDir, "logs").also { it.mkdirs() }
    Logger.addLogWriter(
        RollingFileLogWriter(
            config = RollingFileLogWriterConfig(
                logFileName = "pano-scrobbler",
                logFilePath = Path(logsDir.absolutePath),
                rollOnSize = 20 * 1024, // 50 KB
                maxLogFiles = 5,
            )
        )
    )

//    todo fix RollingFileLogWriter: Uncaught exception in writer coroutine, deletion failed
//    System.setErr(PrintStream(StderrOutputStream(), true))

    setAppLocale(null, Stuff.mainPrefsInitialValue.locale, force = false)

    PanoNativeComponents.init()
    test()
}

fun main(args: Array<String>) {
    init()

    return application {
        var windowShown by remember { mutableStateOf(!shouldStartMinimized(args)) }
        var initialRoute by remember { mutableStateOf<PanoRoute?>(null) }
        val subsequentRoute = remember { MutableSharedFlow<PanoRoute>() }
        val scope = rememberCoroutineScope()
        val windowState = rememberWindowState()

        fun forceNavigateTo(route: PanoRoute) {
            if (!windowShown) {
                windowShown = true
                initialRoute = route
            } else {
                scope.launch {
                    subsequentRoute.emit(route)
                }
            }
        }

        fun onExit() {
            DesktopWorkManager.clearAll()
            exitApplication()
        }

        var isScrobbling by remember { mutableStateOf(false) }

        var hasError by remember { mutableStateOf(false) }

        val trayIconRes = remember(isScrobbling, hasError) {
            if (hasError)
                Res.drawable.vd_noti_err
            else if (isScrobbling)
                Res.drawable.vd_noti
            else
                Res.drawable.vd_noti_persistent
        }

        val trayIconPainter = painterResource(trayIconRes)

        LaunchedEffect(Unit) {
            PanoNotifications.playingTrackTrayInfo.mapLatest {
                val tooltip = it.entries.firstOrNull {
                    val it = it.value
                    it is PlayingTrackNotificationState.Scrobbling
                }?.let { (appId, it) ->
                    it.trackInfo.title + "\n" + it.trackInfo.artist + "\n" + appId
                }
                    ?: BuildKonfig.APP_NAME

                tooltip
            }.distinctUntilChanged()
                .collectLatest {
                    PanoNativeComponents.setTrayTooltip(it)
                }
        }

        LaunchedEffect(trayIconPainter) {
            val bmp = trayIconPainter.toImageBitmap()
            val argb = IntArray(bmp.width * bmp.height)
            bmp.readPixels(argb)

            PanoNativeComponents.setTrayIcon(argb, bmp.width, bmp.height)
        }

        LaunchedEffect(Unit) {
            PanoNotifications.playingTrackTrayInfo.mapLatest {
                hasError = it.values.any { it is PlayingTrackNotificationState.Error }
                isScrobbling = it.isNotEmpty()

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
                                    "✓ "

                            val lovedString =
                                if (trackInfo.userLoved)
                                    "❤️ " + getString(Res.string.unlove)
                                else
                                    "♡  " +
                                            getString(Res.string.love)

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
                trayItems += PanoTrayUtils.ItemId.Settings.name to getString(Res.string.settings)
                trayItems += PanoTrayUtils.ItemId.Close.name to getString(Res.string.close)

                trayItems
            }
                .distinctUntilChanged()
                .collectLatest { trayItems ->
                    PanoNativeComponents.setTrayMenu(
                        menuItemIds = trayItems.map { it.first }.toTypedArray(),
                        menuItemTexts = trayItems.map { it.second }.toTypedArray()
                    )
                }
        }

        LaunchedEffect(Unit) {
            trayMenuClickListener(
                onOpen = { initialRoute = null; windowShown = true },
                onForceNavigateTo = ::forceNavigateTo,
                onExit = ::onExit
            )
        }

        LaunchedEffect(Unit) {
            Platform.setImplicitExit(false)
        }

        LaunchedEffect(windowShown) {
            if (!windowShown) {
                delay(10.seconds)
                // todo: cleanup, also make it 60 seconds
                Logger.d { "running cleanup" }
                SingletonImageLoader.reset()
                Platform.exit()
                delay(2.seconds)
                System.gc()
            }
        }

        if (windowShown) {
            Window(
                onCloseRequest = {
                    windowShown = false
                    initialRoute = null
                },
                state = windowState,
                title = BuildKonfig.APP_NAME,
                icon = painterResource(Res.drawable.ic_launcher_with_bg)
            ) {
                val navController = rememberNavController()

                LaunchedEffect(subsequentRoute) {
                    subsequentRoute.collectLatest { route ->
                        navController.navigate(route)
                    }
                }

                AppTheme {
                    PanoAppContent(
                        navController,
                        customInitialRoute = initialRoute
                    )
                }
            }
        }
    }
}

private suspend fun trayMenuClickListener(
    onOpen: () -> Unit,
    onForceNavigateTo: (PanoRoute) -> Unit,
    onExit: () -> Unit
) {
    PanoTrayUtils.onTrayMenuItemClicked.collect { id ->
        val splits = id.split(":", limit = 2)
        val itemId = splits.first().let { PanoTrayUtils.ItemId.valueOf(it) }
        val suffix = splits.getOrNull(1)
        val playingTrackTrayInfo = PanoNotifications.playingTrackTrayInfo.value

        when (itemId) {
            PanoTrayUtils.ItemId.Close -> {
                onExit()
            }

            PanoTrayUtils.ItemId.Settings -> {
                onForceNavigateTo(PanoRoute.Prefs)
            }

            PanoTrayUtils.ItemId.Open -> {
                onOpen()
            }

            PanoTrayUtils.ItemId.Error -> {
                val errorState =
                    (playingTrackTrayInfo[suffix] as? PlayingTrackNotificationState.Error)
                        ?: return@collect

                val trackInfo = errorState.trackInfo
                val scrobbleError = errorState.scrobbleError

                if (scrobbleError.canFixMetadata) {
                    val route = PanoRoute.EditScrobble(
                        scrobbleData = trackInfo.toScrobbleData(),
                        hash = trackInfo.hash
                    )
                    onForceNavigateTo(route)
                }
            }

            else -> {
                val user = Scrobblables.currentScrobblableUser ?: return@collect
                val scrobblingTrackInfo =
                    (playingTrackTrayInfo[suffix] as? PlayingTrackNotificationState.Scrobbling)
                        ?.trackInfo
                        ?: return@collect

                when (itemId) {
                    PanoTrayUtils.ItemId.TrackName -> {
                        val route = PanoRoute.MusicEntryInfo(
                            track = scrobblingTrackInfo.toTrack(),
                            user = user
                        )
                        onForceNavigateTo(route)
                    }

                    PanoTrayUtils.ItemId.ArtistName -> {
                        val route = PanoRoute.MusicEntryInfo(
                            artist = scrobblingTrackInfo.toTrack().artist,
                            user = user
                        )
                        onForceNavigateTo(route)
                    }

                    PanoTrayUtils.ItemId.Love -> {
                        notifyPlayingTrackEvent(
                            PlayingTrackNotifyEvent.TrackLovedUnloved(
                                hash = scrobblingTrackInfo.hash,
                                loved = !scrobblingTrackInfo.userLoved
                            )
                        )
                    }

                    PanoTrayUtils.ItemId.Edit -> {
                        val route = PanoRoute.EditScrobble(
                            scrobbleData = scrobblingTrackInfo.toScrobbleData(),
                            hash = scrobblingTrackInfo.hash
                        )
                        onForceNavigateTo(route)
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

                    PanoTrayUtils.ItemId.Block -> {
                        val blockedMetadata = BlockedMetadata(
                            track = scrobblingTrackInfo.title,
                            artist = scrobblingTrackInfo.artist,
                            album = scrobblingTrackInfo.album,
                            albumArtist = scrobblingTrackInfo.albumArtist,
                        )

                        val route = PanoRoute.BlockedMetadataAdd(
                            blockedMetadata = blockedMetadata,
                            hash = scrobblingTrackInfo.hash
                        )

                        onForceNavigateTo(route)
                    }

                    else -> {
                        Logger.d { "Unknown tray menu item clicked: $id" }
                    }
                }
            }
        }
    }
}

private fun shouldStartMinimized(args: Array<String>): Boolean {
//    return true
    return args.contains(DesktopStuff.MINIMIZED_ARG) || args.contains("-m")
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