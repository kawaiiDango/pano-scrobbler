package com.arn.scrobble.work

import kotlinx.coroutines.flow.Flow

data class CommonWorkProgress(
    val message: String,
    val progress: Float,
    val state: CommonWorkState = CommonWorkState.RUNNING,
) {
    val isError: Boolean
        get() = state == CommonWorkState.FAILED
}

enum class CommonWorkState {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    OTHER;

    val isFinished: Boolean
        get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
}

sealed interface CommonWorkerResult {
    data object Success : CommonWorkerResult
    data object Retry : CommonWorkerResult
    data class Failure(val errorMsg: String) : CommonWorkerResult
}

interface CommonWork {
    fun checkAndSchedule(force: Boolean = false)
    fun getProgress(): Flow<CommonWorkProgress>
    fun state(): CommonWorkState?
    fun cancel()
}

interface CommonWorker {
    suspend fun doWork(): CommonWorkerResult
    val setProgress: suspend (CommonWorkProgress) -> Unit
}

fun getWorker(
    name: String,
    setCommonProgress: suspend (CommonWorkProgress) -> Unit,
): CommonWorker {
    return when (name) {
        PendingScrobblesWorker.NAME -> PendingScrobblesWorker(setCommonProgress)
        IndexerWorker.NAME_FULL_INDEX -> IndexerWorker(true, setCommonProgress)
        IndexerWorker.NAME_DELTA_INDEX -> IndexerWorker(false, setCommonProgress)
        DigestType.DIGEST_DAILY.name -> DigestWorker(DigestType.DIGEST_DAILY, setCommonProgress)
        DigestType.DIGEST_WEEKLY.name -> DigestWorker(DigestType.DIGEST_WEEKLY, setCommonProgress)
        DigestType.DIGEST_MONTHLY.name -> DigestWorker(DigestType.DIGEST_MONTHLY, setCommonProgress)
        UpdaterWorker.NAME -> UpdaterWorker(setCommonProgress)
        else -> throw IllegalArgumentException("Unknown work name")
    }
}