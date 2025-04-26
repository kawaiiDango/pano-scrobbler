package com.arn.scrobble.work

import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.LastfmPeriod
import com.arn.scrobble.api.listenbrainz.ListenbrainzRanges
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.Stuff.setUserFirstDayOfWeek
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
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
    private val cal by lazy { Calendar.getInstance() }

    override suspend fun doWork(): CommonWorkerResult {
        var error: Throwable? = null
        cal.setUserFirstDayOfWeek()

        if (PlatformStuff.isNotiChannelEnabled(Stuff.CHANNEL_NOTI_DIGEST_WEEKLY)) {
            val coExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                throwable.printStackTrace()
                error = throwable
            }
            val lastfmPeriod = when (digestType) {
                DigestType.DIGEST_DAILY,
                DigestType.DIGEST_WEEKLY,
                    -> LastfmPeriod.WEEK

                DigestType.DIGEST_MONTHLY -> LastfmPeriod.MONTH
            }

            withContext(coExceptionHandler) {
                fetchAndNotify(lastfmPeriod)
                // yearly digest
                if (lastfmPeriod == LastfmPeriod.MONTH && cal[Calendar.MONTH] == Calendar.DECEMBER) {
                    fetchAndNotify(LastfmPeriod.YEAR)
                }
            }

        } else {
            return CommonWorkerResult.Failure("Notifications disabled")
        }

        DigestWork.checkAndSchedule()

        return if (error != null)
            CommonWorkerResult.Failure(error.redactedMessage)
        else
            CommonWorkerResult.Success
    }

    private suspend fun fetchAndNotify(lastfmPeriod: LastfmPeriod) {
        supervisorScope {
            val limit = 3
            val scrobblable = Scrobblables.current.value ?: return@supervisorScope

            val timeLastfmPeriod = TimePeriod(lastfmPeriod).apply {
                tag = when (lastfmPeriod) {
                    LastfmPeriod.WEEK -> ListenbrainzRanges.week.name
                    LastfmPeriod.MONTH -> ListenbrainzRanges.month.name
                    LastfmPeriod.YEAR -> ListenbrainzRanges.year.name
                    else -> null
                }
            }

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
            ).mapNotNull { (type, defered) ->
                val kResult = defered.await()
                val result = kResult.getOrNull() ?: return@mapNotNull null
                if (result.entries.isEmpty()) return@mapNotNull null
                type to result.entries.joinToString { it.name }
            }

            PanoNotifications.notifyDigest(timeLastfmPeriod, textsList)
        }
    }

    companion object {
        suspend fun getScheduleTimes(): Map<DigestType, Long> {
            val storedDigestSeconds = PlatformStuff.mainPrefs.data
                .map { it.digestSeconds }.first()

            val digestSeconds = storedDigestSeconds ?: (60..(30 * 60)).random()

            if (storedDigestSeconds == null)
                PlatformStuff.mainPrefs.updateData { it.copy(digestSeconds = digestSeconds) }

            val secondsToAdd = -digestSeconds

            val timesMap = mutableMapOf<DigestType, Long>()

            val now = System.currentTimeMillis()

            val cal = Calendar.getInstance()
            cal.setUserFirstDayOfWeek()
            cal[Calendar.DAY_OF_WEEK] = cal.firstDayOfWeek

            cal.setMidnight()

            cal.add(Calendar.WEEK_OF_YEAR, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
            if (cal.timeInMillis < now)
                cal.add(Calendar.WEEK_OF_YEAR, 1)

            timesMap[DigestType.DIGEST_WEEKLY] = cal.timeInMillis

            cal.timeInMillis = now
            cal.setMidnight()

            cal[Calendar.DAY_OF_MONTH] = 1
            cal.add(Calendar.MONTH, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
            if (cal.timeInMillis < now)
                cal.add(Calendar.MONTH, 1)

            timesMap[DigestType.DIGEST_MONTHLY] = cal.timeInMillis

            cal.timeInMillis = now
            cal.setMidnight()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.add(Calendar.SECOND, secondsToAdd)
//            cal.add(Calendar.MINUTE, 1)
            if (cal.timeInMillis < now)
                cal.add(Calendar.DAY_OF_YEAR, 1)
            timesMap[DigestType.DIGEST_DAILY] = cal.timeInMillis


            return timesMap
        }

    }
}