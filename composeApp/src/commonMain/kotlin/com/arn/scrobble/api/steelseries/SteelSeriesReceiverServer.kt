package com.arn.scrobble.api.steelseries

import com.arn.scrobble.api.lastfm.ScrobbleData

expect object SteelSeriesReceiverServer {
    var serverStartAttempted: Boolean
        private set

    fun startServer()

    fun stopServer()

    fun putAlbum(scrobbleData: ScrobbleData): ScrobbleData?
}