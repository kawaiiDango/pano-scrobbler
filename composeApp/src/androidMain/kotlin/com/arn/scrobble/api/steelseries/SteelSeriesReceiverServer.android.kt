package com.arn.scrobble.api.steelseries

import com.arn.scrobble.api.AdditionalMetadataResult
import com.arn.scrobble.api.lastfm.ScrobbleData

actual object SteelSeriesReceiverServer {
    actual suspend fun getAdditionalData(scrobbleData: ScrobbleData): AdditionalMetadataResult =
        AdditionalMetadataResult.Empty
}