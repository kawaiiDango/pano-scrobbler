package com.arn.scrobble.recents

import androidx.paging.PagingSource
import androidx.paging.PagingState
import co.touchlab.kermit.Logger
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastFm
import com.arn.scrobble.api.lastfm.PageResult
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.listenbrainz.ListenBrainz
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.ScrobbleSource
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.TreeMap
import kotlin.math.abs

class ScrobblesPagingSource(
    private val username: String,
    private val loadLovedTracks: Boolean,
    private val timeJumpMillis: Long?,
    private val track: Track?,
    private val cachedOnly: Boolean,
    private val addLastScrobbleOfTheDay: (Long) -> Unit,
    private val addToPkgMap: (Long, String) -> Unit,
    private val onSetFirstScrobbleTime: (Long) -> Unit,
    private val onSetLastRecentsRefreshTime: (Long) -> Unit,
    private val onClearOverrides: () -> Unit,
) : PagingSource<ScrobblesLoaderPage, Track>() {

    private val limit = Stuff.DEFAULT_PAGE_SIZE
    private val cal = Calendar.getInstance()
    private val scrobbleSourcesDao = PanoDb.db.getScrobbleSourcesDao()

    override suspend fun load(params: LoadParams<ScrobblesLoaderPage>): LoadResult<ScrobblesLoaderPage, Track> {
        val page = params.key?.page ?: 1
        val lastScrobbleTimestamp = params.key?.lastScrobbleTimestamp

        val includeNowPlaying = timeJumpMillis == null && page == 1
        val shouldMarkFirstScrobbleOfTheDays = page == 1 && track == null && !loadLovedTracks
        val currentScrobblable = Scrobblables.current

        val result = when {
            track != null -> {
                (currentScrobblable as? LastFm)
                    ?.userGetTrackScrobbles(track, page, username, limit)
            }

            loadLovedTracks -> {
                currentScrobblable?.getLoves(page, username)
            }

            else -> {
                if (page == 1)
                    onSetLastRecentsRefreshTime(System.currentTimeMillis())

                val isListenBrainz = currentScrobblable is ListenBrainz

                Logger.d { "load page $page cached: $cachedOnly" }
                currentScrobblable?.getRecents(
                    page,
                    username,
                    cached = cachedOnly,
                    to = if (isListenBrainz)
                        (lastScrobbleTimestamp ?: -1)
                    else
                        (timeJumpMillis ?: -1),
                    includeNowPlaying = includeNowPlaying,
                    limit = limit
                )
            }
        }

        return if (result?.isSuccess == true) {
            val pr = result.getOrThrow()
            // all entries should be older than lastScrobbleTimestamp
            val entries = if (lastScrobbleTimestamp != null)
                pr.entries.filter { (it.date ?: 0) < lastScrobbleTimestamp }
            else
                pr.entries


            val prevPage = if (pr.attr.page <= 1) null else pr.attr.page - 1
            val nextPage = if (pr.attr.totalPages <= pr.attr.page) null else pr.attr.page + 1
            val total = pr.attr.total ?: 0

            if (page == 1)
                onClearOverrides()

            if (shouldMarkFirstScrobbleOfTheDays)
                markFirstScrobbleOfTheDays(entries)

            if (
                PlatformStuff.billingRepository.isLicenseValid &&
                PlatformStuff.mainPrefs.data.map { it.showScrobbleSources }.first()
            ) {
                val earliest = entries.lastOrNull()?.date
                val latest = entries.find { it.date != null }?.date
                if (earliest != null && latest != null) {
                    val scrobbleSources = scrobbleSourcesDao.selectBetween(
                        earliest - Stuff.SCROBBLE_SOURCE_THRESHOLD,
                        latest + Stuff.SCROBBLE_SOURCE_THRESHOLD
                    )

                    val treeMap = TreeMap<Long, ScrobbleSource>()
                    for (source in scrobbleSources) {
                        treeMap[source.timeMillis] = source
                    }

                    entries.forEach { track ->
                        if (track.date != null) {
                            val source = findClosestScrobbleSource(treeMap, track.date)
                            if (source != null) {
                                addToPkgMap(track.date, source.pkg)
                            }
                        }
                    }
                }
            }

            if (track != null) {
                val firstScrobbleTime = loadFirstScrobbleTime(track, pr, total)
                firstScrobbleTime?.let { onSetFirstScrobbleTime(it) }
            }

            LoadResult.Page(
                data = entries,
                prevKey = prevPage?.let { ScrobblesLoaderPage(it, null) },
                nextKey = nextPage?.let { ScrobblesLoaderPage(it, entries.lastOrNull()?.date) },
                itemsAfter = if (nextPage == null) 0 else 2
            )
        } else {
            val exception = result?.exceptionOrNull() ?: IllegalStateException("Not logged in")
            if (cachedOnly)
                LoadResult.Page(emptyList(), null, null)
            else
                LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<ScrobblesLoaderPage, Track>): ScrobblesLoaderPage {
        return ScrobblesLoaderPage(
            page = 1,
            lastScrobbleTimestamp = null
        )
    }

    private suspend fun loadFirstScrobbleTime(
        track: Track,
        pr: PageResult<Track>,
        total: Int,
    ): Long? {
        return if (total > pr.entries.size)
            (Scrobblables.current as? LastFm)
                ?.userGetTrackScrobbles(
                    track = track,
                    username = username,
                    page = total,
                    limit = 1,
                )
                ?.getOrNull()
                ?.entries
                ?.firstOrNull()
                ?.date
        else
            pr.entries.lastOrNull()?.date
    }

    private fun findClosestScrobbleSource(
        treeMap: TreeMap<Long, ScrobbleSource>,
        targetTime: Long,
        threshold: Long = Stuff.SCROBBLE_SOURCE_THRESHOLD,
    ): ScrobbleSource? {
        val floorEntry = treeMap.floorEntry(targetTime)
        val ceilingEntry = treeMap.ceilingEntry(targetTime)

        val closestEntry = when {
            floorEntry == null -> ceilingEntry
            ceilingEntry == null -> floorEntry
            else -> {
                val floorDiff = abs(floorEntry.key - targetTime)
                val ceilingDiff = abs(ceilingEntry.key - targetTime)
                if (floorDiff <= ceilingDiff) floorEntry else ceilingEntry
            }
        }

        return if (closestEntry != null && abs(closestEntry.key - targetTime) < threshold) {
            closestEntry.value
        } else {
            null
        }
    }

    private fun markFirstScrobbleOfTheDays(tracks: List<Track>) {
        var prevDate = -1
        // dont mark the first scrobble
        val minIdx = if (tracks.firstOrNull()?.isNowPlaying == true) 1 else 0

        tracks.forEachIndexed { idx, track ->
            if (track.date == null)
                return@forEachIndexed

            cal.timeInMillis = track.date
            val currentDate = cal[Calendar.DAY_OF_YEAR]


            if (prevDate != currentDate && idx > minIdx)
                addLastScrobbleOfTheDay(track.date)
            prevDate = currentDate
        }
    }
}