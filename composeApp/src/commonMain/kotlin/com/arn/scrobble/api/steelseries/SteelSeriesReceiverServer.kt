package com.arn.scrobble.api.steelseries

import com.arn.scrobble.api.AdditionalMetadataResult
import com.arn.scrobble.api.lastfm.ScrobbleData

expect object SteelSeriesReceiverServer {
    suspend fun getAdditionalData(scrobbleData: ScrobbleData): AdditionalMetadataResult
}