package com.arn.scrobble.charts

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.api.Requesters
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Period
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toDuration
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.friends.UserCached
import com.arn.scrobble.utils.AcceptableTags
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.mapConcurrently
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.setUserFirstDayOfWeek
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

class ChartsOverviewVM : ChartsPeriodVM() {
    private val _listeningActivity = MutableStateFlow<Map<TimePeriod, Int>?>(null)
    val listeningActivity = _listeningActivity.asStateFlow()
    private val _tagCloud = MutableStateFlow<Map<String, Float>?>(null)
    val tagCloud = _tagCloud.asStateFlow()
    private val _tagCloudError = MutableSharedFlow<Throwable>(replay = 1)
    val tagCloudError = _tagCloudError.asSharedFlow()
    private val _listeningActivityVisible = MutableStateFlow(false)
    private val _tagCloudVisible = MutableStateFlow(false)
    private val _tagCloudProgress = MutableStateFlow(0.0)
    val tagCloudProgress = _tagCloudProgress.asSharedFlow()
    var tagCloudBitmap: Pair<Int, Bitmap?>? = null
    private val totalsMap = mutableMapOf(
        Stuff.TYPE_ARTISTS to 0,
        Stuff.TYPE_ALBUMS to 0,
        Stuff.TYPE_TRACKS to 0
    )

    private val entriesMap = mutableMapOf(
        Stuff.TYPE_ARTISTS to MutableStateFlow<List<MusicEntry>?>(null),
        Stuff.TYPE_ALBUMS to MutableStateFlow<List<MusicEntry>?>(null),
        Stuff.TYPE_TRACKS to MutableStateFlow<List<MusicEntry>?>(null)
    )
    private val _hasLoaded = mutableMapOf(
        Stuff.TYPE_ARTISTS to MutableStateFlow(false),
        Stuff.TYPE_ALBUMS to MutableStateFlow(false),
        Stuff.TYPE_TRACKS to MutableStateFlow(false),
    )
    private val _listeningActivityHasLoaded = MutableStateFlow(false)
    val listeningActivityHasLoaded = _listeningActivityHasLoaded.asStateFlow()

    val allHaveLoaded = combine(_hasLoaded.values) { it.all { it } }

    init {
        viewModelScope.launch {
            selectedPeriod.collectLatest {
                resetRequestedState()
            }
        }

        viewModelScope.launch {
            entriesMap[Stuff.TYPE_ARTISTS]!!
                .filterNotNull()
                .combine(_listeningActivityVisible) { entries, visible ->
                    entries to visible
                }.collectLatest { (entries, visible) ->
                    if (!visible || _listeningActivity.value != null)
                        return@collectLatest

                    loadListeningActivity(
                        input.value!!.user, entries, selectedPeriod.value
                    )
                }
        }

        viewModelScope.launch {
            entriesMap[Stuff.TYPE_ARTISTS]!!
                .filterNotNull()
                .combine(_tagCloudVisible) { entries, visible ->
                    entries to visible
                }

                .collectLatest { (entries, visible) ->
                    if (!visible || _tagCloud.value != null)
                        return@collectLatest

                    _tagCloudProgress.emit(0.0)
                    try {
                        _tagCloud.emit(loadTagCloud(entries))
                    } catch (e: Exception) {
                        _tagCloudError.emit(e)
                    } finally {
                        _tagCloudProgress.emit(1.0)
                    }
                }
        }
    }

    fun getTotal(type: Int) = totalsMap[type]!!

    fun getEntries(type: Int) = entriesMap[type]!!.asStateFlow()

    fun getHasLoaded(type: Int) = _hasLoaded[type]!!.asStateFlow()

    override suspend fun loadCharts(
        type: Int,
        page: Int,
        timePeriod: TimePeriod,
        username: String,
        networkOnly: Boolean
    ) {
        var prevPeriod: TimePeriod? = null

        if (periodType.value != TimePeriodType.CONTINUOUS) {
            timePeriods.value.inverse[timePeriod]?.let { idx ->
                prevPeriod = timePeriods.value[idx + 1]
            }
        } else {
            if (timePeriod.period != null && timePeriod.period != Period.OVERALL) {
                val cal = Calendar.getInstance()
                cal.setUserFirstDayOfWeek()
                cal.setMidnight()
                val duration = timePeriod.period.toDuration(endTime = cal.timeInMillis)
                prevPeriod =
                    timePeriod.period.toTimePeriod(endTime = cal.timeInMillis - duration)
            }
        }


        listOf(
            Stuff.TYPE_ARTISTS,
            Stuff.TYPE_ALBUMS,
            Stuff.TYPE_TRACKS,
        ).mapConcurrently(3) { type ->

            _hasLoaded[type]!!.emit(false)

            val result = Scrobblables.current!!
                .getChartsWithStonks(
                    type = type,
                    timePeriod = timePeriod,
                    prevTimePeriod = prevPeriod,
                    page = page,
                    username = username,
                    networkOnly = networkOnly
                )

            result.onSuccess {
                it.attr.total?.let { totalsMap[type] = it }
            }

            _hasLoaded[type]!!.emit(true)

            emitEntries(entriesMap[type]!!, result, page > 1)
        }

    }

    private suspend fun loadListeningActivity(
        user: UserCached,
        artists: List<MusicEntry>,
        timePeriod: TimePeriod
    ) {

        _listeningActivityHasLoaded.emit(false)

        if (artists.isEmpty()) {
            _listeningActivity.emit(emptyMap())
            _listeningActivityHasLoaded.emit(true)
            return
        }

        val la = Scrobblables.current
            ?.getListeningActivity(timePeriod, user)

        _listeningActivityHasLoaded.emit(true)

        la?.let {
            if (it.all { it.value == 0 })
                _listeningActivity.emit(emptyMap())
            else
                _listeningActivity.emit(it)
        }
    }

    private suspend fun loadTagCloud(artists: List<MusicEntry>): Map<String, Float> {
        val nArtists = 30
        val minArtists = 10
        val nTags = 65
        val minTags = 20
        val nParallel = 2
        val tags = mutableMapOf<String, TagStats>()
        var currentIndex = 0
        val tagScales = mutableMapOf<String, Float>()

        if (artists.size < minArtists) {
            throw IllegalStateException("Not enough artists")
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

            _tagCloudProgress.emit(
                (++currentIndex).toDouble() / min(
                    nArtists,
                    artists.size
                )
            )
        }

        if (tags.size < minTags) {
            throw IllegalStateException("Not enough tags")
        }

        val topTags = tags
            .toList()
            .filter { AcceptableTags.isAcceptable(it.first) }
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

        return tagScales.toMap()
    }

    fun setListeningActivityVisible(visible: Boolean) {
        _listeningActivityVisible.value = visible
    }

    fun setTagCloudVisible(visible: Boolean) {
        _tagCloudVisible.value = visible
    }

    private fun resetRequestedState() {
        viewModelScope.launch {
            _listeningActivity.emit(null)
            _tagCloud.emit(null)
        }
        tagCloudBitmap = null
    }

    class TagStats {
        var score = 0.0
        var artists = 0
        var percentSum = 0
    }
}