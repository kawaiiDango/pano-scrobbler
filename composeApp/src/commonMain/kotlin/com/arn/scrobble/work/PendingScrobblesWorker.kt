package com.arn.scrobble.work

import co.touchlab.kermit.Logger
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEvent
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ScrobbleData
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

class PendingScrobblesWorker(
    override val setProgress: suspend (CommonWorkProgress) -> Unit,
) : CommonWorker {

    private val dao by lazy { PanoDb.db.getPendingScrobblesDao() }

    private val failsMap = AccountType.entries.associateWith { 0 }.toMutableMap()

    override suspend fun doWork(): CommonWorkerResult {
        // do not run if offline, I was unable to infer from the docs whether this constraint is applied to expedited work
        if (!Stuff.isOnline)
            return CommonWorkerResult.Failure("Offline")

        var errored: Boolean

        val exHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            throwable.printStackTrace()
            errored = true
        }

        withContext(Dispatchers.IO + exHandler) {
            errored = !run()
        }

        return if (errored) {
            CommonWorkerResult.Retry
        } else {
            CommonWorkerResult.Success
        }
    }

    private suspend fun run(): Boolean {
        deleteForLoggedOutServices()

        if (!submitLoves())
            return false

        if (!submitScrobbles())
            return false

        return true
    }

    private suspend fun submitScrobbles(): Boolean {
        val entries = dao.allScrobbles(HARD_LIMIT)

        for (chunk in entries.chunked(BATCH_SIZE)) {
            if (!submitScrobbleBatch(chunk))
                return false
        }

        return true
    }

    private suspend fun submitScrobbleBatch(entries: List<PendingScrobble>): Boolean {
        setProgress(
            CommonWorkProgress(
                // todo cannot use compose string resource here on android
                message = "Submitting a batch",
                progress = 0.5f,
            )
        )

        val scrobbleDataToEntry = mutableMapOf<ScrobbleData, PendingScrobble>()
        entries.forEach {
            val sd = ScrobbleData(
                artist = it.artist,
                album = it.album,
                track = it.track,
                albumArtist = it.albumArtist,
                timestamp = it.timestamp,
                duration = it.duration.takeIf { it > 30 * 1000 },
                packageName = it.packageName.ifEmpty { null }
            )
            scrobbleDataToEntry[sd] = it
        }
        if (scrobbleDataToEntry.isNotEmpty()) {
            val scrobbleResults = mutableMapOf<Scrobblable, Result<ScrobbleIgnored>>()
            //if an error occurs, there will be only one result

            Scrobblables.all.value.forEach {
                val filteredData by lazy { filterForService(it, scrobbleDataToEntry) }
                if (filteredData.isNotEmpty()) {
                    val result = it.scrobble(filteredData)

                    if (result.isFailure) {
                        // Rate Limit Exceeded - Too many scrobbles in a short period. Please try again later (29)
                        // Invalid session key - Please re-authenticate (9)
//                            if ((result.exceptionOrNull() as? ApiException)?.code in arrayOf(29, 9))
//                                return false

                        failsMap[it.userAccount.type] = failsMap[it.userAccount.type]!! + 1
                    }

                    scrobbleResults[it] = result
                }
            }

            val idsToDelete = mutableListOf<Int>()
            scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
                var state = pendingScrobble.state

                scrobbleResults.forEach { (scrobblable, result) ->
                    val err = result.exceptionOrNull() as? ApiException

                    if (err?.code == 6 ||
                        err?.code == 7 ||
                        result.isSuccess
                    ) {
                        state = state and (1 shl scrobblable.userAccount.type.ordinal).inv()
                    }
                }

                if (state == 0)
                    idsToDelete += pendingScrobble._id
                else if (state != pendingScrobble.state) {
                    val newPendingScrobble = pendingScrobble.copy(state = state)
                    dao.update(newPendingScrobble)
                }
            }

            if (!MOCK && idsToDelete.isNotEmpty())
                dao.delete(idsToDelete)

            scrobbleResults.forEach { (scrobblable, result) ->
                if (result.isFailure) {
                    Logger.w {
                        "PendingScrobblesWorker: err for " + scrobblable.userAccount.type.ordinal +
                                ": " + result
                    }
                    return false
                }
            }
        }
        return true
    }

    private suspend fun submitLoves(): Boolean {
        val entries = dao.allLoves(100)
        val total = entries.size
        for ((submitted, entry) in entries.withIndex()) {
            setProgress(
                CommonWorkProgress(
                    message = "Submitting loves",
                    progress = submitted.toFloat() / total,
                )
            )

            val results = mutableMapOf<Scrobblable, Boolean>()

            Scrobblables.all.value.forEach {
                val shouldSubmit by lazy { filterOneForService(it, entry) }
                if (shouldSubmit) {
                    val track = Track(
                        name = entry.track,
                        artist = Artist(entry.artist),
                        album = entry.album.ifEmpty { null }?.let { Album(it) }
                    )

                    val result = it.loveOrUnlove(track, entry.event == ScrobbleEvent.love)

                    if (result.isFailure) {
                        // Rate Limit Exceeded - Too many scrobbles in a short period. Please try again later
//                            if ((result.exceptionOrNull() as? ApiException)?.code in arrayOf(29, 9))
//                                return false
                        failsMap[it.userAccount.type] = failsMap[it.userAccount.type]!! + 1
                    }

                    results[it] = result.isSuccess
                }
            }

            var state = entry.state

            results.forEach { (scrobblable, success) ->
                if (success)
                    state = state and (1 shl scrobblable.userAccount.type.ordinal).inv()
            }

            if (state == 0 && !MOCK)
                dao.delete(entry)
            else if (state != entry.state) {
                val newPendingLove = entry.copy(state = state)
                dao.update(newPendingLove)
            }
            delay(DELAY)
        }
        return true
    }

    private fun filterOneForService(scrobblable: Scrobblable, pl: PendingScrobble): Boolean {
        if (failsMap[scrobblable.userAccount.type]!! > MAX_FAILURES_PER_SERVICE)
            return false

        return (pl.state and (1 shl scrobblable.userAccount.type.ordinal)) != 0
    }

    private fun filterForService(
        scrobblable: Scrobblable,
        scrobbleDataToEntry: MutableMap<ScrobbleData, PendingScrobble>,
    ): List<ScrobbleData> {
        val filtered = mutableListOf<ScrobbleData>()

        if (failsMap[scrobblable.userAccount.type]!! > MAX_FAILURES_PER_SERVICE)
            return filtered

        scrobbleDataToEntry.forEach { (scrobbleData, pendingScrobble) ->
            if (pendingScrobble.state and (1 shl scrobblable.userAccount.type.ordinal) != 0)
                filtered += scrobbleData
        }
        return filtered
    }

    private suspend fun deleteForLoggedOutServices() {
        var loggedInServicesBitset = 0
        Scrobblables.all.value.forEach {
            loggedInServicesBitset =
                loggedInServicesBitset or (1 shl it.userAccount.type.ordinal)
        }
        dao.removeLoggedOutAccounts(loggedInServicesBitset)
        dao.deleteStateZero()
    }


    companion object {
        const val NAME = "pending_scrobbles"
        private val MOCK = PlatformStuff.isDebug && false
        private const val HARD_LIMIT = 2500
        private var BATCH_SIZE = 40 //max 50
        private const val DELAY = 400L
        private const val MAX_FAILURES_PER_SERVICE = 3
    }
}