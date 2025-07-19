package com.arn.scrobble.work

import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblable
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.ScrobbleEvent
import com.arn.scrobble.api.ScrobbleIgnored
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.ApiException
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.db.AccountBitmaskConverter
import com.arn.scrobble.db.PanoDb
import com.arn.scrobble.db.PendingScrobble
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import pano_scrobbler.composeapp.generated.resources.Res
import pano_scrobbler.composeapp.generated.resources.pending_batch
import pano_scrobbler.composeapp.generated.resources.submitting_loves

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
                message = getString(Res.string.pending_batch),
                progress = 0.5f,
            )
        )

        if (entries.isNotEmpty()) {
            val scrobbleResults = mutableMapOf<Scrobblable, Result<ScrobbleIgnored>>()
            // if an error occurs, there will be only one result

            Scrobblables.all.forEach {
                val filteredData by lazy { filterForService(it, entries) }
                if (filteredData.isNotEmpty()) {
                    val result = it.scrobble(filteredData.map { it.scrobbleData })

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

            val idsAll = entries.map { it._id }.toSet()
            val idsToDelete = mutableSetOf<Int>()

            entries.forEach { pendingScrobble ->
                val services = pendingScrobble.services -
                        scrobbleResults.mapNotNull { (scrobblable, result) ->
                            val err = result.exceptionOrNull() as? ApiException

                            if (err?.code == 6 ||
                                err?.code == 7 ||
                                result.isSuccess
                            ) {
                                scrobblable.userAccount.type
                            } else
                                null
                        }

                if (services.isEmpty())
                    idsToDelete += pendingScrobble._id
                else if (services != pendingScrobble.services) {
                    val newPendingScrobble = pendingScrobble.copy(services = services)
                    dao.update(newPendingScrobble)
                }
            }

            if (!MOCK && idsToDelete.isNotEmpty())
                dao.delete(idsToDelete.toList())

            (idsAll - idsToDelete)
                .takeIf { it.isNotEmpty() }
                ?.let {
                    val lastFailedTimestamp = System.currentTimeMillis()
                    val lastFailedReason = scrobbleResults.values.firstOrNull { it.isFailure }
                        ?.exceptionOrNull()?.redactedMessage?.take(100)

                    dao.logFailure(
                        it.toList(),
                        lastFailedTimestamp,
                        lastFailedReason
                    )
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
                    message = getString(Res.string.submitting_loves, total - submitted),
                    progress = submitted.toFloat() / total,
                )
            )

            val loveResults = mutableMapOf<Scrobblable, Result<ScrobbleIgnored>>()

            Scrobblables.all.forEach {
                val shouldSubmit by lazy { filterOneForService(it, entry) }
                if (shouldSubmit) {
                    val track = Track(
                        name = entry.scrobbleData.track,
                        artist = Artist(entry.scrobbleData.artist),
                        album = entry.scrobbleData.album?.ifEmpty { null }?.let { Album(it) }
                    )

                    val result = it.loveOrUnlove(track, entry.event == ScrobbleEvent.love)

                    if (result.isFailure) {
                        // Rate Limit Exceeded - Too many scrobbles in a short period. Please try again later
//                            if ((result.exceptionOrNull() as? ApiException)?.code in arrayOf(29, 9))
//                                return false
                        failsMap[it.userAccount.type] = failsMap[it.userAccount.type]!! + 1
                    }

                    loveResults[it] = result
                }
            }

            val services = entry.services -
                    loveResults.mapNotNull { (scrobblable, result) ->
                        if (result.isSuccess)
                            scrobblable.userAccount.type
                        else
                            null
                    }

            if (services.isEmpty() && !MOCK)
                dao.delete(entry)
            else {
                val lastFailedTimestamp = System.currentTimeMillis()
                val lastFailedReason = loveResults.values.firstOrNull { it.isFailure }
                    ?.exceptionOrNull()?.redactedMessage?.take(100)

                val newPendingLove = entry.copy(
                    services = services,
                    lastFailedTimestamp = lastFailedTimestamp,
                    lastFailedReason = lastFailedReason
                )

                dao.update(newPendingLove)
            }

            delay(DELAY)
        }
        return true
    }

    private fun filterOneForService(scrobblable: Scrobblable, pl: PendingScrobble): Boolean {
        if (failsMap[scrobblable.userAccount.type]!! > MAX_FAILURES_PER_SERVICE)
            return false

        return scrobblable.userAccount.type in pl.services
    }

    private fun filterForService(
        scrobblable: Scrobblable,
        pendingScrobbles: List<PendingScrobble>,
    ): List<PendingScrobble> {
        if (failsMap[scrobblable.userAccount.type]!! > MAX_FAILURES_PER_SERVICE)
            return emptyList()

        return pendingScrobbles.filter { pendingScrobble ->
            scrobblable.userAccount.type in pendingScrobble.services
        }
    }

    private suspend fun deleteForLoggedOutServices() {
        val loggedOutAccounts =
            AccountType.entries.toSet() - Scrobblables.all.map { it.userAccount.type }

        val loggedOutAccountsBitset =
            AccountBitmaskConverter.accountTypesToBitMask(loggedOutAccounts)

        if (loggedOutAccounts.isNotEmpty())
            dao.removeLoggedOutAccounts(loggedOutAccountsBitset)

        dao.deleteEmptyAccounts()
    }


    companion object {
        const val NAME = "pending_scrobbles"
        private val MOCK = PlatformStuff.isDebug && false
        private const val HARD_LIMIT = 2500
        private var BATCH_SIZE = 40 //max 50
        private const val DELAY = 400L
        private const val MAX_FAILURES_PER_SERVICE = 2
    }
}