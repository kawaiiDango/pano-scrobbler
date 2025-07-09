package com.arn.scrobble.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.arn.scrobble.utils.AndroidStuff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

fun mapWorkState(state: WorkInfo.State): CommonWorkState {
    return when (state) {
        WorkInfo.State.RUNNING -> CommonWorkState.RUNNING
        WorkInfo.State.SUCCEEDED -> CommonWorkState.SUCCEEDED
        WorkInfo.State.FAILED -> CommonWorkState.FAILED
        WorkInfo.State.CANCELLED -> CommonWorkState.CANCELLED
        else -> CommonWorkState.OTHER
    }
}

fun dataToProgress(data: Data, state: WorkInfo.State): CommonWorkProgress {
    return CommonWorkProgress(
        message = data.getString(CommonWorkProgress::message.name)
            ?: if (state == WorkInfo.State.SUCCEEDED) "Done" else "",
        progress = data.getFloat(CommonWorkProgress::progress.name, 0f),
        state = mapWorkState(state)
    )
}

abstract class CommonWorkImpl(protected val name: String) : CommonWork {
    final override fun getProgress(): Flow<CommonWorkProgress> {
        return WorkManager.getInstance(AndroidStuff.application)
            .getWorkInfosForUniqueWorkFlow(name)
            .map { it.firstOrNull() }
            .filterNotNull()
            .map { workInfo ->
                if (workInfo.state.isFinished) {
                    dataToProgress(workInfo.outputData, workInfo.state)
                } else {
                    dataToProgress(workInfo.progress, workInfo.state)
                }
            }
    }

    final override fun cancel() {
        WorkManager.getInstance(AndroidStuff.application).cancelUniqueWork(name)
    }
}

class PlatformWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters) {

    val workName = workerParameters.inputData.getString(WORK_NAME_KEY)!!
    val worker = getWorker(workName, ::setProgress)

    override suspend fun getForegroundInfo() = AndroidStuff
        .createForegroundInfoNotification(workName)

    override suspend fun doWork(): Result {
        val result = worker.doWork()
        return when (result) {
            CommonWorkerResult.Success -> Result.success()
            is CommonWorkerResult.Failure -> Result.failure(
                workDataOf(
                    CommonWorkProgress::message.name to result.errorMsg
                )
            )

            CommonWorkerResult.Retry -> Result.retry()
        }
    }

    private suspend fun setProgress(progress: CommonWorkProgress) {
        val data = Data.Builder()
            .putString(CommonWorkProgress::message.name, progress.message)
            .putFloat(CommonWorkProgress::progress.name, progress.progress)
            .putString(CommonWorkProgress::state.name, progress.state.name)
            .build()

        setProgress(data)
    }

    companion object {
        const val WORK_NAME_KEY = "work_name"
    }
}
