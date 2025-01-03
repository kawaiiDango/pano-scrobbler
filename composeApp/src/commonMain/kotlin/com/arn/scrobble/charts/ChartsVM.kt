package com.arn.scrobble.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.UserCached
import com.arn.scrobble.utils.AcceptableTags
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.pow

class ChartsVM : ViewModel() {

    private val _input = MutableStateFlow<ChartsLoaderInput?>(null)
    private val _inputDebounced = _input.debounce(300).filterNotNull()

    private val pagingConfig = PagingConfig(
        pageSize = 50,
        enablePlaceholders = true,
        initialLoadSize = 50,
        prefetchDistance = 4
    )

    private val _artistCount = MutableStateFlow(0)
    val artistCount = _artistCount.asStateFlow()
    val artists = _inputDebounced.flatMapLatest { input ->
        Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                ChartsPagingSource(
                    input,
                    Stuff.TYPE_ARTISTS
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
                    input,
                    Stuff.TYPE_ALBUMS
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
                    input,
                    Stuff.TYPE_TRACKS
                ) { _trackCount.value = it }
            }
        ).flow
    }
        .cachedIn(viewModelScope)

    private val _listeningActivity = MutableStateFlow<Map<TimePeriod, Int>?>(null)
    val listeningActivity = _listeningActivity.asStateFlow()

    private val _tagCloud = MutableStateFlow<Map<String, Float>?>(null)
    val tagCloud = _tagCloud.asStateFlow()

    fun setChartsInput(input: ChartsLoaderInput) {
        _input.value = input
    }

    fun loadListeningActivity(
        user: UserCached,
        artists: List<MusicEntry>,
        timePeriod: TimePeriod,
    ) {
        viewModelScope.launch {

            if (artists.isEmpty()) {
                _listeningActivity.value = emptyMap()
                return@launch
            }

            val la = Scrobblables.current.value
                ?.getListeningActivity(timePeriod, user)

            la?.let {
                if (it.all { it.value == 0 })
                    _listeningActivity.value = emptyMap()
                else
                    _listeningActivity.value = it
            }
        }
    }

    fun loadTagCloud(
        artists: List<MusicEntry>,
    ) {
        viewModelScope.launch {
            val nArtists = 30
            val minArtists = 10
            val nTags = 65
            val minTags = 20
            val nParallel = 2
            val tags = mutableMapOf<String, TagStats>()
            val tagScales = mutableMapOf<String, Float>()

            // not enough artists
            if (artists.size < minArtists) {
                _tagCloud.value = emptyMap()
                return@launch
            }

            artists.take(nArtists).mapConcurrently(nParallel) { musicEntry ->
                Requesters.lastfmUnauthedRequester
                    .getTopTags(musicEntry)
                    .map { it.toptags.tag }
                    .onSuccess {
                        it.forEach {
                            val s = musicEntry.playcount!!.toDouble() * it.count!! / 100
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

//            _tagCloudProgress.value = (++currentIndex).toFloat() / min(nArtists, artists.size)
            }

            // not enough tags
            if (tags.size < minTags) {
                _tagCloud.value = emptyMap()
                return@launch
            }

            val hiddenTags = PlatformStuff.mainPrefs.data.mapLatest { it.hiddenTags }.first()

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
            _tagCloud.value = tagScales
        }
    }

    fun reset() {
        _listeningActivity.value = null
        _tagCloud.value = null
    }

    private data class TagStats(
        var score: Double = 0.0,
        var artists: Int = 0,
        var percentSum: Int = 0,
    )
}