package com.arn.scrobble.api.steelseries

import com.arn.scrobble.api.lastfm.ScrobbleData

actual object SteelSeriesReceiverServer {
    actual var serverStartAttempted = false

    actual fun startServer() {
    }

    actual fun stopServer() {
    }

    actual fun putAlbum(scrobbleData: ScrobbleData): ScrobbleData? = null
}