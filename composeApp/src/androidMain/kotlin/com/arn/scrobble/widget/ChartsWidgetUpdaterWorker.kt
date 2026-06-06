package com.arn.scrobble.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.arn.scrobble.BuildKonfig
import com.arn.scrobble.R
import com.arn.scrobble.api.AccountType
import com.arn.scrobble.api.Scrobblables
import com.arn.scrobble.api.lastfm.Album
import com.arn.scrobble.api.lastfm.Artist
import com.arn.scrobble.api.lastfm.ImageSize
import com.arn.scrobble.api.lastfm.LastFmImage
import com.arn.scrobble.api.lastfm.MusicEntry
import com.arn.scrobble.api.lastfm.Track
import com.arn.scrobble.api.lastfm.webp300
import com.arn.scrobble.charts.TimePeriod
import com.arn.scrobble.imageloader.MusicEntryImageReq
import com.arn.scrobble.imageloader.PanoImageLoader
import com.arn.scrobble.imageloader.StarMapper
import com.arn.scrobble.pref.WidgetPrefs
import com.arn.scrobble.utils.AndroidStuff
import com.arn.scrobble.utils.PanoNotifications
import com.arn.scrobble.utils.PlatformStuff
import com.arn.scrobble.utils.Stuff
import com.arn.scrobble.utils.Stuff.setMidnight
import com.arn.scrobble.utils.redactedMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds


class ChartsWidgetUpdaterWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val specificWidgetIds = inputData.getInt(WIDGET_ID, -1)
        .let { if (it != -1) intArrayOf(it) else null }

    private val workName = inputData.getString(WORK_NAME_KEY)!!

    override suspend fun getForegroundInfo() = PanoNotifications.createForegroundInfo(
        applicationContext.getString(R.string.charts)
    )

    // runs in Dispatchers.DEFAULT
    override suspend fun doWork(): Result {
        Stuff.initializeMainPrefsCache()

        // not logged in
        if (Scrobblables.all.isEmpty())
            return Result.failure(
                Data.Builder()
                    .putString("reason", "Not logged in")
                    .build()
            )

        logTimestampToFile("started $workName")

        if (workName == NAME_IMAGE_FETCH) {
            // keep checking for new requests (pendingReqs.size) until debounce of 3 seconds
            var prevReqsSize = 0
            while (true) {
                val currentReqsSize = pendingWidgetItems.size
                if (currentReqsSize == prevReqsSize) {
                    break
                }
                prevReqsSize = currentReqsSize
                delay(DEBOUNCE_S.seconds)
            }
        }

        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)

        val appWidgetIds = specificWidgetIds
            ?: if (workName == NAME_IMAGE_FETCH) {
                val widgetIds = pendingWidgetIds.toSet()
                pendingWidgetIds.removeAll(widgetIds)
                widgetIds.toIntArray()
            } else {
                appWidgetManager.getAppWidgetIds(
                    ComponentName(
                        applicationContext,
                        ChartsWidgetProvider::class.java
                    )
                )
            }

        val widgetPrefs = AndroidStuff.widgetPrefs.data.first()
        val firstDayOfWeek = PlatformStuff.mainPrefs.data.map { it.firstDayOfWeek }.first()

        if (appWidgetIds.isEmpty()) {
            logTimestampToFile("skipped")
            return Result.failure(
                Data.Builder()
                    .putString("reason", "No widgets to update")
                    .build()
            )
        }

        var errorData: Data? = null

        val dataKeyToIds = mutableMapOf<WidgetPrefs.ChartsDataKey, MutableList<Int>>()
        SingletonImageLoader.setSafe { PanoImageLoader.newImageLoader(applicationContext) }

        if (workName == NAME_IMAGE_FETCH) {
            val keyToWidgetItems = pendingWidgetItems.toSet()
            pendingWidgetItems.removeAll(keyToWidgetItems)

            val keysToWidgetItemsToCachedImage = keyToWidgetItems
                .mapNotNull { (key, item) ->
                    val tab = key.tab ?: return@mapNotNull null

                    val cachedImage = cachedImagePath(
                        key.accountType ?: return@mapNotNull null,
                        item.toMusicEntry(tab),
                        allowNetwork = true
                    )

                    Triple(key, item, cachedImage)
                }.groupBy { it.first }

            AndroidStuff.widgetPrefs.updateData {
                val chartsData = it.chartsData.toMutableMap()

                keysToWidgetItemsToCachedImage.forEach { (key, tripleList) ->

                    val m = tripleList.associate { (_, item, cachedImage) ->
                        Pair(item.title, item.subtitle) to cachedImage
                    }

                    val newValues = chartsData[key]?.data
                        ?.map {
                            val k = it.title to it.subtitle
                            if (k in m)
                                it.copy(cachedImage = m[k])
                            else
                                it
                        }

                    if (chartsData[key] != null && newValues != null)
                        chartsData[key] = chartsData[key]!!.copy(data = newValues)
                }

                it.copy(chartsData = chartsData)
            }

            ChartsListUtils.updateWidgets(appWidgetIds)

            return Result.success()
        }

        widgetPrefs.widgets
            .filterKeys { id -> id in appWidgetIds }
            .forEach { (id, pref) ->
                dataKeyToIds.getOrPut(pref.dataKey) { mutableListOf() }.add(id)
            }

        // support a max of n periods
        dataKeyToIds
            .asSequence()
            .take(6)
            .forEach { (dataKey, ids) ->
                val timePeriod = dataKey.period?.toTimePeriod(firstDayOfWeek) ?: return@forEach
                val cal = Calendar.getInstance()
                cal.setMidnight()
                if (firstDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY)
                    cal.firstDayOfWeek = firstDayOfWeek

                val prevTimeLastfmPeriod =
                    if (timePeriod.lastfmPeriod == null) {
                        cal.timeInMillis = timePeriod.start

                        when (dataKey.period) {
                            WidgetPeriod.THIS_WEEK -> cal.add(Calendar.WEEK_OF_YEAR, -1)
                            WidgetPeriod.THIS_MONTH -> cal.add(Calendar.MONTH, -1)
                            WidgetPeriod.THIS_YEAR -> cal.add(Calendar.YEAR, -1)
                            else -> null
                        }?.let {
                            TimePeriod(cal.timeInMillis, timePeriod.start)
                        }
                    } else
                        null

                if (specificWidgetIds == null && widgetPrefs.chartsData[dataKey]?.canFetchAgain != false)
                    return@forEach

                val scrobblable =
                    Scrobblables.all.first { it.userAccount.type == dataKey.accountType }

                val res = scrobblable.getChartsWithStonks(
                    dataKey.tab ?: return@forEach,
                    timePeriod,
                    prevTimeLastfmPeriod,
                    1,
                    limit = 50
                )
                    .onSuccess {
//                            if (dataKey.tab != Stuff.TYPE_ALBUMS && it.entries.isEmpty()) {
//                                noData = true
//                            }
                    }.onFailure {
                        it.printStackTrace()

                        logTimestampToFile("errored " + it.redactedMessage)
                        errorData = Data.Builder()
                            .putString("reason", it.redactedMessage)
                            .build()
                    }
                    .map { pr ->
                        pr.entries.map { entry ->
                            val subtitle = when (entry) {
                                is Album -> entry.artist!!.name
                                is Track -> entry.artist.name
                                else -> null
                            }

                            val imgUrl =
                                if (entry is Album && entry.webp300?.contains(StarMapper.STAR_PATTERN) == false)
                                    entry.webp300
                                else
                                    null

                            val cachedImage = cachedImagePath(
                                scrobblable.userAccount.type,
                                entry,
                                allowNetwork = false
                            )

                            WidgetPrefs.ChartsWidgetListItem(
                                entry.name,
                                subtitle,
                                entry.playcount?.toInt() ?: 0,
                                imgUrl,
                                cachedImage,
                                entry.stonksDelta
                            )
                        }
                    }

                AndroidStuff.widgetPrefs.updateData {
                    val chartsData = it.chartsData.toMutableMap()
                    val fetchTime = System.currentTimeMillis()
                    val chartsDataForKey = chartsData[dataKey]
                    chartsData[dataKey] = WidgetPrefs.ChartsData(
                        data = res.getOrNull() ?: chartsDataForKey?.data.orEmpty(),
                        fetchTime = fetchTime,
                        timePeriodString = timePeriod.name,
                    )

                    it.copy(chartsData = chartsData)
                }

                ChartsListUtils.updateWidgets(ids.toIntArray())
                logTimestampToFile("updated for period $dataKey")
            }

        logTimestampToFile("finished")

        return if (errorData != null)
            Result.failure(errorData)
        else
            Result.success()
    }

    private suspend fun cachedImagePath(
        accountType: AccountType,
        entry: MusicEntry,
        allowNetwork: Boolean
    ): String? {
        if (entry !is Artist) {
            val req = MusicEntryImageReq(
                musicEntry = entry,
                accountType = accountType,
                fetchAlbumInfoIfMissing = true,
                allowNetwork = allowNetwork
            )

            val fetchedImageUrls = PanoImageLoader.resolveImageUrls(req)

            val imageLoader = SingletonImageLoader.get(applicationContext)

            if (allowNetwork && fetchedImageUrls?.mediumImage != null) {
                val request = ImageRequest.Builder(applicationContext)
                    .data(fetchedImageUrls.mediumImage)
                    .memoryCachePolicy(CachePolicy.DISABLED)  // widget doesn't benefit from memory cache
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .allowHardware(false)
                    .build()

                val result = imageLoader.execute(request)
                if (result is ErrorResult && result.throwable !is StarMapper.StarException) {
                    Logger.d("ChartsImageFetcherWorker failed: $req", result.throwable)
                }
            }

            Logger.d { "Image for ${entry.name}: $fetchedImageUrls" }

            val snapshot = fetchedImageUrls?.mediumImage?.let {
                imageLoader.diskCache?.openSnapshot(it)
            }

            return when {
                // snapshot.data is an okio.Path pointing to the actual cache file on disk
                snapshot != null -> snapshot.use { it.data.name }

                fetchedImageUrls != null &&
                        (fetchedImageUrls.mediumImage.isNullOrEmpty() || StarMapper.STAR_PATTERN in fetchedImageUrls.mediumImage)
                    -> "" // mark as "looked up but not found"
                // local paths are not disk cached by coil
                fetchedImageUrls?.mediumImage?.startsWith("content://") == true -> fetchedImageUrls.mediumImage

                else -> null
            }
        }

        return null
    }

    private fun WidgetPrefs.ChartsWidgetListItem.toMusicEntry(type: Int) = when (type) {
        Stuff.TYPE_ARTISTS -> Artist(title)
        Stuff.TYPE_ALBUMS -> Album(
            name = title,
            artist = Artist(subtitle!!),
            image = if (!imageUrl.isNullOrBlank())
                listOf(LastFmImage(ImageSize.extralarge.name, imageUrl))
            else
                emptyList()
        )

        Stuff.TYPE_TRACKS -> Track(
            name = title,
            artist = Artist(subtitle!!),
            album = null
        )

        else -> throw IllegalArgumentException("Invalid type: $type")
    }

    private fun logTimestampToFile(event: String) {
        if (!BuildKonfig.DEBUG) return

        val file = File(PlatformStuff.filesDir, "timestamps.txt")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.appendText(
            "$event: ${
                DateFormat.getDateTimeInstance().format(System.currentTimeMillis())
            }\n"
        )
    }

    companion object {

        private const val NAME_ONE_TIME = "charts_widget_updater_one_time"
        private const val NAME_PERIODIC = "charts_widget_updater_periodic"
        private const val NAME_IMAGE_FETCH = "charts_widget_updater_image_fetch"
        private const val WORK_NAME_KEY = "uniqueWorkName"
        private const val WIDGET_ID = "widget_id"
        private const val DEBOUNCE_S = 3L
        private val pendingWidgetItems =
            ConcurrentHashMap.newKeySet<Pair<WidgetPrefs.ChartsDataKey, WidgetPrefs.ChartsWidgetListItem>>()
        private val pendingWidgetIds = ConcurrentHashMap.newKeySet<Int>()

        fun schedule(context: Context, specificWidgetId: Int?) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME_IMAGE_FETCH)

            val constraintsBuilder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)

            if (specificWidgetId != null) {
                val inputData = Data.Builder()
                    .putString(WORK_NAME_KEY, NAME_ONE_TIME)
                    .putInt(WIDGET_ID, specificWidgetId)
                    .build()

                val oneTimeWork = OneTimeWorkRequestBuilder<ChartsWidgetUpdaterWorker>()
                    .setConstraints(constraintsBuilder.build())
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(inputData)
                    .addTag(NAME_ONE_TIME)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    NAME_ONE_TIME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeWork
                )

                Logger.i { "scheduling ${ChartsWidgetUpdaterWorker::class.java.simpleName} runImmediately" }
            }

            val inputData = Data.Builder()
                .putString(WORK_NAME_KEY, NAME_PERIODIC)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<ChartsWidgetUpdaterWorker>(
                Stuff.CHARTS_WIDGET_REFRESH_INTERVAL_HOURS.toLong(),
                TimeUnit.HOURS
            )
                .setConstraints(
                    constraintsBuilder
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setInputData(inputData)
                .addTag(NAME_PERIODIC)
                .setInitialDelay(
                    Stuff.CHARTS_WIDGET_REFRESH_INTERVAL_HOURS.toLong(),
                    TimeUnit.HOURS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )

            Logger.i { "scheduling ${ChartsWidgetUpdaterWorker::class.java.simpleName}" }
        }

        fun scheduleImageFetch(
            context: Context,
            widgetId: Int,
            dataKey: WidgetPrefs.ChartsDataKey,
            item: WidgetPrefs.ChartsWidgetListItem
        ) {
            pendingWidgetItems.add(dataKey to item)
            pendingWidgetIds.add(widgetId)

            val constraintsBuilder = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)

            val inputData = Data.Builder()
                .putString(WORK_NAME_KEY, NAME_IMAGE_FETCH)
                .build()

            val oneTimeWork = OneTimeWorkRequestBuilder<ChartsWidgetUpdaterWorker>()
                .setConstraints(constraintsBuilder.build())
                .setInitialDelay(DEBOUNCE_S, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag(NAME_IMAGE_FETCH)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                NAME_IMAGE_FETCH,
                ExistingWorkPolicy.KEEP,
                oneTimeWork
            )

            Logger.i { "scheduling $NAME_IMAGE_FETCH" }
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME_PERIODIC)
            Logger.i { "cancelled ${ChartsWidgetUpdaterWorker::class.java.simpleName}" }
        }
    }
}