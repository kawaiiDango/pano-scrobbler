package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.utils.AcceptableTags
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.math.log10
import kotlin.math.pow

class ChartsVM(
    user: UserCached,
    firstPageOnly: Boolean
) : ViewModel() {

    private val _input = MutableStateFlow<ChartsLoaderInput?>(null)
    private val _inputDebounced = _input.debounce(500)
        .filterNotNull()

    private val pagingConfig = PagingConfig(
        pageSize = 50,
        enablePlaceholders = true,
        initialLoadSize = 50,
        prefetchDistance = 4
    )

    private val _firstPageArtists = MutableStateFlow(emptyList<MusicEntry>())
    private val _artistCount = MutableStateFlow(0)
    val artistCount = _artistCount.asStateFlow()
    val artists = _inputDebounced.flatMapLatest { input ->
        _firstPageArtists.value = emptyList()

        Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                ChartsPagingSource(
                    user.name,
                    firstPageOnly,
                    input,
                    Stuff.TYPE_ARTISTS,
                    networkOnly = input.refreshCount > 0,
                    onFirstPage = { _firstPageArtists.value = it },  // ADD THIS
                ) { _artistCount.value = it }
            }
        ).flow
    }
        .cachedIn(viewModelScope)

    private val _albumCount = MutableStateFlow(0)
    val albumCount = _albumCount.asStateFlow()
    val albums = _inputDebounced.flatMapLatest { input ->
        Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                ChartsPagingSource(
                    user.name,
                    firstPageOnly,
                    input,
                    Stuff.TYPE_ALBUMS,
                    networkOnly = input.refreshCount > 0,
                ) { _albumCount.value = it }
            }
        ).flow
    }
        .cachedIn(viewModelScope)

    private val _trackCount = MutableStateFlow(0)
    val trackCount = _trackCount.asStateFlow()
    val tracks = _inputDebounced.flatMapLatest { input ->
        Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                ChartsPagingSource(
                    user.name,
                    firstPageOnly,
                    input,
                    Stuff.TYPE_TRACKS,
                    networkOnly = input.refreshCount > 0,
                ) { _trackCount.value = it }
            }
        ).flow
    }
        .cachedIn(viewModelScope)

    private val tagCloudVisible = MutableStateFlow(false)
    private val listeningActivityVisible = MutableStateFlow(false)

    val tagCloud = _firstPageArtists
        .flatMapLatest { artists ->
            flow {
                emit(null) // reset to "not loaded" state on change
                tagCloudVisible.first { it } // don't even start until visible
                emit(loadTagCloud(artists, tagCloudVisible))
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val listeningActivity = combine(
        _inputDebounced,
        _firstPageArtists
    ) { input, artists ->
        input.timePeriod to artists
    }
        .flatMapLatest { (timePeriod, artists) ->
            flow {
                emit(null) // reset to "not loaded" state on change
                listeningActivityVisible.first { it } // don't even start until visible
                val la = if (artists.isEmpty())
                    ListeningActivity()
                else
                    loadListeningActivity(user, timePeriod)
                emit(la)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val scrobblesCount = _inputDebounced.mapLatest { input ->
        if (firstPageOnly && Scrobblables.current?.userAccount?.type == AccountType.LASTFM) {
            val from = input.timePeriod.lastfmPeriod
                ?.toTimePeriod()
                ?.start ?: input.timePeriod.start


            Scrobblables.current
                ?.getRecents(
                    1,
                    user.name,
                    from = from,
                    limit = 1
                )
                ?.getOrNull()?.attr?.total ?: 0
        } else
            0
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun setChartsInput(input: ChartsLoaderInput) {
        _input.value = input
    }

    private suspend fun loadListeningActivity(
        user: UserCached,
        timePeriod: TimePeriod,
    ): ListeningActivity {
        val la = Scrobblables.current
            ?.getListeningActivity(timePeriod, user)
            ?: ListeningActivity()

        return if (la.timePeriodsToCounts.all { it.value == 0 })
            ListeningActivity()
        else
            la
    }

    fun setListeningActivityVisible(visible: Boolean) {
        listeningActivityVisible.value = visible
    }

    private suspend fun loadTagCloud(
        artists: List<MusicEntry>,
        visibilityFlow: StateFlow<Boolean>
    ): Map<String, Float> {
        val nArtists = 30
        val minArtists = 10
        val nTags = 65
        val minTags = 20

        // not enough artists
        if (artists.size < minArtists) {
            return emptyMap()
        }
        val tags = mutableMapOf<String, TagStats>()
        val tagScales = mutableMapOf<String, Float>()

        for (artist in artists.take(nArtists)) {
            visibilityFlow.first { it } // ← suspends here if scrolled away; resumes when visible again

            val t1 = System.currentTimeMillis()
            Requesters.lastfmUnauthedRequester
                .getTopTags(artist)
                .map { it.toptags.tag }
                .onSuccess {
                    it.forEach {
                        val s = artist.playcount!!.toDouble() * it.count!! / 100
                        val name = it.name.trim()
                        synchronized(tags) {
                            if (!tags.containsKey(name))
                                tags[name] = TagStats()
                            tags[name]!!.score += s
                            tags[name]!!.artists += 1
                            tags[name]!!.percentSum += it.count
                        }
                    }
                }
                .onFailure {
                    // cancel all other requests
                    break
                }
            val t2 = System.currentTimeMillis()

            if (t2 - t1 > 50) { // probably not from cache
                delay(400)
            }
//            _tagCloudProgress.value = (++currentIndex).toFloat() / min(nArtists, artists.size)
        }

        // not enough tags
        if (tags.size < minTags) {
            return emptyMap()
        }

        val hiddenTags = PlatformStuff.mainPrefs.data.map { it.hiddenTags }.first()

        val topTags = tags
            .toList()
            .filter { AcceptableTags.isAcceptable(it.first, hiddenTags) }
            .sortedByDescending { it.second.score }
            .take(nTags)
            .toMap()

        topTags.forEach { (_, stats) ->
            stats.score = stats.score.pow(3.0)
        }
        val scoresList = topTags.map { it.value.score }
        val minScore = scoresList.minOrNull()!!
        val maxScore = scoresList.maxOrNull()!!
        topTags.forEach { (tag, stats) ->
            tagScales[tag] =
                (log10((stats.score - minScore) * 99 / maxScore + 1) / 2 * 50).toFloat()// + 10
//            tagScales[tag] = log10(stats.score)
            //in sp
        }
        return tagScales
    }

    fun setTagCloudVisible(visible: Boolean) {
        tagCloudVisible.value = visible
    }

    private data class TagStats(
        var score: Double = 0.0,
        var artists: Int = 0,
        var percentSum: Long = 0,
    )
}