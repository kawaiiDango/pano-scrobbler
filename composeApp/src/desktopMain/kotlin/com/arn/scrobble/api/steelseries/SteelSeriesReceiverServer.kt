package com.arn.scrobble.api.steelseries

import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.utils.DesktopStuff
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import java.io.File
import java.io.IOException

actual object SteelSeriesReceiverServer {
    private var lastGameEvent: SteelSeriesGameEvent? = null

    actual var serverStartAttempted = false
        private set

    private var server: ReceiverServer? = null

    actual fun startServer() {
        if (serverStartAttempted) {
            Logger.w { "SteelSeriesReceiverServer already started or attempted to start." }
            return
        }
        serverStartAttempted = true

        if (DesktopStuff.os != DesktopStuff.Os.Windows) return

        val programDataPath = System.getenv("programdata") ?: return
        try {
//            Server address is in "%programdata%\SteelSeries\SteelSeries Engine 3\coreProps.json"

            val corePropsFile = File(
                programDataPath,
                "SteelSeries\\SteelSeries Engine 3\\coreProps.json"
            )

            if (!corePropsFile.exists()) {
                Logger.w { "SteelSeries coreProps.json file not found, creating it" }

                corePropsFile.parentFile.mkdirs()
                corePropsFile.writeText("{\"address\":\"127.0.0.1:3650\"}")
            }

            val address = Stuff.myJson.decodeFromString<SteelSeriesSeverAddress>(
                corePropsFile.readText()
            ).address

            val (hostname, port) = address.split(':', limit = 2)

            server = ReceiverServer(hostname, port.toInt()).apply {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
            Logger.i { "SteelSeriesReceiverServer started on port $port" }
        } catch (e: IOException) {
            Logger.e(e) { "Failed to start SteelSeriesReceiverServer" }
        }
    }

    actual fun stopServer() {
        server?.stop()
        server = null
        serverStartAttempted = false
        lastGameEvent = null
        Logger.i { "SteelSeriesReceiverServer stopped." }
    }

    actual fun putAlbum(scrobbleData: ScrobbleData): ScrobbleData? {
        /*
        sample data:
        {
            "data": {
                "frame": {
                    "album": "Odd Behavior",
                    "artist": "Woe, Is Me, Bad Wolves, Aj Rebollo",
                    "duration": 188,
                    "imageUrl": "https://resources.tidal.com/images/5243dfc0/9695/458d/9902/13ef49c240b4/1280x1280.jpg",
                    "state": "paused",
                    "time": 3,
                    "title": "Odd Behavior",
                    "url": "http://www.tidal.com/track/438774191"
                },
                "value": 2
            },
            "event": "MEDIA_PLAYBACK",
            "game": "TIDAL"
        }

        It removes things in brackets from track names.
        "CORAÇAO (20th Anniversary Mix)" becomes "CORAÇAO"
         */

        val lastGameEvent = lastGameEvent
        if (
            lastGameEvent != null &&
            lastGameEvent.game == "TIDAL" &&
            lastGameEvent.event == "MEDIA_PLAYBACK" &&
            lastGameEvent.data.frame.title in scrobbleData.track
        ) {
            return scrobbleData.copy(
                album = lastGameEvent.data.frame.album,
            )
        } else {
            Logger.w { "SteelSeries game event did not match: $lastGameEvent" }
        }

        return null
    }

    private class ReceiverServer(
        hostname: String,
        port: Int,
    ) : NanoHTTPD(hostname, port) {
        override fun serve(session: IHTTPSession): Response {
            return if (session.method == Method.POST && session.uri == "/game_event") {

                // https://stackoverflow.com/questions/42504056/why-do-i-get-messy-code-of-chinese-filename-when-i-upload-files-to-nanohttpd-ser
                // add "; charset=UTF-8" to the content type
                val ct = ContentType(session.headers["content-type"]).tryUTF8()
                session.headers["content-type"] = ct.contentTypeHeader

                val files = mutableMapOf<String, String>()
                session.parseBody(files)
                val postData = files["postData"]
                try {
                    if (postData != null) {
                        lastGameEvent =
                            Stuff.myJson.decodeFromString<SteelSeriesGameEvent>(postData)
                    }
                } catch (e: Exception) {
                    Logger.e { "Failed to decode SteelSeries game event postData: $postData" }
                }

                newFixedLengthResponse(
                    Status.OK,
                    MIME_PLAINTEXT,
                    "Ok"
                )
            } else if (BuildKonfig.DEBUG && session.method == Method.GET && session.uri == "/test") {
                newFixedLengthResponse("Ok")
            } else {
                return super.serve(session)
            }

        }
    }
}