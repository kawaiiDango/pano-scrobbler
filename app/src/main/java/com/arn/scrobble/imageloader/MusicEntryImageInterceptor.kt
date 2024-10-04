package com.arn.scrobble.imageloader

import android.util.LruCache
import coil3.intercept.Interceptor
import coil3.request.ImageResult
import com.arn.scrobble.PlatformStuff
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.api.spotify.SpotifySearchType
import com.arn.scrobble.db.PanoDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class MusicEntryImageInterceptor : Interceptor {

    private val delayMs = 200L
    private val musicEntryCache by lazy { LruCache<String, OptionalString>(500) }
    private val semaphore = Semaphore(3)
    private val customSpotifyMappingsDao by lazy { PanoDb.db.getCustomSpotifyMappingsDao() }
    private val spotifyArtistSearchApproximate by lazy { PlatformStuff.mainPrefs.data.map { it.spotifyArtistSearchApproximate } }


    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val musicEntryImageReq =
            chain.request.data as? MusicEntryImageReq ?: return chain.proceed()
        val entry = musicEntryImageReq.musicEntry
        val key = MusicEntryReqKeyer.genKey(musicEntryImageReq)
        val cachedOptional = musicEntryCache[key]
        var fetchedImageUrl = cachedOptional?.value

        if (cachedOptional == null) {
            withContext(Dispatchers.IO) {
                fetchedImageUrl = when (entry) {
                    is Artist -> {
                        semaphore.withPermit {
                            delay(delayMs)

                            val customMapping = customSpotifyMappingsDao.searchArtist(entry.name)
                            val imageUrl = if (customMapping != null) {
                                if (customMapping.spotifyId != null) {
                                    Requesters.spotifyRequester.artist(
                                        customMapping.spotifyId
                                    ).getOrNull()?.mediumImageUrl
                                } else
                                    customMapping.fileUri
                            } else {
                                Requesters.spotifyRequester.search(
                                    entry.name,
                                    SpotifySearchType.artist,
                                    1
                                ).getOrNull()
                                    ?.artists
                                    ?.items
                                    ?.firstOrNull()
                                    ?.takeIf {
                                        if (spotifyArtistSearchApproximate.first())
                                            it.popularity != null && it.popularity > 0
                                        else
                                            it.name.equals(entry.name, ignoreCase = true)
                                    }
                                    ?.mediumImageUrl
                            }
                            imageUrl
                        }
                    }

                    is Album -> {
                        val customMapping = customSpotifyMappingsDao.searchAlbum(
                            entry.artist!!.name,
                            entry.name
                        )

                        if (customMapping != null) {
                            if (customMapping.spotifyId != null) {
                                semaphore.withPermit {
                                    delay(delayMs)
                                    Requesters.spotifyRequester.album(
                                        customMapping.spotifyId
                                    ).getOrNull()?.mediumImageUrl
                                }
                            } else
                                customMapping.fileUri
                        } else {
                            val webp300 = entry.webp300
                            val needImage = webp300 == null ||
                                    StarMapper.STAR_PATTERN in webp300

                            if (needImage && musicEntryImageReq.fetchAlbumInfoIfMissing)
                                semaphore.withPermit {
                                    delay(delayMs)
                                    Requesters.lastfmUnauthedRequester.getInfo(entry)
                                        .getOrNull()?.webp300
                                }
                            else
                                webp300
                        }
                    }

                    is Track -> {
                        val webp300 = entry.webp300
                        val needImage = webp300 == null ||
                                StarMapper.STAR_PATTERN in webp300

                        if (needImage && musicEntryImageReq.fetchAlbumInfoIfMissing)
                            semaphore.withPermit {
                                delay(delayMs)
                                Requesters.lastfmUnauthedRequester.getInfo(entry)
                                    .getOrNull()?.webp300
                            }
                        else
                            webp300
                    }
                }
                musicEntryCache.put(key, OptionalString(fetchedImageUrl))
            }
        }

        if (musicEntryImageReq.isHeroImage && (entry is Album || entry is Track))
            fetchedImageUrl = fetchedImageUrl?.replace("300x300", "600x600")

        val request = chain.request.newBuilder()
            .data(fetchedImageUrl ?: "")
            .build()

        return chain.withRequest(request).proceed()
    }

    fun clearCacheForEntry(entry: MusicEntry) {
        musicEntryCache.remove(MusicEntryReqKeyer.genKey(MusicEntryImageReq(entry)))
    }

    private data class OptionalString(val value: String?)
}