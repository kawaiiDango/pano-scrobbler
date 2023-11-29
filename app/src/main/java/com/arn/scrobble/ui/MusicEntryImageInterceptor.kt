package com.arn.scrobble.ui

import android.util.LruCache
import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext


class MusicEntryImageInterceptor : Interceptor {

    private val delayMs = 200L
    private val musicEntryCache by lazy { LruCache<String, Optional<String>>(500) }
    private val semaphore = Semaphore(3)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val musicEntryImageReq = when (val data = chain.request.data) {
            is MusicEntryImageReq -> data
            is MusicEntry -> MusicEntryImageReq(data) // defaults
            else -> return chain.proceed(chain.request)
        }
        val entry = musicEntryImageReq.musicEntry
        val key = genKey(entry)
        val cachedOptional = musicEntryCache[key]
        var fetchedImage = cachedOptional?.value

        if (cachedOptional == null) {
            withContext(Dispatchers.IO) {
                try {
                    fetchedImage = when (entry) {
                        is Artist -> {
                            semaphore.withPermit {
                                delay(delayMs)
                                Requesters.spotifyRequester.getSpotifyArtistImage(entry.name)
                            }
                        }

                        is Album -> {
                            val webp300 = entry.webp300
                            val needImage = webp300 == null ||
                                    StarInterceptor.starPattern in webp300

                            if (needImage)
                                semaphore.withPermit {
                                    delay(delayMs)
                                    Requesters.lastfmUnauthedRequester.getInfo(entry)
                                        .getOrNull()?.webp300
                                }
                            else
                                webp300
                        }

                        is Track -> semaphore.withPermit {
                            delay(delayMs)
                            Requesters.lastfmUnauthedRequester.getInfo(entry)
                                .getOrNull()?.webp300
                        }
                    }
                    musicEntryCache.put(key, Optional(fetchedImage))
                } catch (e: Exception) {
                    throw e
                }
            }
        }

        val request = ImageRequest.Builder(chain.request)
            .data(fetchedImage ?: "")
            .allowHardware(false)
            .build()

        return chain.proceed(request)
    }

    private fun genKey(entry: MusicEntry) = when (entry) {
        is Artist -> Artist::class.java.name + entry.name
        is Album -> entry.artist!!.name + Album::class.java.name + entry.name
        is Track -> entry.artist.name + Track::class.java.name + entry.name
    }
}

class MusicEntryImageReq(
    val musicEntry: MusicEntry,
)

private class Optional<T>(val value: T?)