package com.arn.scrobble.work

import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.charts.TimePeriodsGenerator.Companion.toTimePeriod
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PanoTimeFormatter
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.s_top_scrobbles
import pano_scrobbler.composeapp.generated.resources.top_albums
import pano_scrobbler.composeapp.generated.resources.top_artists
import pano_scrobbler.composeapp.generated.resources.top_tracks
import pano_scrobbler.composeapp.generated.resources.weekly
import java.util.Calendar

enum class DigestType {
    DIGEST_DAILY,
    DIGEST_WEEKLY,
    DIGEST_MONTHLY,
}

class DigestWorker(
    private val digestType: DigestType,
    override val setProgress: suspend (CommonWorkProgress) -> Unit,
) : CommonWorker {

    override suspend fun doWork(): CommonWorkerResult {
        var error: Throwable? = null

        val lastfmPeriod = when (digestType) {
            DigestType.DIGEST_DAILY,
            DigestType.DIGEST_WEEKLY,
                -> LastfmPeriod.WEEK

            DigestType.DIGEST_MONTHLY -> LastfmPeriod.MONTH
        }

        val channelId = if (lastfmPeriod == LastfmPeriod.WEEK)
            Stuff.CHANNEL_NOTI_DIGEST_WEEKLY
        else
            Stuff.CHANNEL_NOTI_DIGEST_MONTHLY

        if (PanoNotifications.isNotiChannelEnabled(channelId)) {
            val coExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                throwable.printStackTrace()
                error = throwable
            }

            withContext(coExceptionHandler) {
                fetchAndNotify(lastfmPeriod)
                // yearly digest
                val cal = Calendar.getInstance()
                if (lastfmPeriod == LastfmPeriod.MONTH &&
                    (cal[Calendar.DAY_OF_YEAR] == cal.getActualMaximum(Calendar.DAY_OF_YEAR) ||
                            cal[Calendar.DAY_OF_YEAR] == 1)
                ) {
                    fetchAndNotify(LastfmPeriod.YEAR)
                }
            }

        } else {
            error = IllegalStateException("Digest notifications are disabled")
        }

        // self-schedule next digest
        val (nextWeek, nextMonth) = nextWeekAndMonth()

        DigestWork.schedule(
            nextWeek,
            nextMonth,
        )

        return if (error != null)
            CommonWorkerResult.Failure(error.redactedMessage)
        else
            CommonWorkerResult.Success
    }

    private suspend fun fetchAndNotify(lastfmPeriod: LastfmPeriod) {
        supervisorScope {
            val limit = 3
            val scrobblable = Scrobblables.current
                ?: throw IllegalStateException("Not logged in")

            val timeLastfmPeriod = TimePeriod(lastfmPeriod)

            val artists = async {
                scrobblable.getCharts(Stuff.TYPE_ARTISTS, timeLastfmPeriod, 1, limit = limit)
            }
            val albums = async {
                scrobblable.getCharts(Stuff.TYPE_ALBUMS, timeLastfmPeriod, 1, limit = limit)
            }
            val tracks = async {
                scrobblable.getCharts(Stuff.TYPE_TRACKS, timeLastfmPeriod, 1, limit = limit)
            }

            val textsList = mapOf(
                Stuff.TYPE_ARTISTS to artists,
                Stuff.TYPE_ALBUMS to albums,
                Stuff.TYPE_TRACKS to tracks
            ).mapNotNull { (type, deferred) ->
                val kResult = deferred.await()
                val result = kResult.getOrNull() ?: return@mapNotNull null
                if (result.entries.isEmpty()) return@mapNotNull null
                type to result.entries.joinToString { it.name }
            }

            // formatted SpannedString no longer works on my version of Android 16
            val notificationText = StringBuilder()
            textsList.forEachIndexed { index, (type, text) ->
                val title = when (type) {
                    Stuff.TYPE_ARTISTS -> getString(Res.string.top_artists)
                    Stuff.TYPE_ALBUMS -> getString(Res.string.top_albums)
                    Stuff.TYPE_TRACKS -> getString(Res.string.top_tracks)
                    else -> throw IllegalArgumentException("Invalid musicEntry type")
                }

                notificationText.apply {
                    if (index > 0) {
                        append('\n') // newline between sections
                    }
                    append("[$title]")
                    append("\n")
                    append(text)
                }
            }

            val periodString = lastfmPeriod.toTimePeriod().let {
                when (lastfmPeriod) {
                    LastfmPeriod.WEEK ->
                        getString(Res.string.weekly)
//                        PanoTimeFormatter.dateRange(it.start, it.end)

                    LastfmPeriod.MONTH ->
                        PanoTimeFormatter.month(it.start)

                    LastfmPeriod.YEAR ->
                        PanoTimeFormatter.year(it.start)

                    else -> throw IllegalArgumentException("Invalid period")
                }
            }

            val notificationTitle = getString(Res.string.s_top_scrobbles, periodString)

            PanoNotifications.notifyDigest(
                lastfmPeriod,
                notificationTitle,
                notificationText.toString()
            )
        }
    }

    companion object {
        suspend fun nextWeekAndMonth(): Pair<Long, Long> {
            val (storedDigestSeconds, firstDayOfWeek) = PlatformStuff.mainPrefs.data
                .map { it.digestSeconds to it.firstDayOfWeek }.first()

            val digestSeconds = storedDigestSeconds ?: (60..(30 * 60)).random()

            if (storedDigestSeconds == null)
                PlatformStuff.mainPrefs.updateData { it.copy(digestSeconds = digestSeconds) }

            val secondsToAdd = -digestSeconds

            val now = System.currentTimeMillis()

            val cal = Calendar.getInstance()
            if (firstDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY)
                cal.firstDayOfWeek = firstDayOfWeek

            cal[Calendar.DAY_OF_WEEK] = cal.firstDayOfWeek

            cal.setMidnight()

            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
            if (cal.timeInMillis < now)
                cal.add(Calendar.WEEK_OF_YEAR, 1)

            val nextWeek = cal.timeInMillis

            cal.timeInMillis = now
            cal.setMidnight()

            cal[Calendar.DAY_OF_MONTH] = 1
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
            if (cal.timeInMillis < now)
                cal.add(Calendar.MONTH, 1)

            val nextMonth = cal.timeInMillis

            val test = false

            if (BuildKonfig.DEBUG && test) {
                val nextMinute = System.currentTimeMillis() + 60 * 1000
                return nextMinute to nextMinute
            } else
                return nextWeek to nextMonth
        }
    }
}