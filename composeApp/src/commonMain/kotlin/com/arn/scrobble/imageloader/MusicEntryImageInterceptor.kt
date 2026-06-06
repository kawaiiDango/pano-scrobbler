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
import kotlin.time.Duration.Companion.milliseconds

class MusicEntryImageInterceptor : Interceptor {

    private val delayMs = 400L
    private val musicEntryCache by lazy { LruCache<String, FetchedImageUrls>(500) }
    private val semaphore = Semaphore(1)
    private val customSpotifyMappingsDao by lazy { PanoDb.db.getCustomSpotifyMappingsDao() }
    private val spotifyArtistSearchApproximate by lazy { PlatformStuff.mainPrefs.data.map { it.spotifyArtistSearchApproximate } }
    private val useSpotify by lazy { PlatformStuff.mainPrefs.data.map { it.spotifyApi } }

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val musicEntryImageReq =
            chain.request.data as? MusicEntryImageReq ?: return chain.proceed()

        val fetchedImageUrls = resolveUrls(musicEntryImageReq)

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
                    if (httpException != null && httpException.response.code >= 400) {
                        val key = MusicEntryReqKeyer.genKey(musicEntryImageReq)
                        musicEntryCache.put(key, FetchedImageUrls(null))
                    }
                }
            )
            .build()

        return chain.withRequest(request).proceed()
    }

    suspend fun resolveUrls(musicEntryImageReq: MusicEntryImageReq): FetchedImageUrls? {
        suspend fun <T> networkCallWithSemaphore(block: suspend () -> Result<T>): Result<T>? {
            return if (!musicEntryImageReq.allowNetwork) null
            else
                semaphore.withPermit {
                    delay(delayMs.milliseconds)
                    block()
                }
        }

        suspend fun customAlbumArtMapping(
            artistName: String,
            albumName: String
        ): FetchedImageUrls? {
            val customMapping = customSpotifyMappingsDao.searchAlbum(artistName, albumName)

            return if (customMapping != null && customMapping.fileUri != null)
                FetchedImageUrls(customMapping.fileUri)
            else if (customMapping != null && useSpotify.first() && customMapping.spotifyId != null) {
                networkCallWithSemaphore {
                    Requesters.spotifyRequester.album(
                        customMapping.spotifyId
                    )
                }?.map {
                    FetchedImageUrls(it.mediumImageUrl, it.largeImageUrl)
                }?.recover { FetchedImageUrls(null) }
                    ?.getOrNull()
            } else {
                null
            }
        }

        val entry = musicEntryImageReq.musicEntry
        val key = MusicEntryReqKeyer.genKey(musicEntryImageReq)
        val cachedOptional = musicEntryCache[key]

        if (cachedOptional != null) {
            return cachedOptional
        }

        val fetchedImageUrls = withContext(Dispatchers.IO) {
            when (entry) {
                is Artist -> {
                    val customMapping = customSpotifyMappingsDao.searchArtist(entry.name)
                    val imageUrls = if (customMapping != null) {
                        if (customMapping.spotifyId == null) {
                            FetchedImageUrls(customMapping.fileUri)
                        } else if (useSpotify.first()) {
                            networkCallWithSemaphore {
                                Requesters.spotifyRequester.artist(customMapping.spotifyId)
                            }?.map {
                                FetchedImageUrls(it.mediumImageUrl, it.largeImageUrl)
                            }?.recover {
                                FetchedImageUrls(null)
                            }?.getOrNull()
                        } else {
                            FetchedImageUrls(null)
                        }
                    } else if (useSpotify.first()) {
                        val spotifyArtists = networkCallWithSemaphore {
                            Requesters.spotifyRequester.search(
                                entry.name,
                                SpotifySearchType.artist,
                                market = PlatformStuff.mainPrefs.data.map { it.spotifyCountryP }
                                    .first(),
                                limit = 3
                            )
                        }
                            ?.map { it.artists?.items.orEmpty() }
                            ?.recover { emptyList() }
                            ?.getOrNull()

                        val caseInsensitiveMatch = spotifyArtists?.find {
                            it.name.equals(entry.name, ignoreCase = true) &&
                                    !it.images.isNullOrEmpty()
                        }

                        val approximateMatch = spotifyArtists?.firstOrNull()

                        val artistItem = if (spotifyArtistSearchApproximate.first())
                            caseInsensitiveMatch ?: approximateMatch
                        else
                            caseInsensitiveMatch

                        if (artistItem != null)
                            FetchedImageUrls(artistItem.mediumImageUrl, artistItem.largeImageUrl)
                        else if (spotifyArtists != null)
                            FetchedImageUrls(null)
                        else
                            null
                    } else {
                        null
                    }
                    imageUrls
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
                    var fetchedAlbumImageUrls: FetchedImageUrls? = null

                    val customMappingUrls = if (album != null && artist != null) {
                        customAlbumArtMapping(artist.name, album.name)
                    } else {
                        null
                    }

                    if (customMappingUrls != null) {
                        fetchedAlbumImageUrls = customMappingUrls
                    } else {
                        val needFetch = album?.webp300 == null ||
                                album.webp300?.contains(StarMapper.STAR_PATTERN) == true

                        if (!needFetch) {
                            fetchedAlbumImageUrls = FetchedImageUrls(album.webp300)
                        } else if (musicEntryImageReq.fetchAlbumInfoIfMissing &&
                            musicEntryImageReq.accountType == AccountType.LASTFM
                        ) {
                            val dao = PanoDb.db.getSeenEntitiesDao()
                            val seenAlbum = when (entry) {
                                is Album -> {
                                    dao.getAlbumWithFetchedArt(
                                        entry.artist!!.name,
                                        entry.name
                                    )
                                }

                                is Track -> {
                                    val bestAlbums = dao.getBestAlbumsForTrack(
                                        entry.artist.name,
                                        entry.name
                                    )

                                    val bestAlbumWithArt =
                                        bestAlbums.firstOrNull { it.artUpdatedAt != null }

                                    bestAlbumWithArt ?: bestAlbums.firstOrNull()
                                }
                            }

                            if (seenAlbum != null)
                                fetchedAlbumImageUrls = FetchedImageUrls(seenAlbum.artUrl)

                            // if the image from cache was still a placeholder, don't do a lookup

                            when (entry) {
                                is Album -> {
                                    if (seenAlbum == null) {
                                        fetchedAlbumImageUrls = networkCallWithSemaphore {
                                            Requesters.lastfmUnauthedRequester.getAlbumInfo(entry)
                                        }?.map {
                                            FetchedImageUrls(it.webp300)
                                        }?.recover {
                                            FetchedImageUrls(null)
                                        }?.getOrNull()
                                    }
                                }

                                is Track -> {
                                    if (seenAlbum == null) {
                                        val t = dao.getTrack(
                                            entry.artist.name,
                                            entry.name
                                        )
                                        fetchedAlbumImageUrls = if (t?.trackInfoFetchedAt == null) {
                                            networkCallWithSemaphore {
                                                Requesters.lastfmUnauthedRequester.getTrackInfo(
                                                    entry
                                                )
                                            }?.map {
                                                FetchedImageUrls(it.album?.webp300)
                                            }?.recover {
                                                FetchedImageUrls(null)
                                            }?.getOrNull()
                                        } else
                                            FetchedImageUrls(null)
                                    } else {
                                        val customMapping = customAlbumArtMapping(
                                            seenAlbum.artist,
                                            seenAlbum.album
                                        )

                                        if (customMapping != null) {
                                            fetchedAlbumImageUrls = customMapping
                                        } else {
                                            if (seenAlbum.artUpdatedAt == null) {
                                                fetchedAlbumImageUrls = networkCallWithSemaphore {
                                                    Requesters.lastfmUnauthedRequester.getAlbumInfo(
                                                        Album(
                                                            seenAlbum.album,
                                                            Artist(entry.artist.name)
                                                        )
                                                    )
                                                }?.map {
                                                    FetchedImageUrls(it.webp300)
                                                }?.recover {
                                                    FetchedImageUrls(null)
                                                }?.getOrNull()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    fetchedAlbumImageUrls
                }
            }
        }

        if (fetchedImageUrls != null)
            musicEntryCache.put(key, fetchedImageUrls)

        return fetchedImageUrls
    }

    fun clearCacheForEntry(req: MusicEntryImageReq) {
        val key = MusicEntryReqKeyer.genKey(req)
        musicEntryCache.remove(key)

        if (req.musicEntry is Album) {
            // remove all tracks of the album

            musicEntryCache.snapshot().keys
                .filter { it.startsWith(key) }
                .forEach { musicEntryCache.remove(it) }
        }
    }

    data class FetchedImageUrls(val mediumImage: String?, val largeImage: String?) {
        constructor(webp300: String?) : this(
            webp300,
            webp300
                ?.takeIf { it.startsWith("https://lastfm.freetls.fastly.net") }
                ?.replace("300x300", "600x600")
        )
    }
}