package com.arn.scrobble.ui

import androidx.collection.LruCache
import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult
import com.arn.scrobble.LFMRequester
import com.arn.scrobble.SpotifyRequester
import com.arn.scrobble.Stuff
import com.arn.scrobble.Tokens
import de.umass.lastfm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


class MusicEntryImageInterceptor : Interceptor {

    private var nwReqCount = 0
    private val delayMs = 50L
    private val musicEntryCache by lazy { LruCache<String, Optional<MusicEntry>>(500) }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val musicEntryImageReq = when (val data = chain.request.data) {
            is MusicEntryImageReq -> data
            is MusicEntry -> MusicEntryImageReq(data, ImageSize.EXTRALARGE, false) // defaults
            else -> return chain.proceed(chain.request)
        }
        val entry = musicEntryImageReq.musicEntry
        val key = genKey(entry)
        val cachedOptional = musicEntryCache[key]
        var fetchedEntry = cachedOptional?.value

        if (cachedOptional == null) {
            withContext(Dispatchers.IO) {
                try {
                    delay(nwReqCount * delayMs)
                    nwReqCount++
                    fetchedEntry = when (entry) {
                        is Artist -> {
                            if (Tokens.SPOTIFY_ARTIST_INFO_SERVER.isNotEmpty()) {
                                val info = LFMRequester.getArtistInfoSpotify(entry.name)
                                if (info?.name.equals(entry.name, ignoreCase = true))
                                    info
                                else
                                    null
                            } else {
                                val imagesMap = SpotifyRequester.getSpotifyArtistImages(entry.name)
                                Artist(entry.name, null)
                                    .apply { imageUrlsMap = imagesMap }
                            }
                        }
                        is Album -> Album.getInfo(entry.artist, entry.name, Stuff.LAST_KEY)
                        is Track -> Track.getInfo(entry.artist, entry.name, Stuff.LAST_KEY)
                        else -> throw IllegalArgumentException("Not valid MusicEntry")
                    }
                    musicEntryCache.put(key, Optional(fetchedEntry))
                } catch (e: Exception) {
                    throw e
                } finally {
                    if (nwReqCount > 0)
                        nwReqCount--
                }
            }
        }

        if (musicEntryImageReq.replace)
            entry.imageUrlsMap = fetchedEntry?.imageUrlsMap

        val imgUrl = if (entry is Artist)
            fetchedEntry?.getImageURL(musicEntryImageReq.size) ?: ""
            // Spotify image isnt webp
        else
            fetchedEntry?.getWebpImageURL(musicEntryImageReq.size) ?: ""

        val request = ImageRequest.Builder(chain.request)
            .data(imgUrl)
            .build()

        return chain.proceed(request)
    }

    private fun genKey(entry: MusicEntry) = when (entry) {
        is Artist -> Artist::class.java.name + entry.name
        is Album -> entry.artist + Album::class.java.name + entry.name
        is Track -> entry.artist + Track::class.java.name + entry.name
        else -> throw IllegalArgumentException("Invalid MusicEntry")
    }
}

class MusicEntryImageReq(
    val musicEntry: MusicEntry,
    val size: ImageSize,
    val replace: Boolean
)

private class Optional<T>(val value: T?)