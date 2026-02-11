package com.arn.scrobble.imageloader

import androidx.collection.LruCache
import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ImageResult
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.api.spotify.SpotifySearchType
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.utils.PlatformStuff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class MusicEntryImageInterceptor : Interceptor {

    private val delayMs = 400L
    private val musicEntryCache by lazy { LruCache<String, FetchedImageUrls>(500) }
    private val semaphore = Semaphore(2)
    private val customSpotifyMappingsDao by lazy { PanoDb.db.getCustomSpotifyMappingsDao() }
    private val spotifyArtistSearchApproximate by lazy { PlatformStuff.mainPrefs.data.map { it.spotifyArtistSearchApproximate } }
    private val useSpotify by lazy { PlatformStuff.mainPrefs.data.map { it.spotifyApi } }


    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val musicEntryImageReq =
            chain.request.data as? MusicEntryImageReq ?: return chain.proceed()
        val entry = musicEntryImageReq.musicEntry
        val key = MusicEntryReqKeyer.genKey(musicEntryImageReq)
        val cachedOptional = musicEntryCache[key]

        var fetchedImageUrls = cachedOptional

        if (cachedOptional == null) {
            withContext(Dispatchers.IO) {
                fetchedImageUrls = when (entry) {
                    is Artist -> {
                        semaphore.withPermit {
                            delay(delayMs)

                            val customMapping = customSpotifyMappingsDao.searchArtist(entry.name)
                            val imageUrls = if (customMapping != null) {
                                if (customMapping.spotifyId == null) {
                                    FetchedImageUrls(customMapping.fileUri)
                                } else if (useSpotify.first()) {
                                    Requesters.spotifyRequester.artist(
                                        customMapping.spotifyId
                                    ).getOrNull()?.let {
                                        FetchedImageUrls(it.mediumImageUrl, it.largeImageUrl)
                                    }
                                } else {
                                    FetchedImageUrls(null)
                                }
                            } else if (useSpotify.first()) {
                                val spotifyArtists = Requesters.spotifyRequester.search(
                                    entry.name,
                                    SpotifySearchType.artist,
                                    market = PlatformStuff.mainPrefs.data.map { it.spotifyCountryP }
                                        .first(),
                                    limit = 3
                                ).getOrNull()
                                    ?.artists
                                    ?.items

                                val caseInsensitiveMatch = spotifyArtists?.find {
                                    it.name.equals(entry.name, ignoreCase = true) &&
                                            !it.images.isNullOrEmpty()
                                }

                                val approximateMatch = spotifyArtists?.firstOrNull()

                                val artistItem = if (spotifyArtistSearchApproximate.first())
                                    caseInsensitiveMatch ?: approximateMatch
                                else
                                    caseInsensitiveMatch

                                artistItem?.let {
                                    FetchedImageUrls(it.mediumImageUrl, it.largeImageUrl)
                                }
                            } else {
                                null
                            }
                            imageUrls
                        }
                    }

                    is Album,
                    is Track -> {
                        val album = when (entry) {
                            is Album -> entry
                            is Track -> entry.album
                        }

                        val artist = when (entry) {
                            is Track -> entry.album?.artist ?: entry.artist
                            is Album -> entry.artist
                        }

                        val customMapping = if (album != null && artist != null) {
                            customSpotifyMappingsDao.searchAlbum(
                                artist.name,
                                album.name
                            )
                        } else {
                            null
                        }

                        if (customMapping != null) {
                            if (customMapping.spotifyId == null)
                                FetchedImageUrls(customMapping.fileUri)
                            else if (useSpotify.first()) {
                                semaphore.withPermit {
                                    delay(delayMs)
                                    Requesters.spotifyRequester.album(
                                        customMapping.spotifyId
                                    ).getOrNull()?.let {
                                        FetchedImageUrls(it.mediumImageUrl, it.largeImageUrl)
                                    }
                                }
                            } else {
                                FetchedImageUrls(null)
                            }
                        } else {
                            var webp300 = album?.webp300
                            val needImage = webp300 == null ||
                                    StarMapper.STAR_PATTERN in webp300

                            if (needImage && musicEntryImageReq.fetchAlbumInfoIfMissing &&
                                musicEntryImageReq.accountType == AccountType.LASTFM
                            ) {
                                semaphore.withPermit {
                                    delay(delayMs)
                                    val albumOrTrack =
                                        Requesters.lastfmUnauthedRequester.getInfo(entry)
                                            .getOrNull()

                                    webp300 = when (albumOrTrack) {
                                        is Album -> albumOrTrack.webp300
                                        is Track -> albumOrTrack.album?.webp300
                                        else -> null
                                    }
                                }
                            }

                            if (!webp300.isNullOrEmpty()) {
                                FetchedImageUrls(
                                    webp300,
                                    webp300
                                        .takeIf { it.startsWith("https://lastfm.freetls.fastly.net") }
                                        ?.replace("300x300", "600x600")
                                )
                            } else {
                                FetchedImageUrls(null)
                            }
                        }
                    }
                }
                musicEntryCache.put(key, fetchedImageUrls ?: FetchedImageUrls(null))
            }
        }

        val fetchedImageUrl = if (musicEntryImageReq.isHeroImage) {
            fetchedImageUrls?.largeImage ?: fetchedImageUrls?.mediumImage
        } else {
            fetchedImageUrls?.mediumImage
        }

        val request = chain.request.newBuilder()
            .data(fetchedImageUrl ?: "")
            .listener(
                onError = { req, err ->
                    // cache the errors too, to avoid repeated failed fetches
                    // e.g. coil3.network.HttpException: HTTP 404
                    val httpException = err.throwable as? HttpException
                    if (httpException != null && httpException.response.code >= 400)
                        musicEntryCache.put(key, FetchedImageUrls(null))
                }
            )
            .build()

        return chain.withRequest(request).proceed()
    }

    fun clearCacheForEntry(req: MusicEntryImageReq) {
        musicEntryCache.remove(MusicEntryReqKeyer.genKey(req))
    }

    private data class FetchedImageUrls(val mediumImage: String?, val largeImage: String? = null)
}