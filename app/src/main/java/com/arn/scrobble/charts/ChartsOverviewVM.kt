package com.arn.scrobble.charts

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.arn.scrobble.App
import com.arn.scrobble.R
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow

class ChartsOverviewVM : BaseChartsVM() {
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded = _hasLoaded.asStateFlow()
    private val _listeningActivity = MutableStateFlow<Map<TimePeriod, Int>?>(null)
    val listeningActivity = _listeningActivity.asStateFlow()
    private val _tagCloud = MutableStateFlow<Map<String, Float>?>(null)
    val tagCloud = _tagCloud.asStateFlow()
    private val _tagCloudError = MutableSharedFlow<Throwable>()
    val tagCloudError = _tagCloudError.asSharedFlow()
    var listeningActivityRequested = false
    var tagCloudRequested = false
    private val _tagCloudProgress = MutableSharedFlow<Double>()
    val tagCloudProgress = _tagCloudProgress.asSharedFlow()
    private val _listeningActivityHeader =
        MutableStateFlow(App.context.getString(R.string.listening_activity))
    val listeningActivityHeader = _listeningActivityHeader.asSharedFlow()
    private var lastListeningActivityJob: Job? = null
    var tagCloudBitmap: Pair<Int, Bitmap?>? = null
    private var lastTagCloudTask: Job? = null
    private var lastListeningActivityTask: Job? = null
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

    init {
        input.filterNotNull().onEach {
            resetRequestedState()
        }.launchIn(viewModelScope)
    }

    fun getTotal(type: Int) = totalsMap[type]!!

    fun getEntries(type: Int) = entriesMap[type]!!.asStateFlow()

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

        _hasLoaded.emit(false)

        listOf(
            Stuff.TYPE_ARTISTS,
            Stuff.TYPE_ALBUMS,
            Stuff.TYPE_TRACKS,
        ).mapConcurrently(3) { type ->
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

            _hasLoaded.emit(true)
            emitEntries(entriesMap[type]!!, result, page > 1)
        }

    }

    fun loadListeningActivity(user: UserCached, timePeriod: TimePeriod) {
        lastListeningActivityJob?.cancel()
        lastListeningActivityJob =
            viewModelScope.launch {
                if (getTotal(Stuff.TYPE_ARTISTS) == 0)
                    _listeningActivity.emit(emptyMap())

                Scrobblables.current
                    ?.getListeningActivity(timePeriod, user)
                    ?.let {
                        _listeningActivity.emit(it)
                    }
            }
    }


    fun loadTagCloud() {
        val artists = getEntries(Stuff.TYPE_ARTISTS).value

        if (artists.isNullOrEmpty())
            return

        tagCloudRequested = true
        lastTagCloudTask?.cancel()
        lastTagCloudTask = viewModelScope.launch {

            _tagCloudProgress.emit(0.0)

            val nArtists = 30
            val minArtists = 5
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

            _tagCloud.emit(tagScales.toMap())
        }
    }

    private fun resetRequestedState() {
        viewModelScope.launch {
            _listeningActivity.emit(null)
            _tagCloud.emit(null)
        }
        tagCloudBitmap = null
        listeningActivityRequested = false
        tagCloudRequested = false
        lastTagCloudTask?.cancel()
        lastListeningActivityTask?.cancel()
    }

    class TagStats {
        var score = 0.0
        var artists = 0
        var percentSum = 0
    }
}