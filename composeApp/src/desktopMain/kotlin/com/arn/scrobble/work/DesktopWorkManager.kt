package com.arn.scrobble.work

import co.touchlab.kermit.Logger
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

data class WorkState(
    val id: String,
    val status: CommonWorkerResult? = null,
    val progress: CommonWorkProgress? = null,
)


object DesktopWorkManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val workMap = mutableMapOf<String, Job>()

    private val _workStateFlow = MutableStateFlow<Map<String, WorkState>>(emptyMap())

    fun scheduleWork(
        uniqueName: String,
        delayMillis: Long,
        worker: ((CommonWorkProgress) -> Unit) -> CommonWorker,
        retryPolicy: RetryPolicy = RetryPolicy(),
        replace: Boolean = true,
    ) {
        if (!replace && workMap.containsKey(uniqueName)) {
            return
        }

        workMap[uniqueName]?.cancel()
        val job = scope.launch {
            delay(delayMillis)
            executeWork(uniqueName, worker, retryPolicy)
        }
        workMap[uniqueName] = job
        updateState(uniqueName, WorkState(uniqueName))
    }

    private suspend fun executeWork(
        id: String,
        worker: ((CommonWorkProgress) -> Unit) -> CommonWorker,
        retryPolicy: RetryPolicy,
    ) {
        var attempt = 0
        var backoffDelay = retryPolicy.initialDelayMillis
        var result: CommonWorkerResult

        while (attempt < retryPolicy.maxAttempts) {
            result = try {
                withTimeout(120_000L) {
                    worker {
                        setProgress(id, it)
                    }
                        .doWork().also { updateState(id, WorkState(id, it)) }
                }
                return
            } catch (e: Exception) {
                if (e is CancellationException) {
                    throw e
                }

                if (attempt >= retryPolicy.maxAttempts - 1) {
                    updateState(id, WorkState(id, CommonWorkerResult.Failure(e.redactedMessage)))
                    return
                }
                CommonWorkerResult.Failure(e.redactedMessage)
            }

            updateState(id, WorkState(id, result))
            attempt++
            delay(backoffDelay)
            backoffDelay = (backoffDelay * retryPolicy.backoffFactor).toLong()
        }
    }

    fun getProgress(id: String): Flow<CommonWorkProgress?> {
        return _workStateFlow.map {
            it[id]?.progress
        }
    }

    fun setProgress(id: String, progress: CommonWorkProgress) {
        updateState(id, WorkState(id, progress = progress))
    }

    fun cancelWork(id: String) {
        workMap[id]?.cancel()
        workMap.remove(id)
        updateState(id, WorkState(id, CommonWorkerResult.Failure("Cancelled")))
    }

    private fun updateState(id: String, state: WorkState) {
        Logger.d { "WorkManager: $id - $state" }

        _workStateFlow.update { it + (id to state) }
    }

    fun clearAll() {
        workMap.values.forEach { it.cancel() }
        workMap.clear()
        _workStateFlow.value = emptyMap()
    }
}

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMillis: Long = 10_000,
    val backoffFactor: Double = 2.0,
)