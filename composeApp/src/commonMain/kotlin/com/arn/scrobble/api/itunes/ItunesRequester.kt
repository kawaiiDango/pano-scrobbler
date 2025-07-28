package com.arn.scrobble.api.itunes

import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import io.ktor.client.request.parameter

class ItunesRequester {
    private val client = Requesters.genericKtorClient

    suspend fun searchTrack(
        term: String,
        country: String,
        lang: String? = null,
        limit: Int
    ) =
        client.getResult<ItunesTrackResponse>("https://itunes.apple.com/search") {
            parameter("term", term)
            parameter("country", country)
            parameter("media", "music")
            parameter("entity", "musicTrack")
            parameter("lang", lang)
            parameter("limit", limit)
        }

    suspend fun lookupArtist(
        artistId: Long,
    ) =
        client.getResult<ItunesArtistResponse>("https://itunes.apple.com/lookup") {
            parameter("id", artistId)
            parameter("entity", "musicArtist")
        }

    suspend fun lookupTrack(
        trackId: Long,
    ) =
        client.getResult<ItunesTrackResponse>("https://itunes.apple.com/lookup") {
            parameter("id", trackId)
            parameter("entity", "musicTrack")
        }
}