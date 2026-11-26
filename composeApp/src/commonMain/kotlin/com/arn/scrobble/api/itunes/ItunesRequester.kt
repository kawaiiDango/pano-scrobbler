package com.arn.scrobble.api.itunes

import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Requesters.getResult
import io.ktor.client.request.parameter

class ItunesRequester {
    private val client get() = Requesters.genericKtorClient

    suspend fun searchTracks(
        term: String,
        country: String,
        lang: String? = null,
        limit: Int
    ) = search<ItunesTrackResponse>("musicTrack", term, country, lang, limit)

    suspend fun searchAlbums(
        term: String,
        country: String,
        lang: String? = null,
        limit: Int
    ) = search<ItunesAlbumResponse>("album", term, country, lang, limit)

    private suspend inline fun <reified T> search(
        entity: String,
        term: String,
        country: String,
        lang: String? = null,
        limit: Int
    ) =
        client.getResult<T>("https://itunes.apple.com/search") {
            parameter("term", term)
            parameter("country", country)
            parameter("media", "music")
            parameter("entity", entity)
            parameter("lang", lang)
            parameter("limit", limit)
        }
}