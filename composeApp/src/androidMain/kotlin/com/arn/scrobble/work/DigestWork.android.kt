package com.arn.scrobble.work

import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.utils.AndroidStuff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

actual object DigestWork : CommonWork {
    private const val DIGEST_TAG = "DIGEST"
    private const val LOCALTIME_PREFIX = "LOCALTIME: "
    private const val DAILY_TEST_DIGESTS = false

    actual fun schedule(
        weeklyDigestTime: Long,
        monthlyDigestTime: Long,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val workManager = try {
            WorkManager.getInstance(AndroidStuff.applicationContext)
        } catch (e: IllegalStateException) {
            return
        }

//        if (BuildKonfig.DEBUG && DAILY_TEST_DIGESTS)
//            enqueue(workManager, DigestType.DIGEST_DAILY, System.currentTimeMillis() + 15_000L)

        val weeklyDigestTimeLocal = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(weeklyDigestTime),
            ZoneId.systemDefault()
        )

        val monthlyDigestTimeLocal = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(monthlyDigestTime),
            ZoneId.systemDefault()
        )

        enqueue(workManager, DigestType.DIGEST_WEEKLY, weeklyDigestTimeLocal)
        enqueue(workManager, DigestType.DIGEST_MONTHLY, monthlyDigestTimeLocal)
    }


    private fun enqueue(
        workManager: WorkManager,
        digestType: DigestType,
        scheduleAtLocal: LocalDateTime
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val inputData = workDataOf(
            PlatformWorker.WORK_NAME_KEY to digestType.name
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = OneTimeWorkRequestBuilder<PlatformWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(digestType.name)
            .addTag(DIGEST_TAG)
            .addTag("$LOCALTIME_PREFIX$scheduleAtLocal")
            .apply {
                val delay = Duration.between(
                    ZonedDateTime.now(),
                    scheduleAtLocal.atZone(ZoneId.systemDefault()),
                ).seconds

                if (delay > 0)
                    setInitialDelay(delay, TimeUnit.SECONDS)
            }
            .build()

        workManager.enqueueUniqueWork(digestType.name, ExistingWorkPolicy.REPLACE, work)

        Logger.i { "scheduling ${digestType.name}" }
    }

    fun reschedule(
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val workManager = try {
            WorkManager.getInstance(AndroidStuff.applicationContext)
        } catch (e: IllegalStateException) {
            return
        }

        val workInfos = workManager.getWorkInfosByTag(DIGEST_TAG).get()

        workInfos.forEach { info ->
            val digestType = DigestType.entries.find { it.name in info.tags } ?: return@forEach
            val localTag =
                info.tags.firstOrNull { tag -> tag.startsWith(LOCALTIME_PREFIX) } ?: return@forEach
            val localTime = LocalDateTime.parse(localTag.removePrefix(LOCALTIME_PREFIX))
            enqueue(workManager, digestType, localTime)
        }
    }

    override fun getProgress(): Flow<CommonWorkProgress> {
        throw NotImplementedError("Not implemented")
    }

    override fun state(): Flow<CommonWorkState?> {
        return WorkManager.getInstance(AndroidStuff.applicationContext)
            .getWorkInfosByTagFlow(DIGEST_TAG)
            .map {
                val expectedSize = if (BuildKonfig.DEBUG && DAILY_TEST_DIGESTS) 3 else 2
                if (it.size < expectedSize) {
                    null
                } else
                    it.minOfOrNull {
                        mapWorkState(it.state)
                    }
            }
    }

    override fun cancel() {
        WorkManager.getInstance(AndroidStuff.applicationContext).apply {
            cancelUniqueWork(DigestType.DIGEST_DAILY.name)
            cancelUniqueWork(DigestType.DIGEST_WEEKLY.name)
            cancelUniqueWork(DigestType.DIGEST_MONTHLY.name)
        }
    }
}